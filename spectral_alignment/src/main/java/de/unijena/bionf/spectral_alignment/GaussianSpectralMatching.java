/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bionf.spectral_alignment;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.OrderedSpectrum;

import java.util.BitSet;

/**
 * treat peaks as (unnormalized) Gaussians and score overlapping areas of PDFs. Each peak might score agains multiple peaks in the other spectrum.
 */
public class GaussianSpectralMatching extends AbstractSpectralMatching {

    public GaussianSpectralMatching(Deviation deviation) {
        super(deviation);
    }

    @Override
    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right, double precursorLeft, double precursorRight) {
        return scoreAllAgainstAll(left, right);
    }

    public SpectralSimilarity score(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        return scoreAllAgainstAll(left, right);
    }

    public SpectralSimilarity scoreAllAgainstAll(OrderedSpectrum<Peak> left, OrderedSpectrum<Peak> right) {
        final BitSet usedIndicesLeft = new BitSet();
        final BitSet usedIndicesRight = new BitSet();

        int i = 0, j = 0;
        double score = 0d;

        final int nl=left.size(), nr=right.size();
        while (i < nl && left.getMzAt(i) < 0.5d) ++i; //skip negative peaks of inversed spectra
        while (j < nr && right.getMzAt(j) < 0.5d) ++j;
        while (i < nl && j < nr) {
            Peak lp = left.getPeakAt(i);
            Peak rp = right.getPeakAt(j);
            final double difference = lp.getMass()- rp.getMass();
            final double allowedDifference = maxAllowedDifference(Math.min(lp.getMass(), rp.getMass()));

            if (Math.abs(difference) <= allowedDifference) {
                double matchScore = scorePeaks(lp,rp);
                score += matchScore;
                usedIndicesLeft.set(i);
                usedIndicesRight.set(j);
                for (int k=i+1; k < nl; ++k) {
                    Peak lp2 = left.getPeakAt(k);
                    final double difference2 = lp2.getMass()- rp.getMass();
                    if (Math.abs(difference2) <= allowedDifference) {
                        matchScore = scorePeaks(lp2,rp);
                        score += matchScore;
                        usedIndicesLeft.set(k);
                    } else break;
                }
                for (int l=j+1; l < nr; ++l) {
                    Peak rp2 = right.getPeakAt(l);
                    final double difference2 = lp.getMass()- rp2.getMass();
                    if (Math.abs(difference2) <= allowedDifference) {
                        matchScore = scorePeaks(lp,rp2);
                        score += matchScore;
                        usedIndicesRight.set(l);
                    } else break;
                }
                ++i; ++j;
            } else if (difference > 0) {
                ++j;

            } else {
                ++i;
            }
        }
        int matchedPeaks = Math.min(usedIndicesLeft.cardinality(), usedIndicesRight.cardinality());
        return  new SpectralSimilarity(score, matchedPeaks);

    }

    protected double scorePeaks(Peak lp, Peak rp) {
        //formula from Jebara: Probability Product Kernels. multiplied by intensities
        // (1/(4*pi*sigma**2))*exp(-(mu1-mu2)**2/(4*sigma**2))
        final double mzDiff = Math.abs(lp.getMass()-rp.getMass());

        final double variance = Math.pow(deviation.absoluteFor(Math.min(lp.getMass(), rp.getMass())),2);
//        final double variance = Math.pow(0.01,2); //todo same sigma for all?
        final double varianceTimes4 = 4*variance;
        final double constTerm = 1.0/(Math.PI*varianceTimes4);

        final double propOverlap = constTerm*Math.exp(-(mzDiff*mzDiff)/varianceTimes4);
        return (lp.getIntensity()*rp.getIntensity())*propOverlap;
    }


    protected double maxAllowedDifference(double mz) {
        //change to, say 3*dev, when using gaussians
        return deviation.absoluteFor(mz);
//        return 0.01;
    }
}
