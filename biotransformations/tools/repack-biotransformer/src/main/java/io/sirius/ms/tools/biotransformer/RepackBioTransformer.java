package io.sirius.ms.tools.biotransformer;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Builds the BioTransformer jar {@code :biotransformations} depends on.
 * <p>
 * Its input is {@code biotransformer-sirius-mod-relocated-min-no-envi-path}, one of our own earlier
 * repackagings of BioTransformer 3.0, which carries CDK 2.7.1 relocated to
 * {@code io.siriusms.shadow.bt.cdk} while leaving {@code org.openscience.cdk.interfaces} and
 * {@code .tools} unrelocated. That puts two CDK versions in one object graph: relocated 2.7.1
 * implementation classes bound to the project's 2.12 interfaces, missing everything those
 * interfaces gained since - {@code IStereoElement.updateCarriers} above all, so any substrate with
 * a stereo centre fails with an {@link AbstractMethodError}.
 * <p>
 * The output carries no CDK at all and refers to {@code org.openscience.cdk} throughout, so exactly
 * one CDK is on the classpath. See the README beside this file for the full list of changes, the
 * published coordinates and how to publish them.
 * <p>
 * BioTransformer itself, Ambit2, Weka, the knowledge base and the models are copied byte for byte,
 * and repeated runs produce an identical jar.
 */
public final class RepackBioTransformer {

    private static final String SHADOW = "io/siriusms/shadow/bt/cdk/";
    private static final String CDK = "org/openscience/cdk/";

    /**
     * Libraries the jar bundles that the SIRIUS classpath already provides - 3825 duplicate classes
     * in all, almost none of them the same build. Which copy wins is decided by classpath order,
     * which the launch scripts leave to a {@code lib/*} wildcard, so it can differ between machines.
     * <p>
     * This is not hypothetical: CDK 2.12's {@code CDKToBeam} calls {@code GraphBuilder.atomStereo},
     * which exists in the beam on the classpath but not in the older copy inside the jar. With the
     * bundled beam winning, generating SMILES for any molecule with a stereo centre dies with a
     * {@code NoSuchMethodError} inside CDK itself.
     * <p>
     * Every one of these is a transitive dependency of CDK or unreachable from BioTransformer's own
     * code, so the classpath copy is the one that should be used. Weka, Ambit2, Jena, Jackson 1 and
     * HttpClient are deliberately not in this list: nothing on the SIRIUS classpath provides them.
     */
    private static final List<String> PROVIDED_BY_SIRIUS = List.of(
            "uk/ac/ebi/beam/",                      // beam-core/beam-func - CDK's SMILES backend
            "com/google/common/", "com/google/thirdparty/",   // guava
            "org/apache/commons/math/",             // commons-math 2
            "org/xmlcml/",                          // cmlxom, euclid, stml
            "nu/xom/",                              // xom
            "org/joda/",                            // joda-time
            "org/freehep/",                         // freehep graphics
            "javax/vecmath/",                       // vecmath
            "signature/",                           // signatures
            "org/ccil/",                            // tagsoup
            "Jama/",                                // jama
            "blogspot/software_and_algorithms/",    // stern library, ships with euclid
            "javax/xml/", "org/w3c/", "org/xml/");  // xml-apis, also owned by the java.xml module

