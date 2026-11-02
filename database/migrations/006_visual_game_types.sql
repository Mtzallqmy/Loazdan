BEGIN;
-- Register implemented engines only. Does not publish puzzles or seed production content.
INSERT INTO content.game_types(id,engine_version,schema_version,title) VALUES
 ('four_pictures',3,1,'أربع صور وكلمة'),
 ('guess_picture',3,1,'خمن الصورة'),
 ('shadow',3,1,'خمن الظل'),
 ('symbols',3,1,'لغز الرموز'),
 ('visual_association',3,1,'اكتشف العلاقة'),
 ('visual_logic',3,1,'المنطق البصري')
ON CONFLICT(id) DO NOTHING;
COMMIT;
