import type {Pool,PoolClient} from 'pg';
import type {SyncOperation,SyncRequest,SyncResult} from '../../../packages/contracts/content.js';
import {digest} from './content.js';
import {ApiError} from './errors.js';
export class SyncRepository {
 constructor(private readonly pool:Pool){}
 private async transaction<T>(userId:string,fn:(client:PoolClient)=>Promise<T>):Promise<T>{
  const c=await this.pool.connect();try{await c.query('BEGIN');await c.query('SET LOCAL ROLE api_sync');await c.query("SELECT set_config('app.user_id',$1,true)",[userId]);await c.query("SET LOCAL statement_timeout='5s'");const result=await fn(c);await c.query('COMMIT');return result;}catch(e){await c.query('ROLLBACK');throw e;}finally{c.release();}
 }
 async push(userId:string,request:SyncRequest){return this.transaction(userId,async c=>{
  await c.query('INSERT INTO ops.user_sync_clock(user_id) VALUES($1) ON CONFLICT DO NOTHING',[userId]);
  let version=Number((await c.query('SELECT version FROM ops.user_sync_clock WHERE user_id=$1 FOR UPDATE',[userId])).rows[0].version);
  const results:SyncResult[]=[];
  for(const op of request.operations){
   if(!(await c.query('SELECT 1 FROM identity.devices WHERE user_id=$1 AND device_id=$2 AND revoked_at IS NULL',[userId,op.deviceId])).rowCount)throw new ApiError(403,'DEVICE_NOT_AUTHORIZED','Unauthorized device');
   const hash=digest(op);const old=(await c.query('SELECT operation_id,payload_sha256,result FROM ops.sync_operations WHERE user_id=$1 AND (operation_id=$2 OR (device_id=$3 AND device_seq=$4))',[userId,op.operationId,op.deviceId,op.deviceSeq])).rows;
   if(old.length){const receipt=old[0];if(old.length!==1||receipt.operation_id!==op.operationId||receipt.payload_sha256!==hash)throw new ApiError(409,'OPERATION_CONFLICT','Operation conflict');results.push(receipt.result as SyncResult);continue;}
   let status:SyncResult['status']='accepted';let currentValue:unknown;let entityType='';let entityId='';let value:unknown;
   if(op.kind==='complete_session'){
    const p=op.payload;const sessionHash=digest(p);const prior=(await c.query('SELECT result_hash FROM play.player_sessions WHERE user_id=$1 AND id=$2',[userId,p.sessionId])).rows[0];
    if(prior){if(prior.result_hash!==sessionHash)throw new ApiError(409,'SESSION_CONFLICT','Session conflict');status='duplicate';}
    else{
     const puzzle=(await c.query("SELECT rules_version FROM content.puzzle_versions WHERE puzzle_id=$1 AND revision=$2 AND status='published'",[p.puzzleId,p.puzzleRevision])).rows[0];if(!puzzle||puzzle.rules_version!==p.rulesVersion)throw new ApiError(400,'INVALID_PUZZLE','Invalid puzzle');
     version++;await c.query('INSERT INTO play.player_sessions(user_id,id,device_id,puzzle_id,puzzle_revision,outcome,score,result_hash) VALUES($1,$2,$3,$4,$5,$6,$7,$8)',[userId,p.sessionId,op.deviceId,p.puzzleId,p.puzzleRevision,p.outcome,p.score,sessionHash]);
     value=(await c.query(`INSERT INTO play.player_progress(user_id,puzzle_id,completed,best_score,attempts,server_version) VALUES($1,$2,$3,$4,1,$5) ON CONFLICT(user_id,puzzle_id) DO UPDATE SET completed=play.player_progress.completed OR EXCLUDED.completed,best_score=GREATEST(play.player_progress.best_score,EXCLUDED.best_score),attempts=play.player_progress.attempts+1,server_version=EXCLUDED.server_version,updated_at=now() RETURNING puzzle_id AS "puzzleId",completed,best_score AS "bestScore",attempts,server_version::double precision AS "serverVersion"`,[userId,p.puzzleId,p.outcome==='won',p.score,version])).rows[0];entityType='progress';entityId=p.puzzleId;
    }
   }else{
    const p=op.payload;const preference=op.kind==='set_preference';const table=preference?'preferences':'saved_words';const key=preference?'key':'vocabulary_id';const val=preference?'value':'saved';const id=preference?(p as Extract<SyncOperation,{kind:'set_preference'}>['payload']).key:(p as Extract<SyncOperation,{kind:'save_word'}>['payload']).vocabularyId;
    const v=preference?(p as Extract<SyncOperation,{kind:'set_preference'}>['payload']).value:(p as Extract<SyncOperation,{kind:'save_word'}>['payload']).saved;
    const current=(await c.query(`SELECT *,server_version::double precision AS "serverVersion" FROM play.${table} WHERE user_id=$1 AND ${key}=$2`,[userId,id])).rows[0];if(Number(current?.server_version??0)!==p.baseVersion){status='conflict';currentValue=current?(preference?{key:id,value:current.value,serverVersion:current.serverVersion}:{vocabularyId:id,saved:current.saved,serverVersion:current.serverVersion}):null;}else{version++;await c.query(`INSERT INTO play.${table}(user_id,${key},${val},server_version) VALUES($1,$2,$3,$4) ON CONFLICT(user_id,${key}) DO UPDATE SET ${val}=EXCLUDED.${val},server_version=EXCLUDED.server_version`,[userId,id,v,version]);entityType=preference?'preference':'saved_word';entityId=id;value=preference?{key:id,value:v,serverVersion:version}:{vocabularyId:id,saved:v,serverVersion:version};}
   }
   if(entityType)await c.query('INSERT INTO ops.sync_changes(user_id,version,ordinal,entity_type,entity_id,value) VALUES($1,$2,0,$3,$4,$5)',[userId,version,entityType,entityId,value]);
   if(op.kind==='complete_session'&&status==='accepted'&&op.payload.outcome==='won'){
    const awards=(await c.query(`SELECT id FROM content.achievements a WHERE a.rule_version=1 AND a.rule='{"kind":"first_win"}'::jsonb AND NOT EXISTS(SELECT 1 FROM play.player_achievements p WHERE p.user_id=$1 AND p.achievement_id=a.id) ORDER BY id`,[userId])).rows;
    for(const award of awards){version++;const achievement=(await c.query('INSERT INTO play.player_achievements(user_id,achievement_id,server_version) VALUES($1,$2,$3) ON CONFLICT DO NOTHING RETURNING achievement_id AS id,server_version::double precision AS "serverVersion",awarded_at AS "awardedAt"',[userId,award.id,version])).rows[0];if(achievement)await c.query("INSERT INTO ops.sync_changes(user_id,version,ordinal,entity_type,entity_id,value) VALUES($1,$2,0,'achievement',$3,$4)",[userId,version,award.id,achievement]);}
   }
   const result:SyncResult={operationId:op.operationId,status,serverVersion:version,...(status==='conflict'?{current:currentValue,code:'VERSION_CONFLICT'}:{})};await c.query('INSERT INTO ops.sync_operations(user_id,operation_id,device_id,device_seq,payload_sha256,kind,result) VALUES($1,$2,$3,$4,$5,$6,$7)',[userId,op.operationId,op.deviceId,op.deviceSeq,hash,op.kind,result]);results.push(result);
  }
  await c.query('UPDATE ops.user_sync_clock SET version=$2 WHERE user_id=$1',[userId,version]);return {results,cursor:String(version)};
 });}
 async pull(userId:string,cursor:number,limit:number){return this.transaction(userId,async c=>{const rows=(await c.query('SELECT version::double precision AS version,ordinal,entity_type AS "entityType",entity_id AS "entityId",value FROM ops.sync_changes WHERE user_id=$1 AND version>$2 ORDER BY version,ordinal LIMIT $3',[userId,cursor,limit+1])).rows;const hasMore=rows.length>limit;const changes=rows.slice(0,limit);return{changes,cursor:String(changes.at(-1)?.version??cursor),hasMore};});}
}
