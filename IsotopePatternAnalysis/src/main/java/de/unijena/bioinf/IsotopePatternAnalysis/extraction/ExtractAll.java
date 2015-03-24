package de.unijena.bioinf.IsotopePatternAnalysis.extraction;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MeasurementProfile;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.IsotopePatternAnalysis.IsotopePattern;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

public class ExtractAll implements PatternExtractor {
    @Override
    public List<IsotopePattern> extractPattern(MeasurementProfile profile, Spectrum<Peak> spectrum) {
        final SimpleMutableSpectrum byInt = new SimpleMutableSpectrum(spectrum);
        final SimpleSpectrum byMz = new SimpleSpectrum(spectrum);
        Spectrums.sortSpectrumByDescendingIntensity(byInt);
        final BitSet allreadyUsed = new BitSet(byInt.size());
        final Deviation window = getIsotopeDeviation(profile);
        final SimpleMutableSpectrum buffer = new SimpleMutableSpectrum();
        final ArrayList<IsotopePattern> candidates = new ArrayList<IsotopePattern>();
        for (int k=0; k < byInt.size(); ++k) {
            final int mzIndex = Spectrums.mostIntensivePeakWithin(byMz, byInt.getMzAt(k),  window);
            if (allreadyUsed.get(mzIndex)) continue;
            final double monomz = byMz.getMzAt(mzIndex);
            buffer.addPeak(byMz.getMzAt(mzIndex), byMz.getIntensityAt(mzIndex));
            int j=mzIndex+1;
            eachPatternPos:
            for (int f=1; f <= 10; ++f) {
                boolean found=false;
                for (; j < byMz.size(); ++j) {
                    final double mz = byMz.getMzAt(j);
                    final double expectedMass = monomz + f;
                    if (window.inErrorWindow(expectedMass, mz) && !allreadyUsed.get(j)) {
                        buffer.addPeak(byMz.getMzAt(j), byMz.getIntensityAt(j));
                        allreadyUsed.set(j);
                        found = true;
                    } else if (byMz.getMzAt(j) > (expectedMass+0.3)) {
                        if (found) continue eachPatternPos;
                        else break eachPatternPos;
                    }
                }
            }
            // check also positions before
            // TODO: very heuristically approach optimized for metabolomics (=small molecules, restricted set of elements)
            if (monomz > 1000) {
                j = mzIndex-1;
                eachPatternPos:
                for (int f=1; f <= 10; ++f) {
                    boolean found=false;
                    for (; j >= 0; --j) {
                        final double mz = byMz.getMzAt(j);
                        final double expectedMass = monomz - f;
                        if (window.inErrorWindow(expectedMass, mz) && !allreadyUsed.get(j) && byMz.getIntensityAt(j)/byMz.getIntensityAt(mzIndex) > 0.33) {
                            buffer.addPeak(byMz.getMzAt(j), byMz.getIntensityAt(j));
                            allreadyUsed.set(j);
                            found = true;
                        } else if (byMz.getMzAt(j) > (expectedMass+0.3)) {
                            if (found) continue eachPatternPos;
                            else break eachPatternPos;
                        }
                    }
                }
            }
            if (buffer.size() >= 2) {
                candidates.add(new IsotopePattern(new SimpleSpectrum(buffer)));
            }
            for (int x=buffer.size()-1; x >= 0; --x) buffer.removePeakAt(x);
        }
        return candidates;
    }

    @Override
    public List<IsotopePattern> extractPattern(MeasurementProfile profile, Spectrum<Peak> spectrum, double targetMz, boolean allowAdducts) {
        // TODO: implement in more efficient way
        final List<IsotopePattern> list = extractPattern(profile, spectrum);
        final PeriodicTable table = PeriodicTable.getInstance();
        final Iterator<IsotopePattern> iter = list.iterator();
        while (iter.hasNext()) {
            final IsotopePattern pattern = iter.next();
            final double mono = pattern.getMonoisotopicMass();
            // TODO: use adducts!
            if (mono-targetMz > 2e-3) {
                iter.remove();
            }
        }
        return list;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        // nothing
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        // nothing
    }

    public Deviation getIsotopeDeviation(MeasurementProfile profile) {
        final PeriodicTable pt = PeriodicTable.getInstance();
        double delta = 0d;
        final IsotopicDistribution distr = pt.getDistribution();
        for (Element e : profile.getFormulaConstraints().getChemicalAlphabet()) {
            Isotopes iso = distr.getIsotopesFor(e);
            for (int k=1; k < iso.getNumberOfIsotopes(); ++k) {
                final double diff = iso.getMass(k)-iso.getIntegerMass(k);
                delta = Math.max(delta, Math.abs(diff));
            }
        }
        return new Deviation(2*profile.getAllowedMassDeviation().getPpm(), 3*profile.getAllowedMassDeviation().getAbsolute()+delta);
    }
}
