
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

package de.unijena.bioinf.sirius;

import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.Annotated;
import de.unijena.bioinf.ms.annotations.AnnotatedWithDefaults;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ProcessedInput is the intermediate data structure for an MSMS experiment belonging to one compound. It contains a list of merged peaks, the
 * original input from which it is computed as well as the parent peak of the compound.
 *
 * Annotation-System:
 *  ProcessedInput can be extended by two types of annotations:
 *  - Peak Annotations are additional fields of the peaks
 *  - Annotations are additional fields of the processed input
 *  The annotation system is a alternative system to the common Class-based inheritance system. It is used for the
 *  following scenario:
 *  Multiple (probably independend) modules want to process data. They have to add additional fields to ProcessedInput
 *
 *  Please note the differences to inheritance:
 *  - java supports single-inheritance, so if Module A needs a new field x, you could create a subclass ProcessedInputA
 *    that extends ProcessedInput by the field x. If Module B needs a new field y, you can proceed analogously,
 *    But if you want to use modules A and B, you would have to create a new class for each possible combination.
 *  - The component based system instead can handle this case: ProcessedInput has a map of components. Module A adds
 *    a new slot into this list called A, module B adds a new slot B. Now both modules can read their fields from
 *    the same class
 *  - The component based system cannot handle inheritance: So don't let module A add a slot X and module B add a slot
 *    Y extends X. This may lead to confusing errors
 *
 *  Example:
 *    Module A needs a new field x. So create a class AExt with a field x. Then call
 *    processedInput.setAnnotation(AExt.class);
 *    Now you can access this field by
 *    processedInput.getAnnotation(AExt.class).getX()
 *
 *    For performance reasons you have to use a special PeakAnnotation class to access peak annotations.
 *    PeakAnnotation{@literal <}BExt{@literal >} ext = processedInput.addPeakAnnotation(BExt.class);
 *    ext.get(somePeak).getX();
 *
 *  Important notes:
 *    - annotation classes should have an parameter-free constructor
 *    - annotation classes should be final
 *
 *
 */
public class ProcessedInput implements Cloneable, AnnotatedWithDefaults<DataAnnotation> {

    private final Ms2Experiment originalExperiment;
    private MutableMs2Experiment experiment;
    private List<ProcessedPeak> mergedPeaks;
    private ProcessedPeak parentPeak;
    private HashMap<Class, PeakAnnotation> peakAnnotations;
    private Annotated.Annotations<DataAnnotation> annotations;

    @Override
    public Annotations<DataAnnotation> annotations() {
        return annotations;
    }

    public ProcessedInput(MutableMs2Experiment experiment, Ms2Experiment originalExperiment) {
        this.experiment = experiment;
        this.originalExperiment = originalExperiment;
        this.mergedPeaks = new ArrayList<>();
        this.annotations = new Annotations<>();
        setAnnotation(MsInstrumentation.class, experiment.getAnnotation(MsInstrumentation.class, () -> MsInstrumentation.Unknown));
        this.peakAnnotations = new HashMap<>();
        experiment.forEachAnnotation(this::setAnnotation);
    }

    public ProcessedInput(MutableMs2Experiment experiment, Ms2Experiment originalExperiment,
                          List<ProcessedPeak> mergedPeaks, ProcessedPeak parentPeak) {
        this.experiment = experiment;
        this.originalExperiment = originalExperiment;
        this.mergedPeaks = mergedPeaks;
        this.parentPeak = parentPeak;
        this.annotations = new Annotations<>();
       setAnnotation(MsInstrumentation.class, experiment.getAnnotation(MsInstrumentation.class, () -> MsInstrumentation.Unknown));
        this.peakAnnotations = new HashMap<>();
    }

    /*
    public ProcessedInput getRecalibratedVersion(SpectralRecalibration rec) {
        final ProcessedInput p = clone();
        // at this point we do not copy the MS experiment data. HOWEVER; this is somewhat dangerous as
        // it might introduce side effects. I just hope that nobody access this object at this stage of computation
        p.mergedPeaks = new ArrayList<>(mergedPeaks.size());
        p.peakAnnotations = (HashMap<Class, PeakAnnotation>) peakAnnotations.clone();
        p.annotations = annotations.clone();
        final PeakAnnotation<DecompositionList> dl = p.getPeakAnnotationOrThrow(DecompositionList.class);
        for (ProcessedPeak peak : mergedPeaks) {
            ProcessedPeak recalibrated = peak.recalibrate(rec);
            dl.set(recalibrated, new DecompositionList(new ArrayList<>()));
            p.mergedPeaks.add(recalibrated);
            if (peak==parentPeak)
                p.setParentPeak(recalibrated);
        }
        p.setAnnotation(SpectralRecalibration.class, rec);
        p.setAnnotation(Scoring.class,new Scoring());
        return p;
    }




    // we have to do this smarter -_-
    public ProcessedInput cloneForBeautification() {
        final ProcessedInput p = clone();
        p.annotations = annotations.clone();
        p.setAnnotation(Scoring.class, getAnnotation(Scoring.class, Scoring::new).clone());
        return p;
    }

    */

