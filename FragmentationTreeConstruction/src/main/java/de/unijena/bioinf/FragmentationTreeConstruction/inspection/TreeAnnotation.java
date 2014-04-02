package de.unijena.bioinf.FragmentationTreeConstruction.inspection;

import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.FragmentationPatternAnalysis;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.DecompositionScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakPairScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.PeakScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.model.*;

import java.util.*;

public class TreeAnnotation {

    private final HashMap<Fragment, Map<Class<?>, Double>> vertexAnnotations;
    private final HashMap<Loss, Map<Class<?>, Double>> edgeAnnotations;
    private final HashMap<Fragment, List<String>> additionalProperties;

    public TreeAnnotation(FragmentationTree tree, FragmentationPatternAnalysis analysis) {
        this(tree, analysis, tree.getInput());
    }

    public TreeAnnotation(FragmentationPathway pathway, FragmentationPatternAnalysis analysis, ProcessedInput input) {
        final int N = pathway.numberOfVertices();
        this.vertexAnnotations = new HashMap<Fragment, Map<Class<?>, Double>>(N*2);
        this.edgeAnnotations = new HashMap<Loss, Map<Class<?>, Double>>(N*2);
        this.additionalProperties = new HashMap<Fragment, List<String>>(N*2);
        annotate(pathway, analysis, input);
    }

    public HashMap<Fragment, List<String>> getAdditionalProperties() {
        return additionalProperties;
    }

    public HashMap<Fragment, Map<Class<?>, Double>> getVertexAnnotations() {
        return vertexAnnotations;
    }

    public HashMap<Loss, Map<Class<?>, Double>> getEdgeAnnotations() {
        return edgeAnnotations;
    }

    public Map<Class<?>, Double> getAnnotationForFragment(Fragment fragment) {
        return vertexAnnotations.get(fragment);
    }

    public Map<Class<?>, Double> getAnnotationsForEdge(Loss l) {
        return edgeAnnotations.get(l);
    }

    protected void annotate(FragmentationPathway pathway, FragmentationPatternAnalysis analysis, ProcessedInput input) {
        // initialize scorers
        final Object[] decompositionInits, lossInits;
        decompositionInits = new Object[analysis.getDecompositionScorers().size()];
        int k=0;
        for (DecompositionScorer s : analysis.getDecompositionScorers()) decompositionInits[k++] = s.prepare(input);
        k=0;
        lossInits = new Object[analysis.getLossScorers().size()];
        for (LossScorer s : analysis.getLossScorers()) lossInits[k++] = s.prepare(input);
        // calculate peak scores
        final double[][] peakScores = new double[analysis.getFragmentPeakScorers().size()][input.getMergedPeaks().size()];
        k=0;
        for (PeakScorer s : analysis.getFragmentPeakScorers()) {
            s.score(input.getMergedPeaks(), input, peakScores[k++]);
        }
        // klasses
        final Class[] rootClasses = new Class[analysis.getRootScorers().size()];
        for (int i=0; i < analysis.getRootScorers().size(); ++i) rootClasses[i] = analysis.getRootScorers().get(i).getClass();
        final Class[] vertexClasses = new Class[analysis.getDecompositionScorers().size() + analysis.getFragmentPeakScorers().size()];
        for (int i=0; i < analysis.getDecompositionScorers().size(); ++i) vertexClasses[i] = analysis.getDecompositionScorers().get(i).getClass();
        for (int i=analysis.getDecompositionScorers().size(); i < vertexClasses.length; ++i) vertexClasses[i] =
                analysis.getFragmentPeakScorers().get(i-analysis.getDecompositionScorers().size()).getClass();
        final Class[] lossClasses = new Class[analysis.getLossScorers().size() + analysis.getPeakPairScorers().size()];
        for (int i=0; i < analysis.getLossScorers().size(); ++i) lossClasses[i] = analysis.getLossScorers().get(i).getClass();
        for (int i=analysis.getLossScorers().size(); i < analysis.getPeakPairScorers().size()+analysis.getLossScorers().size(); ++i)
            lossClasses[i] = analysis.getPeakPairScorers().get(i-analysis.getLossScorers().size()).getClass();
        // calculate peak pair scores
        final double[][][] peakPairScores = new double[analysis.getPeakPairScorers().size()][input.getMergedPeaks().size()][input.getMergedPeaks().size()];
        k=0;
        for (PeakPairScorer s : analysis.getPeakPairScorers()) {
            s.score(input.getMergedPeaks(), input, peakPairScores[k++]);
        }
        final ScoreReportMap rootAnnotation = new ScoreReportMap(rootClasses);
        final Fragment root = pathway.getRoot();
        for (DecompositionScorer s : analysis.getRootScorers()) {
            rootAnnotation.put(s.getClass(), s.score(root.getFormula(), root.getPeak(), input, s.prepare(input)));
        }
        vertexAnnotations.put(root, rootAnnotation);
        annotateFragmentsAndEdges(pathway, analysis, decompositionInits, lossInits, peakScores, peakPairScores, vertexClasses, lossClasses, input);
    }

