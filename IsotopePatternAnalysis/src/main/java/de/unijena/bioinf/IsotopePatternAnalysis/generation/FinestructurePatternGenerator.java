package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * Created by kaidu on 07.12.2014.
 */
public class FinestructurePatternGenerator extends IsotopePatternGenerator {

    protected final CachedIsoTable cache;
    protected double resolution = 75000d;


    public FinestructurePatternGenerator(IsotopicDistribution distribution, Normalization mode) {
        super(distribution, mode);
        this.cache = new CachedIsoTable(distribution);
    }

    public FinestructurePatternGenerator() {
        super();
        this.cache = new CachedIsoTable(distribution);
    }

    public FinestructurePatternGenerator(Normalization mode) {
        super(mode);
        this.cache = new CachedIsoTable(distribution);
    }

    @Override
    public SimpleSpectrum simulatePattern(MolecularFormula formula, Ionization ionization) {
        final FineStructureMerger merger = new FineStructureMerger(resolution);
        //final SimpleSpectrum spectrum = merger.merge(new FinestructureGenerator(distribution, mode, cache).iteratorSumingUpTo(formula, ionization, 0.999d), ionization.addToMass(formula.getMass()));
        final SimpleSpectrum spectrum = merger.merge(new FinestructureGenerator(distribution, mode, cache).iterator(formula, ionization), ionization.addToMass(formula.getMass()));
        //final SimpleSpectrum spectrum = merger.merge(new FinestructureGenerator(distribution, mode, cache).iteratorWithIntensityThreshold(formula, ionization, 0.0001), ionization.addToMass(formula.getMass()));
        // cut spectrum to allow only maxNumber peaks
        if (spectrum.size() <= maximalNumberOfPeaks) return Spectrums.getNormalizedSpectrum(spectrum, mode);
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(spectrum);
        for (int k = spec.size() - 1; k >= maximalNumberOfPeaks; --k) {
            spec.removePeakAt(k);
        }
        Spectrums.normalize(spec, mode);
        return new SimpleSpectrum(spec);
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }


}
