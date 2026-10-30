"""Offline knowledge ingestion. No network, generated scripture, or implicit approval."""
import hashlib
import json
import re
import uuid
from pathlib import Path

KINDS = {'root','sense','relation','poetry','rhetoric','quran_verse','quran_meaning'}
FIELDS = {
 'root': {'root'}, 'sense': {'headword','meaning','rootId'},
 'relation': {'relation','senseIds'}, 'poetry': {'poet','lines'},
 'rhetoric': {'device','example','explanation'},
 'quran_verse': {'datasetId','surah','ayah','text'},
 'quran_meaning': {'verseId','meaning'},
}

def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',',':'), allow_nan=False).encode('utf-8')

def digest(value):
    return hashlib.sha256(canonical(value)).hexdigest()

def text(value):
    if not isinstance(value,str) or not value.strip() or len(value)>20000:
        raise ValueError('expected nonempty bounded text')
    return value

def exact(obj, keys):
    if not isinstance(obj,dict) or set(obj)!=set(keys):
        raise ValueError('unexpected or missing fields: '+str(keys))

def indexed(items):
    if not isinstance(items,list): raise ValueError('expected list')
    out={}
    for item in items:
        ident=text(item.get('id'))
        if ident in out: raise ValueError('duplicate id: '+ident)
        out[ident]=item
    return out

def load(path):
    def pairs(values):
        out={}
        for k,v in values:
            if k in out: raise ValueError('duplicate JSON key: '+k)
            out[k]=v
        return out
    return json.loads(Path(path).read_text(encoding='utf-8'), object_pairs_hook=pairs,
                      parse_constant=lambda x: (_ for _ in ()).throw(ValueError('nonfinite JSON')))

