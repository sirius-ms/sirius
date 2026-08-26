/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package aioverview;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Generates a compact, token-lean Markdown "map" of the codebase for AI agents: one file per Gradle
 * module listing every public/protected type and its public surface (Lombok members included, tagged
 * {@code (lombok)}), plus a root {@code index.md}.
 *
 * <p>Runs a pure JavaParser source parse — no compilation, no classpath — so it is fast and survives
 * a broken build. Output lives under {@code build/ai-overview} (gitignored); the whole task is
 * skipped when no {@code .java} source changed thanks to {@link InputFiles}/{@link OutputDirectory}
 * up-to-date checking. Re-run with {@code ./gradlew aiOverview}.
 */
@CacheableTask
public abstract class AiOverviewTask extends DefaultTask {

    /** All {@code src/main/java} sources across source-bearing modules — drives up-to-date checks. */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    public abstract ConfigurableFileCollection getSources();

    /** Gradle module path (e.g. {@code :chemistry_base}) to its {@code src/main/java} directory. */
    @Internal
    public abstract MapProperty<String, Directory> getModuleRoots();

    /**
     * Declared project→project dependency edges, keyed by module path. Each value entry is
     * {@code "<configuration> <targetModulePath>"} (single space separator), e.g.
     * {@code "api :chemistry_base"}. Only inter-module (project) dependencies — external libraries
     * are intentionally omitted. Drives the generated {@code module-dependencies.md}.
     */
    @Input
    public abstract MapProperty<String, List<String>> getModuleDependencies();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /** Short git revision for the header; {@code @Internal} so a new commit alone never forces a rerun. */
    @Internal
    public abstract Property<String> getRevision();

    /** Class Javadoc detail: {@code summary} (first sentence, default) or {@code full} (whole description). */
    @Input
    public abstract Property<String> getClassDocs();

    /** Method Javadoc detail: {@code none} (default), {@code summary} (first sentence, 200 chars) or {@code full}. */
    @Input
    public abstract Property<String> getMethodDocs();

