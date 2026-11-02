BEGIN;
-- Add only implemented rule metadata. Does not publish puzzles or alter existing rows.
INSERT INTO content.game_types(id,engine_version,schema_version,title) VALUES
 ('choice',1,1,'تحدي المفردات'),
 ('crossword',2,1,'الكلمات المتقاطعة'),
 ('wordsearch',2,1,'البحث عن الكلمات'),
 ('anagram',2,1,'تركيب الكلمات'),
 ('matching',2,1,'العلاقات الدلالية'),
 ('order',2,1,'ترتيب الكلمات')
ON CONFLICT (id) DO NOTHING;
COMMIT;
