package de.unijena.bioinf.ms.projectspace;

import com.google.gson.stream.JsonWriter;
import de.unijena.bioinf.ChemistryBase.fp.*;
import de.unijena.bioinf.canopus.Canopus;
import de.unijena.bioinf.fingerid.CanopusResult;
import de.unijena.bioinf.fingerid.FingerIdResult;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import gnu.trove.map.hash.TIntFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

import static de.unijena.bioinf.ms.projectspace.FingerIdResultSerializer.isFingerIdCompatible;

public class CanopusResultSerializer implements MetaDataSerializer, SummaryWriter {
    protected final Canopus canopus;

    public CanopusResultSerializer(Canopus canopus) {
        this.canopus = canopus;
    }

    protected Boolean readFingerprints = null;
    @Override
    public void read(@NotNull ExperimentResult expResult, @NotNull DirectoryReader reader, @NotNull Set<String> names) throws IOException {
        final DirectoryReader.ReadingEnvironment env = reader.env;
        final List<IdentificationResult> results = expResult.getResults();

        if (!env.list().contains(FingerIdLocations.CANOPUS_FINGERPRINT.directory)) return;

        // begin ugly
        if (readFingerprints == null) {
            reader.env.leaveDirectory();
            Map<String, String> versionInfo = reader.env.readKeyValueFile(FingerIdLocations.SIRIUS_VERSION_FILE.fileName());
            readFingerprints = isFingerIdCompatible(versionInfo.get("csi:fingerid"));
            reader.env.enterDirectory(expResult.getAnnotation(ExperimentDirectory.class).getDirectoryName());
        }
        // ugly end

        if (!readFingerprints) return;

        try {
            env.enterDirectory(FingerIdLocations.CANOPUS_FINGERPRINT.directory);
            for (IdentificationResult result : results) {
                try {
                    result.setAnnotation(CanopusResult.class, new CanopusResult(env.read(FingerIdLocations.CANOPUS_FINGERPRINT.fileName(result), w -> {
                        return new ProbabilityFingerprint(canopus.getCanopusMask(), new BufferedReader(w).lines().mapToDouble(Double::valueOf).toArray());
                    })));

                } catch (IllegalArgumentException e) {
                    LoggerFactory.getLogger(getClass()).warn("CanopusFingerprint version of the imported data is imcompatible with the current version. " +
                            "CanopusFingerprint has to be recomputed!", e);
                }
            }
        } finally {
            env.leaveDirectory();
        }


    }

    @Override
    public void write(@NotNull ExperimentResult input, @NotNull DirectoryWriter writer) throws IOException {
        final List<IdentificationResult> results = input.getResults();

        if (writer.isAllowed(FingerIdResult.CANDIDATE_LISTS) && hasCanopus(results)) {
            writer.env.enterDirectory(FingerIdLocations.CANOPUS_FINGERPRINT.directory);
            for (IdentificationResult result : results) {
                if (hasCanopusResult(result))
                    writeCanopus(result, result.getAnnotation(CanopusResult.class), writer);
            }
            writer.env.leaveDirectory();
        }
    }

    private void writeCanopus(final IdentificationResult result, final CanopusResult r, DirectoryWriter writer) throws IOException {
        writer.write(FingerIdLocations.CANOPUS_FINGERPRINT.fileName(result), w -> {
            for (FPIter iter : r.getCanopusFingerprint()) {
                w.write(String.format(Locale.US, "%.2f\n", iter.getProbability()));
            }
        });

    }

    private boolean hasCanopus(List<IdentificationResult> results) {
        return results.stream().anyMatch(this::hasCanopusResult);
    }

    private boolean hasCanopusResult(IdentificationResult result) {
        final CanopusResult cr = result.getAnnotation(CanopusResult.class);
        return (cr != null && cr.getCanopusFingerprint() != null);
    }



    /////////////////// summary stuff ////////////////////
    @Override
    public void writeSummary(Iterable<ExperimentResult> experiments, DirectoryWriter writer) {
        TIntFloatHashMap canopusSummary = new TIntFloatHashMap(2048, 0.75f, -1, -1);
        FingerprintVersion canopusVersion = null;

        for (ExperimentResult expResult : experiments) {
            final List<IdentificationResult> results = expResult.getResults();
            if (writer.isAllowed(FingerIdResult.CANDIDATE_LISTS) && hasCanopus(results)) {
                for (IdentificationResult result : results) {
                    if (hasCanopusResult(result)) {
                        final CanopusResult r = result.getAnnotation(CanopusResult.class);
                        if (canopusSummary.isEmpty()) {
                            for (FPIter iter : r.getCanopusFingerprint()) {
                                canopusSummary.adjustValue(iter.getIndex(), 0);
                            }
                            canopusVersion = r.getCanopusFingerprint().getFingerprintVersion();
                        }
                        for (FPIter iter : r.getCanopusFingerprint()) {
                            float prob = (float) iter.getProbability();
                            if (prob < 0.1f) prob = 0f;
                            if (prob > 0.9f) prob = 1f;
                            if (prob > 0f)
                                canopusSummary.adjustOrPutValue(iter.getIndex(), prob, prob);
                        }
                    }
                }

            }
        }

        try {
            final FingerprintVersion finalCanopusVersion = canopusVersion;
            final StringWriter string = new StringWriter(2048);
            JsonWriter jsonWriter = new JsonWriter(string);
            jsonWriter.beginObject();
            writer.write("summary_canopus.csv", w -> {
                w.write("index\tid\tname\texpectedFrequency\tdescription\n");
                final int[] indizes = canopusSummary.keys();
                Arrays.sort(indizes);
                int k = 0;
                for (int index : indizes) {
                    final ClassyfireProperty prop = (ClassyfireProperty) finalCanopusVersion.getMolecularProperty(index);
                    w.write(String.format(Locale.US, "%d\tCHEMONT:%07d\t%s\t%.1f\t%s\n", k, prop.getChemOntId(), prop.getName(), canopusSummary.get(index), prop.getDescription()));
                }
            });

            //todo reenable canopus html summary?
            /*writer.write("summary_canopus.html", w -> {
                final String html = FileUtils.read(FileUtils.resource(FingerIdResultWriter.class, "/sirius/canopus.html.gz"));
                html.replace("null;//{{INSERT JSON DATA HERE}}//", json);
            });*/

            //write index
            writeCanopusIndex(FingerIdLocations.CANOPUS_FINGERPRINT_INDEX.fileName(), canopusVersion, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void writeCanopusIndex(String filename, FingerprintVersion version, DirectoryWriter writer) throws IOException {
        if (version == null) return;
        writer.write(filename, (w) -> {
            final int[] indizes;
            if (version instanceof MaskedFingerprintVersion) {
                indizes = ((MaskedFingerprintVersion) version).allowedIndizes();
            } else {
                indizes = new int[version.size()];
                for (int i = 0; i < indizes.length; ++i) indizes[i] = i;
            }
            w.write("relativeIndex\tabsoluteIndex\tname\tchemontId\tdescription\n");
            int k = 0;
            for (int index : indizes) {
                final ClassyfireProperty prop = (ClassyfireProperty) version.getMolecularProperty(index);
                w.write(String.valueOf(k++));
                w.write('\t');
                w.write(String.valueOf(index));
                w.write('\t');
                w.write(prop.getName());
                w.write('\t');
                w.write(prop.getChemontIdentifier());
                w.write('\t');
                w.write(prop.getDescription().replaceAll("[\t\n]", " "));
                w.write('\n');
            }
        });
    }
}
