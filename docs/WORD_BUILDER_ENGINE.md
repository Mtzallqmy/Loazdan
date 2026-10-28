# Word builder content

`anagram` retains tiles, fixedSlots and acceptedAnswers. Optional prompt and variant identify anagram, missing_letter, word_from_letters and word_from_word. Fixed tokens consume the full tile inventory and reserve zero-based answer positions; the UI must remove their tile instances from the movable tray. The missing-letter fixture uses inventory ت/ا/ب, fixed ب at index zero, and answer بات. Repeated letters require separate instances. Subset extraction permits unused tiles, but only explicit acceptedAnswers are solutions (مكتبة → كتب).

The content validator checks answer inventory, fixed positions, crossword intersections, search paths, matching bijections, ordering permutations, coverage, SHA-256 and stable original choice bytes. Bundle version is 2; schema/rules remain 1 with optional additive fields.

These are original engineering drafts, not human-reviewed or approved. Linguistic review and rights/publication clearance are pending. They live only in debug assets and cannot ship in release. No invented sources, approvals or scholarly review records are present.
