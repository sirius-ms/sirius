package de.unijena.bioinf.chemdb.annotations;

import de.unijena.bioinf.chemdb.DataSource;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code makeDB} is the path every {@code --db} list from the CLI and the config takes - FormulaSearchDB,
 * StructureSearchDB and SpectralSearchDB all go through it. It used to resolve names itself and drop the
 * unknown ones silently, which is how a typo produced empty results and no explanation (#885, and the public
 * sirius-ms/sirius#314).
 */
public class SearchableDBAnnotationTest {

    private final List<LogRecord> warnings = new ArrayList<>();
    private Handler handler;
    private Logger logger;

    @BeforeEach
    public void captureWarnings() {
        handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue())
                    warnings.add(record);
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger = Logger.getLogger(CustomDataSources.class.getName());
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }

    @AfterEach
    public void releaseHandler() {
        logger.removeHandler(handler);
    }

    private List<String> messages() {
        return warnings.stream().map(LogRecord::getMessage).toList();
    }

    /** the case from the review: a wrong name in the CLI must not pass unremarked */
    @Test
    public void anUnknownNameInADbListIsReported() {
        List<CustomDataSources.Source> dbs = SearchableDBAnnotation.makeDB(
                DataSource.PUBCHEM.name() + ",NOT_A_DATABASE");

        assertEquals(1, dbs.size(), "the unknown name is still dropped, only now it says so");
        assertEquals(1, messages().size(), "expected exactly one warning: " + messages());
        assertTrue(messages().getFirst().contains("NOT_A_DATABASE"), messages().getFirst());
    }

    @Test
    public void aSingleUnknownNameIsReported() {
        List<CustomDataSources.Source> dbs = SearchableDBAnnotation.makeDB("STILL_NOT_A_DATABASE");

        assertTrue(dbs.isEmpty(), "nothing resolved - which is exactly the silent case that was reported");
        assertEquals(1, messages().size(), "expected exactly one warning: " + messages());
    }

    @Test
    public void knownNamesAreQuiet() {
        List<CustomDataSources.Source> dbs = SearchableDBAnnotation.makeDB(
                DataSource.PUBCHEM.name() + "," + DataSource.CHEBI.name());

        assertEquals(2, dbs.size());
        assertTrue(warnings.isEmpty(), "nothing to warn about: " + messages());
    }

    @Test
    public void allStillResolvesToEverySelectableDb() {
        assertEquals(CustomDataSources.getAllSelectableDbs().size(),
                SearchableDBAnnotation.makeDB(DataSource.ALL.name()).size());
        assertTrue(warnings.isEmpty(), "the ALL shortcut names no database: " + messages());
    }

    /** the list is comma separated with optional spaces, and duplicates collapse */
    @Test
    public void spacingAndDuplicatesAreUnchanged() {
        List<CustomDataSources.Source> dbs = SearchableDBAnnotation.makeDB(
                " " + DataSource.PUBCHEM.name() + " , " + DataSource.PUBCHEM.name() + " ");

        assertEquals(1, dbs.size(), "a repeated name collapses");
        assertTrue(warnings.isEmpty(), "spacing must not turn a valid name into an unknown one: " + messages());
    }
}
