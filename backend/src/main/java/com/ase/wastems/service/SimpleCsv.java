package com.ase.wastems.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small RFC 4180 reader and WKT point parser for the included source snapshots. */
final class SimpleCsv {

    private static final Pattern WKT_POINT =
            Pattern.compile("^\\s*POINT\\s*\\(\\s*(\\S+)\\s+(\\S+)\\s*\\)\\s*$", Pattern.CASE_INSENSITIVE);

    private SimpleCsv() {}

    /** Parses CSV text; the first record is the header. Records are keyed by header name. */
    static List<Map<String, String>> parse(String text) {
        List<List<String>> records = readRecords(text);
        List<Map<String, String>> result = new ArrayList<>();
        if (records.isEmpty()) {
            return result;
        }
        List<String> header = records.get(0);
        for (int i = 1; i < records.size(); i++) {
            List<String> fields = records.get(i);
            if (fields.size() != header.size()) {
                throw new IllegalArgumentException("Malformed CSV record " + i);
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                row.put(header.get(c), fields.get(c));
            }
            result.add(row);
        }
        return result;
    }

    /** Parses {@code POINT (lon lat)} into {@code {lon, lat}}. */
    static double[] parseWktPoint(String wkt) {
        Matcher m = wkt == null ? null : WKT_POINT.matcher(wkt);
        if (m == null || !m.matches()) {
            throw new IllegalArgumentException("Not a WKT point: " + wkt);
        }
        try {
            return new double[] {Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2))};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a WKT point: " + wkt.toUpperCase(Locale.ROOT), e);
        }
    }

    private static List<List<String>> readRecords(String text) {
        List<List<String>> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean any = false;
        int n = text.length();
        for (int i = 0; i < n; i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }
            if (ch == '"') {
                quoted = true;
                any = true;
            } else if (ch == ',') {
                fields.add(field.toString());
                field.setLength(0);
                any = true;
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
                if (any || field.length() > 0) {
                    fields.add(field.toString());
                    records.add(fields);
                }
                fields = new ArrayList<>();
                field.setLength(0);
                any = false;
            } else {
                field.append(ch);
                any = true;
            }
        }
        if (any || field.length() > 0) {
            fields.add(field.toString());
            records.add(fields);
        }
        return records;
    }
}
