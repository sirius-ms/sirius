package de.unijena.bioinf.ms;

import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.utils.DataQuality;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

class FeatureFilterTest extends CLITest {

    @Test
    void testMzRtFilters() throws IOException {
        AlignedFeatures af1 = AlignedFeatures.builder().alignedFeatureId(1L).averageMass(1.0).retentionTime(new RetentionTime(1d)).build();
        AlignedFeatures af2 = AlignedFeatures.builder().alignedFeatureId(2L).averageMass(2.0).retentionTime(new RetentionTime(2d)).build();
        AlignedFeatures af3 = AlignedFeatures.builder().alignedFeatureId(3L).averageMass(3.0).retentionTime(new RetentionTime(3d)).build();
        AlignedFeatures af4 = AlignedFeatures.builder().alignedFeatureId(4L).build();
        ps.getStorage().insertAll(List.of(af1, af2, af3, af4));
        ps.close();

        runAssert(expectedInstanceIds("2", "3"), "--mzmin=2");
        runAssert(expectedInstanceIds("2", "3"), "--rtmin=2");
        runAssert(expectedInstanceIds("1", "4"), "--mzmax=1");
        runAssert(expectedInstanceIds("3"), "--rtmin=2.5");
        runAssert(expectedInstanceIds("1", "2"), "--mzmin=1", "--mzmax=2");
        runAssert(expectedInstanceIds("1", "2", "3"), "--rtmin=1", "--mzmax=3");
        runAssert(expectedInstanceIds("3"), "--mzmin=3", "--mzmax=5");
        runAssert(expectedInstanceIds(), "--mzmin=4");
        runAssert(expectedInstanceIds(), "--mzmin=3", "--rtmax=1");
        runAssert(expectedInstanceIds("2"), "--rtmin=2", "--mzmax=2");
    }

    @Test
    void testQualityFilter() throws IOException {
        AlignedFeatures af1 = AlignedFeatures.builder().alignedFeatureId(1L).dataQuality(DataQuality.NOT_APPLICABLE).build();
        AlignedFeatures af2 = AlignedFeatures.builder().alignedFeatureId(2L).dataQuality(DataQuality.BAD).build();
        AlignedFeatures af3 = AlignedFeatures.builder().alignedFeatureId(3L).dataQuality(DataQuality.DECENT).build();
        ps.getStorage().insertAll(List.of(af1, af2, af3));
        ps.close();

        runAssert(expectedInstanceIds("1"), "--quality=NOT_APPLICABLE");
        runAssert(expectedInstanceIds(), "--quality=GOOD");
        runAssert(expectedInstanceIds("2", "3"), "--quality=BAD,DECENT");
        runAssert(expectedInstanceIds("2", "3"), "--quality=BAD", "--quality=DECENT");
    }

    @Test
    void testMsMsFilter() throws IOException {
        AlignedFeatures af1 = AlignedFeatures.builder().alignedFeatureId(1L).build();
        AlignedFeatures af2 = AlignedFeatures.builder().alignedFeatureId(2L).hasMsMs(true).build();
        ps.getStorage().insertAll(List.of(af1, af2));
        ps.close();

        runAssert(expectedInstanceIds("2"), "--hasmsms");
    }

}