class Knowledge:
    def __init__(self, bundle, trusted_reviewers, trusted_datasets):
        exact(bundle, {'version','sources','references','records','reviews'})
        if type(bundle['version']) is not int or bundle['version']!=1: raise ValueError('unsupported version')
        self.sources=indexed(bundle['sources']); self.references=indexed(bundle['references'])
        self.records=indexed(bundle['records']); self.reviews=bundle['reviews']
        self.reviewers=indexed(trusted_reviewers); self.datasets=indexed(trusted_datasets)
        self._digests={}
        for s in self.sources.values():
            exact(s, {'id','name','version','rights','permittedUses','rightsEvidence','status'})
            for k in ['name','version','rights','rightsEvidence']: text(s[k])
            if s['status'] not in {'candidate','rights_verified','withdrawn','blocked'}: raise ValueError('source status')
            if not isinstance(s['permittedUses'],list) or any(x not in {'display','adapt','redistribute'} for x in s['permittedUses']): raise ValueError('permitted uses')
        for r in self.references.values():
            exact(r, {'id','sourceId','locator','sourceVersion','excerpt','excerptSha256'})
            s=self.sources[r['sourceId']]; text(r['locator']); text(r['excerpt'])
            if r['sourceVersion']!=s['version']: raise ValueError('reference source version mismatch')
            if r['excerptSha256']!=hashlib.sha256(r['excerpt'].encode('utf-8')).hexdigest(): raise ValueError('excerpt digest mismatch')
        for r in self.records.values(): self.validate_record(r)
        for ident in self.records: self.record_digest(ident)
        if not isinstance(self.reviews,list): raise ValueError('reviews must be list')
        for review in self.reviews:
            exact(review, {'recordId','digest','reviewerId','role','decision'})
            if review['recordId'] not in self.records or review['role'] not in {'linguist','scholar','publisher'} or review['decision'] not in {'approved','rejected'}: raise ValueError('invalid review')
            reviewer=self.reviewers.get(review['reviewerId'])
            if not reviewer or reviewer.get('human') is not True or reviewer.get('active') is not True or review['role'] not in reviewer.get('roles',[]): raise ValueError('untrusted reviewer')
            if review['reviewerId']==self.records[review['recordId']]['authorId']: raise ValueError('self approval forbidden')
            if review['digest']!=self.record_digest(review['recordId']): raise ValueError('stale review digest')

    def validate_record(self,r):
        exact(r, {'id','kind','authorId','referenceIds','links','data','exercise','fixtureOnly'})
        kind=r['kind']
        if kind not in KINDS: raise ValueError('unknown kind')
        text(r['authorId']); exact(r['data'],FIELDS[kind])
        if type(r['fixtureOnly']) is not bool: raise ValueError('fixture flag')
        for key in ['referenceIds','links']:
            if not isinstance(r[key],list) or len(set(r[key]))!=len(r[key]): raise ValueError('unique list required')
        if not r['referenceIds'] or any(x not in self.references for x in r['referenceIds']): raise ValueError('missing source reference')
        if any(x not in self.records or x==r['id'] for x in r['links']): raise ValueError('invalid dependency')
        d=r['data']
        for k,v in d.items():
            if k in {'surah','ayah'}:
                if type(v) is not int or v<1: raise ValueError('numbering')
            elif k in {'lines','senseIds'}:
                if not isinstance(v,list) or not 2<=len(v)<=20: raise ValueError('list bounds')
                for x in v: text(x)
            else: text(v)
        def dependency(key,kind):
            target=self.records.get(d[key])
            if not target or target['kind']!=kind or d[key] not in r['links']: raise ValueError('typed dependency missing')
        if kind=='sense': dependency('rootId','root')
        if kind=='relation':
            if d['relation'] not in {'synonym','antonym','difference'}: raise ValueError('relation type')
            if len(set(d['senseIds']))!=len(d['senseIds']): raise ValueError('duplicate relation members')
            for x in d['senseIds']:
                if x not in r['links'] or self.records[x]['kind']!='sense': raise ValueError('relation sense link')
        if kind=='quran_meaning': dependency('verseId','quran_verse')
        if kind=='quran_verse': self.verify_verse(r)
        if r['exercise'] is not None: self.validate_exercise(r)

    def verify_verse(self,r):
        d=r['data']; dataset=self.datasets.get(d['datasetId'])
        if not dataset or dataset.get('verified') is not True: raise ValueError('untrusted Quran dataset')
        exact(dataset, {'id','verified','sha256','verses'})
        if digest(dataset['verses'])!=dataset['sha256']: raise ValueError('Quran dataset digest mismatch')
        verses=dataset['verses']; seen=set(); last=(0,0)
        for verse in verses:
            exact(verse,{'surah','ayah','text'})
            s,a=verse['surah'],verse['ayah']
            if type(s) is not int or type(a) is not int or not 1<=s<=114 or a<1 or (s,a) in seen or (s,a)<=last: raise ValueError('dataset numbering')
            if (s==last[0] and a!=last[1]+1) or (s!=last[0] and a!=1): raise ValueError('dataset numbering gap')
            text(verse['text']); seen.add((s,a));last=(s,a)
        expected=[v['text'] for v in verses if (v['surah'],v['ayah'])==(d['surah'],d['ayah'])]
        if expected!=[d['text']]: raise ValueError('Quran exact text/number mismatch')
        if not any(self.references[x]['excerpt']==d['text'] for x in r['referenceIds']): raise ValueError('Quran exact excerpt required')

    def validate_exercise(self,r):
        e=r['exercise']; exact(e,{'type','title','prompt','options','correctIds','explanation'})
        for key in ['title','prompt','explanation']: text(e[key])
        if e['type'] not in {'choice','order'}: raise ValueError('export type')
        if not isinstance(e['options'],list) or not 2<=len(e['options'])<=(6 if e['type']=='choice' else 20): raise ValueError('option bounds')
        ids=[]
        for o in e['options']:
            exact(o,{'id','text'});text(o['id']);text(o['text']);ids.append(o['id'])
        if len(set(ids))!=len(ids) or not isinstance(e['correctIds'],list): raise ValueError('option IDs')
        if e['type']=='choice' and (len(e['correctIds'])!=1 or e['correctIds'][0] not in ids): raise ValueError('choice answer')
        if e['type']=='order' and (len(e['correctIds'])!=len(ids) or set(e['correctIds'])!=set(ids)): raise ValueError('order answer')
        if r['kind']=='quran_verse': raise ValueError('scripture cannot be reordered or turned into distractors')

    def record_digest(self,ident, visiting=None):
        if ident in self._digests: return self._digests[ident]
        visiting=set() if visiting is None else set(visiting)
        if ident in visiting: raise ValueError('knowledge dependency cycle')
        visiting.add(ident); r=self.records[ident]
        refs=[{'reference':self.references[x],'source':self.sources[self.references[x]['sourceId']]} for x in sorted(r['referenceIds'])]
        value={'record':r,'references':refs,'dependencies':{x:self.record_digest(x,visiting) for x in sorted(r['links'])}}
        if r['kind']=='quran_verse': value['datasetSha256']=self.datasets[r['data']['datasetId']]['sha256']
        self._digests[ident]=digest(value);return self._digests[ident]

    def closure(self,ident):
        out={ident}
        for child in self.records[ident]['links']:out.update(self.closure(child))
        return out

    def publishable(self,ident):
        for node in self.closure(ident):
            r=self.records[node]
            if r['fixtureOnly']: return False
            for ref in r['referenceIds']:
                s=self.sources[self.references[ref]['sourceId']]
                if s['status']!='rights_verified' or not {'display','adapt','redistribute'}<=set(s['permittedUses']):return False
            reviews=[v for v in self.reviews if v['recordId']==node]
            if any(v['decision']=='rejected' for v in reviews):return False
            required={'linguist','publisher'}|({'scholar'} if r['kind'] in {'quran_verse','quran_meaning','poetry','rhetoric'} else set())
            if not required<={v['role'] for v in reviews if v['decision']=='approved'}:return False
            # Publisher must be independent of the scientific reviewers.
            pubs={v['reviewerId'] for v in reviews if v['role']=='publisher'}
            if pubs & {v['reviewerId'] for v in reviews if v['role']!='publisher'}:return False
        return True

    def stats(self):
        signatures=[digest({'kind':r['kind'],'data':r['data']}) for r in self.records.values()]
        return {'records':len(signatures),'uniqueContents':len(set(signatures)),'duplicateContents':len(signatures)-len(set(signatures)), 'publishable':sum(self.publishable(x) for x in self.records)}

    def export(self, ident, game_id):
        uuid.UUID(game_id)
        if not self.publishable(ident): raise ValueError('record/dependency rights and human review gates incomplete')
        r=self.records[ident];e=r['exercise']
        if e is None:raise ValueError('no reviewed exercise')
        refs=sorted({x for node in self.closure(ident) for x in self.records[node]['referenceIds']})
        payload={'prompt':e['prompt']}
        if e['type']=='choice':payload.update(options=e['options'],correctId=e['correctIds'][0])
        else:payload.update(items=e['options'],correctOrder=e['correctIds'])
        puzzle={'schemaVersion':1,'puzzleId':str(uuid.uuid5(uuid.NAMESPACE_URL,ident+':'+self.record_digest(ident))), 'revision':1,'gameId':game_id,'type':e['type'],'world':{'poetry':'poetry','rhetoric':'rhetoric','quran_meaning':'quran'}.get(r['kind'],'words'),'difficulty':1,'rulesVersion':1,'normalization':'exact_v1','title':e['title'],'status':'approved','fixtureOnly':False,'sourceRefs':refs,'explanation':e['explanation'],'hints':[],'payload':payload}
        return {'puzzle':puzzle,'provenance':{'recordId':ident,'recordDigest':self.record_digest(ident),'puzzleDigest':digest(puzzle),'dependencies':{x:self.record_digest(x) for x in sorted(self.closure(ident))},'referenceIds':refs}}