    /**
     * Bundled libraries that nothing in the jar reaches from SIRIUS. Each was measured by walking
     * the reference graph from {@code :biotransformations}' own classes through the jar: every one
     * of these has zero reachable classes.
     * <p>
     * Weka's GUI and Jena are deliberately not here. {@code weka.core.Utils} references
     * {@code weka.gui}, and Weka loads classes reflectively while deserialising its models, so
     * static reachability understates what it needs. Jena keeps seven classes alive through
     * {@code ChemStructureFingerprinter}'s use of {@code jena.util.FileUtils}.
     */
    private static final List<String> UNREACHABLE = List.of(
            "org/apache/xerces/", "org/apache/xalan/", "org/apache/xml/", "org/apache/xpath/",
            "org/apache/bcel/", "org/apache/html/", "org/apache/wml/", "com/sun/xml/",
            "org/apache/regexp/", "org/apache/xmlcommons/",   // XML stack; the JDK's JAXP is used
            "com/google/protobuf/", "google/protobuf/",
            "org/netlib/", "com/github/fommil/", "no/uib/cipr/",   // linear algebra
            "org/junit/", "junit/", "org/hamcrest/",               // a test framework in a runtime jar
            "com/opencsv/", "org/apache/commons/cli/", "org/j_paine/",
            "executable/",                                         // BioTransformer's own CLI entry points
            "randomThings/", "compareDataset/", "ForUNC/",          // scratch classes left in the build
            // The library's own unit tests. They reference JUnit, which is dropped above, so they
            // would otherwise ship as dead weight with dangling references. Named individually:
            // weka/datagenerators/Test, org/apache/jena/iri/impl/Test and the jena vocabulary
            // classes also end in "Test" but are real code.
            "biotransformer/BioTransformerTest", "biotransformer/EnvMicroBTTest",
            "biotransformer/BiotransformerSequenceTest", "biotransformer/CYP450BTTest",
            "biotransformer/ChemStructureTest", "biotransformer/ECBasedBTTest",
            "biotransformer/HGutBTTest", "biotransformer/MetaboliteFinderTest",
            "biotransformer/Phase2BTTest", "phase2filter/BiotransformerAPITest",
            "phase2filter/P2FilterTest", "reactantpredictor/AppTest",
            "reactantpredictor/BioTransformerAPITest");

    /** Build and IDE leftovers, plus logging configuration for a framework that is not present. */
    private static final Set<String> JUNK = Set.of(
            ".classpath", ".project", "CypReact.iml", "cyProduct.iml", "pom.xml", "manifest.txt",
            "log4j.properties", "jena-log4j.properties", "Log4j-charsets.properties",
            "Log4j-config.xsd", "Log4j-events.dtd", "Log4j-events.xsd", "Log4j-levels.xsd",
            "opencsv.properties", "opencsv_de.properties");

    /** CDK's own resources. CDK 2.12 ships its own copies; keeping these would shadow them. */
    private static final Set<String> DROPPED_RESOURCES = Set.of(
            "build.props",                  // cdk-core - declares the CDK version, would report 2.7.1
            "io-formats.set",               // cdk-ioformats
            "qsar-descriptors.set",         // cdk-qsar
            "libio-cml-customizers.set");   // lists CDK classes that no longer ship

    /**
     * Ambit2's {@code SuppleAtomContainer} implements {@code IAtomContainer} directly rather than
     * extending CDK's {@code AtomContainer}, so it does not inherit methods added to the interface
     * after it was compiled. {@code addBond(int,int,Order,Display)} is the only one missing, and
     * without it the class cannot be instantiated against CDK 2.12.
     */
    private static final String PATCHED_CLASS = "ambit2/core/groups/SuppleAtomContainer";
    private static final String ADD_BOND = "addBond";
    private static final String ADD_BOND_WITH_DISPLAY =
            "(IILorg/openscience/cdk/interfaces/IBond$Order;Lorg/openscience/cdk/interfaces/IBond$Display;)V";
    private static final String ADD_BOND_DELEGATE =
            "(IILorg/openscience/cdk/interfaces/IBond$Order;)V";

    /**
     * {@code InChIGeneratorFactory.getInChIGenerator(IAtomContainer)} was an instance method in CDK
     * 2.7.1 and is static in 2.12. The compiled call sites use {@code invokevirtual} and would fail
     * with {@code IncompatibleClassChangeError}, so they are rewritten to {@code invokestatic}.
     * The factory instance they push is discarded; it is a singleton and fetching it has no effect
     * beyond the exception {@code getInstance()} may throw, which still happens.
     */
    private static final String INCHI_FACTORY = "org/openscience/cdk/inchi/InChIGeneratorFactory";
    private static final String GET_INCHI_GENERATOR = "getInChIGenerator";
    private static final String GET_INCHI_GENERATOR_DESC =
            "(Lorg/openscience/cdk/interfaces/IAtomContainer;)Lorg/openscience/cdk/inchi/InChIGenerator;";

