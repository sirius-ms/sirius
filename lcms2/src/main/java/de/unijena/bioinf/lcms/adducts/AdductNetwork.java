package de.unijena.bioinf.lcms.adducts;

import org.apache.commons.lang3.Range;
import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.chem.RetentionTime;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.adducts.assignment.AdductAssignment;
import de.unijena.bioinf.lcms.adducts.assignment.SubnetworkResolver;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.CorrelatedIonPair;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class AdductNetwork {

    protected AdductNode[] rtOrderedNodes;
    protected AdductManager adductManager;
    List<List<AdductNode>> subgraphs = new ArrayList<>();
    List<AdductNode> singletons = new ArrayList<>();

    protected Deviation deviation;
    ProjectSpaceTraceProvider provider;

    double retentionTimeTolerance;
    PValueStats pValueStats;

    public AdductNetwork(ProjectSpaceTraceProvider provider, AlignedFeatures[] features, AdductManager manager, double retentionTimeTolerance) {
        this.rtOrderedNodes = new AdductNode[features.length];
        for (int k=0; k < features.length; ++k) {
            rtOrderedNodes[k] = new AdductNode(features[k], k);
        }
        Arrays.sort(rtOrderedNodes, Comparator.comparingDouble(AdductNode::getRetentionTime));
        adductManager = manager;
        this.provider = provider;
        pValueStats = new PValueStats();
        deviation = new Deviation(10);
        this.retentionTimeTolerance = retentionTimeTolerance;
    }

    record NetworkResult(AdductEdge[] realEdges, AdductEdge[] decoyEdges) {
    }

    public void buildNetworkFromMassDeltas(JobManager jjobs) {
        Scorer scorer = new Scorer();
        List<BasicJJob<NetworkResult>> jobs = new ArrayList<>();
        for (int rr=0; rr < rtOrderedNodes.length; ++rr) {
            final int r = rr;
            jobs.add(jjobs.submitJob(new BasicJJob<NetworkResult>() {
                @Override
                protected NetworkResult compute() throws Exception {
                    List<AdductEdge> realEdges = new ArrayList<>();
                    List<AdductEdge> decoyEdges = new ArrayList<>();
                    final AdductNode rightNode = rtOrderedNodes[r];
                    final double thresholdStart = Math.min(rtOrderedNodes[r].getFeature().getRetentionTime().getMiddleTime() - retentionTimeTolerance, rtOrderedNodes[r].getFeature().getRetentionTime().getStartTime());
                    final double thresholdEnd = Math.max(rtOrderedNodes[r].getFeature().getRetentionTime().getMiddleTime() + retentionTimeTolerance, rtOrderedNodes[r].getFeature().getRetentionTime().getEndTime());
                    final Range<Double> threshold = Range.of(thresholdStart, thresholdEnd);

                    // obtain potential fragment peaks
                    List<MergedMSnSpectrum> ms2Right = provider.getMs2SpectraOf(rightNode.getFeatures());
                    if (!ms2Right.isEmpty()) rightNode.hasMsMs = true;
                    SimpleSpectrum preparedRight = null;
                    boolean ms2rightGood = false;
                    MassMap<Peak> potentialInsourceFragments = getPotentialInsourceFragments(ms2Right, rightNode);

                    int rStart=r;
                    int rEnd=r+1;
                    for (; rEnd < rtOrderedNodes.length; ++rEnd) {
                        if (!threshold.contains(rtOrderedNodes[rEnd].getRetentionTime() )  ) {
                            break;
                        }
                    }
                    --rEnd;

                    for (; rStart >= 0; --rStart) {
                        if (!threshold.contains(rtOrderedNodes[rStart].getRetentionTime() )  ) {
                            break;
                        }
                    }
                    ++rStart;

                    for (int i=rStart; i <= rEnd; ++i) {
                        if (i != r) {
                            final AdductNode leftNode = rtOrderedNodes[i];
                            RetentionTime rt = leftNode.getFeature().getRetentionTime();
                            final double thresholdStart2 = Math.min(rt.getStartTime(),  rt.getMiddleTime() - retentionTimeTolerance);
                            final double thresholdEnd2 = Math.max(rt.getEndTime(), rt.getMiddleTime() + retentionTimeTolerance);
                            final Range<Double> threshold2 = Range.of(thresholdStart2, thresholdEnd2);
                            if (rightNode.getMass() > leftNode.getMass() && Math.abs(rightNode.getRetentionTime() - leftNode.getRetentionTime()) < retentionTimeTolerance &&  threshold2.contains(rightNode.getRetentionTime())) {
                                final double massDelta = rightNode.getMass() - leftNode.getMass();
                                List<KnownMassDelta> knownMassDeltas = adductManager.retrieveMassDeltas(massDelta, deviation);

                                // add multimere edge if present
                                adductManager.checkForMultimere(rightNode.getMass(), leftNode.getMass(), deviation).ifPresent(knownMassDeltas::add);

                                if (knownMassDeltas.isEmpty()) {
                                    List<Peak> potentialInsourcePeaks = potentialInsourceFragments == null ? Collections.emptyList() : potentialInsourceFragments.retrieveAll(massDelta, deviation);
                                    if (!potentialInsourcePeaks.isEmpty()) {
                                        UnknownLossRelationship insourceFragment = new UnknownLossRelationship();
                                        knownMassDeltas.add(insourceFragment);
                                    }
                                }
                                if (!knownMassDeltas.isEmpty()) {
                                    final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, knownMassDeltas.toArray(KnownMassDelta[]::new));
                                    scorer.computeScore(provider, adductEdge);
                                    if (Double.isFinite(adductEdge.ratioScore)) {

                                        // add MS/MS score
                                        if (!ms2Right.isEmpty()) {
                                            List<MergedMSnSpectrum> ms2Left = provider.getMs2SpectraOf(leftNode.getFeatures());
                                            if (!ms2Left.isEmpty()) {
                                                if (preparedRight==null) {
                                                    preparedRight = scorer.prepareForCosine(rightNode, ms2Right);
                                                    ms2rightGood = scorer.hasMinimumMs2Quality(preparedRight);
                                                }
                                                if (ms2rightGood) {
                                                    SimpleSpectrum ms2left = scorer.prepareForCosine(leftNode, ms2Left);
                                                    if (scorer.hasMinimumMs2Quality(ms2left)) {
                                                        scorer.computeMs2Score(adductEdge, ms2left, preparedRight);
                                                    }
                                                }
                                            }
                                        }

                                        realEdges.add(adductEdge);
                                    }
                                } else if (decoyEdges.size() < 10 && adductManager.hasDecoy(massDelta)) {
                                    final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, new KnownMassDelta[0]);
                                    scorer.computeScore(provider, adductEdge);
                                    decoyEdges.add(adductEdge);
                                }
                            }
                        }
                    }
                    return new NetworkResult(realEdges.toArray(AdductEdge[]::new), decoyEdges.toArray(AdductEdge[]::new));
                }
            }));
        }
        {
            Iterator<BasicJJob<NetworkResult>> iter = jobs.listIterator();
            while (iter.hasNext()) {
                NetworkResult r = iter.next().takeResult();
                for (AdductEdge e : r.realEdges) addEdge(e);
                for (AdductEdge e : r.decoyEdges) pValueStats.add(e);
                iter.remove(); // release memory
            }
        }
        pValueStats.done();
        assignPValues(pValueStats);

        BitSet visited = new BitSet(rtOrderedNodes.length);
        int numberOfIonsWeCouldAnnotate = 0;
        for (int k=0; k < rtOrderedNodes.length; ++k) {
            if (!visited.get(rtOrderedNodes[k].index)) {
                List<AdductNode> nodes = spread(rtOrderedNodes[k], visited);
                if (nodes.size()>1) {
                    subgraphs.add(nodes);
                    long adducts = nodes.stream().filter(x -> x.edges.stream().anyMatch(AdductEdge::isAdductEdge)).count();
                    if (adducts>0) {
                        numberOfIonsWeCouldAnnotate+=nodes.size();
                    }
                }
                else singletons.add(nodes.get(0));
            }
        }
        System.out.println("Number of potentially annotatable adducts: " + numberOfIonsWeCouldAnnotate);
    }

    private MassMap<Peak> getPotentialInsourceFragments(List<MergedMSnSpectrum> data, AdductNode rightNode) {
        if (!data.isEmpty()) {
            MassMap<Peak> potentialInsourceFragments = new MassMap<>(500);
            MergedMSnSpectrum mergedMSnSpectrum = data.stream().min(Comparator.comparingDouble(x->x.getMergedCollisionEnergy().getMaxEnergy(false))).get();
            SimpleSpectrum ms2 = mergedMSnSpectrum.getPeaks();
            double maximalIntensity = Spectrums.getMaximalIntensity(ms2);
            double intensityThreshold = 0.1*maximalIntensity;
            for (int k=0; k < ms2.size(); ++k) {
                if (ms2.getMzAt(k) < (rightNode.getMass()-4) && ms2.getIntensityAt(k)>=intensityThreshold) {
                    potentialInsourceFragments.put(ms2.getMzAt(k), ms2.getPeakAt(k));
                }
            }
            return potentialInsourceFragments;
        } else return null;
    }

    public void assign(JobManager manager, SubnetworkResolver resolver, int charge, Consumer<Compound> updateRoutine) {
        final ArrayList<BasicJJob<Object>> jobs = new ArrayList<>();
        for (List<AdductNode> subgraph : subgraphs) {
            jobs.add(manager.submitJob(new BasicJJob<Object>() {
                @Override
                protected Object compute() throws Exception {
                    AdductNode[] nodes = subgraph.toArray(AdductNode[]::new);
                    AdductAssignment[] assignments = resolver.resolve(nodes, charge);
                    HashMap<AdductNode, AdductAssignment> assignmentMap = new HashMap<>();
                    if (assignments!=null) {

                            for (int i = 0; i < assignments.length; ++i) {
                                List<DetectedAdduct> pas = assignments[i].toPossibleAdducts(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN);
                                if (!pas.isEmpty()) {
                                    AlignedFeatures feature = subgraph.get(i).getFeature();
                                    DetectedAdducts detectedAdducts = feature.getDetectedAdducts();
                                    if (detectedAdducts == null) {
                                        detectedAdducts = new DetectedAdducts();
                                        feature.setDetectedAdducts(detectedAdducts);
                                    }
                                    detectedAdducts.add(pas.toArray(DetectedAdduct[]::new));
                                }
                                assignmentMap.put(nodes[i], assignments[i]);
                            }
                            final HashSet<AdductNode> visited = new HashSet<>();
                            boolean before = false;
                            for (int i = 0; i < assignments.length; ++i) {
                                if (!visited.contains(nodes[i])) {
                                    Compound c = extractCompound(assignmentMap, visited, nodes[i], 0.5);
                                    updateRoutine.accept(c);
                                }
                            }
                        }
                    return "";
                }
            }));
        }
        jobs.add(manager.submitJob(new BasicJJob<Object>() {
            @Override
            protected Object compute() throws Exception {
                for (AdductNode node : singletons) {
                    updateRoutine.accept(singletonCompound(node));
                }
                return "";
            }
        }));
        jobs.forEach(JJob::takeResult);
    }



    public void assignWithDebugOutput(JobManager manager, SubnetworkResolver resolver, int charge, Consumer<Compound> updateRoutine) {
        final ArrayList<BasicJJob<Object>> jobs = new ArrayList<>();
        for (List<AdductNode> subgraph : subgraphs) {
            jobs.add(manager.submitJob(new BasicJJob<Object>() {
                @Override
                protected Object compute() throws Exception {
                    AdductNode[] nodes = subgraph.toArray(AdductNode[]::new);
                    AdductAssignment[] assignments = resolver.resolve(nodes, charge);
                    HashMap<AdductNode, AdductAssignment> assignmentMap = new HashMap<>();
                    if (assignments!=null) {

                        synchronized (AdductNetwork.class) {

                            System.out.println("~~~~~~~~    " + Arrays.stream(nodes).mapToDouble(AdductNode::getRetentionTime).average().orElse(0d) +  " min + (" + nodes.length + " nodes)   ~~~~~~~~~");

                            for (int i = 0; i < assignments.length; ++i) {
                                List<DetectedAdduct> pas = assignments[i].toPossibleAdducts(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN);
                                if (!pas.isEmpty()) {
                                    AlignedFeatures feature = subgraph.get(i).getFeature();
                                    DetectedAdducts detectedAdducts = feature.getDetectedAdducts();
                                    if (detectedAdducts == null) {
                                        detectedAdducts = new DetectedAdducts();
                                        feature.setDetectedAdducts(detectedAdducts);
                                    }
                                    detectedAdducts.add(pas.toArray(DetectedAdduct[]::new));
                                }
                                assignmentMap.put(nodes[i], assignments[i]);
                            }
                            final HashSet<AdductNode> visited = new HashSet<>();
                            boolean before = false;
                            for (int i = 0; i < assignments.length; ++i) {
                                if (!visited.contains(nodes[i])) {
                                    Compound c = extractCompound(assignmentMap, visited, nodes[i], 0.5);
                                    if (before ) {
                                        System.out.println("\n");
                                    }
                                    for (AlignedFeatures features : c.getAdductFeatures().get()) {
                                        System.out.println("Assign " +
                                                String.format(Locale.US, "%.4f @ %.2f", features.getApexMass(), (features.getRetentionTime().getRetentionTimeInSeconds() / 60d))
                                                + " minutes  with " + features.getDetectedAdducts().asMap().values().stream().flatMap(Collection::stream).map(x -> x.getAdduct() + " (" + x.getScore() + ")").collect(Collectors.joining(", ")) + (!provider.getMs2SpectraOf(features).isEmpty() ? "\thas MS/MS" : ""));

                                    }
                                    before = true;

                                    updateRoutine.accept(c);
                                }

                            }





                            System.out.println("~~~~~~~~~~~~~~~~~~~~");

                        }
                    }
                    return "";
                }
            }));
        }
        jobs.forEach(JJob::takeResult);
    }

    private Compound extractCompound(Map<AdductNode, AdductAssignment> assignments, Set<AdductNode> visited, AdductNode seed, double probabilityThreshold) {
        if (assignments.get(seed).likelyUnknown() || assignments.get(seed).probabilityOfMostLikelyAdduct() < probabilityThreshold) {
            return new Compound(0, seed.features.getRetentionTime(), null, null, seed.hasMsMs, Collections.singletonList(seed.features), Collections.emptyList());
        }
        ArrayList<AdductNode> compound = new ArrayList<>();
        ArrayList<CorrelatedIonPair> pairs = new ArrayList<>();
        HashSet<AdductNode> exclusion = new HashSet<>();
        compound.add(seed);
        visited.add(seed);
        DoubleArrayList mzs = new DoubleArrayList();
        double mzint = 0d;
        int done = 0;
        for (; done < compound.size(); ++done) {
            AdductNode u = compound.get(done);
            IonType ut = assignments.get(u).mostLikelyAdduct();
            if (ut.toPrecursorIonType().isPresent()) {
                mzs.add(ut.toPrecursorIonType().get().precursorMassToNeutralMass(u.getMass()) * u.features.getApexIntensity());
                mzint += u.features.getApexIntensity();
            }
            for (AdductEdge uv : u.edges) {

                AdductNode left = uv.left;
                AdductNode right = uv.right;

                // special rule: if two edges have MS/MS and cosine is very low, we exclude this node from the compound
                AdductNode v = uv.getOther(u);
                if (visited.contains(v)) continue;
                if (Float.isNaN(uv.ms2score)) {
                    // no MS/MS score
                    if (exclusion.contains(v)) continue;
                } else if (uv.ms2score <= 0.25) {
                    exclusion.add(v);
                    continue;
                } else {
                    // if at least one edge has high cosine, we forgive the low cosine edge
                }

                AdductAssignment la = assignments.get(left);
                AdductAssignment ra = assignments.get(right);
                if (la==null || ra==null) continue;
                IonType lt = la.mostLikelyAdduct();
                IonType rt = ra.mostLikelyAdduct();
                for (KnownMassDelta D : uv.explanations) {
                    if (!D.isCompatible(lt,rt))
                        continue;
                    visited.add(v);
                    exclusion.remove(v);
                    compound.add(v);
                    pairs.add(new CorrelatedIonPair(
                            0,
                            0,
                            u.features.getAlignedFeatureId(),
                            v.features.getAlignedFeatureId(),
                            u.features,
                            v.features,
                            typeFor(D),
                            (double)uv.getScore(),
                            Float.isFinite(uv.correlationScore) ? Double.valueOf(uv.correlationScore) : null,
                            Float.isFinite(uv.ms2score) ? Double.valueOf(uv.ms2score) : null
                    ));
                    break;
                }
            }
        }
        for (AdductNode u : exclusion) visited.remove(u);
        if (compound.isEmpty()) return null;
        double rt = 0d;
        double intens = 0d;
        double minRt = Double.POSITIVE_INFINITY, maxRt = Double.NEGATIVE_INFINITY;
        for (AdductNode n : compound) {
            rt += n.features.getApexIntensity() * n.features.getRetentionTime().getMiddleTime();
            intens += n.features.getApexIntensity();
            minRt = Math.min(minRt, n.features.getRetentionTime().getStartTime());
            maxRt = Math.max(maxRt, n.features.getRetentionTime().getEndTime());
        }
        rt /= intens;
        // sometimes rt is < minRt or > maxRt
        // this is probably just a floating point issue
        rt = Math.min(Math.max(minRt+1e-8, rt), maxRt-1e-8);
        if (compound.size()==1) {
            // damned, have to look closer into that. But if a compound cannot be resolved properly, then
            // adduct detection is likely wrong
            compound.get(0).features.getDetectedAdducts().add(DetectedAdduct.builder().adduct(PrecursorIonType.unknown(compound.get(0).getFeature().getCharge())).score(0.5d).source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN).build());
        }
        return new Compound(
                0,
                new RetentionTime(minRt, maxRt, rt),
                mzs.doubleStream().sum()/mzint,
                null,compound.stream().anyMatch(x->x.hasMsMs),
                compound.stream().map(AdductNode::getFeature).toList(),
                pairs
        );
    }

    public Compound singletonCompound(AdductNode n) {
        AlignedFeatures f = n.features;
        if (f.getDetectedAdducts()==null) {
            f.setDetectedAdducts(DetectedAdducts.singleton(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN, PrecursorIonType.unknown(f.getCharge())));
        }
        return new Compound(
                0,
                f.getRetentionTime(),
                null,
                f.getName(),
                n.hasMsMs,
                new ArrayList<>(List.of(f)),
                new ArrayList<>()
        );
    }

    private String pp(SimpleSpectrum spec) {
        StringBuilder buf = new StringBuilder();
        SimpleSpectrum xs =Spectrums.getNormalizedSpectrum(spec, Normalization.Max);
        xs = Spectrums.extractMostIntensivePeaks(xs, 5, 300);
        for (Peak p : xs) {
            buf.append(String.format(Locale.US, "\t%.4f\t%.3f\n", p.getMass(), p.getIntensity()));
        }
        return buf.toString();
    }

    private CorrelatedIonPair.Type typeFor(KnownMassDelta D) {
        if (D instanceof AdductRelationship) return CorrelatedIonPair.Type.ADDUCT;
        if (D instanceof LossRelationship) return CorrelatedIonPair.Type.INSOURCE;
        if (D instanceof UnknownLossRelationship) return CorrelatedIonPair.Type.INSOURCE;
        if (D instanceof MultimereRelationship) return CorrelatedIonPair.Type.MULTIMERE;
        return CorrelatedIonPair.Type.UNKNOWN;
    }

    private void assignPValues(PValueStats pValueStats) {
        int count = 0;
        for (AdductNode node : rtOrderedNodes) {
            for (AdductEdge e : node.getEdges()){
                if (Double.isNaN(e.pvalue)) {
                    double score = pValueStats.logPvalue(e);
                    // for ms2 we do not compute p-values as this would take quite a lot of time
                    // so we just "guess" an exponential distribution
                    double ms2value = Float.isNaN(e.ms2score) ? 0f : (e.ms2score - 0.25)*4;
                    e.pvalue = (float) (score + ms2value);
                    ++count;
                }
            }
        }
        double correction = Math.log(Math.sqrt(count))+1d;
        int above5=0, aboveCorrect=0;
        for (AdductNode node : rtOrderedNodes) {
            for (AdductEdge e : node.getEdges()){
                if (e.getLeft()==node) {
                    if (e.getScore()>=5) ++above5;
                    if (e.getScore()>=(correction)) ++aboveCorrect;
                }
            }
        }
        System.out.println(count + " edges in total, so correction would be " + Math.log(count));
        System.out.println(above5 + " edges have a score above 5, " + aboveCorrect + " also have a score above correction value.");
        deleteEdgesWithLowPvalue(Math.max(correction, 3d));
    }

    private void deleteEdgesWithLowPvalue(double threshold) {
        for (AdductNode node : rtOrderedNodes) {
            Iterator<AdductEdge> iterator = node.edges.iterator();
            while (iterator.hasNext()) {
                AdductEdge e = iterator.next();
                if (-e.pvalue < threshold) {
                    iterator.remove();
                }
            }
        }
    }

    private void addEdge(AdductEdge edge) {
        edge.left.edges.add(edge);
        edge.right.edges.add(edge);
    }

    private List<AdductNode> spread(AdductNode seed, BitSet colors) {
        ArrayList<AdductNode> stack = new ArrayList<>();
        stack.add(seed);
        colors.set(seed.index);
        int i = 0;
        while (i < stack.size()) {
            AdductNode n = stack.get(i);
            for (AdductNode m : n.getNeighbours()) {
                if (!colors.get(m.index)) {
                    stack.add(m);
                    colors.set(m.index);
                }
            }
            ++i;
        }
        return stack;
    }

    protected AdductNode[] findNodesByRt(double rtFrom, double rtTo) {
        if (rtTo < rtFrom) {
            throw new IllegalArgumentException("Illegal mass range: rtFrom = " + rtFrom +  " and is larger than rtTo = " + rtTo);
        }
        int i = BinarySearch.searchForDoubleByIndex(j-> rtOrderedNodes[j].getRetentionTime(),
                0, rtOrderedNodes.length, rtFrom);
        if (i < 0)  {
            i = -(i + 1);
        }
        if (i >= rtOrderedNodes.length || rtOrderedNodes[i].getMass() > rtTo) return new AdductNode[0];
        final int start = i;
        for (i=start+1; i < rtOrderedNodes.length; ++i) {
            if (rtOrderedNodes[i].getRetentionTime() > rtTo) {
                break;
            }
        }
        final int end = i;
        final AdductNode[] sublist = new AdductNode[start-end];
        System.arraycopy(rtOrderedNodes, start, sublist, 0, end-start);
        return sublist;
    }

    class PValueStats {
        private IntArrayList ratioScore5, cor1, cor2;
        private double[] pvalueRatio, pvalueCor1, pvalueCor2;
        private double pbase;
        private int count;

        public PValueStats() {
            ratioScore5 = new IntArrayList();
            cor1 = new IntArrayList();
            cor2 = new IntArrayList();
            count = 0;
        }

        private void done() {
            pvalueRatio = new double[ratioScore5.size()+1];
            pvalueCor1 = new double[cor1.size()+1];
            pvalueCor2 = new double[cor2.size()+1];

            int cumsum=1;
            double base = Math.log(count+1);
            pbase=base;
            pvalueRatio[pvalueRatio.length-1] = Math.log(cumsum)-base;
            for (int i=ratioScore5.size()-1; i >= 0; --i) {
                cumsum += ratioScore5.getInt(i);
                double logprob = Math.log(cumsum) - base;
                pvalueRatio[i] = logprob;
            }
            cumsum=1;
            pvalueCor1[pvalueCor1.length-1] = Math.log(cumsum)-base;
            for (int i=cor1.size()-1; i >= 0; --i) {
                cumsum += cor1.getInt(i);
                double logprob = Math.log(cumsum) - base;
                pvalueCor1[i] = logprob;
            }
            cumsum=1;
            pvalueCor2[pvalueCor2.length-1] = Math.log(cumsum)-base;
            for (int i=cor2.size()-1; i >= 0; --i) {
                cumsum += cor2.getInt(i);
                double logprob = Math.log(cumsum) - base;
                pvalueCor2[i] = logprob;
            }
        }

        public double logPvalue(AdductEdge edge) {
            double pvalue = 0d;
            if (Double.isFinite(edge.ratioScore)) {
                int b = ratio2bin(edge.ratioScore);
                if (b >= pvalueRatio.length) pvalue += pvalueRatio[pvalueRatio.length-1];
                else pvalue += pvalueRatio[b];
            }
            if (Double.isFinite(edge.correlationScore)) {
                int b = cor2bin(edge.correlationScore);
                if (b >= pvalueCor1.length) pvalue += pvalueCor1[pvalueCor1.length-1];
                else pvalue += pvalueCor1[b];
            }
            if (Double.isFinite(edge.representativeCorrelationScore)) {
                int b = cor2bin(edge.representativeCorrelationScore);
                if (b >= pvalueCor2.length) pvalue += pvalueCor2[pvalueCor2.length-1];
                else pvalue += pvalueCor2[b];
            }
            return pvalue;
        }



        public void add(AdductEdge edge) {
            ++count;
            if (Double.isFinite(edge.ratioScore)) {
                int b = ratio2bin(edge.ratioScore);
                while (b >= ratioScore5.size()) ratioScore5.add(0);
                ratioScore5.set(b, ratioScore5.getInt(b)+1);
            }
            if (Double.isFinite(edge.correlationScore)) {
                int b = cor2bin(edge.correlationScore);
                while (b >= cor1.size()) cor1.add(0);
                cor1.set(b, cor1.getInt(b)+1);
            }
            if (Double.isFinite(edge.representativeCorrelationScore)) {
                int b = cor2bin(edge.representativeCorrelationScore);
                while (b >= cor2.size()) cor2.add(0);
                cor2.set(b, cor2.getInt(b)+1);
            }
        }

        private int ratio2bin(double score) {
            return (int)Math.max(0,Math.round((score+0)/5));
        }
        private static double[] corbins = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.75, 0.8, 0.85, 0.9, 0.92, 0.94, 0.96, 0.97, 0.98, 0.99, 0.995};
        private int cor2bin(double score) {
            int index = Arrays.binarySearch(corbins, score);
            if (index >= 0) return index;
            else return -index +1;
        }
    }

}