    protected void annotateFragmentsAndEdges(FragmentationPathway pathway, FragmentationPatternAnalysis analysis,
                                             Object[] decompositionInits, Object[] lossInits, double[][] peakScores,
                                             double[][][] peakPairScores, Class[] vertexClasses, Class[] lossClasses, ProcessedInput input) {
        // iterate tree in post-order
        for (Fragment vertex : pathway.getFragmentsWithoutRoot()) {
            annotateFragment(analysis, decompositionInits, peakScores, vertexClasses, vertex, input);
            annotateLoss(analysis, lossClasses, lossInits, peakPairScores, vertex, input);
            additionalAnnotationsForFragments(analysis, vertex, input);
        }
    }

    private void additionalAnnotationsForFragments(FragmentationPatternAnalysis analysis, Fragment vertex, ProcessedInput input) {
        final List<String> annotations = new ArrayList<String>();
        // add recalibration
        if (Math.abs(vertex.getPeak().getRecalibrationShift()) > 1e-5 ) {
            final Deviation dev = Deviation.fromMeasurementAndReference(vertex.getPeak().getMz(), vertex.getPeak().getOriginalMz());
            annotations.add(String.format(Locale.US, "Calibration: %+.2f ppm (%.3g m/z)", dev.getPpm(), dev.getAbsolute()));
        }
        if (!annotations.isEmpty()) additionalProperties.put(vertex, annotations);
    }

    protected void annotateFragment(FragmentationPatternAnalysis analysis, Object[] decompositionInits, double[][] peakScores, Class[] vertexClasses, Fragment vertex, ProcessedInput input) {
        final ScoreReportMap vertexAnnotation = new ScoreReportMap(vertexClasses);
        // Formula Scorer
        int j=0;
        for (DecompositionScorer scorer : analysis.getDecompositionScorers()) {
            final double score = scorer.score(vertex.getFormula(), vertex.getPeak(), input, decompositionInits[j++]);
            vertexAnnotation.put(scorer.getClass(), score);
        }
        // Peak Scorer
        j=0;
        for (PeakScorer s : analysis.getFragmentPeakScorers()) {
            vertexAnnotation.put(s.getClass(), peakScores[j++][vertex.getPeak().getIndex()]);
        }
        vertexAnnotations.put(vertex, vertexAnnotation);
    }

    protected void annotateLoss(FragmentationPatternAnalysis analysis, Class[] lossClasses, Object[] lossInits, double[][][] peakPairScores, Fragment vertex, ProcessedInput input) {
        for (final Loss loss : vertex.getIncomingEdges()) {
            int j;// Loss Scorer
            //final Loss loss = vertex.getIncomingEdge();
            final ScoreReportMap edgeAnnotation = new ScoreReportMap(lossClasses);
            j=0;
            for (LossScorer s : analysis.getLossScorers()) {
                edgeAnnotation.put(s.getClass(), s.score(loss, input, lossInits[j++]));
            }
            // peak pair Scorers
            j=0;
            for (PeakPairScorer s : analysis.getPeakPairScorers()) {
                edgeAnnotation.put(s.getClass(), peakPairScores[j++][loss.getHead().getPeak().getIndex()][loss.getTail().getPeak().getIndex()]);
            }
            edgeAnnotations.put(loss, edgeAnnotation);
        }
    }

    private static class ScoreReportMap extends AbstractMap<Class<?>, Double> {

        private final double[] scores;
        private final Class[] klasses;

        private ScoreReportMap(Class[] klasses) {
            this.scores = new double[klasses.length];
            this.klasses = klasses;
        }

        @Override
        public int size() {
            return scores.length;
        }

        @Override
        public boolean isEmpty() {
            return scores.length==0;
        }

        @Override
        public boolean containsValue(Object value) {
            if (value==null || !(value instanceof Double)) return false;
            double s = (Double)value;
            for (double v : scores) if (v==s) return true;
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return key2index(key)>=0;
        }

        @Override
        public Double get(Object key) {
            final int index = key2index(key);
            if (index < 0) return null;
            return scores[index];
        }

        @Override
        public Double put(Class<?> key, Double value) {
            final int index = key2index(key);
            if (index < 0) throw new IllegalArgumentException("Unsupported key '" + key + "'");
            final double oldValue = scores[index];
            scores[index] = value;
            return oldValue;
        }

        @Override
        public Set<Entry<Class<?>, Double>> entrySet() {
            return new AbstractSet<Entry<Class<?>, Double>>() {
                @Override
                public Iterator<Entry<Class<?>, Double>> iterator() {
                    return new Iterator<Entry<Class<?>, Double>>() {
                        int i=0;
                        @Override
                        public boolean hasNext() {
                            return i < scores.length;
                        }

                        @Override
                        public Entry<Class<?>, Double> next() {
                            if (!hasNext()) throw new NoSuchElementException();
                            final int index=i++;
                            return new Entry<Class<?>, Double>() {
                                @Override
                                public Class<?> getKey() {
                                    return klasses[index];
                                }

                                @Override
                                public Double getValue() {
                                    return scores[index];
                                }

                                @Override
                                public Double setValue(Double value) {
                                    double oldValue = scores[index];
                                    scores[index] = value;
                                    return oldValue;
                                }
                            };
                        }

                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                @Override
                public int size() {
                    return scores.length;
                }
            };
        }

        private int key2index(Object key) {
            for (int i=0; i < klasses.length; ++i)
                if (klasses[i]==key) return i;
            return -1;
        }
    }


}
