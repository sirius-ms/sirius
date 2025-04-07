package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SpectraCache {
    @Getter
    private final @NotNull WebWithCustomDatabase chemDB;
    @Getter
    private final @NotNull List<CustomDataSources.Source> selectedDbs;
    private volatile Map<CustomDataSources.Source, List<MergedReferenceSpectrum>> mergedSpectra;

    public SpectraCache(@NotNull WebWithCustomDatabase chemDB) {
        this(chemDB, CustomDataSources.getAllSelectableDbs());
    }

    public SpectraCache(@NotNull WebWithCustomDatabase chemDB, @NotNull List<CustomDataSources.Source> selectedDbs) {
        this.chemDB = chemDB;
        this.selectedDbs = selectedDbs;
    }

    Map<CustomDataSources.Source, List<MergedReferenceSpectrum>> getAllMergedSpectra() throws IOException {
        if (mergedSpectra == null) {
            synchronized (this) {
                if (mergedSpectra == null)
                    mergedSpectra = Collections.unmodifiableMap(chemDB.getAllMergedSpectra(selectedDbs));
            }
        }
        return mergedSpectra;
    }
}
