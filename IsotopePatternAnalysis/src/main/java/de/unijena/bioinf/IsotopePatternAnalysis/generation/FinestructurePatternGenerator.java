package de.unijena.bioinf.IsotopePatternAnalysis.generation;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

/**
 * Created by kaidu on 07.12.2014.
 */
public class FinestructurePatternGenerator extends IsotopePatternGenerator {

    protected final CachedIsoTable cache;
    protected double resolution = 7500d;


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
        final SimpleSpectrum spectrum = merger.merge(new FinestructureGenerator(distribution, mode, cache).iteratorSumingUpTo(formula, ionization, 1.0d - minimalProbabilityThreshold), ionization.addToMass(formula.getMass()));
        return Spectrums.getNormalizedSpectrum(spectrum, mode);
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        this.resolution = resolution;
    }


}
