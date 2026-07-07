package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ms.annotations.Ms2ExperimentAnnotation;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DetectedElements are an input annotation that stems from different sources, in particular:
 * - user specified
 * - MS1 isotope pattern detection
 *
 * In the InputProcessor (when transforming Ms2Experiment -> ProcessedInput) the annotation is transformed
 * into two annotations:
 *  - FormulaConstraints defines which elements are allowed in de novo decomposition
 *  - ElementsPredictedAsBeingAbsent defines which elements are forbidden even when performing database search or bottom up search
 *  From that point, only these two annotations are used during tree computation. So this annotation here should be
 *  solely added to Ms2Experiment objects.
 */
public final class DetectedElements implements Ms2ExperimentAnnotation, Cloneable {

    /*
     * SPECIAL RULES
     * --------------
     * I add all special rules here, even though they are wildly applied in the code, such that it is easier
     * to find them (and we have an overview about all extra rules for element detection which, otherwise, would
     * be deeply hidden in the code)
     */
    public static void patternsWithLessThan3PeaksAlwaysIncludeSulfur(float[] probs, int patternLen, int sulfurIndex) {
        if (patternLen<3) probs[sulfurIndex] = Math.max(probs[sulfurIndex], 0f);
    }
    public static boolean pfasNotDetectedDoesNotMeanFluorIsNotDetected(float pfasLogit) {
        if (pfasLogit < NEGATIVE_THRESHOLD) return false; // if we predict pfas as absent, do not include it
        if (pfasLogit < POSITIVE_THRESHOLD) return false; // if we do not know if pfas is present or not, treat its logit as 0.0
        return true;
    }





    public record DetectionResult(FormulaConstraints constraints, Set<Element> forbiddenElements) {

    }

    /*
     * Thresholds for which we assume that an element is present or absent. Everything in between means we neither forbid
     * nor allow the element (=> it is left out in de novo prediction but can be used for bottom up or database)
     */
    public final static float POSITIVE_THRESHOLD = -0.7f; // corresponds to ~33%
    public final static float NEGATIVE_THRESHOLD = -3f; // corresponds to ~5%

    /*
    I changed the source from "time where it happens" to "what input data the prediction used"
    the reason: we might at some point decide to move the element detection from ms1preprocessor to the
    preprocessing or input or somewhere else. To be legacy compatible, it would be good if we "remember" if an
    element detection from isotope pattern was already performed (no matter from where).
     */
    public enum Source {
        USER_SPECIFIED(true),
        HOMOLOGUE_SERIES(false),
        ISOTOPE_PATTERN_DETECTION(false),
        FRAGMENT_PATTERN_DETECTION(false),
        SPECTRAL_LIBRARY_SEARCH(false),
        UNSPECIFIED_SOURCE(false);

        private final boolean forbidAdditionalSources;

        Source(boolean forbidAdditionalSources) {
            this.forbidAdditionalSources = forbidAdditionalSources;
        }

        public boolean isForbidAdditionalSources() {
            return forbidAdditionalSources;
        }
    }

    private final HashMap<Source, PossibleElement[]> map;

    public DetectedElements(Map<Source, PossibleElement[]> map) {
        this.map = new HashMap<>(map);
    }

    public static DetectedElements singleton(Source source, PossibleElement... elements) {
        return new DetectedElements(Map.of(source, elements));
    }
    public static DetectedElements singleton(Source source, Element element, float logit) {
        return new DetectedElements(Map.of(source, new PossibleElement[]{new PossibleElement(element, logit)}));
    }

    public DetectedElements with(Source source, PossibleElement... elements) {
        DetectedElements copy = new DetectedElements(map);
        copy.map.put(source,elements);
        return copy;
    }
    public DetectedElements with(DetectedElements elems) {
        DetectedElements copy = new DetectedElements(map);
        copy.map.putAll(elems.map);
        return copy;
    }

    public DetectionResult getFormulaConstraints(FormulaConstraints fallback) {
        return getFormulaConstraints(fallback, POSITIVE_THRESHOLD, NEGATIVE_THRESHOLD);
    }

    public DetectionResult getFormulaConstraints(FormulaConstraints fallback, float positiveThreshold, float negativeThreshold) {
        ArrayList<PossibleElement> buffer = new ArrayList<>();
        for (Source s : map.keySet()) {
            if (s.isForbidAdditionalSources()) {
                buffer.clear();
                buffer.addAll(Arrays.asList(map.get(s)));
                break;
            } else {
                buffer.addAll(Arrays.asList(map.get(s)));
            }
        }
        // add fallback with zero logits
        for (Element e : fallback.getChemicalAlphabet().getElements()) {
            int u = fallback.getUpperbound(e);
            int l = fallback.getLowerbound(e);
            buffer.add(new PossibleElement(e, 0, l==0 ? -1 : l, u==Integer.MAX_VALUE ? -1 : Math.min(u, Byte.MAX_VALUE)));
        }
        PossibleElement[] merged = PossibleElement.merge(buffer.toArray(PossibleElement[]::new), positiveThreshold);
        final FormulaConstraints constraints = PossibleElement.toFormulaConstraints(
                Arrays.stream(merged).filter(x->x.getLogit()>=positiveThreshold).toArray(PossibleElement[]::new), positiveThreshold
        );
        Set<Element> forbiddenElements = Arrays.stream(merged).filter(e->e.getLogit() < negativeThreshold).map(PossibleElement::getElement).collect(Collectors.toSet());
        return new DetectionResult(constraints.withNewFilters(fallback.getFilters()), forbiddenElements);
    }

    public HashMap<Source, PossibleElement[]> getDetections() {
        return map;
    }
}
