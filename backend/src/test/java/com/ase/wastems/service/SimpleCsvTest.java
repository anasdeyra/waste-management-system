package com.ase.wastems.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests of {@link SimpleCsv}. */
class SimpleCsvTest {

    @Test
    void parsesQuotedFieldsWithCommasAndEscapedQuotes() {
        List<Map<String, String>> rows = SimpleCsv.parse("a,b\n\"x, y\",\"he said \"\"hi\"\"\"\n");
        assertEquals(1, rows.size());
        assertEquals("x, y", rows.get(0).get("a"));
        assertEquals("he said \"hi\"", rows.get(0).get("b"));
    }

    @Test
    void crlfParsesTheSame() {
        List<Map<String, String>> rows = SimpleCsv.parse("a,b\r\n\"x, y\",\"he said \"\"hi\"\"\"\r\n");
        assertEquals(1, rows.size());
        assertEquals("x, y", rows.get(0).get("a"));
        assertEquals("he said \"hi\"", rows.get(0).get("b"));
    }

    @Test
    void trailingNewlineAddsNoRecord() {
        assertEquals(2, SimpleCsv.parse("a\n1\n2\n").size());
        assertEquals(2, SimpleCsv.parse("a\n1\n2").size());
    }

    @Test
    void fieldCountMismatchThrows() {
        assertThrows(IllegalArgumentException.class, () -> SimpleCsv.parse("a,b\n1\n"));
    }

    @Test
    void parsesWktPoint() {
        assertArrayEquals(
                new double[] {-74.0104183829902, 40.7255225360961},
                SimpleCsv.parseWktPoint("POINT (-74.0104183829902 40.7255225360961)"));
        assertThrows(IllegalArgumentException.class, () -> SimpleCsv.parseWktPoint("LINESTRING (1 2)"));
    }
}
