/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.FragmentationTreeConstruction.computation.filtering;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.Isotopes;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.MutableMs2Spectrum;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.FormulaSettings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;

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
    public MutableMs2Experiment process(MutableMs2Experiment experiment) {
        final PeriodicTable p = PeriodicTable.getInstance();
        final IsotopicDistribution dist = p.getDistribution();
        final Isotopes HIso = dist.getIsotopesFor(p.getByName("H"));
        final double[] minDists = new double[maxNumberOfIsotopePeaks];
        final double[] maxDists = new double[maxNumberOfIsotopePeaks];
        final double Hdiff = HIso.getMass(1)-HIso.getMass(0);
        for (int k=0; k < maxNumberOfIsotopePeaks; ++k) minDists[k] = maxDists[k] = Hdiff*(k+1);


        for (Element e : experiment.getAnnotation(FormulaSettings.class).getConstraints().getChemicalAlphabet().getElements()) {
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

        final List<MutableMs2Spectrum> newSpectras = new ArrayList<MutableMs2Spectrum>();

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
            newSpectras.add(new MutableMs2Spectrum(sms, spec.getPrecursorMz(), spec.getCollisionEnergy(), spec.getMsLevel()));
        }

        experiment.setMs2Spectra(newSpectras);
        return experiment;
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