    @TaskAction
    public void generate() throws IOException {
        Path outDir = getOutputDir().get().getAsFile().toPath();
        Path modulesDir = outDir.resolve("modules");
        deleteRecursively(modulesDir);
        Files.createDirectories(modulesDir);

        ModuleScanner.DocMode classDocs =
                ModuleScanner.DocMode.parse(getClassDocs().getOrElse("summary"), ModuleScanner.DocMode.SUMMARY);
        ModuleScanner.DocMode methodDocs =
                ModuleScanner.DocMode.parse(getMethodDocs().getOrElse("none"), ModuleScanner.DocMode.NONE);
        getLogger().lifecycle("aiOverview: classDocs={}, methodDocs={}", classDocs, methodDocs);

        // Sorted for deterministic output.
        Map<String, Directory> roots = new TreeMap<>(getModuleRoots().get());

        List<String[]> index = new ArrayList<>(); // {modulePath, fileName, typeCount, parseErrors}
        int totalTypes = 0;
        int totalParseErrors = 0;

        for (Map.Entry<String, Directory> entry : roots.entrySet()) {
            String modulePath = entry.getKey();
            Path srcDir = entry.getValue().getAsFile().toPath();
            ModuleScanner.ScanResult scan = ModuleScanner.scan(srcDir, classDocs, methodDocs);

            String sanitized = sanitize(modulePath);
            writeModule(modulesDir, sanitized, modulePath, scan);

            index.add(new String[]{modulePath, sanitized + ".md",
                    String.valueOf(scan.types.size()), String.valueOf(scan.parseErrors)});
            totalTypes += scan.types.size();
            totalParseErrors += scan.parseErrors;
        }

        Files.writeString(outDir.resolve("index.md"),
                renderIndex(index, totalTypes, totalParseErrors), StandardCharsets.UTF_8);

        Files.writeString(outDir.resolve("module-dependencies.md"),
                renderDependencies(new TreeMap<>(getModuleDependencies().get())), StandardCharsets.UTF_8);

        getLogger().lifecycle("aiOverview: {} modules, {} types, {} parse error(s) -> {}",
                roots.size(), totalTypes, totalParseErrors, outDir.resolve("index.md"));
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    /** Split a module map into per-package files once it grows past this many characters (~15K tokens). */
    private static final int SPLIT_THRESHOLD = 60_000;

    private void writeModule(Path modulesDir, String sanitized, String modulePath,
                             ModuleScanner.ScanResult scan) throws IOException {
        Map<String, List<ModuleScanner.TypeInfo>> byPackage = new TreeMap<>();
        for (ModuleScanner.TypeInfo type : scan.types) {
            byPackage.computeIfAbsent(type.packageName, k -> new ArrayList<>()).add(type);
        }

        String header = renderModuleHeader(modulePath, scan);
        StringBuilder full = new StringBuilder(header);
        if (scan.types.isEmpty()) full.append("_No public types._\n");
        byPackage.forEach((pkg, types) -> full.append(renderPackage(pkg, types)));

        // Small enough (or single package) -> one flat file.
        if (full.length() <= SPLIT_THRESHOLD || byPackage.size() <= 1) {
            Files.writeString(modulesDir.resolve(sanitized + ".md"), full.toString(),
                    StandardCharsets.UTF_8);
            return;
        }

        // Large module -> a sub-index plus one file per package.
        Path pkgDir = modulesDir.resolve(sanitized);
        Files.createDirectories(pkgDir);
        StringBuilder subIndex = new StringBuilder(header);
        subIndex.append("_Large module — split per package._\n\n| Package | Types | Map |\n|---|---:|---|\n");
        for (Map.Entry<String, List<ModuleScanner.TypeInfo>> e : byPackage.entrySet()) {
            String label = e.getKey().isEmpty() ? "(default package)" : e.getKey();
            String pkgFile = (e.getKey().isEmpty() ? "default-package" : e.getKey()) + ".md";
            Files.writeString(pkgDir.resolve(pkgFile),
                    "# `" + modulePath + "` — " + label + "\n\n" + renderPackage(e.getKey(), e.getValue()),
                    StandardCharsets.UTF_8);
            subIndex.append("| ").append(label).append(" | ").append(e.getValue().size())
                    .append(" | [map](").append(sanitized).append("/").append(pkgFile).append(") |\n");
        }
        Files.writeString(modulesDir.resolve(sanitized + ".md"), subIndex.toString(),
                StandardCharsets.UTF_8);
    }

    private String renderModuleHeader(String modulePath, ModuleScanner.ScanResult scan) {
        StringBuilder sb = new StringBuilder();
        sb.append("# `").append(modulePath).append("` — code map\n\n");
        sb.append("_Generated by `./gradlew aiOverview` — do not edit. ")
                .append(scan.types.size()).append(" types, ")
                .append(scan.sourceFiles).append(" source files");
        if (scan.parseErrors > 0) {
            sb.append(", ").append(scan.parseErrors).append(" unparsed");
        }
        sb.append("._\n\n");
        sb.append("> Markers: `+` public · `~` protected · `(lombok)` synthesized from Lombok "
                + "annotations · `(static)` · `(abstract)` · `(record)`. Bodies and parameter names "
                + "are omitted.\n\n");
        return sb.toString();
    }

    private String renderPackage(String packageName, List<ModuleScanner.TypeInfo> types) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n## ").append(packageName.isEmpty() ? "(default package)" : packageName).append("\n");
        for (ModuleScanner.TypeInfo type : types) {
            sb.append("\n### ").append(type.displayName)
                    .append("  (").append(type.kind).append(")");
            if (type.extendsClause != null) sb.append(" extends ").append(type.extendsClause);
            if (type.implementsClause != null) sb.append(" implements ").append(type.implementsClause);
            if (type.abstractMarker != null) sb.append(type.abstractMarker);
            if (type.isProtected) sb.append(" (protected)");
            sb.append("\n");
            if (type.javadoc != null) {
                for (String line : type.javadoc.split("\n", -1)) {
                    sb.append("> ").append(line).append("\n");
                }
            }
            if (!type.members.isEmpty()) {
                sb.append("\n");
                for (String member : type.members) {
                    sb.append(member).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String renderIndex(List<String[]> index, int totalTypes, int totalParseErrors) {
        String revision = getRevision().getOrElse("unknown");
        StringBuilder sb = new StringBuilder();
        sb.append("# SIRIUS — AI code overview (generated)\n\n");
        sb.append("_Generated ").append(LocalDate.now()).append(" @ `").append(revision)
                .append("`. Run `./gradlew aiOverview` to refresh. Gitignored — do not commit._\n\n");
        sb.append(index.size()).append(" modules · ").append(totalTypes).append(" public types");
        if (totalParseErrors > 0) sb.append(" · ").append(totalParseErrors).append(" unparsed files");
        sb.append(".\n\n");
        sb.append("Each module's map lists its public/protected types & signatures (Lombok members included). "
                + "Start here, open the module you need, then read source for details.\n\n");
        sb.append("For how the modules depend on each other (which module to change, blast radius of a "
                + "change), see the **[module dependency map](module-dependencies.md)**.\n\n");
        sb.append("| Module | Types | Map |\n|---|---:|---|\n");
        index.sort(Comparator.comparing(row -> row[0]));
        for (String[] row : index) {
            sb.append("| `").append(row[0]).append("` | ").append(row[2])
                    .append(" | [map](modules/").append(row[1]).append(") |\n");
        }
        return sb.toString();
    }

    /**
     * Display order only — <b>not</b> a filter. These standard Gradle configurations (from the
     * {@code java}, {@code java-library} and {@code java-test-fixtures} plugins) are rendered first,
     * in this order. Any other configuration encountered — including custom ones — is still rendered,
     * appended after these under its own real name; nothing is dropped. So new dependencies or new
     * configurations need no change here; edit this list only to reposition a config in the output.
     */
    private static final List<String> CONFIG_ORDER = List.of(
            "api", "implementation", "compileOnly", "compileOnlyApi", "runtimeOnly",
            "annotationProcessor", "testImplementation", "testCompileOnly", "testRuntimeOnly",
            "testAnnotationProcessor", "testFixturesApi", "testFixturesImplementation");

    /**
     * Renders the inter-module dependency map: for every module its declared forward project
     * dependencies (grouped by configuration) and the reverse "used by" set, computed from the
     * forward edges. External libraries are omitted by construction (only {@code project(...)}
     * edges are passed in).
     */
    private String renderDependencies(Map<String, List<String>> deps) {
        String revision = getRevision().getOrElse("unknown");

        // Reverse index: target module -> sorted set of modules declaring a dependency on it.
        Map<String, TreeSet<String>> usedBy = new TreeMap<>();
        for (Map.Entry<String, List<String>> e : deps.entrySet()) {
            for (String edge : e.getValue()) {
                String target = edge.substring(edge.indexOf(' ') + 1);
                usedBy.computeIfAbsent(target, k -> new TreeSet<>()).add(e.getKey());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# SIRIUS — module dependency map (generated)\n\n");
        sb.append("_Generated ").append(LocalDate.now()).append(" @ `").append(revision)
                .append("`. Run `./gradlew aiOverview` to refresh. Gitignored — do not commit._\n\n");
        sb.append(deps.size()).append(" modules.\n\n");
        sb.append("Direct **project→project** dependencies declared in each module's `build.gradle`. "
                + "External libraries are omitted here — resolve those with "
                + "`./gradlew :<module>:dependencies` (see the Dependencies section of AGENTS.md).\n\n");
        sb.append("> Reading the edges: **`api`** dependencies are re-exported to consumers (part of the "
                + "module's public surface); **`implementation`** are internal and not visible downstream. "
                + "**used by** is the reverse edge — a module's blast radius: change it and these modules "
                + "may be affected.\n\n");
        sb.append("---\n");

        for (Map.Entry<String, List<String>> e : deps.entrySet()) {
            String module = e.getKey();
            sb.append("\n## `").append(module).append("`\n\n");

            // Group forward edges by configuration, preserving CONFIG_ORDER then alphabetical.
            Map<String, TreeSet<String>> byConfig = new LinkedHashMap<>();
            for (String cfg : CONFIG_ORDER) byConfig.put(cfg, new TreeSet<>());
            for (String edge : e.getValue()) {
                int sp = edge.indexOf(' ');
                String cfg = edge.substring(0, sp);
                String target = edge.substring(sp + 1);
                byConfig.computeIfAbsent(cfg, k -> new TreeSet<>()).add(target);
            }

            boolean any = false;
            for (Map.Entry<String, TreeSet<String>> c : byConfig.entrySet()) {
                if (c.getValue().isEmpty()) continue;
                any = true;
                sb.append("- **").append(c.getKey()).append("** → ")
                        .append(joinCode(c.getValue())).append("\n");
            }
            if (!any) sb.append("- _no internal module dependencies_\n");

            TreeSet<String> consumers = usedBy.get(module);
            if (consumers != null && !consumers.isEmpty()) {
                sb.append("- **used by** ← ").append(joinCode(consumers)).append("\n");
            }
        }
        return sb.toString();
    }

    private static String joinCode(Iterable<String> modules) {
        StringBuilder sb = new StringBuilder();
        for (String m : modules) {
            if (sb.length() > 0) sb.append(", ");
            sb.append('`').append(m).append('`');
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** {@code :a:b:c} -> {@code a--b--c}; keeps deep module paths as single flat filenames. */
    static String sanitize(String modulePath) {
        String s = modulePath.startsWith(":") ? modulePath.substring(1) : modulePath;
        s = s.replace(":", "--");
        return s.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void deleteRecursively(Path dir) {
        if (!Files.exists(dir)) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
