#!/usr/bin/env python3
"""
Summarises the JDepend XML report.

For every package, JDepend reports:

* ``TotalClasses``, ``ConcreteClasses``, ``AbstractClasses`` — class counts;
* ``Ca`` — afferent coupling: how many other packages depend on this one;
* ``Ce`` — efferent coupling: how many other packages this one depends on;
* ``A``  — ratio of abstract classes (a high value means the package is an abstraction);
* ``I``  — instability, i.e. ``Ce / (Ca + Ce)``;
* ``D``  — dependency ratio.

Usage:
    python3 tools/jdepend-summary.py metrics/jdepend-report.xml
"""

from __future__ import annotations

import argparse
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


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
    for package in packages.findall("Package"):
        stats = package.find("Stats")
        if stats is None:
            continue

        def number(tag: str, default: str = "0") -> float:
            value = stats.findtext(tag, default)
            try:
                return float(value)
            except (TypeError, ValueError):
                return 0.0

        rows.append(
            {
                "name": package.get("name"),
                "classes": int(number("TotalClasses")),
                "concrete": int(number("ConcreteClasses")),
                "abstract": int(number("AbstractClasses")),
                "ca": int(number("Ca")),
                "ce": int(number("Ce")),
                "instability": number("I"),
            }
        )

    rows.sort(key=lambda r: r["ce"], reverse=True)

    header = (
        f"{'package':<44}{'classes':>8}{'concrete':>9}{'abstract':>9}"
        f"{'Ca':>5}{'Ce':>5}{'instability':>14}"
    )
    print(header)
    print("-" * len(header))
    for row in rows:
        print(
            f"{row['name']:<44}{row['classes']:>8}{row['concrete']:>9}"
            f"{row['abstract']:>9}{row['ca']:>5}{row['ce']:>5}{row['instability']:>14.2f}"
        )

    return 0


if __name__ == "__main__":
    sys.exit(main())
