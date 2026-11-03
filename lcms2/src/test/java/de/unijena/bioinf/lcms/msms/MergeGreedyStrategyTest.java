package de.unijena.bioinf.lcms.msms;

import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.IsolationWindow;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.spectrum.Ms2SpectrumHeader;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Which spectra of one feature are merged into one consensus, and which are kept apart.
 * <p>
 * The merge is greedy: the highest scoring spectrum seeds a consensus and only spectra that agree with it are folded
 * in, which is how a chimeric outlier is kept out. That only works between spectra that are comparable in the first
 * place, so they are partitioned before it runs.
 */
public class MergeGreedyStrategyTest {

    private static final double PRECURSOR = 400.2;
    private static final SimpleSpectrum MS1 =
            new SimpleSpectrum(new double[]{PRECURSOR}, new double[]{1_000_000d});

    private static Ms2SpectrumHeader header(int uid, CollisionEnergy ce, boolean dataIndependent) {
        final Ms2SpectrumHeader h = new Ms2SpectrumHeader(uid, uid, "scan=" + uid, 1, 2, true, ce,
                new IsolationWindow(0d, dataIndependent ? 25d : 1d), uid, PRECURSOR, PRECURSOR, 100d);
        return dataIndependent ? h.asDataIndependent() : h;
    }

    private static MsMsQuerySpectrum query(int uid, CollisionEnergy ce, boolean dia, double[] mz, double[] in) {
        return new MsMsQuerySpectrum(header(uid, ce, dia), 0, new SimpleSpectrum(mz, in), MS1);
    }

    private static final double[] PEAKS = {100.05, 150.08, 200.11, 250.14, 300.17};
    private static final double[] INTENSITY = {100, 80, 60, 40, 20};

    @Test
    public void spectraOfOneAcquisitionAndEnergyBecomeOneConsensus() {
        final MergedSpectrum[] merged = MergeGreedyStrategy.groupByCeAndMerge(List.of(
                query(1, new CollisionEnergy(30), false, PEAKS, INTENSITY),
                query(2, new CollisionEnergy(30), false, PEAKS, INTENSITY)));
        assertEquals(1, merged.length);
        assertEquals(2, merged[0].getHeaders().length, "both agree, so both are in it");
    }

    @Test
    public void differentCollisionEnergiesAreKeptApart() {
        final MergedSpectrum[] merged = MergeGreedyStrategy.groupByCeAndMerge(List.of(
                query(1, new CollisionEnergy(30), false, PEAKS, INTENSITY),
                query(2, new CollisionEnergy(50), false, PEAKS, INTENSITY)));
        assertEquals(2, merged.length, "the same fragments at another energy are another measurement");
    }

    /**
     * A computed spectrum and a measured one are not the same kind of thing, so they may not compete: the peak
     * heights of the first are fitted shares and of the second recorded intensity, and whichever kind is in the
     * majority would otherwise outvote the other and discard it.
     */
    @Test
    public void aMeasuredSpectrumIsNotMergedWithComputedOnes() {
        final MergedSpectrum[] merged = MergeGreedyStrategy.groupByCeAndMerge(List.of(
                query(1, new CollisionEnergy(30), true, PEAKS, INTENSITY),
                query(2, new CollisionEnergy(30), true, PEAKS, INTENSITY),
                query(3, new CollisionEnergy(30), false, PEAKS, INTENSITY)));
        assertEquals(2, merged.length);
        final MergedSpectrum dia = merged[0].isDataIndependent() ? merged[0] : merged[1];
        final MergedSpectrum dda = merged[0].isDataIndependent() ? merged[1] : merged[0];
        assertEquals(2, dia.getHeaders().length, "the two computed spectra still merge with each other");
        assertEquals(1, dda.getHeaders().length, "and the measured one survives on its own");
        assertFalse(dda.isDataIndependent(), "a spectrum of measured peaks may not be marked as computed");
    }

    /** the outlier filter still does its job inside one kind */
    @Test
    public void aDisagreeingSpectrumOfTheSameKindIsStillDropped() {
        final MergedSpectrum[] merged = MergeGreedyStrategy.groupByCeAndMerge(List.of(
                query(1, new CollisionEnergy(30), false, PEAKS, INTENSITY),
                query(2, new CollisionEnergy(30), false,
                        new double[]{111.1, 222.2, 333.3, 444.4}, new double[]{100, 80, 60, 40})));
        assertEquals(1, merged.length);
        assertEquals(1, merged[0].getHeaders().length, "the outlier is not in the consensus");
    }

    @Test
    public void spectraWithoutACollisionEnergyAreAlsoPartitionedByAcquisition() {
        final MergedSpectrum[] merged = MergeGreedyStrategy.groupByCeAndMerge(List.of(
                query(1, null, true, PEAKS, INTENSITY),
                query(2, null, false, PEAKS, INTENSITY)));
        assertEquals(2, merged.length);
    }
}
