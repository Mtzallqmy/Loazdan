"""Check the literary references, reading exercises and bundled Android data."""
from collections import Counter
import json
from pathlib import Path
from urllib.parse import urlsplit


CATALOG = Path(__file__).resolve().parents[1] / "apps/android/feature/games/src/main/assets/diwan/readings.json"


def check(data):
    works = json.loads(data)
    ids = {work["id"] for work in works}
    assert len(works) == len(ids) == 80
    assert Counter(work["id"][0] for work in works) == {"p": 60, "n": 20}
    assert sum(len(work["questions"]) for work in works) == 300
    for work in works:
        ident = work["id"]
        assert all(work[key].strip() for key in ("author", "title", "theme", "era", "note")), ident
        assert work["text"] and all(line.strip() for line in work["text"]), ident
        assert urlsplit(work["source"]).scheme == "https" and urlsplit(work["source"]).hostname, ident
        assert work.get("companion", ident) in ids
        questions = work["questions"]
        assert len({q["prompt"] for q in questions}) == (4 if ident.startswith("p") else 3)
        for q in questions:
            assert q["explanation"].strip() and len(set(q["options"])) == len(q["options"]) == 3
            assert 0 <= q["correct"] < 3


if __name__ == "__main__":
    check(CATALOG.read_bytes())
    print("60 poems, 20 prose selections and 300 questions verified")
