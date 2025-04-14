package de.unijena.bioinf.ms.frontend.subtools.spectra_search;

import de.unijena.bioinf.chemdb.WebWithCustomDatabase;
import de.unijena.bioinf.chemdb.custom.CustomDataSources;
import de.unijena.bioinf.spectraldb.entities.MergedReferenceSpectrum;
import it.unimi.dsi.fastutil.Pair;
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
    private volatile Pair<Map<CustomDataSources.Source, List<MergedReferenceSpectrum>>, Map<CustomDataSources.Source, List<MergedReferenceSpectrum>>> mergedSpectra;

    public SpectraCache(@NotNull WebWithCustomDatabase chemDB) {
        this(chemDB, CustomDataSources.getAllSelectableDbs());
    }

    public SpectraCache(@NotNull WebWithCustomDatabase chemDB, @NotNull List<CustomDataSources.Source> selectedDbs) {
        this.chemDB = chemDB;
        this.selectedDbs = selectedDbs;
    }

    Map<CustomDataSources.Source, List<MergedReferenceSpectrum>> getAllMergedSpectra(int ionmode) throws IOException {
        if (mergedSpectra == null) {
            synchronized (this) {
                if (mergedSpectra == null){
                    Pair<Map<CustomDataSources.Source, List<MergedReferenceSpectrum>>, Map<CustomDataSources.Source, List<MergedReferenceSpectrum>>> tmppair =
                            chemDB.getAllMergedSpectra(selectedDbs);
                    mergedSpectra = Pair.of(Collections.unmodifiableMap(tmppair.left()), Collections.unmodifiableMap(tmppair.right()));
                }
            }
        }
        return ionmode > 0 ? mergedSpectra.left() : mergedSpectra.right();
    }
}
