package de.unijena.bioinf.chemdb.custom;

import de.unijena.bioinf.chemdb.DataSource;
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
 * An unknown database name is dropped from the list of databases to search. That is what left the reporter of
 * the public issue sirius-ms/sirius#314 with a spectra-search that produced nothing at all and no explanation,
 * so it has to be reported (#885) - but only once per name, since these lists are built per compound.
 */
public class UnknownDatabaseWarningTest {

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

    private List<String> warningMessages() {
        return warnings.stream().map(LogRecord::getMessage).toList();
    }

    @Test
    public void anUnknownNameIsReportedAndDropped() {
        List<CustomDataSources.Source> sources =
                CustomDataSources.getSourcesFromNames(List.of(DataSource.PUBCHEM.name(), "NOT_A_DATABASE"));

        assertEquals(1, sources.size(), "the unknown name is still dropped, only now it says so");
        assertEquals(DataSource.PUBCHEM.name(), sources.getFirst().name());

        assertEquals(1, warningMessages().size(), "exactly one warning: " + warningMessages());
        assertTrue(warningMessages().getFirst().contains("NOT_A_DATABASE"), warningMessages().getFirst());
    }

    /**
     * The lists are built once per compound, so an unreported repeat would mean thousands of identical lines.
     */
    @Test
    public void theSameUnknownNameIsReportedOnlyOnce() {
        for (int i = 0; i < 5; i++)
            CustomDataSources.getSourcesFromNames(List.of("ALSO_NOT_A_DATABASE"));

        assertEquals(1, warningMessages().size(), "should be deduplicated: " + warningMessages());
    }

    @Test
    public void knownNamesAreQuiet() {
        List<CustomDataSources.Source> sources =
                CustomDataSources.getSourcesFromNames(List.of(DataSource.PUBCHEM.name(), DataSource.CHEBI.name()));

        assertEquals(2, sources.size());
        assertTrue(warnings.isEmpty(), "nothing to warn about: " + warningMessages());
    }

    /**
     * Splitting an empty configuration value yields a blank entry; that is not a name anybody typed.
     */
    @Test
    public void blankEntriesAreNotReported() {
        CustomDataSources.getSourcesFromNames(List.of("", "   "));

        assertTrue(warnings.isEmpty(), "blank entries must stay quiet: " + warningMessages());
    }
}
