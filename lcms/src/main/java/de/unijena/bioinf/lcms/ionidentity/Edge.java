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

package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.Statistics;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.lcms.ProcessedSample;
import de.unijena.bioinf.lcms.align.AlignedFeatures;
import de.unijena.bioinf.model.lcms.CorrelationGroup;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import de.unijena.bioinf.model.lcms.IonGroup;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.*;

class Edge {

    public double debugScoreIntra = 0f, debugScoreExtra = 0f;
    public CorrelationGroup[] correlationGroups;

    public Edge reverse() {
        final Edge e = new Edge(to,from,type,toType,fromType);
        e.cor = cor.invert();
        e.score = score;
        e.totalNumberOfCorrelatedPeaks = totalNumberOfCorrelatedPeaks;
        //
        e.debugScoreIntra = debugScoreIntra;
        e.debugScoreExtra = debugScoreExtra;
        e.correlationGroups = correlationGroups;
        return e;
    }

    public String description() {
        if (fromType!=null && toType!=null) {

            HashSet<ProcessedSample> commonSamples = new HashSet<>(from.getFeature().getFeatures().keySet());
            commonSamples.retainAll(to.getFeature().getFeatures().keySet());

            return String.format(Locale.US, "%s -> %s (%d peaks, %d samples, r = %.2f, score = %.2f)", fromType.toString(), toType.toString(), cor.getNumberOfCorrelatedPeaks(), commonSamples.size(), cor.getCorrelation(), score);
        } else return "";
    }

    @Override
    public String toString() {
        return description();
    }

    public int numberOfCommonSamples() {
        HashSet<ProcessedSample> commonSamples = new HashSet<>(from.getFeature().getFeatures().keySet());
        commonSamples.retainAll(to.getFeature().getFeatures().keySet());
        return commonSamples.size();
    }

    public float assignmentProbability() {
        return (float)(from.assignment.probability(fromType) * to.assignment.probability(toType));
    }

    public double[] calculateIntraSampleCorrelation() {
        return Arrays.stream(this.correlationGroups).mapToDouble(CorrelationGroup::getCorrelation).toArray();
    }

    enum Type {
        ADDUCT, INSOURCE, CORRELATED;
    }

    IonNode from, to;
    Type type;

    CorrelationGroup cor;
    float score;
    int totalNumberOfCorrelatedPeaks;

    protected PrecursorIonType fromType, toType;

    public Edge(IonNode from, IonNode to, Type type) {
        this(from,to,type,null,null);
    }
    public Edge(IonNode from, IonNode to, Type type,PrecursorIonType fromType, PrecursorIonType toType) {
        this.from = from;
        this.to = to;
        this.fromType = fromType;
        this.toType = toType;
        this.type = type;
    }

    public double deltaMz() {
        return from.mz - to.mz;
    }

    protected IonGroup[] getMatchingIons2() {
        return new IonGroup[]{
                new IonGroup(cor.getLeft(),cor.getLeftSegment(), Collections.emptyList()),
                new IonGroup(cor.getRight(),cor.getRightSegment(), Collections.emptyList()),
        };
    }

    protected double calculateEdgeScore(double intensityThreshold) {
        AlignedFeatures a = from.getFeature();
        AlignedFeatures b = to.getFeature();
        {
            // calculate intersample bla
            final double scoreLeft = interSampleCorrelation(a, to.getFeature().getMass(), intensityThreshold);
            final double scoreRight = interSampleCorrelation(b, from.getFeature().getMass(), intensityThreshold);

            return Math.max(scoreLeft, scoreRight);
        }

    }

    protected double[] calculateInterSampleCorrelation(double intensityThreshold) {

        AlignedFeatures a = from.getFeature();
        AlignedFeatures b = to.getFeature();

        // find samples which contains both features
        double maxIntens = Double.NEGATIVE_INFINITY;
        ProcessedSample bestSample = null;
        final TDoubleArrayList vecLeft = new TDoubleArrayList(), vecRight = new TDoubleArrayList();
        for (ProcessedSample sa : a.getFeatures().keySet()) {
            final FragmentedIon left = a.getFeatures().get(sa);
            final FragmentedIon right = b.getFeatures().get(sa);
            if (right!=null) {
                vecLeft.add(left.getIntensity());
                vecRight.add(right.getIntensity());
            }
        }
        if (vecLeft.size()<=2) return new double[]{0,vecLeft.size(),0,1};
        final double corr = Statistics.pearson(vecLeft.toArray(), vecRight.toArray());



        return new double[]{
                -Math.log(Math.max(0.01d, 1d- corr)) + Math.log(0.3d),
                vecLeft.size(),corr, ((1/(1+Math.exp(-vecLeft.size()/10d)))-0.5)*2*vecLeft.size() };
    }


    protected int evidencesIntra=0, evidencesInter=0;

    protected double interSampleCorrelation(AlignedFeatures a, double adductMass, double intensityThreshold) {
        final TDoubleArrayList left = new TDoubleArrayList(), right = new TDoubleArrayList();
        {
            final Deviation dev = new Deviation(20);
            final List<ProcessedSample> samples = new ArrayList<>(a.getFeatures().keySet());
            samples.sort(Comparator.comparingDouble(x->-a.getFeatures().get(x).getIntensity()));
            // take first 4 samples OR all samples above threshold
            Iterator<ProcessedSample> iter = samples.iterator();
            int k=0;
            boolean found=false;
            while (iter.hasNext()) {
                ProcessedSample s = iter.next();
                FragmentedIon ion = a.getFeatures().get(s);
                int i = Spectrums.mostIntensivePeakWithin(ion.getAdductSpectrum(), adductMass, dev);
                if (i>=0) found=true;
                left.add(ion.getIntensity());
                right.add(i >= 0 ? ion.getAdductSpectrum().getIntensityAt(i) : 0);
                ++k;
                if (k > 4 && ion.getIntensity() < intensityThreshold) {
                    break;
                }
            }
            if (!found || left.size()<=4) return 0d;

            final double correlation = Statistics.pearson(left.toArray(), right.toArray());
            if (left.size()>evidencesInter) evidencesInter=left.size(); // DEBUG
            return left.size() * (-Math.log(Math.max(0.01d, 1d-correlation)) + Math.log(1d-0.8d));

        }
    }

    protected IonGroup[] getMatchingIons() {

        AlignedFeatures a = from.getFeature();
        AlignedFeatures b = to.getFeature();

        // find samples which contains both features
        double maxIntens = Double.NEGATIVE_INFINITY;
        ProcessedSample bestSample = null;
        for (ProcessedSample sa : a.getFeatures().keySet()) {
            for (ProcessedSample sb : b.getFeatures().keySet()) {
                if (sa==sb) {
                    final double i = a.getFeatures().get(sa).getIntensity() * b.getFeatures().get(sb).getIntensity();
                    if (i > maxIntens) {
                        bestSample = sa;
                        maxIntens = i;
                    }
                }
            }
        }
        if (bestSample==null) {
            System.err.println("Strange correlation without common sample");
            return new IonGroup[0];
        } else {
            final IonGroup xa = a.getFeatures().get(bestSample);
            final IonGroup xb = b.getFeatures().get(bestSample);
            return new IonGroup[]{xa,xb};
        }

    }

}
