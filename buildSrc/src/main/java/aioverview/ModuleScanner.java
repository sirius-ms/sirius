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

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.javadoc.Javadoc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parses one Gradle module's {@code src/main/java} tree with JavaParser (source-only, no symbol
 * resolution) and produces a compact, signature-focused model of its public/protected types.
 *
 * <p>Because Lombok members ({@code getX}/{@code setX}/{@code builder}/constructors) are generated
 * at compile time and are therefore absent from the source AST, this scanner <em>synthesizes</em>
 * their signatures from the Lombok annotations so an AI agent sees the real public surface. Synthesized
 * members are tagged {@code (lombok)}.
 */
final class ModuleScanner {

    /** How much Javadoc to include for a class or a method. */
    enum DocMode {
        NONE, SUMMARY, FULL;

        static DocMode parse(String value, DocMode fallback) {
            if (value == null) return fallback;
            switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "none": return NONE;
                case "summary": return SUMMARY;
                case "full": return FULL;
                default: return fallback;
            }
        }
    }

    /** One public/protected type and its rendered member lines. */
    static final class TypeInfo {
        String packageName = "";
        String displayName;   // e.g. Outer.Inner
        String kind;          // class | interface | enum | record | @interface
        boolean isProtected;  // false => public
        String extendsClause; // may be null
        String implementsClause; // may be null
        String abstractMarker; // " (abstract)" or ""
        String javadoc;       // one-liner or null
        final List<String> members = new ArrayList<>();
    }

    static final class ScanResult {
        final List<TypeInfo> types = new ArrayList<>();
        int parseErrors = 0;
        int sourceFiles = 0;
    }

    private ModuleScanner() {
    }

    static ScanResult scan(Path srcDir, DocMode classDocs, DocMode methodDocs) {
        ScanResult result = new ScanResult();
        ParserConfiguration config = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setAttributeComments(true);
        JavaParser parser = new JavaParser(config);

        List<Path> files;
        try (Stream<Path> walk = Files.walk(srcDir)) {
            files = walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return result; // module dir vanished mid-build; caller reports empty
        }

        for (Path file : files) {
            result.sourceFiles++;
            try {
                ParseResult<CompilationUnit> parsed = parser.parse(file);
                Optional<CompilationUnit> cu = parsed.getResult();
                if (parsed.isSuccessful() && cu.isPresent()) {
                    CompilationUnit unit = cu.get();
                    String pkg = unit.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString()).orElse("");
                    for (TypeDeclaration<?> td : unit.getTypes()) {
                        collectType(td, "", pkg, result.types, classDocs, methodDocs);
                    }
                } else {
                    result.parseErrors++;
                }
            } catch (Exception e) {
                // A single unparseable file must never fail the whole task.
                result.parseErrors++;
            }
        }
        result.types.sort(Comparator
                .comparing((TypeInfo t) -> t.packageName)
                .thenComparing(t -> t.displayName));
        return result;
    }

    private static void collectType(TypeDeclaration<?> td, String outerPrefix, String pkg,
                                    List<TypeInfo> out, DocMode classDocs, DocMode methodDocs) {
        boolean isPublic = td.isPublic();
        boolean isProtected = td.isProtected();
        // Top-level: public only. Nested: public or protected. Skip private/package-private.
        boolean topLevel = outerPrefix.isEmpty();
        if (topLevel ? !isPublic : !(isPublic || isProtected)) {
            // Still recurse: a public nested type can live inside a package-private one only if
            // that outer is itself exposed, which it is not here, so we stop descending.
            return;
        }

        TypeInfo info = new TypeInfo();
        info.packageName = pkg;
        info.displayName = outerPrefix + td.getNameAsString();
        info.isProtected = isProtected && !isPublic;
        info.javadoc = describe(td.getJavadoc(), classDocs, 300);

        Set<String> declared = declaredCallableKeys(td);

        if (td instanceof ClassOrInterfaceDeclaration coid) {
            info.kind = coid.isInterface() ? "interface" : "class";
            info.abstractMarker = (!coid.isInterface() && coid.isAbstract()) ? " (abstract)" : "";
            info.extendsClause = typeList(coid.getExtendedTypes());
            info.implementsClause = typeList(coid.getImplementedTypes());
            addDeclaredMembers(td, info, methodDocs);
            if (!coid.isInterface()) {
                LombokSynth.synthesize(coid, declared, info.members);
            }
        } else if (td instanceof RecordDeclaration rec) {
            info.kind = "record";
            info.abstractMarker = "";
            info.implementsClause = typeList(rec.getImplementedTypes());
            // Components -> accessor methods + canonical constructor.
            List<String> compTypes = new ArrayList<>();
            for (Parameter c : rec.getParameters()) {
                info.members.add(memberLine(false, c.getType().asString() + " "
                        + c.getNameAsString() + "()", " (record)"));
                compTypes.add(c.getType().asString());
            }
            info.members.add(memberLine(false,
                    td.getNameAsString() + "(" + String.join(", ", compTypes) + ")", " (record)"));
            addDeclaredMembers(td, info, methodDocs);
        } else if (td instanceof EnumDeclaration en) {
            info.kind = "enum";
            info.abstractMarker = "";
            info.implementsClause = typeList(en.getImplementedTypes());
            String constants = en.getEntries().stream()
                    .map(EnumConstantDeclaration::getNameAsString)
                    .collect(Collectors.joining(", "));
            if (!constants.isEmpty()) {
                info.members.add("- constants: " + constants);
            }
            addDeclaredMembers(td, info, methodDocs);
        } else if (td instanceof AnnotationDeclaration) {
            info.kind = "@interface";
            info.abstractMarker = "";
            addDeclaredMembers(td, info, methodDocs);
        } else {
            info.kind = "type";
            info.abstractMarker = "";
            addDeclaredMembers(td, info, methodDocs);
        }

        info.members.sort(Comparator.naturalOrder());
        out.add(info);

        // Recurse into nested types.
        for (BodyDeclaration<?> member : td.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                collectType(nested, info.displayName + ".", pkg, out, classDocs, methodDocs);
            }
        }
    }

    /** Public/protected methods, constructors and fields declared explicitly in the source.
     *  When {@code methodDocs} is not NONE, method/constructor Javadoc is attached as an indented note. */
    private static void addDeclaredMembers(TypeDeclaration<?> td, TypeInfo info, DocMode methodDocs) {
        for (BodyDeclaration<?> member : td.getMembers()) {
            if (member instanceof MethodDeclaration m) {
                if (!m.isPublic() && !m.isProtected()) continue;
                String params = m.getParameters().stream()
                        .map(ModuleScanner::paramType)
                        .collect(Collectors.joining(", "));
                String sig = m.getType().asString() + " " + m.getNameAsString() + "(" + params + ")";
                info.members.add(withDoc(memberLine(m.isProtected() && !m.isPublic(), sig,
                        m.isStatic() ? " (static)" : ""), m.getJavadoc(), methodDocs));
            } else if (member instanceof ConstructorDeclaration c) {
                if (!c.isPublic() && !c.isProtected()) continue;
                String params = c.getParameters().stream()
                        .map(ModuleScanner::paramType)
                        .collect(Collectors.joining(", "));
                info.members.add(withDoc(memberLine(c.isProtected() && !c.isPublic(),
                        c.getNameAsString() + "(" + params + ")", ""), c.getJavadoc(), methodDocs));
            } else if (member instanceof FieldDeclaration f) {
                if (!f.isPublic() && !f.isProtected()) continue;
                for (VariableDeclarator v : f.getVariables()) {
                    String suffix = (f.isStatic() ? " (static)" : "")
                            + (f.isFinal() ? " (final)" : "");
                    info.members.add(memberLine(f.isProtected() && !f.isPublic(),
                            v.getType().asString() + " " + v.getNameAsString(), suffix));
                }
            }
        }
    }

    static String memberLine(boolean isProtected, String signature, String suffix) {
        return (isProtected ? "~ " : "+ ") + signature + suffix;
    }

    /** Attaches a method/constructor Javadoc (capped at 200 for SUMMARY) as an indented note under
     *  its signature. Returns the signature unchanged when there is no doc / mode is NONE. */
    private static String withDoc(String memberLine, Optional<Javadoc> javadoc, DocMode mode) {
        String doc = describe(javadoc, mode, 200);
        if (doc == null) return memberLine;
        return memberLine + "\n  > " + doc.replace("\n", "\n  > ");
    }

    private static String paramType(Parameter p) {
        return p.getType().asString() + (p.isVarArgs() ? "..." : "");
    }

    private static String typeList(com.github.javaparser.ast.NodeList<ClassOrInterfaceType> types) {
        if (types == null || types.isEmpty()) return null;
        return types.stream().map(ClassOrInterfaceType::asString).collect(Collectors.joining(", "));
    }

    /** name#paramCount keys for every declared method, to suppress colliding Lombok synthesis. */
    private static Set<String> declaredCallableKeys(TypeDeclaration<?> td) {
        Set<String> keys = new HashSet<>();
        for (BodyDeclaration<?> member : td.getMembers()) {
            if (member instanceof MethodDeclaration m) {
                keys.add(m.getNameAsString() + "#" + m.getParameters().size());
            }
        }
        return keys;
    }

    // Structural boundaries that end the first sentence/unit even without terminal punctuation.
    private static final Pattern PARA_BREAK = Pattern.compile("\\n[ \\t]*\\n");               // blank line
    private static final Pattern LIST_ITEM = Pattern.compile("\\n[ \\t]*([-*•]|\\d+\\.)\\s"); // list item on a new line
    private static final Pattern HTML_BLOCK = Pattern.compile("(?i)<\\s*(p|br|ul|ol|li|pre|table|h[1-6])\\b");
    private static final Set<String> ABBREVIATIONS = Set.of(
            "e.g", "i.e", "etc", "vs", "cf", "al", "fig", "eq", "approx", "ca", "resp",
            "no", "dr", "mr", "mrs", "ms", "prof", "inc", "st");

    /** Renders a class/method Javadoc per {@code mode}: null for NONE/absent; the first sentence
     *  (word-safe, capped at {@code summaryCap}) for SUMMARY; or the whole description for FULL. */
    private static String describe(Optional<Javadoc> javadoc, DocMode mode, int summaryCap) {
        if (mode == DocMode.NONE || javadoc.isEmpty()) return null;
        String raw = javadoc.get().getDescription().toText();
        if (raw == null || raw.isBlank()) return null;
        raw = raw.replace("\r\n", "\n").replace("\r", "\n");
        String result = (mode == DocMode.FULL) ? fullText(raw) : summarize(raw, summaryCap);
        return (result == null || result.isBlank()) ? null : result;
    }

    /** First sentence/unit, cleaned to one line, truncated on a word boundary if longer than {@code cap}. */
    private static String summarize(String raw, int cap) {
        String first = clean(raw.substring(0, firstSentenceEnd(raw)));
        if (first.length() > cap) {
            String clipped = first.substring(0, cap);
            int lastSpace = clipped.lastIndexOf(' ');
            if (lastSpace >= 40) clipped = clipped.substring(0, lastSpace);
            first = clipped.stripTrailing() + "…";
        }
        return first;
    }

    /** Whole description as (possibly multi-line) plain text: inline tags/HTML removed, spaces
     *  collapsed, blank lines preserved as paragraph breaks. */
    private static String fullText(String raw) {
        String s = raw.replaceAll("\\{@\\w+\\s+([^}]*)}", "$1"); // {@link Foo} -> Foo
        s = s.replaceAll("<[^>]+>", " ");
        s = s.replaceAll("[ \\t]+", " ");
        s = s.replaceAll(" *\\n *", "\n");
        s = s.replaceAll("\\n{3,}", "\n\n");
        return s.strip();
    }

    /** Inline tags and HTML removed, all whitespace collapsed to single spaces. */
    private static String clean(String s) {
        s = s.replaceAll("\\{@\\w+\\s+([^}]*)}", "$1"); // {@link Foo} -> Foo
        s = s.replaceAll("<[^>]+>", " ");
        return s.replaceAll("\\s+", " ").trim();
    }

    /** End index (exclusive) of the first sentence: the earliest of terminal punctuation
     *  ({@code . ! ?}), a blank line, a list item, or an HTML block tag. */
    private static int firstSentenceEnd(String s) {
        int end = s.length();
        Matcher m;
        if ((m = PARA_BREAK.matcher(s)).find()) end = Math.min(end, m.start());
        if ((m = LIST_ITEM.matcher(s)).find()) end = Math.min(end, m.start());
        if ((m = HTML_BLOCK.matcher(s)).find()) end = Math.min(end, m.start());
        return Math.min(end, terminalPunctuation(s, end));
    }

    /** Index just after the first sentence-ending {@code . ! ?} before {@code limit}, or {@code limit}. */
    private static int terminalPunctuation(String s, int limit) {
        int bound = Math.min(s.length(), limit);
        for (int i = 0; i < bound; i++) {
            char c = s.charAt(i);
            if (c != '.' && c != '!' && c != '?') continue;
            int j = i + 1;
            while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;
            // A sentence ends here if the next non-space char starts a new sentence (uppercase) or text ends.
            boolean endsHere = (j >= s.length()) || Character.isUpperCase(s.charAt(j));
            if (endsHere && c == '.' && isAbbreviation(s, i)) endsHere = false;
            if (endsHere) return i + 1;
        }
        return limit;
    }

    /** True if the word ending at {@code dotIndex} is a known abbreviation or a single-letter initial. */
    private static boolean isAbbreviation(String s, int dotIndex) {
        int start = dotIndex;
        while (start > 0 && (Character.isLetter(s.charAt(start - 1)) || s.charAt(start - 1) == '.')) {
            start--;
        }
        String word = s.substring(start, dotIndex).toLowerCase(Locale.ROOT);
        return word.length() <= 1 || ABBREVIATIONS.contains(word);
    }

    // ------------------------------------------------------------------
    // Lombok signature synthesis
    // ------------------------------------------------------------------

    /** Kept package-private and separate for readability; operates purely on the class AST. */
    static final class LombokSynth {
        private LombokSynth() {
        }

        static void synthesize(ClassOrInterfaceDeclaration cls, Set<String> declared,
                               List<String> out) {
            boolean data = has(cls, "Data");
            boolean value = has(cls, "Value");
            boolean clsGetter = has(cls, "Getter") || data || value;
            boolean clsSetter = has(cls, "Setter") || data;

            // @Accessors config (class level).
            boolean fluent = false;
            List<String> prefixes = new ArrayList<>();
            Optional<AnnotationExpr> acc = cls.getAnnotationByName("Accessors");
            if (acc.isPresent()) {
                fluent = flag(acc.get(), "fluent");
                prefixes = stringValues(acc.get(), "prefix");
            }

            List<FieldDeclaration> instanceFields = new ArrayList<>();
            for (BodyDeclaration<?> m : cls.getMembers()) {
                if (m instanceof FieldDeclaration f && !f.isStatic()) {
                    instanceFields.add(f);
                }
            }

            for (FieldDeclaration f : instanceFields) {
                boolean fieldGetterNone = annotationSaysNone(f, "Getter");
                boolean fieldSetterNone = annotationSaysNone(f, "Setter");
                boolean getter = (clsGetter || hasAnn(f, "Getter")) && !fieldGetterNone;
                boolean setter = (clsSetter || hasAnn(f, "Setter"))
                        && !fieldSetterNone && !f.isFinal() && !value;

                for (VariableDeclarator v : f.getVariables()) {
                    String field = v.getNameAsString();
                    String type = v.getType().asString();
                    boolean isBool = type.equals("boolean");
                    String base = accessorBase(field, prefixes);

                    if (getter) {
                        String name = fluent ? base : (isBool ? "is" : "get") + cap(base);
                        if (!declared.contains(name + "#0")) {
                            out.add(memberLine(false, type + " " + name + "()", " (lombok)"));
                        }
                    }
                    if (setter) {
                        String name = fluent ? base : "set" + cap(base);
                        if (!declared.contains(name + "#1")) {
                            String ret = fluent ? cls.getNameAsString() : "void";
                            out.add(memberLine(false,
                                    ret + " " + name + "(" + type + ")", " (lombok)"));
                        }
                    }
                }
            }

            // Constructors.
            boolean allArgs = has(cls, "AllArgsConstructor") || value;
            boolean noArgs = has(cls, "NoArgsConstructor");
            boolean requiredArgs = has(cls, "RequiredArgsConstructor")
                    || (data && !has(cls, "AllArgsConstructor") && !has(cls, "NoArgsConstructor"));
            String cn = cls.getNameAsString();
            if (noArgs && !declared.contains(cn + "#0")) {
                out.add(memberLine(false, cn + "()", " (lombok)"));
            }
            if (allArgs) {
                List<String> types = fieldTypes(instanceFields, false);
                if (!declared.contains(cn + "#" + types.size())) {
                    out.add(memberLine(false, cn + "(" + String.join(", ", types) + ")", " (lombok)"));
                }
            }
            if (requiredArgs) {
                List<String> types = fieldTypes(instanceFields, true);
                if (!types.isEmpty() && !declared.contains(cn + "#" + types.size())) {
                    out.add(memberLine(false, cn + "(" + String.join(", ", types) + ")", " (lombok)"));
                }
            }

            // Builder.
            if (has(cls, "Builder") && !declared.contains("builder#0")) {
                out.add(memberLine(false, cn + "Builder builder()", " (static, lombok)"));
            }

            // @Data / @Value object methods.
            if (data || value) {
                out.add("+ equals/hashCode/toString  (lombok)");
            }
        }

        /** Field types for constructor synthesis; when {@code requiredOnly}, only final-uninitialised
         *  and {@code @NonNull} fields (the {@code @RequiredArgsConstructor} contract). */
        private static List<String> fieldTypes(List<FieldDeclaration> fields, boolean requiredOnly) {
            List<String> types = new ArrayList<>();
            for (FieldDeclaration f : fields) {
                for (VariableDeclarator v : f.getVariables()) {
                    boolean required = (f.isFinal() && v.getInitializer().isEmpty())
                            || hasAnn(f, "NonNull");
                    if (!requiredOnly || required) {
                        types.add(v.getType().asString());
                    }
                }
            }
            return types;
        }

        private static String accessorBase(String field, List<String> prefixes) {
            for (String pre : prefixes) {
                if (!pre.isEmpty() && field.startsWith(pre) && field.length() > pre.length()) {
                    String rest = field.substring(pre.length());
                    return Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
                }
            }
            return field;
        }

        private static boolean has(ClassOrInterfaceDeclaration cls, String name) {
            return cls.getAnnotationByName(name).isPresent();
        }

        private static boolean hasAnn(FieldDeclaration f, String name) {
            return f.getAnnotationByName(name).isPresent();
        }

        private static boolean annotationSaysNone(FieldDeclaration f, String name) {
            return f.getAnnotationByName(name).map(a -> a.toString().contains("NONE")).orElse(false);
        }

        private static boolean flag(AnnotationExpr ann, String key) {
            if (ann instanceof NormalAnnotationExpr normal) {
                for (MemberValuePair pair : normal.getPairs()) {
                    if (pair.getNameAsString().equals(key)) {
                        return pair.getValue().toString().contains("true");
                    }
                }
            }
            return false;
        }

        private static List<String> stringValues(AnnotationExpr ann, String key) {
            List<String> values = new ArrayList<>();
            if (ann instanceof NormalAnnotationExpr normal) {
                for (MemberValuePair pair : normal.getPairs()) {
                    if (pair.getNameAsString().equals(key)) {
                        pair.getValue().findAll(StringLiteralExpr.class)
                                .forEach(s -> values.add(s.asString()));
                    }
                }
            }
            return values;
        }

        private static String cap(String s) {
            return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }
}
