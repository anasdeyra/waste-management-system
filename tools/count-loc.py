#!/usr/bin/env python3
"""
Counts the physical lines of code of the prototype.

The counter reports, for every source file group:

* ``sloc``  - source lines of code (non-blank, non-comment);
* ``comments`` - comment-only lines;
* ``blanks`` - blank lines;
* ``total``  - physical lines of the file.

Comment detection is language aware for Java (``//``, ``/* */``, ``/** */``), JavaScript,
TypeScript (``//``, ``/* */``), YAML and Markdown (``#``).

Usage:
    python3 tools/count-loc.py backend/src frontend/src
"""

from __future__ import annotations

import argparse
import json
import sys
from collections import defaultdict
from pathlib import Path

LINE_COMMENT_PREFIXES = {
    ".java": "//",
    ".js": "//",
    ".jsx": "//",
    ".ts": "//",
    ".tsx": "//",
    ".mjs": "//",
    ".cjs": "//",
    ".yml": "#",
    ".yaml": "#",
    ".sh": "#",
    ".md": "#",
}

BLOCK_COMMENT_PAIRS = {
    ".java": ("/*", "*/"),
    ".js": ("/*", "*/"),
    ".jsx": ("/*", "*/"),
    ".ts": ("/*", "*/"),
    ".tsx": ("/*", "*/"),
    ".mjs": ("/*", "*/"),
    ".cjs": ("/*", "*/"),
}


def count_file(path: Path) -> tuple[int, int, int]:
    """Returns ``(sloc, comments, blanks)`` for a single file."""
    suffix = path.suffix.lower()
    line_prefix = LINE_COMMENT_PREFIXES.get(suffix)
    block_pair = BLOCK_COMMENT_PAIRS.get(suffix)

    sloc = comments = blanks = 0
    in_block_comment = False

    try:
        text = path.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return 0, 0, 0

    for raw_line in text.splitlines():
        line = raw_line.strip()

        if not line:
            blanks += 1
            continue

        if in_block_comment:
            comments += 1
            if block_pair and block_pair[1] in line:
                in_block_comment = False
            continue

        if block_pair and line.startswith(block_pair[0]):
            comments += 1
            if block_pair[1] not in line[len(block_pair[0]) :]:
                in_block_comment = True
            continue

        if line_prefix and line.startswith(line_prefix):
            comments += 1
            continue

        sloc += 1

    return sloc, comments, blanks


def collect(roots: list[Path], extensions: set[str], excluded: tuple[str, ...] = ()) -> dict[str, dict[str, int]]:
    """Aggregates the counters per group and per file."""
    groups: dict[str, dict[str, int]] = defaultdict(
        lambda: {"files": 0, "sloc": 0, "comments": 0, "blanks": 0}
    )

    for root in roots:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*")):
            if not path.is_file() or path.suffix.lower() not in extensions:
                continue
            if any(fragment in str(path) for fragment in excluded):
                continue
            group = str(path.parent)
            sloc, comments, blanks = count_file(path)
            entry = groups[group]
            entry["files"] += 1
            entry["sloc"] += sloc
            entry["comments"] += comments
            entry["blanks"] += blanks

    return dict(sorted(groups.items()))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("roots", nargs="+", help="directories to analyse")
    parser.add_argument(
        "--extensions",
        default=".java,.ts,.tsx,.js,.jsx,.css,.yml,.yaml,.xml,.md,.sh,.py,.json",
        help="comma separated list of extensions to count",
    )
    parser.add_argument(
        "--exclude",
        default="",
        help="comma separated list of path fragments to exclude (e.g. components/ui)",
    )
    parser.add_argument("--json", action="store_true", help="print the raw JSON result")
    args = parser.parse_args()

    extensions = {e.strip().lower() for e in args.extensions.split(",") if e.strip()}
    excluded = tuple(e.strip() for e in args.exclude.split(",") if e.strip())
    roots = [Path(root) for root in args.roots]
    groups = collect(roots, extensions, excluded)

    totals = {"files": 0, "sloc": 0, "comments": 0, "blanks": 0}
    for entry in groups.values():
        for key in totals:
            totals[key] += entry[key]

    if args.json:
        print(json.dumps({"groups": groups, "totals": totals}, indent=2))
        return 0

    print(f"{'group':<48}{'files':>7}{'sloc':>9}{'comments':>10}{'blanks':>8}")
    print("-" * 82)
    for group, entry in groups.items():
        print(
            f"{group:<48}{entry['files']:>7}{entry['sloc']:>9}"
            f"{entry['comments']:>10}{entry['blanks']:>8}"
        )
    print("-" * 82)
    print(
        f"{'TOTAL':<48}{totals['files']:>7}{totals['sloc']:>9}"
        f"{totals['comments']:>10}{totals['blanks']:>8}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
