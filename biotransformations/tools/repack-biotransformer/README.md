# repack-biotransformer

Rebuilds the BioTransformer jar so it uses the CDK on the SIRIUS classpath instead of the copy it
bundles. Stand-alone: it is not part of the SIRIUS build and is run by hand when the upstream jar
changes.

## Why

The upstream jar carries CDK 2.7.1 relocated to `io.siriusms.shadow.bt.cdk`, but deliberately leaves
`org.openscience.cdk.interfaces` and `.tools` unrelocated so molecules can be handed between SIRIUS
and BioTransformer without conversion. That puts two CDK versions in one object graph: relocated
2.7.1 implementation classes bound to the project's 2.12 interfaces. Anything the interface gained
since 2.7.1 is then missing at runtime.

`IStereoElement.updateCarriers` is the one that bites. Every substrate with a tetrahedral stereo
centre fails with `AbstractMethodError`, in every metabolic transformation mode, and
`BioTransformerJJob` drops the result per substrate with only a log line - so a custom database
import silently yields no transformation products for chiral structures.

Removing the bundled CDK removes the split. It also drops ~7 MB and a few thousand duplicate
classes from the classpath.

## What it does

1. Drops every `io/siriusms/shadow/bt/cdk/**` entry. CDK 2.12 provides all but six of those classes
   (`AtomContainer2`, `silent.AtomContainer2`, the two `io.rdf` OWL classes and the two
   `libio.jena` classes), and nothing outside the shadow package references those six.
2. Rewrites every remaining reference - types, descriptors, signatures and string constants - from
   the shadow package back to `org.openscience.cdk`.
3. Drops `build.props`, `io-formats.set`, `qsar-descriptors.set` and `libio-cml-customizers.set`.
   These are CDK's own resources; cdk-core, cdk-ioformats and cdk-qsar ship their own, and the
   bundled `build.props` would make `CDK.getVersion()` report 2.7.1.
4. Adds `addBond(int,int,Order,Display)` to `ambit2.core.groups.SuppleAtomContainer`, delegating to
   the three-argument form. That class implements `IAtomContainer` directly rather than extending
   CDK's `AtomContainer`, so it does not inherit methods added to the interface later. It is the
   only class in the jar with such a gap.
5. Rewrites the 95 call sites of `InChIGeneratorFactory.getInChIGenerator(IAtomContainer)`, which
   was an instance method in CDK 2.7.1 and is static in 2.12, from `invokevirtual` to `invokestatic`.
6. Drops the libraries the SIRIUS classpath already provides - 3825 duplicate classes, whichever
   copy wins decided by classpath order. This is what broke CDK's own `CDKToBeam` before.
7. Removes the `ENVMICRO` biosystem from `biotransformer-config/config.json`. The EnviPath derived
   knowledge base was already absent from the jar this is built from (its data is CC BY-NC-SA, not
   LGPL), but the configuration still declared it and pointed at a directory that is not there.

Everything else - BioTransformer, Ambit2, Weka, the knowledge base and the models - is copied byte
for byte. The tool fails if any reference to the shadow package survives.

## Running it

```bash
./gradlew -p biotransformations/tools/repack-biotransformer repack
./gradlew -p biotransformations/tools/repack-biotransformer publishToMavenLocal
```

The upstream jar is resolved as a dependency (`-Pinput=<path>` overrides it). The result is
published as:

```
wishartlab.openscience.biotransformer:biotransformer-sirius-cdk-shared-min-no-envi-path:3.0.1-sirius1
```

| part | meaning |
|---|---|
| `cdk-shared` | nothing is relocated any more; BioTransformer uses the CDK the rest of SIRIUS uses |
| `min` | minimised, as the variant this is built from already was |
| `no-envi-path` | the EnviPath derived knowledge base is absent |
| `-sirius1` | our fork counter on top of the upstream version, so the fork is versioned without claiming to be an upstream release |

The version catalog points at these coordinates, and `mavenLocal()` is first in the SIRIUS
repository list, so a local publish is picked up while testing.

To publish it for everyone, into the same public registry the upstream jar comes from
(<https://gitlab.com/bright-giant/registry-public/-/packages/>):

```bash
./gradlew -p biotransformations/tools/repack-biotransformer \
          publishRepackedPublicationToGitLabCommonPublicRepository -PbgGitLabToken=<token>
```

The token is a GitLab personal or deploy token with `write_package_registry`; keep it in
`$GRADLE_USER_HOME/gradle.properties` as `bgGitLabToken`, never in the repository. In CI,
`CI_JOB_TOKEN` is used instead and no property is needed.

## Before this is more than a local spike

- **Publish the artifact properly.** It currently only exists in whoever ran this last person's
  `~/.m2`. It belongs next to the upstream jar in the GitLab package repository, otherwise the
  SIRIUS build does not resolve for anyone else.
- **Give it a real POM.** The publication carries the jar alone; the runtime dependencies (CDK,
  Weka, commons-csv, the Jackson 1 and HttpClient stack) are still declared by hand in
  `biotransformations/build.gradle`.
- **Re-run the golden matrix.** `./gradlew :biotransformations:acceptanceTest` compares against the
  reference recorded from upstream BioTransformer; see
  `biotransformations/src/test/resources/biotransformer/README.md`.

## Still bundled, unrelocated

The jar keeps Weka 3.8.1 (its `*.model` files are serialized Weka objects, so relocating it would
break them), Ambit2, and a long tail that nothing on the SIRIUS classpath currently collides with:
Jena, Jackson 1, HttpClient 4, Xerces/Xalan, commons-math 2, joda-time, protobuf, JUnit 4. Removing
those is a separate exercise from the CDK split.
