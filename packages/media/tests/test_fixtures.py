"""Cross-file fixture integrity: source evidence, hashes, and arithmetic answers."""
import json
from collections import Counter
from hashlib import sha256
from pathlib import Path
import unittest
from PIL import Image

ROOT=Path(__file__).resolve().parents[3]

class FixtureTests(unittest.TestCase):
    def test_bundle_images_and_provenance(self):
        puzzles=[json.loads(line) for line in (ROOT/'debugdata/visual.jsonl').read_text().splitlines()]
        self.assertEqual(Counter(p['type'] for p in puzzles),dict.fromkeys(['four_pictures','guess_picture','shadow','symbols','visual_association','visual_logic'],2))
        provenance=json.loads((ROOT/'debugassets/visual-provenance.json').read_text())
        indexed={record['assetId']:record for record in provenance['records']}
        for puzzle in puzzles:
            self.assertTrue(puzzle['fixtureOnly']); self.assertEqual(puzzle['status'],'draft')
            for image in puzzle['payload']['images']:
                record=indexed[image['id']]
                self.assertIn(image['sourceRef'],puzzle['sourceRefs'])
                self.assertEqual(record['sourceReferenceId'],image['sourceRef'])
                path=ROOT/'debugassets'/image['assetPath']
                self.assertEqual(sha256(path.read_bytes()).hexdigest(),image['sha256'])
                with Image.open(path) as actual:
                    actual.load(); self.assertEqual(actual.size,(image['width'],image['height']))
                source=ROOT/'debugassets'/record['sourcePath']
                self.assertEqual(sha256(source.read_bytes()).hexdigest(),record['sourceSha256'])
                thumb=record['thumbnail']
                self.assertEqual(sha256((ROOT/'debugassets'/thumb['path']).read_bytes()).hexdigest(),thumb['sha256'])
            if puzzle['type']=='visual_logic':
                from math import prod
                payload=puzzle['payload']; rule=payload['logicRule']
                correct=sum(rule['values']) if rule['kind']=='add' else prod(rule['values'])
                self.assertEqual(rule['optionValues'][payload['correctImageId']],correct)

if __name__=='__main__': unittest.main()
