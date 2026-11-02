#!/usr/bin/env python3
"""
Summarises the PMD reports of the ``metrics`` Maven profile.

Two reports are read:

* the quality report (``backend/target/pmd.xml``): code smells and the methods whose
  cyclomatic complexity reaches the PMD threshold of 10;
* the complexity report (``backend/target/pmd-cc-all/pmd.xml``): the cyclomatic complexity
  (CC) of every method and constructor (``methodReportLevel`` 1), used for the distribution.

PMD's CC counts the decision points of a method plus one (``if``, loops, ``case``, ``catch``,
``&&``, ``||``, ``?:``). Percentiles use the nearest-rank definition.

Usage:
    python3 tools/pmd-summary.py metrics/pmd.xml metrics/pmd-cc-all.xml [--top 10]
"""

from __future__ import annotations

import argparse
import math
import re
import statistics
import sys
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

PREFIX = "com.ase.wastems"
METHOD = re.compile(r"^The (method|constructor) '(?P<sig>.+)' has a cyclomatic complexity of (?P<cc>\d+)\.$")
CLASS = re.compile(
    r"^The (class|enum|record|interface) '(?P<name>.+)' has a total cyclomatic complexity of "
    r"(?P<total>\d+) \(highest (?P<highest>\d+)\)\.$"
)
BUCKETS = [("1", 1, 1), ("2", 2, 2), ("3-4", 3, 4), ("5-9", 5, 9), ("10-19", 10, 19), (">= 20", 20, 10**9)]


def short_package(name: str) -> str:
    if name == PREFIX:
        return "(root)"
    return name.removeprefix(PREFIX + ".")


def relative(path: str) -> str:
    marker = "com/ase/wastems/"
    return path.split(marker, 1)[1] if marker in path else path


def percentile(values: list[int], fraction: float) -> int:
    ordered = sorted(values)
    return ordered[max(0, math.ceil(fraction * len(ordered)) - 1)]


def read_violations(path: Path) -> tuple[list[dict], str]:
    root = ET.parse(path).getroot()
    namespace = root.tag[: root.tag.index("}") + 1] if root.tag.startswith("{") else ""
    items = []
    for file_el in root.findall(f"{namespace}file"):
        for v in file_el.findall(f"{namespace}violation"):
            items.append(
                {
                    "file": relative(file_el.get("name", "")),
                    "rule": v.get("rule", ""),
                    "ruleset": v.get("ruleset", ""),
                    "package": v.get("package", ""),
                    "class": v.get("class", ""),
                    "method": v.get("method"),
                    "message": " ".join((v.text or "").split()),
                }
            )
    return items, namespace


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("quality", help="PMD quality report (pmd.xml)")
    parser.add_argument("cc_all", help="PMD full complexity report (pmd-cc-all.xml)")
    parser.add_argument("--top", type=int, default=10, help="number of rows in the top lists")
    args = parser.parse_args()

    quality, _ = read_violations(Path(args.quality))

    print("## Violations")
    print(f"total: {len(quality)}")
    print(f"files: {len({v['file'] for v in quality})}")

    print()
    print("## Violations by rule")
    by_rule = Counter((v["rule"], v["ruleset"]) for v in quality)
    print(f"{'rule':<36}{'category':<16}{'count':>6}")
    for (rule, ruleset), count in sorted(by_rule.items(), key=lambda kv: (-kv[1], kv[0][0])):
        print(f"{rule:<36}{ruleset:<16}{count:>6}")

    print()
    print("## Violations by package")
    by_package = Counter(short_package(v["package"]) for v in quality)
    print(f"{'package':<16}{'count':>6}")
    for package, count in sorted(by_package.items(), key=lambda kv: (-kv[1], kv[0])):
        print(f"{package:<16}{count:>6}")

    print()
    print(f"## Top {args.top} files")
    by_file = Counter(v["file"] for v in quality)
    print(f"{'file':<56}{'count':>6}")
    for file_name, count in sorted(by_file.items(), key=lambda kv: (-kv[1], kv[0]))[: args.top]:
        print(f"{file_name:<56}{count:>6}")

    print()
    print("## Methods with cyclomatic complexity >= 10")
    hot = []
    for v in quality:
        if v["rule"] != "CyclomaticComplexity" or not v["method"]:
            continue
        match = METHOD.match(v["message"])
        if match:
            hot.append((int(match.group("cc")), f"{v['class']}.{v['method']}"))
    for cc, name in sorted(hot, key=lambda t: (-t[0], t[1])):
        print(f"{cc:>3}  {name}")
    print(f"count: {len(hot)}")

    cc_items, _ = read_violations(Path(args.cc_all))
    methods: list[tuple[int, str, str]] = []
    classes: list[tuple[int, int, str]] = []
    unparsed = []
    for v in cc_items:
        m = METHOD.match(v["message"])
        if m and v["method"]:
            methods.append((int(m.group("cc")), v["package"], f"{v['class']}.{v['method']}"))
            continue
        c = CLASS.match(v["message"])
        if c:
            classes.append((int(c.group("total")), int(c.group("highest")), f"{v['package']}.{v['class']}"))
            continue
        unparsed.append(v["message"])
    if unparsed:
        print()
        print("## Unparsed")
        for message in unparsed:
            print(message)
        return 1

    values = [m[0] for m in methods]
    top = max(methods, key=lambda m: (m[0], m[2]))
    print()
    print("## Cyclomatic complexity distribution (all methods and constructors)")
    print(f"methods: {len(values)}")
    print(f"mean: {statistics.mean(values):.2f}")
    print(f"median: {statistics.median(values):.2f}")
    print(f"p90: {percentile(values, 0.9)}")
    print(f"max: {top[0]} ({top[2]})")
    over = sum(1 for x in values if x >= 10)
    print(f">= 10: {over} ({100 * over / len(values):.2f} %)")
    for label, low, high in BUCKETS:
        count = sum(1 for x in values if low <= x <= high)
        print(f"bucket {label}: {count} ({100 * count / len(values):.2f} %)")

    print()
    print("## Cyclomatic complexity per package")
    groups: dict[str, list[int]] = defaultdict(list)
    for cc, package, _ in methods:
        groups[short_package(package)].append(cc)
    print(f"{'package':<16}{'methods':>8}{'mean':>8}{'median':>8}{'max':>5}{'>= 10':>7}")
    for package in sorted(groups):
        g = groups[package]
        print(
            f"{package:<16}{len(g):>8}{statistics.mean(g):>8.2f}{statistics.median(g):>8.2f}"
            f"{max(g):>5}{sum(1 for x in g if x >= 10):>7}"
        )

    print()
    print(f"## Classes by total cyclomatic complexity (top {args.top})")
    print(f"{'class':<48}{'total':>6}{'highest':>8}")
    for total, highest, name in sorted(classes, key=lambda c: (-c[0], c[2]))[: args.top]:
        print(f"{name.removeprefix(PREFIX + '.'):<48}{total:>6}{highest:>8}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
