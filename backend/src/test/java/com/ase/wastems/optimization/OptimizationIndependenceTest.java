package com.ase.wastems.optimization;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Enforces that the algorithm core depends on the JDK only (D-008, D-046). */
class OptimizationIndependenceTest {

    @Test
    void coreImportsOnlyTheJdk() throws IOException {
        Path dir = Path.of("src/main/java/com/ase/wastems/optimization");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.toString().endsWith(".java")).toList();
        }
        assertThat(files.size()).isGreaterThanOrEqualTo(10);
        for (Path f : files) {
            for (String line : Files.readAllLines(f)) {
                if (!line.startsWith("import ")) {
                    continue;
                }
                assertThat(line).as(f + ": " + line)
                        .doesNotContain("org.springframework", "jakarta.", "javax.persistence", "com.graphhopper");
                if (line.contains("com.ase.wastems.")) {
                    assertThat(line).as(f + ": " + line).contains("com.ase.wastems.optimization.");
                }
            }
        }
    }
}
