#!/usr/bin/env python3
"""
Summarises the cyclomatic complexity of the TypeScript code.

Input is the output of oxlint's ``eslint/complexity`` rule configured with ``max: 0`` (see
``tools/oxlint-complexity.json``) in ``-f unix`` format, which reports every function, named or
anonymous, once:

    src/hooks/use-dashboard.ts:6:8: function `useOperationsDashboard` has a complexity of 5. ...

``async function`` and ``constructor`` lines have the same shape and are counted too.

The vendored shadcn/ui components are excluded when the report is produced; this script fails if
one of them appears. Percentiles use the nearest-rank definition.

Usage:
    python3 tools/ts-complexity-summary.py metrics/ts-complexity.txt [--top 10]
"""

from __future__ import annotations

import argparse
import math
import re
import statistics
import sys
from collections import defaultdict
from pathlib import Path

LINE = re.compile(
    r"^(?P<path>[^:]+):(?P<line>\d+):\d+: (async function|function|constructor)( `(?P<name>[^`]+)`)? "
    r"has a complexity of (?P<cc>\d+)\."
)
BUCKETS = [("1", 1, 1), ("2", 2, 2), ("3-4", 3, 4), ("5-9", 5, 9), ("10-19", 10, 19), (">= 20", 20, 10**9)]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", help="oxlint output in unix format")
    parser.add_argument("--top", type=int, default=10, help="number of functions to list")
    args = parser.parse_args()

    functions = []
    for raw in Path(args.report).read_text(encoding="utf-8", errors="replace").splitlines():
        match = LINE.match(raw)
        if not match and "has a complexity of" in raw:
            print(f"Unparsed complexity line: {raw}", file=sys.stderr)
            return 1
        if match:
            functions.append(
                {
                    "path": match.group("path"),
                    "line": int(match.group("line")),
                    "name": match.group("name") or "(anonymous)",
                    "cc": int(match.group("cc")),
                }
            )
    if not functions:
        print("No complexity line found in the report.", file=sys.stderr)
        return 1
    if any("components/ui" in f["path"] for f in functions):
        print("The vendored components/ui must be excluded from the report.", file=sys.stderr)
        return 1

    functions.sort(key=lambda f: (-f["cc"], f["path"], f["line"], f["name"]))
    values = [f["cc"] for f in functions]
    ordered = sorted(values)
    p90 = ordered[max(0, math.ceil(0.9 * len(ordered)) - 1)]
    top = functions[0]
    over = sum(1 for v in values if v >= 10)

    print("## Functions")
    print(f"functions: {len(functions)}")
    print(f"named: {sum(1 for f in functions if f['name'] != '(anonymous)')}")
    print(f"mean: {statistics.mean(values):.2f}")
    print(f"median: {statistics.median(values):.2f}")
    print(f"p90: {p90}")
    print(f"max: {top['cc']} ({top['path']}:{top['line']} {top['name']})")
    print(f">= 10: {over} ({100 * over / len(values):.2f} %)")
    for label, low, high in BUCKETS:
        count = sum(1 for v in values if low <= v <= high)
        print(f"bucket {label}: {count} ({100 * count / len(values):.2f} %)")

    print()
    print("## Per directory")
    groups: dict[str, list[int]] = defaultdict(list)
    for f in functions:
        groups[f["path"].rsplit("/", 1)[0]].append(f["cc"])
    print(f"{'directory':<36}{'functions':>10}{'mean':>8}{'max':>5}{'>= 10':>7}")
    for directory in sorted(groups):
        g = groups[directory]
        print(
            f"{directory:<36}{len(g):>10}{statistics.mean(g):>8.2f}{max(g):>5}"
            f"{sum(1 for x in g if x >= 10):>7}"
        )

    print()
    print(f"## Top {args.top} functions")
    print(f"{'cc':>3}  location")
    for f in functions[: args.top]:
        print(f"{f['cc']:>3}  {f['path']}:{f['line']} {f['name']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
