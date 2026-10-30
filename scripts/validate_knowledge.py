#!/usr/bin/env python3
"""Validate offline import and optionally export an already reviewed exercise."""
import argparse
import json
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'packages'/'knowledge'))
from pipeline import Knowledge, load

def main():
    p=argparse.ArgumentParser();p.add_argument('bundle');p.add_argument('--reviewers',required=True);p.add_argument('--datasets',required=True);p.add_argument('--export-record');p.add_argument('--game-id')
    args=p.parse_args()
    try:
        k=Knowledge(load(args.bundle),load(args.reviewers),load(args.datasets))
        result=k.export(args.export_record,args.game_id) if args.export_record else {'stats':k.stats(),'digests':{x:k.record_digest(x) for x in k.records}}
        print(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2))
    except (ValueError,KeyError,TypeError,AttributeError) as e:
        print('Knowledge validation failed: '+str(e),file=sys.stderr);return 1
    return 0
if __name__=='__main__':sys.exit(main())
