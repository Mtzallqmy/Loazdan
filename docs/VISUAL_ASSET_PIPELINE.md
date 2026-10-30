# Visual asset pipeline

Stage05 fixtures are original geometric drawings created by `scripts/build_visual_fixtures.py`; no downloaded imagery is included. All twelve levels remain `draft`, `fixtureOnly: true`, pending editorial review. They make no scholarly approval claims.

`packages/media/pipeline.py` accepts local PNG/JPEG/WebP raster files only. It bounds bytes before decoding, checks magic against decoder format, checks dimensions and pixel count before allocation, rejects animation, verifies the file, reopens and fully decodes it, applies orientation, and copies pixels into a fresh image to strip metadata. Pillow decompression warnings are fatal. SVG and remote fetching are deliberately outside this raster boundary.

Every ingestion requires a UUID source reference, license, attribution, and explicit permission to derive, redistribute and use offline. A URL or an attribution alone does not grant rights. The original source SHA-256 is retained. A pixel-content hash deduplicates normalized imagery; derivative filenames are hashes. Lossless WebP and a bounded 160×160 thumbnail are emitted with their real byte hashes, byte lengths and dimensions. Outputs must be packaged only after the provenance record has been reviewed.

Run:

```
python -m pip install -r packages/media/requirements.txt
python -m unittest discover -s packages/media/tests -v
python scripts/build_visual_fixtures.py
```

The generator writes `debugdata/visual.jsonl`, `debugassets/media/`, and `debugassets/visual-provenance.json`. The debug bundle assembler owns merging them into the app manifest. Asset paths are relative `media/...`; the runtime loader must resolve them within the verified bundle. The source notice is `debugassets/media/original-rights.txt`. These fixtures are intended for development and do not enable a production publish transition.