    /**
     * The input jar carries no EnviPath-derived knowledge base - that data is CC BY-NC-SA licensed,
     * unlike the LGPL library itself - yet its {@code config.json} still declares the ENVMICRO
     * biosystem and points it at a {@code btkb/ENVMICRO} directory that is not there. Nothing reads
     * it, since biosystems are loaded on demand and SIRIUS has the environmental-microbial
     * transformation disabled, so removing the declaration only makes the configuration describe
     * what the jar actually contains.
     */
    private static final String CONFIG_JSON = "biotransformer-config/config.json";
    private static final String ENVIPATH_BIOSYSTEM = "ENVMICRO";

    /** Fixed timestamp so repeated runs produce an identical jar. */
    private static final long FIXED_TIME = 0L;

    private final Remapper remapper = new Remapper() {
        @Override
        public String map(String internalName) {
            return internalName.startsWith(SHADOW) ? CDK + internalName.substring(SHADOW.length()) : internalName;
        }

        /** Class names also appear as string constants, e.g. in reflective lookups. */
        @Override
        public Object mapValue(Object value) {
            if (value instanceof String s) {
                if (s.contains(SHADOW))
                    return s.replace(SHADOW, CDK);
                String dottedShadow = SHADOW.replace('/', '.');
                if (s.contains(dottedShadow))
                    return s.replace(dottedShadow, CDK.replace('/', '.'));
            }
            return super.mapValue(value);
        }
    };

