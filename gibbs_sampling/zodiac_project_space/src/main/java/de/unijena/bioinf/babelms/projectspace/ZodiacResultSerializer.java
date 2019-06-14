package de.unijena.bioinf.babelms.projectspace;

import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.GibbsSampling.ZodiacScore;
import de.unijena.bioinf.sirius.ExperimentResult;
import de.unijena.bioinf.sirius.IdentificationResult;
import de.unijena.bioinf.sirius.IdentificationResults;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.unijena.bioinf.babelms.projectspace.SiriusLocations.makeFileName;

public class ZodiacResultSerializer implements MetaDataSerializer {
    @Override
    public void read(@NotNull ExperimentResult input, @NotNull DirectoryReader reader, @NotNull Set<String> names) throws IOException {
        if (names.contains(ZodiacLocations.ZODIAC_SUMMARY.directory)) {
            if (input.hasAnnotation(IdentificationResults.class)) {
                try {
                    reader.env.enterDirectory(ZodiacLocations.ZODIAC_SUMMARY.directory);

                    final IdentificationResults results = input.getAnnotation(IdentificationResults.class);
                    reader.env.read(ZodiacLocations.ZODIAC_SUMMARY.fileName(), r -> {
                        BufferedReader buffReader = FileUtils.ensureBuffering(r);
                        Map<String, String> keys = buffReader.lines().map(it -> it.split("\t")).collect(Collectors.toConcurrentMap(it -> it[0], it -> it[1]));
                        for (IdentificationResult result : results) {
                            final String key = makeFileName(result);
                            final String value = keys.get(key);
                            try {
                                if (value != null)
                                    result.setAnnotation(ZodiacScore.class, new ZodiacScore(Double.valueOf(value)));
                                else throw new NumberFormatException("Value for Zodiac Score is NULL!");
                            } catch (NumberFormatException e) {
                                LoggerFactory.getLogger(getClass()).error("Could not parse Zodiac Score for key \"" + key + "\" from value \"" + value + "\". Skipping!.");
                            }
                        }
                        return results;
                    });
                } finally {
                    reader.env.leaveDirectory();
                }
            }
        }
    }

    @Override
    public void write(@NotNull ExperimentResult input, @NotNull DirectoryWriter writer) throws IOException {
        if (input.hasAnnotation(IdentificationResults.class)) {
            IdentificationResults idRes = input.getAnnotation(IdentificationResults.class);
            //writer zodiac score
            //check for any zodiac score
            if (idRes.stream().anyMatch(it -> it.hasAnnotation(ZodiacScore.class))) {
                try {
                    writer.env.enterDirectory(ZodiacLocations.ZODIAC_SUMMARY.directory);
                    // write zodiac score file
                    writer.write(ZodiacLocations.ZODIAC_SUMMARY.fileName(),
                            w -> {
                                w.write("Formula Candidate\tzodiacScore");
                                for (IdentificationResult result : idRes) {
                                    w.write('\n');
                                    w.write(makeFileName(result) + "\t" + result.getAnnotation(ZodiacScore.class, () -> ZodiacScore.NaN).score);
                                }
                            }
                    );
                    // todo write network into zodiac folder
                } finally {
                    writer.env.leaveDirectory();
                }
            }
        }
    }
}
