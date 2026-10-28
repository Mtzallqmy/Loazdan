# Word search engine

WordSearchGenerator (alias WordsearchGenerator) uses editorial tokenized words, dimensions, seed, filler alphabet and a total placement-attempt budget. Candidates use eight straight directions, enforce bounds and allow overlaps only on equal tokens. Filler is added after placement. The completed grid is rescanned for every target in eight directions. Every occurrence must map to a declared path cell set; an accidental occurrence rejects the result. A palindrome and its reversal count as one physical occurrence.

GenerationResult.Failure reports invalid input, failed placement, exhausted budget or duplicate occurrence without exposing partial output. Success returns the exact grid and declared paths. Generation is deterministic for identical inputs. Budgets default to 1000 and cap at 100000. Search gameplay drafts a path then explicitly verifies it. The generator tests report measured paired p50/p95 and runtime environment without promising universal performance.
