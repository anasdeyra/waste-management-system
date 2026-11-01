package com.ase.wastems.service;

import java.util.List;

/** RFC 4180 writer for the evaluation exports: UTF-8, newline-terminated records. */
public final class CsvWriter {

    private CsvWriter() {}

    /** Null becomes empty; a value containing a comma, quote, CR or LF is quoted with doubled quotes. */
    public static String field(String v) {
        if (v == null) {
            return "";
        }
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\r') >= 0 || v.indexOf('\n') >= 0) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    public static String line(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(field(fields.get(i)));
        }
        return sb.append('\n').toString();
    }
}
