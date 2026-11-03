package dev.kaldiroglu.wolfdesk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Billing and tenants are layered (chapter 8): each layer is a package, and each layer
 * depends only on the layer beneath it (p. 120). The one exception is the book's own: a
 * service layer starts and ends the database transaction itself, as {@code UserService.Create}
 * does on p. 123. These tests read every main source file's imports of WolfDesk packages.
 */
class LayeredArchitectureTest {

    private static final Path MAIN = Path.of("src/main/java");
    private static final String ROOT = "dev.kaldiroglu.wolfdesk.";
    private static final Pattern IMPORT = Pattern.compile("^import (?:static )?dev\\.kaldiroglu\\.wolfdesk\\.([a-z.]+)\\.[A-Z]");

    /** Every WolfDesk package that the files in {@code layer} import, other than its own. */
    private static Set<String> dependenciesOf(String layer) throws IOException {
        var dir = MAIN.resolve((ROOT + layer).replace('.', '/'));
        var found = new TreeSet<String>();
        try (Stream<Path> files = Files.list(dir)) {
            for (var file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                for (var line : Files.readAllLines(file)) {
                    var m = IMPORT.matcher(line);
                    if (m.find() && !m.group(1).equals(layer)) found.add(m.group(1));
                }
            }
        }
        return found;
    }

    private static Set<String> packagesUnder(String context) throws IOException {
        var dir = MAIN.resolve((ROOT + context).replace('.', '/'));
        try (Stream<Path> dirs = Files.list(dir)) {
            return new TreeSet<>(dirs.filter(Files::isDirectory)
                    .map(d -> context + "." + d.getFileName()).toList());
        }
    }

    @Test
    void billingHasThreeLayersAndEachDependsOnlyOnTheOneBeneathIt() throws IOException {
        var layers = Map.of(
                "billing.presentation", Set.of("billing.businesslogic", "ticketing.publishedlanguage"),
                "billing.businesslogic", Set.of("billing.dataaccess"),
                "billing.dataaccess", Set.<String>of());
        assertEquals(new TreeSet<>(layers.keySet()), packagesUnder("billing"));
        for (var layer : layers.entrySet()) {
            assertEquals(new TreeSet<>(layer.getValue()), dependenciesOf(layer.getKey()), layer.getKey());
        }
    }

    @Test
    void tenantsHasThreeLayersAndEachDependsOnlyOnTheOneBeneathIt() throws IOException {
        var layers = Map.of(
                "tenants.service", Set.of("tenants.businesslogic", "tenants.dataaccess"),
                "tenants.businesslogic", Set.of("tenants.dataaccess"),
                "tenants.dataaccess", Set.<String>of());
        assertEquals(new TreeSet<>(layers.keySet()), packagesUnder("tenants"));
        for (var layer : layers.entrySet()) {
            assertEquals(new TreeSet<>(layer.getValue()), dependenciesOf(layer.getKey()), layer.getKey());
        }
    }

    @Test
    void tenantsServiceLayerUsesTheDatabaseOnlyForTheTransaction() throws IOException {
        var dir = MAIN.resolve("dev/kaldiroglu/wolfdesk/tenants/service");
        var calls = new TreeSet<String>();
        var call = Pattern.compile("\\bdb\\.(\\w+)\\(");
        try (Stream<Path> files = Files.list(dir)) {
            for (var file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                call.matcher(Files.readString(file)).results().forEach(m -> calls.add(m.group(1)));
            }
        }
        assertEquals(new TreeSet<>(List.of("commit", "rollback", "startTransaction")), calls);
    }
}
