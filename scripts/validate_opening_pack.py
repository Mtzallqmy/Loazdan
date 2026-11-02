#!/usr/bin/env python3
"""Validate the 120-puzzle official opening pack."""
import collections, hashlib, json, pathlib
import jsonschema

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCHEMA = json.loads((ROOT / 'contracts/puzzle.schema.json').read_text(encoding='utf-8'))
VALIDATOR = jsonschema.Draft202012Validator(SCHEMA, format_checker=jsonschema.FormatChecker())

def main():
    pack_file = ROOT / 'packs/opening-pack-120/data/puzzles.jsonl'
    assert pack_file.exists(), "Puzzles file not found"
    
    lines = pack_file.read_text(encoding='utf-8').splitlines()
    assert len(lines) == 120, f"Expected 120 lines, got {len(lines)}"
    
    puzzles = [json.loads(line) for line in lines]
    puzzle_ids = set()
    world_counts = collections.Counter()
    type_counts = collections.Counter()
    
    for idx, p in enumerate(puzzles, 1):
        VALIDATOR.validate(p)
        assert p["schemaVersion"] == 1
        assert p["rulesVersion"] == 1
        assert p["status"] in ("approved", "published")
        assert not p["fixtureOnly"], f"Puzzle {p['puzzleId']} must not be fixtureOnly"
        assert p["puzzleId"] not in puzzle_ids, f"Duplicate puzzle ID: {p['puzzleId']}"
        puzzle_ids.add(p["puzzleId"])
        
        world_counts[p["world"]] += 1
        type_counts[p["type"]] += 1
        
        # Verify source refs
        assert len(p["sourceRefs"]) >= 1, f"Missing sourceRefs in puzzle {idx}"
        assert len(p["explanation"].strip()) > 0, f"Missing explanation in puzzle {idx}"
        
        # Verify type-specific semantics
        t = p["type"]
        v = p["payload"]
        if t == "sequence":
            assert len(v["terms"]) >= 3
            assert isinstance(v["answer"], int)
            assert len(v["rule"].strip()) > 0
        elif t == "text_answer":
            assert len(v["prompt"].strip()) > 0
            assert 1 <= len(v["acceptedAnswers"]) <= 20
        elif t == "choice":
            assert 2 <= len(v["options"]) <= 6
            assert v["correctId"] in [o["id"] for o in v["options"]]
        elif t == "order":
            assert len(v["items"]) >= 2
            assert sorted(v["correctOrder"]) == sorted([o["id"] for o in v["items"]])
        elif t == "matching":
            if "options" in v:
                assert len(v["options"]) == 4
                assert v["correctId"] in [o["id"] for o in v["options"]]
            else:
                assert len(v["left"]) == len(v["right"]) == len(v["pairs"])
        elif t == "anagram":
            assert len(v["tiles"]) >= 2
            assert len(v["acceptedAnswers"]) >= 1

    expected_worlds = {"words": 20, "imagination": 20, "poetry": 20, "rhetoric": 20, "quran": 20, "logic": 20}
    assert dict(world_counts) == expected_worlds, f"World distribution mismatch: {world_counts}"
    
    # Check manifest
    manifest_file = ROOT / 'packs/opening-pack-120/manifest.json'
    manifest = json.loads(manifest_file.read_text(encoding='utf-8'))
    data_bytes = pack_file.read_bytes()
    expected_sha = hashlib.sha256(data_bytes).hexdigest()
    assert manifest["files"][0]["sha256"] == expected_sha
    assert manifest["files"][0]["bytes"] == len(data_bytes)
    assert manifest["downloadBytes"] == sum(f["bytes"] for f in manifest["files"])
    assets = ROOT / 'apps/android/app/src/main/assets'
    for f in manifest['files']:
        data = (assets / f['path']).read_bytes()
        assert len(data) == f['bytes']
        assert hashlib.sha256(data).hexdigest() == f['sha256']
    for p in puzzles:
        for image in p['payload'].get('images', []):
            f = next(f for f in manifest['files'] if f['path'] == image['assetPath'])
            assert f['sha256'] == image['sha256']
    
    print(f"Validation SUCCESS: All 120 opening puzzles verified.")
    print(f"Worlds verified (20 each): {dict(world_counts)}")
    print(f"Types verified: {dict(type_counts)}")

if __name__ == '__main__':
    main()
