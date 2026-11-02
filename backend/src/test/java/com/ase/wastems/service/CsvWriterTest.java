package com.ase.wastems.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class CsvWriterTest {

    @Test
    void quotesWhenNeeded() {
        assertThat(CsvWriter.field("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvWriter.field("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvWriter.field("two\nlines")).isEqualTo("\"two\nlines\"");
        assertThat(CsvWriter.field("plain")).isEqualTo("plain");
        assertThat(CsvWriter.field(null)).isEmpty();
    }

    @Test
    void lineEndsWithNewline() {
        assertThat(CsvWriter.line(Arrays.asList("a", null, "b,c"))).isEqualTo("a,,\"b,c\"\n");
    }
}
