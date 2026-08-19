package de.unijena.bioinf.lcms.align;

import de.unijena.bioinf.lcms.LCMSStorageFactory;
import de.unijena.bioinf.lcms.trace.LCMSStorage;
import de.unijena.bioinf.lcms.trace.Rect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Merging two masses of interest may only remove the two it was given.
 * <p>
 * A uid is not a global identity: {@code MvBasedAlignmentStorage} builds it from the mass and a counter of its own, so
 * the same uid means different things in two storages. The right hand partner of an alignment comes from the sample's
 * storage and was never in the merge storage, so removing it there by uid can only either do nothing or delete an
 * unrelated mass of interest that happens to share the key - a real alignment silently lost.
 * <p>
 * Rare, because a collision needs the same mass to a thousandth of a Dalton and the same counter value, and that is
 * why it went unnoticed: measured on a real experiment it destroyed three of about thirty three thousand merges, and
 * which three depended on how the parallel alignment jobs interleaved.
 */
public class AlignmentStorageRemovalTest {

    private final List<LCMSStorageFactory> factories = new ArrayList<>();

    @AfterEach
    public void tearDown() {
        factories.forEach(LCMSStorageFactory::close);
    }

    private AlignmentStorage newStorage() throws java.io.IOException {
        final LCMSStorageFactory factory = LCMSStorage.temporaryStorage(null, false);
        factories.add(factory);
        return factory.createNewStorage().getAlignmentStorage();
    }

    private static MoI moiAt(double mz, double retentionTime, int sampleIdx) {
        return new MoI(new Rect(mz - 0.001, mz + 0.001, retentionTime - 1, retentionTime + 1, mz),
                (int) retentionTime, retentionTime, 1000f, sampleIdx);
    }

    @Test
    public void mergingDoesNotDeleteAnUnrelatedMassOfInterest() throws java.io.IOException {
        final AlignmentStorage merged = newStorage();
        final AlignmentStorage sample = newStorage();

        // Two storages count independently, so the first mass of interest added at a given mass gets the same uid in
        // both. That is the collision, constructed here rather than waited for.
        final MoI innocent = moiAt(400.1234, 60d, 0);
        merged.addMoI(innocent);
        final MoI right = moiAt(400.1234, 300d, 1);
        sample.addMoI(right);
        assertEquals(innocent.getUid(), right.getUid(),
                "precondition: the two storages handed out the same uid for the same mass");

        // an alignment somewhere else entirely, whose right hand partner is the sample's mass of interest
        final MoI left = moiAt(700.5678, 300d, 0);
        merged.addMoI(left);
        merged.mergeMoIs(AlignWithRecalibration.noRecalibration(), left, right);

        assertNotNull(merged.getMoI(innocent.getUid()),
                "the merge at 700.57 must not remove the unrelated mass of interest at 400.12");
        int survivors = 0;
        for (MoI m : merged) ++survivors;
        assertEquals(2, survivors, "the merged pair and the innocent one, and nothing deleted by accident");
    }

    /** the left hand partner does belong to this storage, so its old entry has to go */
    @Test
    public void mergingRemovesTheEntryItReplaces() throws java.io.IOException {
        final AlignmentStorage merged = newStorage();
        final AlignmentStorage sample = newStorage();
        final MoI left = moiAt(500.25, 100d, 0);
        merged.addMoI(left);
        final long leftUid = left.getUid();
        final MoI right = moiAt(500.2502, 101d, 1);
        sample.addMoI(right);

        merged.mergeMoIs(AlignWithRecalibration.noRecalibration(), left, right);

        assertNull(merged.getMoI(leftUid), "the left hand entry was replaced by the merge");
        int survivors = 0;
        for (MoI m : merged) ++survivors;
        assertEquals(1, survivors, "only the merged mass of interest is left");
    }
}