    private int copied, droppedCdk, droppedResources, remapped, staticCallsFixed, droppedProvided;
    private int enviPathDeclarationsRemoved, droppedUnreachable;
    private boolean patchApplied;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("usage: RepackBioTransformer <input.jar> <output.jar>");
            System.exit(2);
        }
        Path in = Path.of(args[0]), out = Path.of(args[1]);
        if (!Files.isRegularFile(in))
            throw new IOException("Input jar not found: " + in);
        Files.createDirectories(out.toAbsolutePath().getParent());

        RepackBioTransformer repack = new RepackBioTransformer();
        repack.run(in, out);
        repack.verify(out);
    }

    private void run(Path in, Path out) throws IOException {
        try (ZipFile source = new ZipFile(in.toFile());
             ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(out))) {

            // Sorted so the output does not depend on the input's entry order.
            List<ZipEntry> entries = new ArrayList<>();
            for (Enumeration<? extends ZipEntry> e = source.entries(); e.hasMoreElements(); )
                entries.add(e.nextElement());
            entries.sort(Comparator.comparing(ZipEntry::getName));

            for (ZipEntry entry : entries) {
                String name = entry.getName();
                if (entry.isDirectory())
                    continue;
                if (name.startsWith(SHADOW)) {
                    droppedCdk++;
                    continue;
                }
                if (DROPPED_RESOURCES.contains(name)) {
                    droppedResources++;
                    continue;
                }
                if (PROVIDED_BY_SIRIUS.stream().anyMatch(name::startsWith)) {
                    droppedProvided++;
                    continue;
                }
                if (UNREACHABLE.stream().anyMatch(name::startsWith) || JUNK.contains(name)) {
                    droppedUnreachable++;
                    continue;
                }

                byte[] bytes;
                try (InputStream is = source.getInputStream(entry)) {
                    bytes = is.readAllBytes();
                }
                if (name.endsWith(".class")) {
                    bytes = transform(bytes);
                    remapped++;
                } else {
                    if (CONFIG_JSON.equals(name))
                        bytes = removeEnviPathBiosystem(bytes);
                    copied++;
                }

                ZipEntry written = new ZipEntry(name);
                written.setTime(FIXED_TIME);
                target.putNextEntry(written);
                target.write(bytes);
                target.closeEntry();
            }
        }

        System.out.printf("repacked: %d classes rewritten, %d resources copied, "
                        + "%d bundled-CDK entries dropped, %d colliding CDK resources dropped, "
                        + "%d static call sites fixed, %d entries dropped as provided by SIRIUS, "
                        + "%d EnviPath biosystem declarations removed, %d unreachable entries dropped%n",
                remapped, copied, droppedCdk, droppedResources, staticCallsFixed, droppedProvided,
                enviPathDeclarationsRemoved, droppedUnreachable);
        if (!patchApplied)
            throw new IllegalStateException("Expected to patch " + PATCHED_CLASS + " but never saw it");
        if (enviPathDeclarationsRemoved == 0)
            throw new IllegalStateException("Expected to find " + ENVIPATH_BIOSYSTEM + " declarations in "
                    + CONFIG_JSON + " but found none");
    }

    /**
     * Drops every {@code "ENVMICRO": { ... }} object from the configuration, together with the comma
     * that separated it from its sibling. The file carries {@code //} comments and is read with
     * Jackson's ALLOW_COMMENTS, so it is edited as text rather than reformatted by a JSON writer.
     */
    private byte[] removeEnviPathBiosystem(byte[] bytes) {
        List<String> lines = new ArrayList<>(Arrays.asList(
                new String(bytes, java.nio.charset.StandardCharsets.UTF_8).split("\n", -1)));
        for (int i = 0; i < lines.size(); ) {
            if (!lines.get(i).trim().startsWith('"' + ENVIPATH_BIOSYSTEM + '"')) {
                i++;
                continue;
            }
            int end = i, depth = 0;
            boolean sawBrace = false;
            while (end < lines.size()) {
                for (char c : lines.get(end).toCharArray()) {
                    if (c == '{') { depth++; sawBrace = true; }
                    else if (c == '}') depth--;
                }
                if (sawBrace && depth == 0) break;
                end++;
            }
            if (!sawBrace || depth != 0)
                throw new IllegalStateException("Unbalanced " + ENVIPATH_BIOSYSTEM + " block in " + CONFIG_JSON);

            // this entry was the last one, so the sibling before it must lose its trailing comma
            boolean lastInObject = lines.get(end).trim().equals("}");
            lines.subList(i, end + 1).clear();
            if (lastInObject)
                for (int j = i - 1; j >= 0; j--) {
                    String prev = lines.get(j);
                    // blank lines and // comments are not the preceding sibling
                    if (prev.isBlank() || prev.stripLeading().startsWith("//")) continue;
                    if (prev.stripTrailing().endsWith(","))
                        lines.set(j, prev.stripTrailing().substring(0, prev.stripTrailing().length() - 1));
                    break;
                }
            enviPathDeclarationsRemoved++;
        }
        String out = String.join("\n", lines);
        if (out.contains(ENVIPATH_BIOSYSTEM))
            throw new IllegalStateException(ENVIPATH_BIOSYSTEM + " survived in " + CONFIG_JSON);
        long open = out.chars().filter(c -> c == '{').count();
        long close = out.chars().filter(c -> c == '}').count();
        if (open != close)
            throw new IllegalStateException("Braces no longer balance in " + CONFIG_JSON
                    + " after removing " + ENVIPATH_BIOSYSTEM + " (" + open + " vs " + close + ")");
        return out.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] transform(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        // No reader passed to the writer on purpose: it would copy the original constant pool,
        // which still holds the shadow package names. Maxs are copied from the input unchanged,
        // and the one method we synthesise declares its own.
        ClassWriter writer = new ClassWriter(0);
        // The static-call fix sits downstream of the remapper so it matches the final CDK names.
        ClassVisitor visitor = new ClassRemapper(new FixStaticCdkCalls(writer), remapper);
        if (PATCHED_CLASS.equals(reader.getClassName()))
            visitor = new AddMissingAddBond(visitor);
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    /** Rewrites calls to CDK methods that turned static between 2.7.1 and 2.12. */
    private final class FixStaticCdkCalls extends ClassVisitor {
        FixStaticCdkCalls(ClassVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (mv == null)
                return null;
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name,
                                            String descriptor, boolean isInterface) {
                    if (opcode == Opcodes.INVOKEVIRTUAL
                            && INCHI_FACTORY.equals(owner)
                            && GET_INCHI_GENERATOR.equals(name)
                            && GET_INCHI_GENERATOR_DESC.equals(descriptor)) {
                        // stack: factory, container -> container
                        super.visitInsn(Opcodes.SWAP);
                        super.visitInsn(Opcodes.POP);
                        super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, name, descriptor, false);
                        staticCallsFixed++;
                        return;
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }
            };
        }
    }

    /** Adds {@code addBond(int,int,Order,Display)} delegating to the three-argument form. */
    private final class AddMissingAddBond extends ClassVisitor {
        private boolean hasDelegate, alreadyPresent;

        AddMissingAddBond(ClassVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            if (ADD_BOND.equals(name)) {
                if (ADD_BOND_DELEGATE.equals(descriptor)) hasDelegate = true;
                if (ADD_BOND_WITH_DISPLAY.equals(descriptor)) alreadyPresent = true;
            }
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            if (alreadyPresent) {
                System.out.println("  " + PATCHED_CLASS + " already declares addBond(..., Display) - not patching");
                patchApplied = true;
                super.visitEnd();
                return;
            }
            if (!hasDelegate)
                throw new IllegalStateException(PATCHED_CLASS + " has no " + ADD_BOND + ADD_BOND_DELEGATE
                        + " to delegate to; the patch needs rewriting for this jar");

            MethodVisitor mv = super.visitMethod(Opcodes.ACC_PUBLIC, ADD_BOND, ADD_BOND_WITH_DISPLAY, null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 3);
            // The bond display hint is dropped: this container has no field to keep it in, and the
            // three-argument form is what every existing caller of this class already goes through.
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, PATCHED_CLASS, ADD_BOND, ADD_BOND_DELEGATE, false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(4, 5);     // this + 2 ints + order on the stack; display occupies local 4
            mv.visitEnd();

            patchApplied = true;
            System.out.println("  patched " + PATCHED_CLASS + ": added addBond(int,int,Order,Display)");
            super.visitEnd();
        }
    }

    /**
     * Fails the build if any trace of the bundled CDK survived. Every entry is checked, not just
     * classes, and in both spellings: the remapper rewrites the dotted form in string constants as
     * well, and a resource naming the old package would fail at runtime rather than here.
     */
    private void verify(Path jar) throws IOException {
        List<String> offenders = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (entry.getName().startsWith(SHADOW)) {
                    offenders.add("entry " + entry.getName());
                    continue;
                }
                byte[] bytes;
                try (InputStream is = zip.getInputStream(entry)) {
                    bytes = is.readAllBytes();
                }
                if (containsShadowReference(bytes))
                    offenders.add("reference in " + entry.getName());
                if (offenders.size() > 20)
                    break;
            }
        }
        if (!offenders.isEmpty())
            throw new IllegalStateException("Bundled CDK survived the repack:\n  "
                    + String.join("\n  ", offenders));
        System.out.println("verified: no reference to " + SHADOW + " remains");
    }

    private static boolean containsShadowReference(byte[] bytes) {
        return contains(bytes, SHADOW) || contains(bytes, SHADOW.replace('/', '.'));
    }

    private static boolean contains(byte[] haystack, String text) {
        byte[] needle = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++)
                if (haystack[i + j] != needle[j]) continue outer;
            return true;
        }
        return false;
    }
}
