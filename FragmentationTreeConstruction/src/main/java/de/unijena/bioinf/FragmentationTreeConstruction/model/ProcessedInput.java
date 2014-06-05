package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import java.util.HashMap;
import java.util.List;

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
 *    processedInput.addAnnotation(AExt.class);
 *    Now you can access this field by
 *    processedInput.getAnnotation(AExt.class).getX()
 *
 *    For performance reasons you have to use a special PeakAnnotation class to access peak annotations.
 *    PeakAnnotation<BExt> ext = processedInput.addPeakAnnotation(BExt.class);
 *    ext.get(somePeak).getX();
 *
 *  Important notes:
 *    - annotation classes should have an parameter-free constructor
 *    - annotation classes should be final
 *
 *
 */
public class ProcessedInput {

    private final Ms2Experiment experiment, originalExperiment;
    private List<ProcessedPeak> mergedPeaks;
    private ProcessedPeak parentPeak;
    private HashMap<Class, PeakAnnotation> peakAnnotations;
    private HashMap<Class, Object> annotations;

    public ProcessedInput(Ms2Experiment experiment, Ms2Experiment originalExperiment,
                          List<ProcessedPeak> mergedPeaks, ProcessedPeak parentPeak) {
        this.experiment = experiment;
        this.originalExperiment = originalExperiment;
        this.mergedPeaks = mergedPeaks;
        this.parentPeak = parentPeak;
        this.annotations = new HashMap<Class, Object>();
        this.peakAnnotations = new HashMap<Class, PeakAnnotation>();
    }

    @SuppressWarnings("unchecked cast")
    public <T> PeakAnnotation<T> getPeakAnnotationOrThrow(Class<T> klass) {
        final PeakAnnotation<T> ano = peakAnnotations.get(klass);
        if (ano == null) throw new NullPointerException("No peak annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getAnnotationOrThrow(Class<T> klass) {
        final T ano = (T)annotations.get(klass);
        if (ano == null) throw new NullPointerException("No annotation '" + klass.getName() + "' in ProcessedInput");
        return ano;
    }

    public <T> PeakAnnotation<T> addPeakAnnotation(Class<T> klass) {
        if (peakAnnotations.containsKey(klass)) throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        final PeakAnnotation<T> ano = new PeakAnnotation<T>(peakAnnotations.size(), klass);
        peakAnnotations.put(klass, ano);
        return ano;
    }

    public <T> void addAnnotation(Class<T> klass, T annotation) {
        if (annotations.containsKey(klass)) throw new RuntimeException("Peak annotation '" + klass.getName() + "' is already present.");
        annotations.put(klass, annotation);
    }

    @SuppressWarnings("unchecked cast")
    public <T> PeakAnnotation<T> getOrCreatePeakAnnotation(Class<T> klass) {
        if (peakAnnotations.containsKey(klass)) return peakAnnotations.get(klass);
        final PeakAnnotation<T> ano = new PeakAnnotation<T>(peakAnnotations.size(), klass);
        peakAnnotations.put(klass, ano);
        return ano;
    }

    @SuppressWarnings("unchecked cast")
    public <T> T getOrCreateAnnotation(Class<T> klass) {
        if (annotations.containsKey(klass)) return (T)annotations.get(klass);
        try {
            final T obj = klass.newInstance();
            annotations.put(klass, obj);
            return obj;
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public Ms2Experiment getExperimentInformation() {
        return experiment;
    }

    public void setMergedPeaks(List<ProcessedPeak> mergedPeaks) {
        this.mergedPeaks = mergedPeaks;
    }

    public List<ProcessedPeak> getMergedPeaks() {
        return mergedPeaks;
    }

    public ProcessedPeak getParentPeak() {
        return parentPeak;
    }

    public void setParentPeak(ProcessedPeak parentPeak) {
        this.parentPeak = parentPeak;
    }
}
