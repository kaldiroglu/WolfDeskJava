package dev.kaldiroglu.wolfdesk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What the README's "What WolfDesk is, and what it is not" says: a library with a demo, no
 * network API, no database, and no dependency but the test framework. These tests fail when
 * the code stops matching it.
 */
class WolfDeskScopeTest {

    private static final Path MAIN = Path.of("src/main/java");

    private static List<Path> mainSources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN)) {
            return files.filter(f -> f.toString().endsWith(".java")).sorted().toList();
        }
    }

    @Test
    void theOnlyDependencyIsJUnit() throws IOException {
        var pom = Files.readString(Path.of("pom.xml"));
        var dependencies = pom.substring(pom.lastIndexOf("<dependencies>"), pom.lastIndexOf("</dependencies>"));
        var artifacts = Pattern.compile("<artifactId>([\\w.-]+)</artifactId>").matcher(dependencies)
                .results().map(m -> m.group(1)).toList();
        assertEquals(List.of("junit-jupiter"), artifacts);
    }

    @Test
    void theCodeUsesOnlyItsOwnPackagesAndTheStandardLibrarysIoMathTimeAndUtil() throws IOException {
        var allowed = Pattern.compile("import (static )?(dev\\.kaldiroglu\\.|java\\.(io|math|time|util)\\.).*");
        var others = mainSources().stream()
                .flatMap(f -> lines(f).stream().filter(l -> l.startsWith("import ")).map(l -> f + ": " + l))
                .filter(l -> !allowed.matcher(l.substring(l.indexOf(": ") + 2)).matches())
                .toList();
        assertEquals(List.of(), others);
    }

    @Test
    void theDemoIsTheOnlyProgram() throws IOException {
        var programs = mainSources().stream()
                .filter(f -> lines(f).stream().anyMatch(l -> l.contains("static void main(")))
                .map(f -> MAIN.relativize(f).toString())
                .toList();
        assertEquals(List.of("dev/kaldiroglu/wolfdesk/demo/WolfDeskDemo.java"), programs);
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
