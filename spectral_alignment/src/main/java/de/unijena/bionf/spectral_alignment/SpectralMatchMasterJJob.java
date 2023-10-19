package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.jjobs.BasicMasterJJob;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

/**
 * A JJob class for scheduling spectral alignment matches for given pairs of spectra
 */
public class SpectralMatchMasterJJob extends BasicMasterJJob<List<SpectralSimilarity>> {

    private CosineQueryUtils queryUtils;
    private List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> queries;

    public SpectralMatchMasterJJob(CosineQueryUtils queryUtils, List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> queries) {
        super(JobType.CPU);
        this.queryUtils = queryUtils;
        this.queries = queries;
    }

    @Override
    protected List<SpectralSimilarity> compute() throws Exception {
        List<SpectralMatchJJob> jobs = queries.stream().map(q -> new SpectralMatchJJob(queryUtils, q.getLeft(), q.getRight())).toList();

        queryUtils = null;
        queries = null;

        jobs.forEach(this::submitSubJob);
        return jobs.stream().map(SpectralMatchJJob::takeResult).toList();
    }
}
