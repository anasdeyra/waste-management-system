package com.ase.wastems.traffic;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Enforces that the display-only traffic package is isolated from routing and optimization (D-021). */
class TrafficIsolationTest {

    private static final Path MAIN = Path.of("src/main/java/com/ase/wastems");
    private static final List<String> ALLOWED = List.of("java.", "com.fasterxml.jackson.", "org.springframework.",
            "com.ase.wastems.traffic.", "com.ase.wastems.config.ServiceAreaProperties");

    private static List<Path> javaFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    void nothingOutsideReferencesTraffic() throws IOException {
        for (String pkg : List.of("optimization", "routing", "service", "domain", "repository", "config")) {
            for (Path f : javaFiles(MAIN.resolve(pkg))) {
                assertThat(Files.readString(f)).as(f.toString()).doesNotContain("com.ase.wastems.traffic");
            }
        }
    }

    @Test
    void trafficImportsOnlyAllowedPackages() throws IOException {
        List<Path> files = javaFiles(MAIN.resolve("traffic"));
        assertThat(files.size()).isGreaterThanOrEqualTo(10);
        for (Path f : files) {
            for (String line : Files.readAllLines(f)) {
                if (!line.startsWith("import ")) {
                    continue;
                }
                String name = line.replace("import static ", "").replace("import ", "");
                assertThat(ALLOWED.stream().anyMatch(name::startsWith)).as(f + ": " + line).isTrue();
            }
        }
    }

    @Test
    void onlyTheTrafficEndpointInWebUsesTraffic() throws IOException {
        for (Path f : javaFiles(MAIN.resolve("web"))) {
            boolean allowed = f.getFileName().toString().equals("TrafficController.java")
                    || f.getFileName().toString().equals("TrafficOverlayResponse.java");
            boolean mentions = Files.readString(f).contains("com.ase.wastems.traffic");
            assertThat(mentions).as(f.toString()).isEqualTo(allowed);
        }
    }
}
