#!/usr/bin/env python3
"""
Summarises the JDepend XML report.

For every package, JDepend reports:

* ``TotalClasses``, ``ConcreteClasses``, ``AbstractClasses`` — class counts;
* ``Ca`` — afferent coupling: how many other packages depend on this one;
* ``Ce`` — efferent coupling: how many other packages this one depends on;
* ``A``  — ratio of abstract classes (a high value means the package is an abstraction);
* ``I``  — instability, i.e. ``Ce / (Ca + Ce)``;
* ``D``  — distance from the main sequence.

The summary prints the package table, the internal dependency graph (``com.ase.wastems.*``
only), the full dependency list of ``optimization`` and the dependency cycles.

Usage:
    python3 tools/jdepend-summary.py metrics/jdepend-report.xml
"""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


PREFIX = "com.ase.wastems"


def short(name: str) -> str:
    """Package name without the project prefix; the root package is ``(root)``."""
    if name == PREFIX:
        return "(root)"
    return name.removeprefix(PREFIX + ".")


def is_internal(name: str) -> bool:
    return name == PREFIX or name.startswith(PREFIX + ".")


def strongly_connected(graph: dict[str, set[str]]) -> list[list[str]]:
    """Tarjan's algorithm; returns the components with more than one package."""
    index: dict[str, int] = {}
    low: dict[str, int] = {}
    stack: list[str] = []
    on_stack: set[str] = set()
    result: list[list[str]] = []
    counter = [0]

    def visit(node: str) -> None:
        index[node] = low[node] = counter[0]
        counter[0] += 1
        stack.append(node)
        on_stack.add(node)
        for target in sorted(graph.get(node, ())):
            if target not in index:
                visit(target)
                low[node] = min(low[node], low[target])
            elif target in on_stack:
                low[node] = min(low[node], index[target])
        if low[node] == index[node]:
            component = []
            while True:
                member = stack.pop()
                on_stack.discard(member)
                component.append(member)
                if member == node:
                    break
            if len(component) > 1:
                result.append(sorted(component))

    for node in sorted(graph):
        if node not in index:
            visit(node)
    return sorted(result)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", help="path to the JDepend XML report")
    args = parser.parse_args()

    root = ET.parse(Path(args.report)).getroot()
    packages = root.find("Packages")
    if packages is None:
        print("No package information in the report.", file=sys.stderr)
        return 1

    rows = []
    depends: dict[str, list[str]] = {}
    for package in packages.findall("Package"):
        stats = package.find("Stats")
        name = package.get("name")
        if stats is None or not is_internal(name):
            continue

        def number(tag: str) -> float:
            try:
                return float(stats.findtext(tag, "0"))
            except (TypeError, ValueError):
                return 0.0

        uses = [p.text.strip() for p in package.findall("DependsUpon/Package") if p.text]
        depends[name] = uses
        rows.append(
            {
                "name": name,
                "classes": int(number("TotalClasses")),
                "abstract": int(number("AbstractClasses")),
                "ca": int(number("Ca")),
                "ce": int(number("Ce")),
                "ce_internal": sum(1 for u in uses if is_internal(u)),
                "a": number("A"),
                "i": number("I"),
                "d": number("D"),
            }
        )
    rows.sort(key=lambda r: short(r["name"]))

    print("## Packages")
    header = (
        f"{'package':<14}{'classes':>8}{'abstract':>9}{'Ca':>5}{'Ce':>5}"
        f"{'Ce internal':>12}{'A':>7}{'I':>7}{'D':>7}"
    )
    print(header)
    print("-" * len(header))
    for row in rows:
        print(
            f"{short(row['name']):<14}{row['classes']:>8}{row['abstract']:>9}{row['ca']:>5}"
            f"{row['ce']:>5}{row['ce_internal']:>12}{row['a']:>7.2f}{row['i']:>7.2f}{row['d']:>7.2f}"
        )
    print(f"total classes: {sum(r['classes'] for r in rows)}")

    print()
    print("## Internal dependencies")
    graph: dict[str, set[str]] = {}
    for name in sorted(depends, key=short):
        targets = sorted({u for u in depends[name] if is_internal(u) and u != name})
        graph[name] = set(targets)
        print(f"{short(name)} -> {', '.join(short(t) for t in sorted(targets, key=short)) or '(none)'}")

    print()
    print("## External dependencies of optimization")
    print(", ".join(sorted(depends.get(f"{PREFIX}.optimization", []))) or "(none)")

    print()
    print("## Cycles")
    components = strongly_connected(graph)
    if not components:
        print("none")
    for component in components:
        names = sorted(component, key=short)
        print("strongly connected packages: " + ", ".join(short(n) for n in names))
    cycles = root.find("Cycles")
    if cycles is not None:
        for entry in cycles.findall("Package"):
            chain = [entry.get("Name")] + [p.text.strip() for p in entry.findall("Package") if p.text]
            internal = [c for c in chain if is_internal(c)]
            if internal:
                print("JDepend cycle path: " + " -> ".join(short(c) for c in internal))
    return 0


if __name__ == "__main__":
    sys.exit(main())
