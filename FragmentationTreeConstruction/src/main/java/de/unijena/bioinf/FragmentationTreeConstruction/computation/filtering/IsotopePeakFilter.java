package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MS2Peak;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2ExperimentImpl;
import de.unijena.bioinf.FragmentationTreeConstruction.model.Ms2SpectrumImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Removes possible isotope peaks from input. A peak is considered as isotopic peak if it has the correct distance
 * to a previous peak and has low intensity (at least 25% of the previous peak)
 */
public class IsotopePeakFilter implements Preprocessor {

    private int maxNumberOfIsotopePeaks;

    public IsotopePeakFilter(int maxNumberOfIsotopePeaks) {
        this.maxNumberOfIsotopePeaks = maxNumberOfIsotopePeaks;
    }

    public IsotopePeakFilter() {
        this(3);
    }

    @Override
    public Ms2Experiment process(Ms2Experiment experiment) {
        final PeriodicTable p = PeriodicTable.getInstance();
        final IsotopicDistribution dist = p.getDistribution();
        final Isotopes HIso = dist.getIsotopesFor(p.getByName("H"));
        final double[] minDists = new double[maxNumberOfIsotopePeaks];
        final double[] maxDists = new double[maxNumberOfIsotopePeaks];
        final double Hdiff = HIso.getMass(1)-HIso.getMass(0);
        for (int k=0; k < maxNumberOfIsotopePeaks; ++k) minDists[k] = maxDists[k] = Hdiff*(k+1);

        for (Element e : experiment.getMeasurementProfile().getFormulaConstraints().getChemicalAlphabet().getElements()) {
            final Isotopes isotope = dist.getIsotopesFor(e);
            if (isotope != null && isotope.getNumberOfIsotopes()>1) {
                for (int k=1; k < Math.min(maxNumberOfIsotopePeaks+1, isotope.getNumberOfIsotopes()); ++k) {
                    final double distance = isotope.getMass(k)-isotope.getMass(0);
                    final int step = isotope.getIntegerMass(k)-isotope.getIntegerMass(0);
                    if (step<=0) throw new RuntimeException("Strange Isotope definition: +1 peak has same unit mass as +0 peak");
                    int repeats = 1;
                    for (int l=step; l <= maxNumberOfIsotopePeaks; l += step ) {
                        minDists[l-1] = Math.min(minDists[l-1], distance*repeats);
                        maxDists[l-1] = Math.max(maxDists[l-1], distance*repeats);
                        ++repeats;
                    }
                }
            }
        }

        final List<Ms2Spectrum<? extends Peak>> newSpectras = new ArrayList<Ms2Spectrum<? extends Peak>>();

        for (Ms2Spectrum<? extends Peak> spec : experiment.getMs2Spectra()) {
            final SimpleMutableSpectrum sms = new SimpleMutableSpectrum(spec);
            final SimpleMutableSpectrum byInt = new SimpleMutableSpectrum(sms);
            Spectrums.sortSpectrumByDescendingIntensity(byInt);
            Spectrums.sortSpectrumByMass(sms);
            for (int i=0; i < byInt.size(); ++i) {
                final Peak peak = byInt.getPeakAt(i);
                final int index = Spectrums.binarySearch(sms, peak.getMass());
                if (index >= 0) {
                    int toDelete=0;
                    for (int j=index+1; j < Math.min(index+1+maxNumberOfIsotopePeaks, sms.size()); ++j) {
                        final double distance = sms.getMzAt(j)-peak.getMass();
                        if (distance >= minDists[j-index-1] && distance < maxDists[j-index-1] && (sms.getIntensityAt(j)/peak.getIntensity()) <= 0.2d) {
                            ++toDelete;
                        } else break;
                    }
                    for (int n=0; n < toDelete; ++n) {
                        sms.removePeakAt(index+1);
                    }
                }
            }
            newSpectras.add(new Ms2SpectrumImpl(sms, spec.getCollisionEnergy(), spec.getPrecursorMz(), spec.getTotalIonCount()));
        }

        final Ms2ExperimentImpl impl = new Ms2ExperimentImpl(experiment);
        impl.setMs2Spectra(newSpectras);
        return impl;
    }

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        if (document.hasKeyInDictionary(dictionary, "maxNumberOfIsotopePeaks"))
            this.maxNumberOfIsotopePeaks = (int)document.getIntFromDictionary(dictionary, "maxNumberOfIsotopePeaks");
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary, "maxNumberOfIsotopePeaks", maxNumberOfIsotopePeaks);
    }
}
