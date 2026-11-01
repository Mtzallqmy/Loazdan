BEGIN;
INSERT INTO content.game_types(id,engine_version,schema_version,title) VALUES
 ('text_answer',1,1,'الكتابة والتعبير العربي'),
 ('sequence',1,1,'المتتاليات والأنماط المنطقية'),
 ('image_guess',1,1,'تخمين الصور')
ON CONFLICT(id) DO NOTHING;
COMMIT;
