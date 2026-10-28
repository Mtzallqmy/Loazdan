# Ordering games

`order` retains stable-ID `items` and an explicit `correctOrder` permutation. Optional `prompt` supplies the constraint. Supported variants are `sentence`, `story`, `procedure`, `chronology`, `proverb`, `poetry`, and `completion`; absent variant retains the original interaction. All use the same reorder, explicit submit, hint, scoring and saved-session behavior. Tests cover wrong submission, reordered session restoration, winning submission and terminal replay for the three literary variants with synthetic labels.

Four debug levels demonstrate sentence, story, procedure and chronology. The sentence prompt explicitly specifies a verb first to avoid valid alternative Arabic word orders. Story and procedure instructions scope answers to the supplied task. Literary variants are implemented and schema-tested but have no approved playable content yet. Synthetic test labels are not represented as verse or proverbs.

For proverb ordering, each item is an intact word or phrase from a verified source and the prompt identifies the intended saying. For poetry, each item is a complete verse from a specified edition and `correctOrder` follows that edition; do not reorder Qur'anic text. Completion orders the supplied continuation cards after a fixed opening in `prompt`; it does not introduce a free-text-answer interaction. Editors must reject ambiguous permutations, supply an explanation of the intended order, and resolve sourceRefs to the edition, locator, rights and review record before release.

All current fixtures are original drafted text, debug only and unapproved. Historical, religious and poetry content remains gated on authoritative sourcing and appropriate review. Local solving does not grant publication approval or competition eligibility.
