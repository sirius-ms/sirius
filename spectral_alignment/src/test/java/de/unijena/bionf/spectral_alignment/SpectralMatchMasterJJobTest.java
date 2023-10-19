package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SpectralMatchMasterJJobTest {

    @Test
    public void compute() {
        CosineQueryUtils utils = new CosineQueryUtils(new ModifiedCosine(new Deviation(10)));
        CosineQuerySpectrum s1 = utils.createQueryWithoutLoss(new SimpleSpectrum(new double[]{1d}, new double[]{1d}), 1d);
        CosineQuerySpectrum s2 = utils.createQueryWithoutLoss(new SimpleSpectrum(new double[] {1d, 2d}, new double[] {0.5d, 1d}), 3d);
        CosineQuerySpectrum s3 = utils.createQueryWithoutLoss(new SimpleSpectrum(new double[]{2d}, new double[]{1d}), 2d);

        List<SpectralSimilarity> expectedMatches = Arrays.asList(utils.cosineProduct(s1, s1), utils.cosineProduct(s1, s2), utils.cosineProduct(s1, s3));

        List<Pair<CosineQuerySpectrum, CosineQuerySpectrum>> queries = Arrays.asList(Pair.of(s1, s1), Pair.of(s1, s2), Pair.of(s1, s3));

        SpectralMatchMasterJJob job = new SpectralMatchMasterJJob(utils, queries);


        SiriusJobs.getGlobalJobManager().submitJob(job);
        assertEquals(expectedMatches, job.takeResult());
    }
}