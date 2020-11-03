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

package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;

import java.util.Locale;

public class Report {

    protected PredictionPerformance[] performancePerClass;
    protected double meanF1, meanRecall, meanPrecision, accuracy, microF1,microF1Total, microRecall, microPrecision, mcc, microMCC;
    protected double fpr;
    protected int above50, above75, above90, largerClasses, smallerClasses, samples;


    public Report(PredictionPerformance[] ps) {
        this.performancePerClass = ps;
        PredictionPerformance.Modify micro = new PredictionPerformance().modify();
        final PredictionPerformance.Modify microAll = new PredictionPerformance().modify();
        above50=0; above75=0; above90=0;
        meanF1=0; meanRecall=0; meanPrecision=0; accuracy=0;
        smallerClasses = 0; largerClasses = 0; microF1 = 0d; microRecall = 0d; microPrecision = 0d; mcc=0d;
        int emptyClasses = 0;
        for (PredictionPerformance p : ps) {
            if (p.getCount() >= 20) {
                ++largerClasses;
                meanF1 += p.getF();
                mcc += p.getMcc();
                meanPrecision += p.getPrecision();
                meanRecall += p.getRecall();
                if (p.getF()>=0.5) {
                    ++above50;
                    if (p.getF() >= 0.75) {
                        ++above75;
                        if (p.getF() >= 0.9) {
                            ++above90;
                        }
                    }
                }
            } else {
                micro.update(p);
                ++smallerClasses;
                if (p.getCount()<=0) {
                    fpr += p.getFp();
                    ++emptyClasses;
                }
            }
            microAll.update(p);
            accuracy += p.getAccuracy();
        }
        fpr /= ps[0].numberOfSamples();
        PredictionPerformance microPerformance = micro.done();
        microF1 = microPerformance.getF();
        microF1Total = microAll.done().getF();
        microMCC = microPerformance.getMcc();
        microPrecision = microPerformance.getPrecision();
        microRecall = microPerformance.getRecall();
        meanF1 /= largerClasses;
        mcc /= largerClasses;
        meanPrecision /= largerClasses;
        meanRecall /= largerClasses;
        accuracy /= ps.length;
        samples = (int)performancePerClass[0].numberOfSamples();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%.4f micro F1, %d large classes with f1 = %.4f, recall = %.4f, precision = %.4f, MCC = %.4f, accuracy = %.4f. %d small classes with f1 = %.4f, recall = %.4f, precision = %.4f and MCC = %.4f. Number of categories with: f1 >=0.9 = %d, f1>=0.75 = %d, f1>=0.5 = %d., #classes = %d, #samples = %d, \t total false positives: %.4f, score = %.4f", microF1Total, largerClasses, meanF1, meanRecall, meanPrecision, mcc, accuracy, smallerClasses, microF1, microRecall, microPrecision, microMCC, above90, above75, above50, smallerClasses+largerClasses, samples,fpr, score());
    }

    public double score() {
        return mcc + microMCC + (1d-fpr);
    }
}
