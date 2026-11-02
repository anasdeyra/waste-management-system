#!/usr/bin/env python3
"""
Summarises the raw CKJM report.

CKJM-extended prints one line per class with the following columns:

    <class> WMC DIT NOC CBO RFC LCOM Ca Ce NPM LCOM3 LOC DAM MOA MFA CAM IC CBM AMC

This script extracts the classic Chidamber & Kemerer metrics (WMC, DIT, NOC, CBO, RFC,
LCOM, LCOM3) and aggregates them per package.

Usage:
    python3 tools/ckjm-summary.py metrics/ckjm.txt
"""

from __future__ import annotations

import argparse
import sys
from collections import defaultdict
from pathlib import Path

COLUMNS = [
    "WMC",
    "DIT",
    "NOC",
    "CBO",
    "RFC",
    "LCOM",
    "Ca",
    "Ce",
    "NPM",
    "LCOM3",
    "LOC",
    "DAM",
    "MOA",
    "MFA",
    "CAM",
    "IC",
    "CBM",
    "AMC",
]

CORE = ["WMC", "DIT", "NOC", "CBO", "RFC", "LCOM", "LCOM3"]


def parse(report: Path) -> list[dict]:
    """Parses the class lines of a CKJM plain report."""
    classes: list[dict] = []
    for raw in report.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.strip()
        if not line or line.startswith("~"):
            continue
        parts = line.split()
        if len(parts) < len(COLUMNS) + 1:
            continue
        name = parts[0]
        if not name.startswith("com.") and not name.startswith("org."):
            continue
        values = parts[1 : len(COLUMNS) + 1]
        record = {"class": name}
        for column, value in zip(COLUMNS, values):
            try:
                record[column] = int(value)
            except ValueError:
                record[column] = float(value)
        classes.append(record)
    return classes


def package_of(class_name: str) -> str:
    parts = class_name.split(".")
    return ".".join(parts[:-1]) if len(parts) > 1 else class_name


def short(name: str) -> str:
    """Class name without the ``com.ase.wastems.`` prefix."""
    return name.removeprefix("com.ase.wastems.")


def short_package(name: str) -> str:
    return "(root)" if name == "com.ase.wastems" else short(name)


def median(values: list[float]) -> float:
    ordered = sorted(values)
    n = len(ordered)
    mid = n // 2
    return float(ordered[mid]) if n % 2 else (ordered[mid - 1] + ordered[mid]) / 2


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", help="path to the raw CKJM report")
    parser.add_argument("--top", type=int, default=15, help="number of classes to list")
    args = parser.parse_args()

    classes = parse(Path(args.report))
    if not classes:
        print("No class found in the report.", file=sys.stderr)
        return 1

    nested = [r for r in classes if "$" in r["class"]]
    print("## Totals")
    print(f"class files: {len(classes)}")
    print(f"top-level classes: {len(classes) - len(nested)}")
    print(f"nested classes: {len(nested)}")
    print(f"methods (sum of WMC): {sum(r['WMC'] for r in classes)}")
    for column in CORE:
        values = [r[column] for r in classes]
        top = max(classes, key=lambda r: (r[column], r["class"]))
        print(
            f"{column}: mean {sum(values) / len(values):.2f}, median {median(values):.2f}, "
            f"max {max(values)} ({short(top['class'])})"
        )

    print()
    print("## DIT distribution")
    for value in sorted({r["DIT"] for r in classes}):
        print(f"DIT={value}: {sum(1 for r in classes if r['DIT'] == value)}")

    packages: dict[str, list[dict]] = defaultdict(list)
    for record in classes:
        packages[package_of(record["class"])].append(record)

    print()
    print("## Per package (mean per class)")
    header = f"{'package':<18}{'classes':>8}" + "".join(f"{c:>9}" for c in CORE)
    print(header)
    print("-" * len(header))
    for package in sorted(packages, key=short_package):
        records = packages[package]
        row = f"{short_package(package):<18}{len(records):>8}"
        for column in CORE:
            mean = sum(r[column] for r in records) / len(records)
            row += f"{mean:>9.2f}"
        print(row)

    for column in ("WMC", "CBO", "LCOM"):
        print()
        print(f"## Top {args.top} classes by {column}")
        header = f"{'class':<56}" + "".join(f"{c:>8}" for c in CORE)
        print(header)
        print("-" * len(header))
        ranked = sorted(classes, key=lambda r: (-r[column], r["class"]))[: args.top]
        for record in ranked:
            print(f"{short(record['class']):<56}" + "".join(f"{record[c]:>8}" for c in CORE))

    print()
    print("note: WMC = number of methods (CKJM weight 1); records have DIT 2 (java.lang.Record).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
