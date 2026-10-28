# Arabic text processing

Original editorial strings remain unchanged. Comparison applies NFC, trims outside whitespace, rejects bidi embeddings/overrides/isolates and removes zero-width joiners/invisibles. exact_v1 preserves marks; arabic_basic_v1 removes Arabic vocalization and tatweel. Game comparison does not implicitly fold hamza/alif, ya/alif maqsura or ta marbuta/ha. Optional explicit alif and ya folds remain available to callers.

A cell/tile token is one Arabic base letter with following combining marks. Token parsing uses codePoints rather than UTF-16 Char indexing. Multiple bases, whitespace, controls, standalone marks, emoji and Arabic presentation-form ligatures reject. This bounded Arabic model is not full Unicode extended grapheme segmentation. The UI owns IME composition and commits the resulting token.

Coordinates are physical. Across advances toward decreasing column indices; down advances toward increasing rows. The renderer owns RTL labels, shaping and accessibility. The engine does not reverse display strings.
