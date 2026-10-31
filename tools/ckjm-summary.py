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


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", help="path to the raw CKJM report")
    parser.add_argument("--top", type=int, default=15, help="number of classes to list")
    args = parser.parse_args()

    classes = parse(Path(args.report))
    if not classes:
        print("No class found in the report.", file=sys.stderr)
        return 1

    packages: dict[str, list[dict]] = defaultdict(list)
    for record in classes:
        packages[package_of(record["class"])].append(record)

    print("Per-package aggregates (mean per class):")
    header = f"{'package':<52}" + "".join(f"{c:>9}" for c in CORE)
    print(header)
    print("-" * len(header))
    for package in sorted(packages):
        records = packages[package]
        row = f"{package:<52}"
        for column in CORE:
            mean = sum(r[column] for r in records) / len(records)
            row += f"{mean:>9.2f}"
        print(row)

    print()
    print(f"Total classes analysed: {len(classes)}")
    print()
    print(f"Top {args.top} classes by WMC (weighted method per class):")
    print(f"{'class':<62}{'WMC':>6}{'DIT':>5}{'NOC':>5}{'CBO':>5}{'RFC':>5}{'LCOM':>6}")
    print("-" * 94)
    for record in sorted(classes, key=lambda r: r["WMC"], reverse=True)[: args.top]:
        print(
            f"{record['class']:<62}{record['WMC']:>6}{record['DIT']:>5}"
            f"{record['NOC']:>5}{record['CBO']:>5}{record['RFC']:>5}{record['LCOM']:>6}"
        )

    return 0


if __name__ == "__main__":
    sys.exit(main())
