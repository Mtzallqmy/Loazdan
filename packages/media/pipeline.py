"""Bounded, offline raster ingestion. No remote fetching or implicit rights grants."""
from dataclasses import dataclass, asdict
from hashlib import sha256
from io import BytesIO
from pathlib import Path
import warnings
from PIL import Image, ImageOps, UnidentifiedImageError


class MediaError(ValueError):
    pass


@dataclass(frozen=True)
class Rights:
    source_reference_id: str
    license_code: str
    attribution: str
    can_derive: bool
    can_redistribute: bool
    can_use_offline: bool

    def validate(self):
        from uuid import UUID
        try:
            UUID(self.source_reference_id)
        except (ValueError, TypeError, AttributeError) as exc:
            raise MediaError('source_reference_id must be a UUID') from exc
        if not self.license_code.strip() or not self.attribution.strip():
            raise MediaError('Explicit license and attribution required')
        if not all(x is True for x in (self.can_derive, self.can_redistribute, self.can_use_offline)):
            raise MediaError('Derivative, redistribution and offline rights must all be explicit')


@dataclass(frozen=True)
class Limits:
    max_bytes: int = 12 * 1024 * 1024
    max_width: int = 4096
    max_height: int = 4096
    max_pixels: int = 12_000_000


def _magic(data):
    if data.startswith(b'\x89PNG\r\n\x1a\n'): return 'PNG'
    if data.startswith(b'\xff\xd8\xff'): return 'JPEG'
    if data[:4] == b'RIFF' and data[8:12] == b'WEBP': return 'WEBP'
    raise MediaError('Only PNG, JPEG and WebP raster magic is accepted')


def ingest(source: Path, output_dir: Path, rights: Rights, limits=Limits()):
    """Verify then fully decode; normalize pixels and persist hash-addressed derivatives."""
    rights.validate()
    # Bounded read also covers files growing between stat and read.
    with Path(source).open('rb') as stream:
        data = stream.read(limits.max_bytes + 1)
    if not data or len(data) > limits.max_bytes:
        raise MediaError('Input byte limit exceeded or empty image')
    expected = _magic(data)
    try:
        with warnings.catch_warnings():
            warnings.simplefilter('error', Image.DecompressionBombWarning)
            with Image.open(BytesIO(data)) as probe:
                if probe.format != expected:
                    raise MediaError('Format and magic mismatch')
                w, h = probe.size
                if w < 1 or h < 1 or w > limits.max_width or h > limits.max_height or w*h > limits.max_pixels:
                    raise MediaError('Image dimensions exceed limits')
                if getattr(probe, 'n_frames', 1) != 1:
                    raise MediaError('Animated/multipage images are unsupported')
                probe.verify()
            with Image.open(BytesIO(data)) as decoded:
                decoded.load()  # verify() alone is insufficient for damaged compressed data
                normalized = ImageOps.exif_transpose(decoded).convert('RGBA')
                # Fresh image drops EXIF, ICC, text, comments and other source metadata.
                clean = Image.new('RGBA', normalized.size)
                clean.paste(normalized)
    except (UnidentifiedImageError, OSError, SyntaxError, Image.DecompressionBombError, Image.DecompressionBombWarning) as exc:
        raise MediaError('Invalid or unsafe raster image') from exc
    identity = sha256(str(clean.size).encode() + clean.tobytes()).hexdigest()
    output_dir = Path(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    variants = {}
    for name, size in (('image', None), ('thumbnail', (160, 160))):
        image = clean.copy()
        if size: image.thumbnail(size, Image.Resampling.LANCZOS)
        path = output_dir / f'{identity}{".thumb" if size else ""}.webp'
        encoded = BytesIO()
        image.save(encoded, 'WEBP', lossless=True, exact=True, method=6)
        raw = encoded.getvalue()
        if not path.exists() or path.read_bytes() != raw: path.write_bytes(raw)
        variants[name] = {'path': str(path), 'sha256': sha256(raw).hexdigest(), 'bytes': len(raw), 'width': image.width, 'height': image.height, 'mediaType': 'image/webp'}
    return {'contentHash': identity, 'sourceSha256': sha256(data).hexdigest(), 'rights': asdict(rights), **variants}
