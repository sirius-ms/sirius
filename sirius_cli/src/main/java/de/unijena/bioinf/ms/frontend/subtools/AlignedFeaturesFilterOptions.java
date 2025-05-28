package de.unijena.bioinf.ms.frontend.subtools;

import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.storage.db.nosql.Filter;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This filter does not apply to the import. It filters for subsequent computations
 * Note: However, if this filter removes ALL features for computation, the features from peak list import won't be imported at all.
 *       If it only filters a subset, all features will be imported.
 */
public class AlignedFeaturesFilterOptions {

    @CommandLine.Option(names = "--mzmin", description = {"Minimal precursor mass."})
    private Double mzmin;

    @CommandLine.Option(names = "--mzmax", description = {"Maximal precursor mass."})
    private Double mzmax;

    @CommandLine.Option(names = "--rtmin", description = {"Minimal retention time."})
    private Double rtmin;

    @CommandLine.Option(names = "--rtmax", description = {"Maximal retention time."})
    private Double rtmax;

    @CommandLine.Option(names = "--hasmsms", description = {"Only features with MS/MS."})
    private Boolean hasMsMs;

    @CommandLine.Option(names = "--quality", description = {"Only features with given data quality. Valid values: ${COMPLETION-CANDIDATES}."}, split = ",")
    private DataQuality[] quality;

    public Filter createFilter() {
        List<Filter> filters = new ArrayList<>();
        if (mzmin != null) filters.add(Filter.where("averageMass").gte(mzmin));
        if (mzmax != null) filters.add(Filter.where("averageMass").lte(mzmax));
        if (rtmin != null) filters.add(Filter.where("retentionTime.middle").gte(rtmin));
        if (rtmax != null) filters.add(Filter.where("retentionTime.middle").lte(rtmax));
        if (hasMsMs != null) filters.add(Filter.where("hasMsMs").eq(true));
        if (quality != null && quality.length > 0) filters.add(Filter.where("dataQuality").in(Arrays.stream(quality).map(Enum::toString).toArray(String[]::new)));

        if (filters.size() == 1) {
            return filters.getFirst();
        } else {
            return Filter.and(filters.toArray(new Filter[0]));
        }
    }
}
