import importlib.util
from pathlib import Path
import tempfile
import unittest
from PIL import Image, PngImagePlugin
import sys
SPEC = importlib.util.spec_from_file_location('media_pipeline', Path(__file__).parents[1] / 'pipeline.py')
p = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = p
SPEC.loader.exec_module(p)
R = p.Rights('f80200a6-40bd-4ca4-9b46-a1046a3de250','PROJECT-ORIGINAL-FIXTURE','Original fixture',True,True,True)

class PipelineTests(unittest.TestCase):
    def test_metadata_removed_and_pixels_deduplicate(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            meta=PngImagePlugin.PngInfo(); meta.add_text('private','remove me')
            im=Image.new('RGB',(320,240),'red')
            im.save(root/'a.png',pnginfo=meta); im.save(root/'b.png')
            a=p.ingest(root/'a.png',root/'out',R); b=p.ingest(root/'b.png',root/'out',R)
            self.assertEqual(a['contentHash'],b['contentHash'])
            self.assertEqual(len(list((root/'out').iterdir())),2)
            with Image.open(a['image']['path']) as result:
                self.assertNotIn('private',result.info)
                self.assertNotIn('exif',result.info)
            self.assertEqual(a['thumbnail']['width'],160)
    def test_limits_magic_corruption_and_rights(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); src=root/'a.png'
            Image.new('RGB',(200,100),'blue').save(src)
            with self.assertRaises(p.MediaError): p.ingest(src,root/'out',R,p.Limits(max_pixels=100))
            with self.assertRaises(p.MediaError): p.ingest(src,root/'out',R,p.Limits(max_bytes=10))
            src.write_bytes(src.read_bytes()[:45])
            with self.assertRaises(p.MediaError): p.ingest(src,root/'out',R)
            src.write_bytes(b'<svg>not raster</svg>')
            with self.assertRaises(p.MediaError): p.ingest(src,root/'out',R)
            with self.assertRaises(p.MediaError): p.Rights(R.source_reference_id,'X','X',True,False,True).validate()
    def test_animation_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp); src=root/'anim.webp'
            Image.new('RGB',(10,10),'red').save(src,save_all=True,append_images=[Image.new('RGB',(10,10),'blue')],format='WEBP')
            with self.assertRaises(p.MediaError): p.ingest(src,root/'out',R)

if __name__ == '__main__': unittest.main()