    @Override
    public ProcessedInput clone() {
        try {
            ProcessedInput p =  (ProcessedInput) super.clone();
            p.annotations = p.annotations.clone();
            p.mergedPeaks = new ArrayList<>();
            for (ProcessedPeak k : mergedPeaks) p.mergedPeaks.add(k.clone());
            p.setParentPeak(p.mergedPeaks.get(getParentPeak().getIndex()));
            return p;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public Ms2Experiment getOriginalInput() {
        return originalExperiment;
    }

    @SuppressWarnings("unchecked cast")
    public <T> PeakAnnotation<T> getPeakAnnotationOrThrow(Class<T> klass) {
        final PeakAnnotation<T> ano = peakAnnotations.get(klass);
        if (ano == null) throw new NullPointerException("No peak annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

   /* public Map<Class,Object> getAnnotations() {
        return Collections.unmodifiableMap(annotations);
    }
    */
   public Map<Class,PeakAnnotation> getPeakAnnotations() {
       return Collections.unmodifiableMap(peakAnnotations);
   }

    @SuppressWarnings("unchecked cast")
   /* public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T ano = (T)annotations.get(klass);
        if (ano == null) throw new NullPointerException("No annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    public <T> T getAnnotation(Class<T> klass, T defaultval) {
        final T ano = (T)annotations.get(klass);
        if (ano == null) return defaultval;
        return ano;
    }*/

    public <T> PeakAnnotation<T> addPeakAnnotation(Class<T> klass) {
        if (peakAnnotations.containsKey(klass)) throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        final PeakAnnotation<T> ano = new PeakAnnotation<T>(peakAnnotations.size(), klass);
        peakAnnotations.put(klass, ano);
        return ano;
    }



    /*public <T> boolean setAnnotation(Class<T> klass, T annotation) {
        return annotations.put(klass, annotation) == annotation;
    }*/

    @SuppressWarnings("unchecked cast")
    public <T> PeakAnnotation<T> getOrCreatePeakAnnotation(Class<T> klass) {
        if (peakAnnotations.containsKey(klass)) return peakAnnotations.get(klass);
        final PeakAnnotation<T> ano = new PeakAnnotation<T>(peakAnnotations.size(), klass);
        peakAnnotations.put(klass, ano);
        return ano;
    }

    public MutableMs2Experiment getExperimentInformation() {
        return experiment;
    }

    public void setExperimentInformation(Ms2Experiment exp) {
        if (exp instanceof MutableMs2Experiment) {
            this.experiment = (MutableMs2Experiment)exp;
        } else {
            this.experiment = new MutableMs2Experiment(exp);
        }
    }

    public List<ProcessedPeak> getMergedPeaks() {
        return mergedPeaks;
    }

    public void setMergedPeaks(List<ProcessedPeak> mergedPeaks) {
        this.mergedPeaks = mergedPeaks;
    }

    public ProcessedPeak getParentPeak() {
        return parentPeak;
    }

    public void setParentPeak(ProcessedPeak parentPeak) {
        this.parentPeak = parentPeak;
    }

    public void resetIndizes() {
        for (int k=0; k < mergedPeaks.size(); ++k) {
            mergedPeaks.get(k).setIndex(k);
        }
    }

    /**
     * Try to assign each node in the tree to exactly one peak in the spectrum. Update peakId information in
     * tree nodes to match the merged peaks
     */
    public void mapTreeToInput(FTree tree) {
        final Deviation dev = new Deviation(5);
        final ArrayList<ProcessedPeak> copy = new ArrayList<>(mergedPeaks);
        copy.sort(Comparator.comparingDouble(Peak::getMass));
        final Spectrum<ProcessedPeak> wrapper = Spectrums.getAlreadyOrderedSpectrum(Spectrums.wrap(copy));
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for (Fragment f : tree) {
            AnnotatedPeak peak = peakAno.get(f);
            int i = Spectrums.mostIntensivePeakWithin(wrapper, peak.getMass(), dev);
            if (i < 0) {
                if (!peak.isMeasured()) {
                    continue;
                } else {
                    // strange... repeat with higher mass dev
                    i = Spectrums.mostIntensivePeakWithin(wrapper, peak.getMass(), dev.multiply(2));
                    if (i < 0) {
                        LoggerFactory.getLogger(ProcessedInput.class).error("Cannot map fragment " + f.getFormula() + " with mass " + peak.getMass() + " to spectrum " + this);
                        continue;
                    }
                }
            }
            f.setPeakId(i);
        }

    }


}
