export interface SessionPayload { sessionId:string; puzzleId:string; puzzleRevision:number; rulesVersion:number; outcome:'won'|'lost'|'abandoned'; score:number; wrongSubmissions:number; hintIds:string[]; durationMs:number }
export type SyncOperation = {operationId:string;deviceId:string;deviceSeq:number} & ({kind:'complete_session';payload:SessionPayload}|{kind:'save_word';payload:{vocabularyId:string;saved:boolean;baseVersion:number}}|{kind:'set_preference';payload:{key:'theme'|'sound'|'reducedMotion';value:'light'|'dark'|'system'|'on'|'off';baseVersion:number}});
export interface SyncRequest {operations:SyncOperation[]}
export interface SyncResult {operationId:string;status:'accepted'|'conflict'|'duplicate';serverVersion:number;current?:unknown;code?:string}
export interface AuthVerifier { verify(authorization:string|undefined):Promise<{userId:string}> }
