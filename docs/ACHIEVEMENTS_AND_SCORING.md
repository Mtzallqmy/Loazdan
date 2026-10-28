# Achievements and scoring

Plugins compute deterministic scores from difficulty, wrong submissions and used hints. Storage validates the resulting session through its plugin before writing it.

Progress is keyed by puzzle ID across revisions. Best score and XP are maxima across terminal sessions for that puzzle; completion is sticky. Replays can improve maxima but cannot farm XP by repeating the same score. Attempts count distinct terminal sessions including losses. Sum puzzle maxima for per-game and total XP.

A unique completion receipt keyed by session ID prevents duplicate events from changing attempts, XP or achievements. Session, receipt and progress writes share a Room transaction; no extra reward ledger is needed.

The first win awards `first_win`; each game's first win awards `first_win_<type>`. Achievement primary keys make repeated awards idempotent and retain original timestamps. Version 1 achievements and receipts survive migration unchanged. Migration initializes choice XP from its existing best score.

Future reward policy changes require an explicit migration or versioned policy rather than silent historical recalculation.
