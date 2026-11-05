# Next experience — implementation ledger

Baseline: `4348f41446d79e3247db4173f21342b8530e1096`, published 1.0.0 (100).
Branch: `feat/lughzdan-next-experience`.

## Verified source defects
- Six letter-icon navigation destinations; full-history collection at the root.
- 22 choice answers in position one, 16 solved initial orders, 20 aligned pair boards.
- Sequence rule displayed before submission; bitmap sampling divisor not initialized.
- Each action disabled all controls and waited for the complete history observer.

## Evidence policy
Existing Debug emulator screenshots and 92-frame metrics are historical evidence only.
No Release phone baseline has been measured yet. Performance targets are acceptance goals,
not results. CI device measurements must identify emulator, API, build, scenario and iterations.
Published packs remain immutable; editorial changes require new revisions and signed versions.
No release is ready until build, device, restoration, upgrade and signature checks pass.
