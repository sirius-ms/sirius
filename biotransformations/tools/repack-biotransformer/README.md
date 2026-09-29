# repack-biotransformer

Builds the BioTransformer jar that `:biotransformations` depends on. Stand-alone: it is not part of
the SIRIUS build and is run by hand when its input changes.

## Input and output

| | |
|---|---|
| input | `wishartlab.openscience.biotransformer:biotransformer-sirius-mod-relocated-min-no-envi-path:3.0.0` |
| output | `wishartlab.openscience.biotransformer:biotransformer-sirius-cdk-shared-min-no-envi-path:3.0.1-sirius3` |

Both are our own builds, not releases by the BioTransformer authors. Upstream BioTransformer is
<https://bitbucket.org/wishartlab/biotransformer3.0jar> (LGPL-3), which ships a single fat jar; the
input above is an earlier repackaging of it that relocated CDK into `io.siriusms.shadow.bt.cdk` and
removed the EnviPath derived knowledge base.

The output name records what the jar contains:

| part | meaning |
|---|---|
| `cdk-shared` | no CDK inside; BioTransformer uses the CDK on the SIRIUS classpath |
| `min` | minimised, as its input already was |
| `no-envi-path` | no EnviPath derived knowledge base (that data is CC BY-NC-SA, not LGPL) |
| `-siriusN` | fork counter, so our builds are versioned without claiming to be upstream releases |

Result: 58 MB in, 34 MB out.

## What it does

1. **Drops the bundled CDK** — every `io/siriusms/shadow/bt/cdk/**` entry. CDK 2.12 provides all but
   six of those classes (`AtomContainer2`, `silent.AtomContainer2`, the two `io.rdf` OWL classes and
   the two `libio.jena` classes), and nothing outside the shadow package references those six.
2. **Rewrites the references** left behind — types, descriptors, signatures and string constants —
   from the shadow package to `org.openscience.cdk`.
3. **Drops CDK's own resources**: `build.props`, `io-formats.set`, `qsar-descriptors.set` and
   `libio-cml-customizers.set`. cdk-core, cdk-ioformats and cdk-qsar ship their own, and the bundled
   `build.props` would make `CDK.getVersion()` report 2.7.1.
4. **Drops the libraries the SIRIUS classpath provides** — 3825 duplicate classes, all of them CDK
   transitive dependencies or unreachable from BioTransformer: beam, guava, commons-math 2, cmlxom,
   euclid, xom, joda-time, freehep, vecmath, signatures, tagsoup, jama and xml-apis. Leaving them in
   means classpath order decides which copy of each class wins, and CDK needs the newer beam.
5. **Rewrites `InChIGeneratorFactory.getInChIGenerator(IAtomContainer)`** at its 95 call sites from
   `invokevirtual` to `invokestatic`. It is an instance method in CDK 2.7.1 and static in 2.12.
6. **Adds `addBond(int,int,Order,Display)`** to `ambit2.core.groups.SuppleAtomContainer`, delegating
   to the three-argument form. That class implements `IAtomContainer` directly instead of extending
   CDK's `AtomContainer`, so it does not inherit methods added to the interface later. It is the only
   class in the jar with such a gap.
7. **Removes the `ENVMICRO` biosystem** from `biotransformer-config/config.json`. Its knowledge base
   is not in the jar, so the declaration pointed at a directory that does not exist.
8. **Drops what nothing reaches** — measured by walking the reference graph from
   `:biotransformations` through the jar, these have zero reachable classes: the Xerces/Xalan/BCEL
   XML stack (the JDK's JAXP is used), netlib and MTJ, protobuf, JUnit 4 and hamcrest, opencsv,
   commons-cli, j_paine, BioTransformer's own `executable/` entry points, and the `randomThings/`,
   `compareDataset/` and `ForUNC/` scratch classes. Also the IDE and build leftovers (`.classpath`,
   `.project`, the `.iml` files, `pom.xml`, `manifest.txt`) and log4j configuration for a framework
   that is not on the classpath - including a root `log4j.properties` setting `rootLogger=OFF`.

Everything else — BioTransformer, Ambit2, Weka, the knowledge base and the models — is copied byte
for byte, and repeated runs produce an identical jar.

The tool fails rather than producing a questionable artifact: if any reference to the shadow package
survives, if `SuppleAtomContainer` has no three-argument `addBond` to delegate to, or if the edited
`config.json` still mentions `ENVMICRO` or no longer has balanced braces.

## Running it

```bash
./gradlew -p biotransformations/tools/repack-biotransformer repack
./gradlew -p biotransformations/tools/repack-biotransformer publishToMavenLocal
```

The input jar is resolved as a dependency; `-Pinput=<path>` overrides it. `mavenLocal()` is first in
the SIRIUS repository list, so a local publish is picked up while testing.

To publish for everyone, into the registry the input jar also comes from
(<https://gitlab.com/bright-giant/registry-public/-/packages/>):

```bash
./gradlew -p biotransformations/tools/repack-biotransformer \
          publishRepackedPublicationToGitLabPublicRepository
```

Credentials come from the same properties the rest of the build uses, so nothing extra is needed:
`io.sirius-ms.build.artifactory.public.url` for the target, and the `public.username`/`public.token`
pair if set, otherwise `io.sirius-ms.build.artifactory.username`/`.token`. In CI, `CI_JOB_TOKEN` and
`PUBLIC_REGISTRY_URL` are used instead.

## Verifying a rebuild

`./gradlew :biotransformations:test` covers it in seconds; `./gradlew :biotransformations:acceptanceTest`
compares the full matrix against results recorded from upstream BioTransformer. See
`biotransformations/src/test/resources/biotransformer/README.md`.

## Still bundled

Weka 3.8.1 (including its GUI), Ambit2, Jena, Jackson 1, HttpClient 4 and unirest, `org.json`,
commons-csv and the CYP/UGT/SULF models.

Two of those look droppable by name but are not:

- **Weka's GUI** (4.7 MB). `weka.core.Utils`, `LogHandler` and `WekaPackageManager` all reference
  `weka.gui`, and Weka loads classes reflectively while deserialising models, so static reachability
  understates what it needs.
- **Jena** (1.5 MB). Seven of its classes stay alive through `ChemStructureFingerprinter`'s use of
  `jena.util.FileUtils`, and the fingerprinter is live code.

The POM declares what the jar needs at runtime - CDK, guava, joda-time, jgrapht, vecmath, slf4j,
Jackson 2 and the commons libraries - so `:biotransformations` does not repeat them. The list is
derived from the jar's bytecode: every type it references that is neither inside it nor in the JDK.
Weka, Ambit2, Jena, Jackson 1 and HttpClient are absent from it because they are still bundled.

One thing the POM cannot express: the jar does **not** contain
`biotransformer.btransformers.Biotransformer`. That class is maintained as source in
`:biotransformations`, so the jar only works with that module on the classpath.
