import hashlib
import json
import tempfile
from pathlib import Path
import unittest
import sys

SPEC_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SPEC_DIR))
import pipeline as p


def make_source(id='src-1', name='Mufradat', version='1.0', rights='Public Domain',
                permitted_uses=None, rights_evidence='Evidence doc', status='rights_verified'):
    if permitted_uses is None:
        permitted_uses = ['display', 'adapt', 'redistribute']
    return {
        'id': id, 'name': name, 'version': version, 'rights': rights,
        'permittedUses': list(permitted_uses), 'rightsEvidence': rights_evidence,
        'status': status
    }


def make_reference(id='ref-1', source_id='src-1', locator='p. 10',
                   source_version='1.0', excerpt='نص مقتبس موثق'):
    return {
        'id': id, 'sourceId': source_id, 'locator': locator,
        'sourceVersion': source_version, 'excerpt': excerpt,
        'excerptSha256': hashlib.sha256(excerpt.encode('utf-8')).hexdigest()
    }


def make_quran_dataset(id='quran-uthmani-v1'):
    verses = [
        {'surah': 1, 'ayah': 1, 'text': 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ'},
        {'surah': 1, 'ayah': 2, 'text': 'الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ'}
    ]
    return {
        'id': id,
        'verified': True,
        'sha256': p.digest(verses),
        'verses': verses
    }


class KnowledgePipelineTests(unittest.TestCase):
    def setUp(self):
        self.dataset = make_quran_dataset()
        self.datasets = [self.dataset]
        self.reviewers = [
            {'id': 'rev-ling', 'human': True, 'active': True, 'roles': ['linguist']},
            {'id': 'rev-pub', 'human': True, 'active': True, 'roles': ['publisher']},
            {'id': 'rev-sch', 'human': True, 'active': True, 'roles': ['scholar']}
        ]

    def test_canonical_and_digest(self):
        val = {'b': 1, 'a': 2}
        self.assertEqual(p.canonical(val), b'{"a":2,"b":1}')
        self.assertEqual(p.digest(val), hashlib.sha256(b'{"a":2,"b":1}').hexdigest())

    def test_text_and_exact(self):
        self.assertEqual(p.text('hello'), 'hello')
        with self.assertRaises(ValueError):
            p.text('')
        with self.assertRaises(ValueError):
            p.text('   ')
        with self.assertRaises(ValueError):
            p.text(123)
        with self.assertRaises(ValueError):
            p.text('x' * 20001)

        p.exact({'a': 1, 'b': 2}, {'a', 'b'})
        with self.assertRaises(ValueError):
            p.exact({'a': 1}, {'a', 'b'})
        with self.assertRaises(ValueError):
            p.exact({'a': 1, 'c': 3}, {'a', 'b'})

    def test_load_json_duplicate_keys_and_nonfinite(self):
        with tempfile.NamedTemporaryFile('w', encoding='utf-8', delete=False) as f:
            f.write('{"a": 1, "a": 2}')
            path = f.name
        try:
            with self.assertRaises(ValueError):
                p.load(path)
        finally:
            Path(path).unlink(missing_ok=True)

        with tempfile.NamedTemporaryFile('w', encoding='utf-8', delete=False) as f:
            f.write('{"a": NaN}')
            path2 = f.name
        try:
            with self.assertRaises(ValueError):
                p.load(path2)
        finally:
            Path(path2).unlink(missing_ok=True)

    def test_valid_knowledge_root_sense_export(self):
        source = make_source()
        ref = make_reference()
        rec_root = {
            'id': 'rec-root-1',
            'kind': 'root',
            'authorId': 'author-1',
            'referenceIds': ['ref-1'],
            'links': [],
            'data': {'root': 'علم'},
            'exercise': None,
            'fixtureOnly': False
        }
        rec_sense = {
            'id': 'rec-sense-1',
            'kind': 'sense',
            'authorId': 'author-1',
            'referenceIds': ['ref-1'],
            'links': ['rec-root-1'],
            'data': {'headword': 'عَلِمَ', 'meaning': 'أدرك الشيء بحقيقته', 'rootId': 'rec-root-1'},
            'exercise': {
                'type': 'choice',
                'title': 'معنى عَلِمَ',
                'prompt': 'ما معنى كلمة علم؟',
                'options': [{'id': 'opt1', 'text': 'أدرك حقيقته'}, {'id': 'opt2', 'text': 'جهله'}],
                'correctIds': ['opt1'],
                'explanation': 'العلم هو إدراك الشيء'
            },
            'fixtureOnly': False
        }

        # Calculate digests for reviews
        k_temp = KnowledgePipelineTests._create_bundle([source], [ref], [rec_root, rec_sense], [])
        k_pre = p.Knowledge(k_temp, self.reviewers, self.datasets)
        root_digest = k_pre.record_digest('rec-root-1')
        sense_digest = k_pre.record_digest('rec-sense-1')

        reviews = [
            {'recordId': 'rec-root-1', 'digest': root_digest, 'reviewerId': 'rev-ling', 'role': 'linguist', 'decision': 'approved'},
            {'recordId': 'rec-root-1', 'digest': root_digest, 'reviewerId': 'rev-pub', 'role': 'publisher', 'decision': 'approved'},
            {'recordId': 'rec-sense-1', 'digest': sense_digest, 'reviewerId': 'rev-ling', 'role': 'linguist', 'decision': 'approved'},
            {'recordId': 'rec-sense-1', 'digest': sense_digest, 'reviewerId': 'rev-pub', 'role': 'publisher', 'decision': 'approved'}
        ]

        bundle = KnowledgePipelineTests._create_bundle([source], [ref], [rec_root, rec_sense], reviews)
        k = p.Knowledge(bundle, self.reviewers, self.datasets)

        self.assertTrue(k.publishable('rec-sense-1'))
        stats = k.stats()
        self.assertEqual(stats['records'], 2)
        self.assertEqual(stats['publishable'], 2)

        game_id = 'c8b21c4b-610e-436f-870f-1a92e1040854'
        exported = k.export('rec-sense-1', game_id)
        self.assertIn('puzzle', exported)
        self.assertIn('provenance', exported)
        self.assertEqual(exported['puzzle']['gameId'], game_id)
        self.assertEqual(exported['puzzle']['type'], 'choice')
        self.assertEqual(exported['puzzle']['payload']['correctId'], 'opt1')

    def test_quran_verse_and_meaning_validation(self):
        verse_text = 'بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ'
        source = make_source('src-mushaf', 'Mushaf Madinah', '1.0')
        ref = make_reference('ref-q', 'src-mushaf', '1:1', '1.0', verse_text)
        rec_verse = {
            'id': 'rec-v-1',
            'kind': 'quran_verse',
            'authorId': 'author-1',
            'referenceIds': ['ref-q'],
            'links': [],
            'data': {'datasetId': self.dataset['id'], 'surah': 1, 'ayah': 1, 'text': verse_text},
            'exercise': None,
            'fixtureOnly': False
        }
        rec_meaning = {
            'id': 'rec-m-1',
            'kind': 'quran_meaning',
            'authorId': 'author-1',
            'referenceIds': ['ref-q'],
            'links': ['rec-v-1'],
            'data': {'verseId': 'rec-v-1', 'meaning': 'التبرك باسم الله'},
            'exercise': {
                'type': 'order',
                'title': 'ترتيب معاني الآية',
                'prompt': 'رتب معاني الآية الكريمة',
                'options': [{'id': 'm1', 'text': 'البدء بالله'}, {'id': 'm2', 'text': 'الرحمة الواسعة'}],
                'correctIds': ['m1', 'm2'],
                'explanation': 'شرح الترتيب'
            },
            'fixtureOnly': False
        }

        # Scripture cannot have exercises
        rec_verse_bad = dict(rec_verse)
        rec_verse_bad['exercise'] = {
            'type': 'choice', 'title': 'x', 'prompt': 'x',
            'options': [{'id': '1', 'text': 'a'}, {'id': '2', 'text': 'b'}],
            'correctIds': ['1'], 'explanation': 'e'
        }
        bad_bundle = KnowledgePipelineTests._create_bundle([source], [ref], [rec_verse_bad], [])
        with self.assertRaises(ValueError) as ctx:
            p.Knowledge(bad_bundle, self.reviewers, self.datasets)
        self.assertIn('scripture cannot be reordered', str(ctx.exception))

        # Valid quran verse & meaning requires scholar approval for publishable
        bundle_temp = KnowledgePipelineTests._create_bundle([source], [ref], [rec_verse, rec_meaning], [])
        k_temp = p.Knowledge(bundle_temp, self.reviewers, self.datasets)
        v_digest = k_temp.record_digest('rec-v-1')
        m_digest = k_temp.record_digest('rec-m-1')

        # Without scholar approval
        reviews_partial = [
            {'recordId': 'rec-v-1', 'digest': v_digest, 'reviewerId': 'rev-ling', 'role': 'linguist', 'decision': 'approved'},
            {'recordId': 'rec-v-1', 'digest': v_digest, 'reviewerId': 'rev-pub', 'role': 'publisher', 'decision': 'approved'},
            {'recordId': 'rec-m-1', 'digest': m_digest, 'reviewerId': 'rev-ling', 'role': 'linguist', 'decision': 'approved'},
            {'recordId': 'rec-m-1', 'digest': m_digest, 'reviewerId': 'rev-pub', 'role': 'publisher', 'decision': 'approved'}
        ]
        bundle_partial = KnowledgePipelineTests._create_bundle([source], [ref], [rec_verse, rec_meaning], reviews_partial)
        k_partial = p.Knowledge(bundle_partial, self.reviewers, self.datasets)
        self.assertFalse(k_partial.publishable('rec-m-1'))

        # With scholar approval
        reviews_full = reviews_partial + [
            {'recordId': 'rec-v-1', 'digest': v_digest, 'reviewerId': 'rev-sch', 'role': 'scholar', 'decision': 'approved'},
            {'recordId': 'rec-m-1', 'digest': m_digest, 'reviewerId': 'rev-sch', 'role': 'scholar', 'decision': 'approved'}
        ]
        bundle_full = KnowledgePipelineTests._create_bundle([source], [ref], [rec_verse, rec_meaning], reviews_full)
        k_full = p.Knowledge(bundle_full, self.reviewers, self.datasets)
        self.assertTrue(k_full.publishable('rec-m-1'))

    def test_poetry_and_rhetoric_records(self):
        source = make_source('src-poetry', 'Diwan Al-Mutanabbi', '1.0')
        ref = make_reference('ref-p', 'src-poetry', 'p. 50', '1.0', 'الخيل والليل والبيداء تعرفني')
        rec_poetry = {
            'id': 'rec-poet-1',
            'kind': 'poetry',
            'authorId': 'author-1',
            'referenceIds': ['ref-p'],
            'links': [],
            'data': {'poet': 'المتنبي', 'lines': ['الخيل والليل والبيداء تعرفني', 'والسيف والرمح والقرطاس والقلم']},
            'exercise': {
                'type': 'order',
                'title': 'ترتيب شطري بيت المتنبي',
                'prompt': 'رتب الشطرين بالترتيب الصحيح',
                'options': [
                    {'id': 'line1', 'text': 'الخيل والليل والبيداء تعرفني'},
                    {'id': 'line2', 'text': 'والسيف والرمح والقرطاس والقلم'}
                ],
                'correctIds': ['line1', 'line2'],
                'explanation': 'هذا البيت الشهير لأبي الطيب المتنبي'
            },
            'fixtureOnly': False
        }
        rec_rhetoric = {
            'id': 'rec-rhet-1',
            'kind': 'rhetoric',
            'authorId': 'author-1',
            'referenceIds': ['ref-p'],
            'links': ['rec-poet-1'],
            'data': {'device': 'طباق', 'example': 'الليل والنهار', 'explanation': 'الجمع بين الضدين في الكلام'},
            'exercise': None,
            'fixtureOnly': False
        }
        bundle = KnowledgePipelineTests._create_bundle([source], [ref], [rec_poetry, rec_rhetoric], [])
        k = p.Knowledge(bundle, self.reviewers, self.datasets)
        self.assertEqual(len(k.records), 2)

    def test_relation_record_and_sense_links(self):
        source = make_source()
        ref = make_reference()
        rec_root = {
            'id': 'root-1', 'kind': 'root', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': [], 'data': {'root': 'عرف'},
            'exercise': None, 'fixtureOnly': False
        }
        rec_s1 = {
            'id': 's1', 'kind': 'sense', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': ['root-1'],
            'data': {'headword': 'عَرَفَ', 'meaning': 'أدركه بحواسه', 'rootId': 'root-1'},
            'exercise': None, 'fixtureOnly': False
        }
        rec_s2 = {
            'id': 's2', 'kind': 'sense', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': ['root-1'],
            'data': {'headword': 'عَلِمَ', 'meaning': 'تيقنه بالعقل', 'rootId': 'root-1'},
            'exercise': None, 'fixtureOnly': False
        }
        rec_rel = {
            'id': 'rel-1', 'kind': 'relation', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': ['s1', 's2'],
            'data': {'relation': 'difference', 'senseIds': ['s1', 's2']},
            'exercise': None, 'fixtureOnly': False
        }
        bundle = KnowledgePipelineTests._create_bundle([source], [ref], [rec_root, rec_s1, rec_s2, rec_rel], [])
        k = p.Knowledge(bundle, self.reviewers, self.datasets)
        self.assertEqual(k.closure('rel-1'), {'rel-1', 's1', 's2', 'root-1'})

    def test_self_approval_forbidden(self):
        source = make_source()
        ref = make_reference()
        rec = {
            'id': 'rec-1', 'kind': 'root', 'authorId': 'rev-ling',
            'referenceIds': ['ref-1'], 'links': [], 'data': {'root': 'كتب'},
            'exercise': None, 'fixtureOnly': False
        }
        temp_bundle = KnowledgePipelineTests._create_bundle([source], [ref], [rec], [])
        k_temp = p.Knowledge(temp_bundle, self.reviewers, self.datasets)
        rec_digest = k_temp.record_digest('rec-1')
        reviews = [
            {'recordId': 'rec-1', 'digest': rec_digest, 'reviewerId': 'rev-ling', 'role': 'linguist', 'decision': 'approved'}
        ]
        bundle = KnowledgePipelineTests._create_bundle([source], [ref], [rec], reviews)
        with self.assertRaises(ValueError) as ctx:
            p.Knowledge(bundle, self.reviewers, self.datasets)
        self.assertIn('self approval forbidden', str(ctx.exception))

    def test_stale_review_digest(self):
        source = make_source()
        ref = make_reference()
        rec = {
            'id': 'rec-1', 'kind': 'root', 'authorId': 'author-1',
            'referenceIds': ['ref-1'], 'links': [], 'data': {'root': 'كتب'},
            'exercise': None, 'fixtureOnly': False
        }
        reviews = [
            {'recordId': 'rec-1', 'digest': '0' * 64, 'reviewerId': 'rev-ling', 'role': 'linguist', 'decision': 'approved'}
        ]
        bundle = KnowledgePipelineTests._create_bundle([source], [ref], [rec], reviews)
        with self.assertRaises(ValueError) as ctx:
            p.Knowledge(bundle, self.reviewers, self.datasets)
        self.assertIn('stale review digest', str(ctx.exception))

    def test_cycle_detection(self):
        source = make_source()
        ref = make_reference()
        r1 = {
            'id': 'r1', 'kind': 'root', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': ['r2'], 'data': {'root': 'كتب'},
            'exercise': None, 'fixtureOnly': False
        }
        r2 = {
            'id': 'r2', 'kind': 'root', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': ['r1'], 'data': {'root': 'درس'},
            'exercise': None, 'fixtureOnly': False
        }
        bundle = KnowledgePipelineTests._create_bundle([source], [ref], [r1, r2], [])
        with self.assertRaises(ValueError) as ctx:
            p.Knowledge(bundle, self.reviewers, self.datasets)
        self.assertIn('knowledge dependency cycle', str(ctx.exception))

    def test_untrusted_reviewer_and_dataset(self):
        source = make_source()
        ref = make_reference()
        rec = {
            'id': 'r1', 'kind': 'root', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': [], 'data': {'root': 'كتب'},
            'exercise': None, 'fixtureOnly': False
        }
        k_temp = p.Knowledge(KnowledgePipelineTests._create_bundle([source], [ref], [rec], []), self.reviewers, self.datasets)
        d = k_temp.record_digest('r1')
        reviews = [
            {'recordId': 'r1', 'digest': d, 'reviewerId': 'unknown-reviewer', 'role': 'linguist', 'decision': 'approved'}
        ]
        with self.assertRaises(ValueError) as ctx:
            p.Knowledge(KnowledgePipelineTests._create_bundle([source], [ref], [rec], reviews), self.reviewers, self.datasets)
        self.assertIn('untrusted reviewer', str(ctx.exception))

    def test_publisher_overlap_with_scientific_reviewer_rejected(self):
        source = make_source()
        ref = make_reference()
        rec = {
            'id': 'r1', 'kind': 'root', 'authorId': 'a1',
            'referenceIds': ['ref-1'], 'links': [], 'data': {'root': 'كتب'},
            'exercise': None, 'fixtureOnly': False
        }
        dual_reviewer = {'id': 'rev-dual', 'human': True, 'active': True, 'roles': ['linguist', 'publisher']}
        k_temp = p.Knowledge(KnowledgePipelineTests._create_bundle([source], [ref], [rec], []), [dual_reviewer], self.datasets)
        d = k_temp.record_digest('r1')
        reviews = [
            {'recordId': 'r1', 'digest': d, 'reviewerId': 'rev-dual', 'role': 'linguist', 'decision': 'approved'},
            {'recordId': 'r1', 'digest': d, 'reviewerId': 'rev-dual', 'role': 'publisher', 'decision': 'approved'}
        ]
        k = p.Knowledge(KnowledgePipelineTests._create_bundle([source], [ref], [rec], reviews), [dual_reviewer], self.datasets)
        # Publisher cannot overlap with scientific reviewers
        self.assertFalse(k.publishable('r1'))

    @staticmethod
    def _create_bundle(sources, references, records, reviews):
        return {
            'version': 1,
            'sources': sources,
            'references': references,
            'records': records,
            'reviews': reviews
        }


if __name__ == '__main__':
    unittest.main()
