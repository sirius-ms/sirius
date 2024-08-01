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

package de.unijena.bioinf.sirius.plugins;

import de.unijena.bioinf.ChemistryBase.algorithm.Called;
import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.IsotopeMs2Settings;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.SiriusPlugin;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.FragmentScorer;
import de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring.LossScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.generation.FragmentIsotopeGenerator;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.MassDifferenceDeviationScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.scoring.NormalDistributedIntensityScorer;
import de.unijena.bioinf.IsotopePatternAnalysis.util.PiecewiseLinearFunctionIntensityDependency;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import de.unijena.bioinf.sirius.MS2Peak;
import de.unijena.bioinf.sirius.PeakAnnotation;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import de.unijena.bioinf.sirius.deisotope.TargetedIsotopePatternDetection;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.Range;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IsotopePatternInMs2Plugin extends SiriusPlugin {

    @Override
    public void initializePlugin(PluginInitializer initializer) {
        initializer.addLossScorer(new Ms2IsotopePatternScorer());
        initializer.addFragmentScorer(new Ms2IsotopePatternScorer());
    }

    @Override
    protected void afterPreprocessing(ProcessedInput input) {
        if (input.getAnnotation(IsotopeMs2Settings.class,IsotopeMs2Settings::new).value == IsotopeMs2Settings.Strategy.SCORE)
            new Ms2IsotopeDetector().detectAndSetAnnotations(input,input.getExperimentInformation());
    }

    @Override
    public boolean isGraphReductionForbidden(FGraph graph) {
        return graph.getFragmentAnnotationOrNull(Ms2IsotopePattern.class)!=null;
    }

    @Override
    protected void afterGraphBuilding(ProcessedInput input, FGraph graph) {
        if (input.getPeakAnnotations().containsKey(ExtractedMs2IsotopePattern.class))
            new IntroduceIsotopeLosses(input, graph).introduceIsotopeLosses();
    }

    private boolean hasIsotopicPeaks(ProcessedInput input) {
        if(!input.getPeakAnnotations().containsKey(ExtractedMs2IsotopePattern.class))
            return false;
        final PeakAnnotation<ExtractedMs2IsotopePattern> pat = input.getPeakAnnotationOrThrow(ExtractedMs2IsotopePattern.class);
        return input.getMergedPeaks().stream().anyMatch(x->pat.get(x)!=null);
    }

    @Override
    protected void transferAnotationsFromGraphToTree(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2treeFragments) {

        // we set:
        // - Ms2IsotopePattern to monoisotopic node
        // - IsotopeMarker to each artificial loss and node (for easy deletion later)

        // two possibilities
        // 1.) A node has an isotope pattern but no artificial egdes. Easy case, just transfer annotations
        // 2.) A node has isotope children. So set the annotations right

        final FragmentAnnotation<Ms2IsotopePattern> ano = graph.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        if (ano!=null) {
            final PeakAnnotation<ExtractedMs2IsotopePattern> peakPat = input.getPeakAnnotationOrThrow(ExtractedMs2IsotopePattern.class);
            final FragmentAnnotation<IsotopicMarker> markerTree = tree.getOrCreateFragmentAnnotation(IsotopicMarker.class);
            final LossAnnotation<IsotopicMarker> markerTreeLoss = tree.getOrCreateLossAnnotation(IsotopicMarker.class);
            final FragmentAnnotation<IsotopicMarker> markerGraph = graph.getOrCreateFragmentAnnotation(IsotopicMarker.class);
            final LossAnnotation<IsotopicScore> scoreMarker = graph.getLossAnnotationOrNull(IsotopicScore.class);
            final FragmentAnnotation<Ms2IsotopePattern> isoPatternAno = tree.getOrCreateFragmentAnnotation(Ms2IsotopePattern.class);
            // first find all artifical edges
            for (Fragment f : tree) {
                final Fragment g = graph2treeFragments.mapRightToLeft(f);
                if (f.isLeaf() && markerGraph.get(g,IsotopicMarker::isNot).isIsotope()) {
                    final IsotopicScore score = scoreMarker.get(g.getIncomingEdge());
                    double iscore = 0d;
                    // get monoisotopic node
                    Fragment mono = f;
                    while (markerGraph.get(graph2treeFragments.mapRightToLeft(mono),IsotopicMarker::isNot).isIsotope()) {
                        markerTree.set(mono,IsotopicMarker.is());
                        iscore += mono.getIncomingEdge().getWeight();
                        markerTreeLoss.set(mono.getIncomingEdge(),IsotopicMarker.is());
                        mono = mono.getParent();
                    }
                    final Fragment monoG = graph2treeFragments.mapRightToLeft(mono);
                    if (ano.get(monoG)!=null) iscore += ano.get(monoG).getScore();
                    // set isotope pattern
                    final ExtractedMs2IsotopePattern extr = peakPat.get(input.getMergedPeaks().get(mono.getPeakId()));
                    isoPatternAno.set(mono, extr.done(score.patternLength, iscore));
                }
            }

            // find all isotope patterns without artificial edges
            for (Fragment f : tree) {
                final Fragment g = graph2treeFragments.mapRightToLeft(f);
                if (isoPatternAno.get(f)==null && ano.get(g)!=null) {
                    Ms2IsotopePattern pat = ano.get(g);
                    isoPatternAno.set(f, pat);
                }
            }
        }
    }

    protected void transferAnotationsFromGraphToTree2(ProcessedInput input, FGraph graph, FTree tree, IntergraphMapping graph2treeFragments) {
        // we have to transfer the isotope score, such that we can later recalculate it...?

        // add isotope pattern to node
        final FragmentAnnotation<Ms2IsotopePattern> ano = graph.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        final FragmentAnnotation<Ms2IsotopePattern> giso = graph.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        final LossAnnotation<IsotopicScore> marker = graph.getLossAnnotationOrNull(IsotopicScore.class);
        final LossAnnotation<IsotopicMarker> markerTree = tree.getOrCreateLossAnnotation(IsotopicMarker.class);
        final PeakAnnotation<ExtractedMs2IsotopePattern> extrAno = input.getOrCreatePeakAnnotation(ExtractedMs2IsotopePattern.class);

        if (ano!=null) {
            final FragmentAnnotation<Ms2IsotopePattern> anoTree = tree.getOrCreateFragmentAnnotation(Ms2IsotopePattern.class);
            for (Fragment f : tree) {
                final Fragment g = graph2treeFragments.mapRightToLeft(f);
                final ExtractedMs2IsotopePattern extr = extrAno.get(input.getMergedPeaks().get(f.getPeakId()));
                if (extr!=null) {
                    // find isotopic edge
                    Loss isoEdge=null;
                    for (int k=0; k < g.getOutDegree(); ++k) {
                        Loss e = g.getOutgoingEdge(k);
                        if (marker.get(e)!=null) {
                            isoEdge = e;
                            break;
                        }
                    }
                    if (isoEdge!=null) {
                        // follow path
                        Loss out=null;
                        for (int k=0; k < f.getOutDegree(); ++k) {
                            final Loss l = f.getOutgoingEdge(k);
                            if (l.getFormula().isEmpty() && Math.abs(isoEdge.getWeight()-l.getWeight())<1e-8) {
                                out=l;
                                break;
                            }
                        }
                        if (out==null) {
                            throw new RuntimeException("Do not find isotope edge!");
                        }
                        // follow path
                        Loss isotopeTrace = out;
                        markerTree.set(out,IsotopicMarker.is());
                        double score = out.getWeight();
                        while (isotopeTrace.getTarget().getOutDegree()>0) {
                            isotopeTrace = isotopeTrace.getTarget().getOutgoingEdge(0);
                            markerTree.set(isotopeTrace,IsotopicMarker.is());
                            score += isotopeTrace.getWeight();
                            isoEdge = isoEdge.getTarget().getOutgoingEdge(0);
                        }

                        double intrinsicScoreBonus = 0d;
                        if (giso.get(f)!=null) intrinsicScoreBonus += giso.get(f).getScore();

                        anoTree.set(f, extr.done(marker.get(isoEdge).patternLength, score+intrinsicScoreBonus));

                    } else if (giso.get(g)!=null) {
                        final Ms2IsotopePattern miso = giso.get(g);
                        anoTree.set(f, extr.done(miso.getPeaks().length, miso.getScore()));
                    }
                }
            }
        }
    }

    @Override
    protected void releaseTreeToUser(ProcessedInput input, FGraph graph, FTree tree) {
        final LossAnnotation<IsotopicMarker> m = tree.getLossAnnotationOrNull(IsotopicMarker.class);
        final FragmentAnnotation<Ms2IsotopePattern> mm = tree.getFragmentAnnotationOrNull(Ms2IsotopePattern.class);
        double additionalScore = 0d;
        if (m!=null) {
            final List<Fragment> fs = new ArrayList<>(tree.getFragmentsWithoutRoot());
            for (Fragment f : fs) {
                if (f.getVertexId()<0)
                    continue; // already deleted
                if (m.get(f.getIncomingEdge(),IsotopicMarker::isNot).isIsotope()) {
                    final Fragment s = f.getIncomingEdge().getSource();
                    if (s.getInDegree()==0 || !(m.get(s.getIncomingEdge(), IsotopicMarker::isNot).isIsotope())) {
                        // collect iso scores
                        Loss l = f.getIncomingEdge();
                        double isoScore = l.getWeight();
                        while (!l.getTarget().isLeaf()) {
                            l = l.getTarget().getOutgoingEdge(0);
                            isoScore += l.getWeight();
                        }
                        // delete all edges below s
                        tree.deleteSubtree(f);
                        s.getIncomingEdge().setWeight(s.getIncomingEdge().getWeight()+isoScore);

                    }
                }
            }
        }
        tree.setTreeWeight(tree.getTreeWeight());

    }

    public static class ExtractedMs2IsotopePattern implements DataAnnotation {
        protected final SimpleSpectrum pattern;
        protected final int[] peakIds;
        protected final double score;

        public ExtractedMs2IsotopePattern(SimpleSpectrum pattern, int[] peakIds) {
            this(pattern,peakIds,0d);
        }

        public ExtractedMs2IsotopePattern(SimpleSpectrum pattern, int[] peakIds, double score) {
            this.pattern = pattern;
            this.peakIds = peakIds;
            this.score = score;
        }

        public Ms2IsotopePattern done(int numberOfPeaks,double score) {
            final Peak[] peaks = new Peak[numberOfPeaks];
            for (int k=0; k < numberOfPeaks; ++k) peaks[k] = pattern.getPeakAt(k);
            return new Ms2IsotopePattern(peaks,score);
        }
    }

    public static class IsotopicScore implements DataAnnotation  {
        private final double score;
        private final int patternLength;

        public IsotopicScore() {
            this(0,0d);
        }

        public IsotopicScore(int length, double score) {
            this.score = score;
            this.patternLength = length;
        }
    }

    @Called("MS2-Isotopes")
    public static class Ms2IsotopePatternScorer implements LossScorer<Ms2IsotopePatternScorer.Prepared>, FragmentScorer<Ms2IsotopePatternScorer.Prepared> {
        @Override
        public Ms2IsotopePatternScorer.Prepared prepare(ProcessedInput input, AbstractFragmentationGraph graph) {
            return new Prepared(graph.getLossAnnotationOrNull(IsotopicScore.class), graph.getFragmentAnnotationOrNull(Ms2IsotopePattern.class));
        }

        @Override
        public double score(Fragment fragment, ProcessedPeak correspondingPeak, boolean isRoot, Prepared precomputed) {
            double score = 0d;Ms2IsotopePattern f;
            if (precomputed.pattern!=null && (f=precomputed.pattern.get(fragment))!=null) {
                score += f.getScore();
            }
            return score;
        }

        @Override
        public double score(Loss loss, ProcessedInput input, Ms2IsotopePatternScorer.Prepared precomputed) {
            IsotopicScore s;Ms2IsotopePattern f;
            double score = 0d;
            if (precomputed.isotopicScoreLossAnnotation!=null && (s=precomputed.isotopicScoreLossAnnotation.get(loss))!=null) {
                score += s.score;
            }
            return score;
        }

        @Override
        public boolean processArtificialEdges() {
            return true;
        }

        @Override
        public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        @Override
        public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {

        }

        protected final static class Prepared {
            private final LossAnnotation<IsotopicScore> isotopicScoreLossAnnotation;
            private final FragmentAnnotation<Ms2IsotopePattern> pattern;

            public Prepared(LossAnnotation<IsotopicScore> isotopicScoreLossAnnotation, FragmentAnnotation<Ms2IsotopePattern> pattern) {
                this.isotopicScoreLossAnnotation = isotopicScoreLossAnnotation;
                this.pattern = pattern;
            }
        }
    }

    public static class IntroduceIsotopeLosses {

        private final FGraph graph;
        private final ProcessedInput input;

        public IntroduceIsotopeLosses(ProcessedInput input, FGraph graph) {
            this.graph = graph;
            this.input = input;
        }

        /**
         * Achtung: superhacky!
         * Isotopes are implemented via two annotations:
         *  - Ms2IsotopePattern (in fragment node):
         *      - contains the full pattern with score 0
         *  - IsotopicScore (in losses between isotopic peaks)
         *      - contains score bonus for elongation of the pattern
         *
         *  we only create synthetic isotope peak nodes for peaks which are part of the graph. If a peak is NOT part of the graph
         *  we can add it for free and so add its score bonus to the last isotopic edge
         *  If we do not have an isotopic edge, the whole pattern is for free and we add its score bonus to the Ms2IsotopePattern
         *  object in the fragment node
         *
         */
        public void introduceIsotopeLosses() {
            Fragment rootNode = graph.getRoot().getOutgoingEdge(0).getTarget();
            final MolecularFormula root = rootNode.getFormula();
            final PeakAnnotation<ExtractedMs2IsotopePattern> patterns = input.getOrCreatePeakAnnotation(ExtractedMs2IsotopePattern.class);
            TIntIntHashMap peakid2color = null;
            final ExtractedMs2IsotopePattern rootPattern = patterns.get(input.getParentPeak());

            // isotopic marker is set on each artificial edge and note
            // we need this for special operations in reduction or heuristics =/
            final FragmentAnnotation<IsotopicMarker> marker = graph.getOrCreateFragmentAnnotation(IsotopicMarker.class);
            final LossAnnotation<IsotopicMarker> lossMarker = graph.getOrCreateLossAnnotation(IsotopicMarker.class);



            final FragmentAnnotation<Ms2IsotopePattern> otherMarker = graph.getOrCreateFragmentAnnotation(Ms2IsotopePattern.class);
            final LossAnnotation<IsotopicScore> edgeMarker = graph.getOrCreateLossAnnotation(IsotopicScore.class);
            for (Fragment f : graph.getFragmentsWithoutRoot()) {
                if (f.getFormula().equals(root) || marker.get(f,IsotopicMarker::isNot).isIsotope())
                    continue;
                final ExtractedMs2IsotopePattern iso = patterns.get(input.getMergedPeaks().get(f.getPeakId()));
                if (iso==null || iso.pattern.size()<=1)
                    continue;

                final double[] scoredPattern = computeAndScorePattern(f, iso, root, f.getFormula(), f.getIonization(), rootPattern);
                if (scoredPattern.length > 1 && scoredPattern[scoredPattern.length-1] > 0) {
                    if (peakid2color==null) peakid2color = buildIdColorMap();
                    // add isotope edges with corresponding loss
                    Fragment predecessor = f;
                    int peaksForFree = 1;
                    double scoreForFree = 0d;
                    for (int k=1; k < scoredPattern.length; ++k) {

                        final int peakId = iso.peakIds[k];
                        // case 1: we have to add an synthetic edge, because the isotope peak is also a possible fragment peak
                        if (peakId>=0 && peakid2color.get(peakId)>=0) {
                            // add iso node
                            final Fragment isoNode = graph.addFragment(MolecularFormula.emptyFormula(), f.getIonization());
                            isoNode.setColor(peakid2color.get(peakId));
                            isoNode.setPeakId(peakId);
                            marker.set(isoNode, IsotopicMarker.is());

                            // add iso edge
                            final Loss syntheticEdge = graph.addLoss(predecessor, isoNode);
                            lossMarker.set(syntheticEdge, IsotopicMarker.is());
                            edgeMarker.set(syntheticEdge, new IsotopicScore(k+1, scoredPattern[k]-scoredPattern[k-1]));

                            predecessor = isoNode;
                        // case 2: the isotope peak is no fragment peak, so we can add it for free
                        } else if (predecessor == f) {
                            ++peaksForFree;
                            scoreForFree += (scoredPattern[k]-scoredPattern[k-1]);
                        // case 3: like case 2, but we add the score to the previous isotope peak
                        } else {
                            assert predecessor.getInDegree()==1;
                            Loss syn = predecessor.getIncomingEdge(0);
                            IsotopicScore isotopicScore = edgeMarker.get(syn);
                            edgeMarker.set(syn, new IsotopicScore(k+1, (scoredPattern[k]-scoredPattern[k-1])));
                        }
                    }
                    if (peaksForFree>1) {
                        Peak[] pks = new Peak[peaksForFree];
                        for (int k=0; k < pks.length; ++k) pks[k] = iso.pattern.getPeakAt(k);
                        otherMarker.set(f, new Ms2IsotopePattern(pks, scoreForFree));
                    }
                }

            }
        }

        private TIntIntHashMap buildIdColorMap() {
            final TIntIntHashMap map = new TIntIntHashMap(input.getMergedPeaks().size(), 0.75f, -1,-1);
            for (Fragment f : graph) {
                final int peakId = f.getPeakId();
                final int color = f.getColor();
                map.putIfAbsent(peakId,color);
            }
            return map;
        }

        private double[] computeAndScorePattern(Fragment fragment, ExtractedMs2IsotopePattern iso, MolecularFormula root, MolecularFormula formula, Ionization ion, ExtractedMs2IsotopePattern rootPattern) {
            Normalization max = Normalization.Max(1d);
            final FragmentIsotopeGenerator fiso = new FragmentIsotopeGenerator();
            fiso.setMaximalNumberOfPeaks(Math.min(rootPattern.pattern.size(), iso.pattern.size()));
            final SimpleSpectrum simulated = Spectrums.subspectrum(Spectrums.getNormalizedSpectrum(fiso.simulateFragmentPatternWithImperfectFilter(rootPattern.pattern, fragment.getFormula(), root.subtract(formula), ion),max),0, iso.pattern.size()) ;
            final SimpleSpectrum measured = Spectrums.subspectrum(Spectrums.getNormalizedSpectrum(iso.pattern, max), 0, simulated.size());
            // TODO: we want to remove all these constants from the profile.json anyways
            // everything which is configurable should be added to the config
            PiecewiseLinearFunctionIntensityDependency dependency = new PiecewiseLinearFunctionIntensityDependency(
                    new double[]{0.2, 0.1, 0.01}, new double[]{1, 2, 3}
            );
            final MassDifferenceDeviationScorer massdifScorer = new MassDifferenceDeviationScorer(dependency);
            final MassDeviationScorer massScorer = new MassDeviationScorer(dependency);
            final NormalDistributedIntensityScorer normal = new NormalDistributedIntensityScorer();

            final double[] scores = new double[measured.size()];
            massScorer.score(scores, measured,simulated,max,input.getExperimentInformation());
            scores[0] = 0d; // do not double-score the isotope peaks
            massdifScorer.score(scores, measured, simulated, max, input.getExperimentInformation());
            normal.score(scores,measured,simulated,max,input.getExperimentInformation());

            // add missing peak scorer, but for relative intensity to base peak
            final double lambda = 50;
            final ProcessedPeak peak = input.getMergedPeaks().get(fragment.getPeakId());
            final double basePeak = 1d/peak.getRelativeIntensity();
            double missingPeak  = 0d;
            for (int k=simulated.size()-1; k >= 0; --k) {
                if (scores.length > k) scores[k] -= missingPeak;
                missingPeak += (simulated.getIntensityAt(k)/basePeak)*lambda;
            }

            // first peak has always score of 0
            scores[0]=0d;

            // cut off scores from the right
            int maxScore = 0;
            for (int k=1; k < scores.length; ++k) {
                if (scores[k] > scores[maxScore]) {
                    maxScore = k;
                }
            }
            if (maxScore < scores.length) {
                return Arrays.copyOf(scores, maxScore+1);
            } else {
                return scores;
            }
        }
    }


    public static class Ms2IsotopeDetector {
        private final TargetedIsotopePatternDetection patternDetector = new TargetedIsotopePatternDetection();
        public boolean detectAndSetAnnotations(ProcessedInput input, Ms2Experiment experiment) {
            final PeakAnnotation<ExtractedMs2IsotopePattern> ano = input.getOrCreatePeakAnnotation(ExtractedMs2IsotopePattern.class);

            // first check if we find an isotope pattern in MS1
            int maxNumberOfPeaks = 5;
            Ms1IsotopePattern ms1IsotopePattern = input.getAnnotation(Ms1IsotopePattern.class, Ms1IsotopePattern::none);
            if (ms1IsotopePattern.getPeaks().length>1) {
                // as the Ms1 scan is usually cleaned up via correlation analysis, we should trust it more than the
                // MS2 isotope pattern. We will not trust any peak behind
                maxNumberOfPeaks = ms1IsotopePattern.getPeaks().length;
            }

            // first check if we find an isotope pattern for the parent peak
            ProcessedPeak parent = input.getParentPeak();
            final SimpleSpectrum spec = Spectrums.subspectrum(findPatternInMostIntensiveScan(input, parent), 0, maxNumberOfPeaks);
            if (spec.size()<=1) {
                // we are done
                return false;
            }
            ano.set(parent, new ExtractedMs2IsotopePattern(spec,getPeakIds(input, spec)));

            // now check for each peak if we find an isotope pattern
            boolean atLeastOne = false;
            double highestIsotopicIntensity = 0d;
            double highestNonIsotopicIntensity = 0d;
            for (ProcessedPeak peak : input.getMergedPeaks()) {
                if (peak == parent)
                    continue;
                final SimpleSpectrum isoSpec = Spectrums.subspectrum(findPatternInMostIntensiveScan(input, peak), 0, maxNumberOfPeaks);
                if (isoSpec.size()>1) {
                    ano.set(peak, new ExtractedMs2IsotopePattern(isoSpec,getPeakIds(input, isoSpec)));
                    atLeastOne = true;
                    highestIsotopicIntensity = Math.max(highestIsotopicIntensity, peak.getRelativeIntensity());
                } else {
                    highestNonIsotopicIntensity = Math.max(highestNonIsotopicIntensity, peak.getRelativeIntensity());
                }
            }
            // there should be no peak with no isotope pattern which is more intensive than peaks with isotope pattern
            final boolean found = atLeastOne && highestIsotopicIntensity> highestNonIsotopicIntensity;

            // delete annotation if we haven't found isotope peaks
            if (!found) {
                input.getMergedPeaks().forEach(x->ano.set(x,null));
            }
            return found;

        }

        public SimpleSpectrum findPatternInMostIntensiveScan(ProcessedInput input, ProcessedPeak peak) {
            int mostIntensiveScan = peak.getIndexOfMostIntensiveOriginalPeak();
            if (mostIntensiveScan < 0) return Spectrums.empty();
            MS2Peak ms2Peak = peak.getOriginalPeaks().get(mostIntensiveScan);
            return findPattern(input.getAnnotationOrThrow(FormulaConstraints.class), input.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation, ms2Peak.getSpectrum(), peak.getMass());
        }

        public SimpleSpectrum findPattern(FormulaConstraints constraints, Deviation allowedDev, Ms2Spectrum spectrum, double targetMz) {
            final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
            final double abs = allowedDev.absoluteFor(targetMz);
            double from = targetMz-abs;
            double to = targetMz + 4;
            for (int k=0; k < spectrum.size(); ++k) {
                final double mz  = spectrum.getMzAt(k);
                if (mz >= from && mz < to) buf.addPeak(mz, spectrum.getIntensityAt(k));
            }
            if (buf.isEmpty()) return SimpleSpectrum.empty();
            Spectrums.sortSpectrumByMass(buf);
            final SimpleMutableSpectrum pattern = new SimpleMutableSpectrum();
            final SimpleMutableSpectrum merge = new SimpleMutableSpectrum();
            int peakIndex = 0;
            to = targetMz + abs;
            int isotopeIndex = 0;
            final PeriodicTable T = PeriodicTable.getInstance();
            final ChemicalAlphabet alphabet = constraints.getChemicalAlphabet();
            for (; peakIndex < buf.size(); ++peakIndex) {
                final double mz = buf.getMzAt(peakIndex);
                if (mz >= from && mz < to) {
                    merge.addPeak(mz, buf.getIntensityAt(peakIndex));
                } else if (mz > to) {
                    if (merge.isEmpty())
                        break; // we do not find the isotope pattern. So far we do not allow gaps.
                    pattern.addPeak(merge(merge));
                    merge.clear();
                    ++isotopeIndex;

                    final Range<Double> nextMz = T.getIsotopicMassWindow(alphabet, allowedDev, targetMz, isotopeIndex);
                    from = nextMz.getMinimum() - abs;
                    to = nextMz.getMaximum() + abs;
                    if (mz >= from && mz < to) {
                        merge.addPeak(mz, buf.getIntensityAt(peakIndex));
                    } else if (mz > to) break;

                }
            }
            if (merge.size()>0)
                pattern.addPeak(merge(merge));
            return new SimpleSpectrum(pattern);

        }

        private int[] getPeakIds(ProcessedInput input, SimpleSpectrum spec) {
            final Deviation dev = input.getAnnotationOrDefault(MS2MassDeviation.class).allowedMassDeviation;
            final double abs = dev.absoluteFor(spec.getMzAt(spec.size()-1));
            final double from = spec.getMzAt(0)-abs, to = spec.getMzAt(spec.size()-1)+abs;
            final int[] ids = new int[spec.size()];
            Arrays.fill(ids, -1);
            for (ProcessedPeak peak : input.getMergedPeaks()) {
                double mass = peak.getMass();
                if (mass >= from && mass < to) {
                    for (int k=0; k < spec.size(); ++k) {
                        if (dev.inErrorWindow(mass, spec.getMzAt(k))) {
                            ids[k] = peak.getIndex(); // important: check, that we do not reorder the spectrum
                        }
                    }
                }
            }
            return ids;
        }

        private Peak merge(SimpleMutableSpectrum buf) {
            // sum up intensities and m/z
            double mz = 0d, intens = 0d;
            for (int k=0; k < buf.size(); ++k) {
                mz += buf.getMzAt(k)*buf.getIntensityAt(k);
                intens += buf.getIntensityAt(k);
            }
            return new SimplePeak(mz / intens, intens);
        }

    }
}
