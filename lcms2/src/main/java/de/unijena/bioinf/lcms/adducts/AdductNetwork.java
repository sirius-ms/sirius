package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.algorithm.BinarySearch;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.math.MatrixUtils;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Normalization;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ChemistryBase.utils.IOFunctions;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.jjobs.JobManager;
import de.unijena.bioinf.lcms.adducts.assignment.AdductAssignment;
import de.unijena.bioinf.lcms.adducts.assignment.SubnetworkResolver;
import de.unijena.bioinf.lcms.isotopes.IsotopePattern;
import de.unijena.bioinf.ms.persistence.model.core.Compound;
import de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures;
import de.unijena.bioinf.ms.persistence.model.core.feature.CorrelatedIonPair;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdducts;
import de.unijena.bioinf.ms.persistence.model.core.spectrum.MergedMSnSpectrum;
import de.unijena.bioinf.ms.persistence.model.core.trace.TraceRef;
import de.unijena.bioinf.sirius.elementdetection.TransformerElementDetector;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerBasedPredictor;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerPrediction;
import de.unijena.bionf.fastcosine.SearchPreparedSpectrum;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

public class AdductNetwork {

    protected AdductNode[] rtOrderedNodes;
    protected AdductManager adductManager;
    List<List<AdductNode>> subgraphs = new ArrayList<>();
    List<AdductNode> singletons = new ArrayList<>();

    protected Deviation deviation;
    ProjectSpaceTraceProvider provider;

    double retentionTimeTolerance, expectedPeakWidth;
    PValueStats pValueStats;

    public AdductNetwork(ProjectSpaceTraceProvider provider, AlignedFeatures[] features, AdductManager manager, double retentionTimeTolerance, double expectedPeakWidth) {
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
        this.expectedPeakWidth = expectedPeakWidth;
    }

    record NetworkResult(AdductEdge[] realEdges, AdductEdge[] decoyEdges, int densityEstimate) {
    }

    public void buildNetworkFromMassDeltas(JobManager jjobs) {
        Scorer scorer = new Scorer(this.retentionTimeTolerance/3d);

        // we exclude the begin and the end of the LC from p-value estimation, as these might be void volume or
        // high pressure
        double voidVolumeStart = rtOrderedNodes[0].getRetentionTime();
        double voidVolumeEnd = voidVolumeStart + 4*retentionTimeTolerance;
        double endOfLc = rtOrderedNodes[rtOrderedNodes.length-1].getRetentionTime() - 4*retentionTimeTolerance;
        List<BasicJJob<NetworkResult>> jobs = new ArrayList<>();
        for (int rr=0; rr < rtOrderedNodes.length; ++rr) {
            final int r = rr;
            jobs.add(jjobs.submitJob(new BasicJJob<NetworkResult>() {
                @Override
                protected NetworkResult compute() throws Exception {
                    List<AdductEdge> realEdges = new ArrayList<>();
                    List<AdductEdge> decoyEdges = new ArrayList<>();
                    final AdductNode rightNode = rtOrderedNodes[r];
                    final double thresholdStart = Math.min(rtOrderedNodes[r].getFeature().getRetentionTime().getMiddleTime() - retentionTimeTolerance,
                            //rtOrderedNodes[r].getFeature().getRetentionTime().getStartTime());
                            Math.max(rtOrderedNodes[r].getFeature().getRetentionTime().getStartTime(), rtOrderedNodes[r].getFeature().getRetentionTime().getMiddleTime() - 2*retentionTimeTolerance));
                    final double thresholdEnd = Math.max(rtOrderedNodes[r].getFeature().getRetentionTime().getMiddleTime() + retentionTimeTolerance,
                            //rtOrderedNodes[r].getFeature().getRetentionTime().getEndTime());
                            Math.min(rtOrderedNodes[r].getFeature().getRetentionTime().getEndTime(), rtOrderedNodes[r].getFeature().getRetentionTime().getMiddleTime() + 2*retentionTimeTolerance));
                    final Range<Double> threshold = Range.of(thresholdStart, thresholdEnd);

                    // obtain potential fragment peaks
                    List<MergedMSnSpectrum> ms2Right = provider.getMs2SpectraOf(rightNode.getFeatures());
                    if (!ms2Right.isEmpty()) rightNode.hasMsMs = true;
                    Optional<SearchPreparedSpectrum> preparedRight = null;
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
                                List<KnownMassDelta> knownMassDeltas = new ArrayList<>();
                                List<Peak> potentialInsourcePeaks = potentialInsourceFragments == null ? Collections.emptyList() : potentialInsourceFragments.retrieveAll(leftNode.getMass(), deviation);
                                if (!potentialInsourcePeaks.isEmpty()) {
                                    UnknownLossRelationship insourceFragment = new UnknownLossRelationship();
                                    knownMassDeltas.add(insourceFragment);
                                } else {
                                    knownMassDeltas.addAll(adductManager.retrieveMassDeltasWithNoAmbiguity(massDelta, deviation));
                                }

                                // add multimere edge if present
                                adductManager.checkForMultimere(rightNode.getMass(), leftNode.getMass(), deviation).ifPresent(knownMassDeltas::add);

                                // remove adduct edges that require a non-matching isotope pattern
                                removeIsotopePatternMissmatchInAdductEdges(knownMassDeltas, leftNode, rightNode, provider);

                                if (!knownMassDeltas.isEmpty()) {
                                    final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, knownMassDeltas.toArray(KnownMassDelta[]::new));
                                    scorer.computeScore(provider, adductEdge);
                                    if (adductEdge.isValid()) {
                                        // add MS/MS score
                                        if (!ms2Right.isEmpty()) {
                                            List<MergedMSnSpectrum> ms2Left = provider.getMs2SpectraOf(leftNode.getFeatures());
                                            if (!ms2Left.isEmpty()) {
                                                if (preparedRight == null) {
                                                    preparedRight = scorer.prepareForCosine(rightNode, ms2Right);
                                                }
                                                if (preparedRight.isPresent()) {
                                                    Optional<SearchPreparedSpectrum> ms2left =scorer.prepareForCosine(leftNode, ms2Left);
                                                    if (ms2left.isPresent()) {
                                                        scorer.computeMs2Score(adductEdge, ms2left.get(), preparedRight.get());
                                                    }
                                                }
                                            }
                                        }

                                        realEdges.add(adductEdge);
                                    }
                                    // CHECK FOR ISOTOPES
                                } else {
                                    int isotopeShift = maybeIsotope(massDelta, leftNode, rightNode);
                                    if (isotopeShift>=0) {
                                        final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, new KnownMassDelta[]{new IsotopeRelationship(isotopeShift+1)});
                                        scorer.computeScore(provider, adductEdge);
                                        if (adductEdge.isValid()) {
                                            realEdges.add(adductEdge);
                                        }
                                    } else if (rt.getStartTime() > voidVolumeEnd && rt.getEndTime() < endOfLc && decoyEdges.size() < 10 && adductManager.hasDecoy(massDelta)) {
                                        final AdductEdge adductEdge = new AdductEdge(leftNode, rightNode, new KnownMassDelta[0]);
                                        scorer.computeScore(provider, adductEdge);
                                        if (adductEdge.isValid()) {
                                            decoyEdges.add(adductEdge);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return new NetworkResult(realEdges.toArray(AdductEdge[]::new), decoyEdges.toArray(AdductEdge[]::new), rEnd-rStart+1);
                }
            }));
        }
        {
            Iterator<BasicJJob<NetworkResult>> iter = jobs.listIterator();
            while (iter.hasNext()) {
                NetworkResult r = iter.next().takeResult();
                for (AdductEdge e : r.realEdges) addEdge(e);
                for (AdductEdge e : r.decoyEdges) pValueStats.add(e);
                pValueStats.addDensity(r.densityEstimate);
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
                } else {
                    singletons.add(nodes.get(0));
                }
            }
        }
    }

    private void removeIsotopePatternMissmatchInAdductEdges(List<KnownMassDelta> knownMassDeltas, AdductNode leftNode, AdductNode rightNode, ProjectSpaceTraceProvider provider) {
        ListIterator<KnownMassDelta> iter = knownMassDeltas.listIterator();
        while (iter.hasNext()) {
            KnownMassDelta m = iter.next();
            if (m instanceof AdductRelationship) {
                PrecursorIonType left = ((AdductRelationship) m).left;
                PrecursorIonType right = ((AdductRelationship) m).right;
                if (checkIsotopePatternForAdduct(leftNode, left, provider) && checkIsotopePatternForAdduct(rightNode,right,provider)) {
                    // isotope pattern not existing or okay
                } else {
                    // isotope pattern missmatches adduct type
                    iter.remove();
                }
            }
        }
    }
    private final Element Cl = PeriodicTable.getInstance().getByName("Cl");
    private final Element Br = PeriodicTable.getInstance().getByName("Br");
    private boolean checkIsotopePatternForAdduct(AdductNode u, PrecursorIonType ionType, ProjectSpaceTraceProvider provider) {
        if (ionType.getIonization().getAtoms().numberOf(Cl)>0 || ionType.getAdduct().numberOf(Cl)>0) {
            return checkIsotopePatternForAdduct(u, provider, Cl);
        } else if (ionType.getIonization().getAtoms().numberOf(Br)>0 || ionType.getAdduct().numberOf(Br)>0) {
            return checkIsotopePatternForAdduct(u, provider, Br);
        } else return true;
    }
    private boolean checkIsotopePatternForAdduct(AdductNode u, ProjectSpaceTraceProvider provider, Element e) {
        final Optional<SimpleSpectrum> iso = provider.getIsotopes(u.getFeatures());
        if (iso.isPresent() && iso.get().size()>=2) {
            SimpleSpectrum patter = iso.get();
            Optional<TransformerPrediction> predict = predictor.predict(patter, 0);
            if (predict.isPresent()) {
                float[] logits = predict.get().getLogits();
                Element[] elems = predict.get().getDetectableElements();
                for (int k=0; k < elems.length; ++k) {
                    if (elems[k].equals(e) && logits[k] < -5) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private TransformerBasedPredictor predictor = new TransformerElementDetector().getPredictor();
    private int maybeIsotope(double massDelta, AdductNode leftNode, AdductNode rightNode) {
        int charge = leftNode.getFeature().getCharge();
        if (rightNode.getFeature().getCharge()!=charge) return -1;
        final int isotopeShift = IsotopePattern.getPossibleIsotopeShift(leftNode.getMass(), rightNode.getMass(),charge);
        if (isotopeShift<0) return -1;
        Deviation dev = new Deviation(10);
        Optional<SimpleSpectrum> l = provider.getIsotopes(leftNode.getFeature());
        if (l.isPresent()) {
            SimpleSpectrum s = l.get();
            int li = Spectrums.indexOfPeakClosestToMassWithin(s, leftNode.getMass(), dev);
            int ri = Spectrums.indexOfPeakClosestToMassWithin(s, rightNode.getMass(), dev);
            if (li>=0 && ri>=0 && dev.inErrorWindow(s.getMzAt(ri)-s.getMzAt(li),massDelta)) {
                // if peak is ms1-only, and has lower intensity than the peak before, we can safely
                // check if peak is non-monoisotopic
                Optional<TransformerPrediction> predict = predictor.predict(s, ri);
                if (predict.isEmpty()) return isotopeShift;
                else {
                    return -1;
                }
            }
        }
        return -1;
    }

    private MassMap<Peak> getPotentialInsourceFragments(List<MergedMSnSpectrum> data, AdductNode rightNode) {
        if (!data.isEmpty()) {
            MassMap<Peak> potentialInsourceFragments = new MassMap<>(500);
            CollisionEnergy lowestCe = data.stream().map(MergedMSnSpectrum::getMergedCollisionEnergy).min(Comparator.comparingDouble(x->x.getMaxEnergy(false))).orElse(null);
            List<SimpleSpectrum> spectra = data.stream().filter(x->lowestCe==null || !(x.getMergedCollisionEnergy().greaterThan(lowestCe))).map(x->x.getPeaks()).toList();
            SimpleSpectrum peaks = Spectrums.mergeSpectra(deviation, true, false, spectra);

            double maximalIntensity = Spectrums.getMaximalIntensity(peaks);
            double intensityThreshold = 0.05*maximalIntensity;
            for (int k=0; k < peaks.size(); ++k) {
                if (peaks.getMzAt(k) < (rightNode.getMass()-4) && peaks.getIntensityAt(k)>=intensityThreshold) {
                    potentialInsourceFragments.put(peaks.getMzAt(k), peaks.getPeakAt(k));
                }
            }
            return potentialInsourceFragments;
        } else return null;
    }

    /**
     * Call the given delete function for all isotopic peaks in the network. Delete these peaks from the network afterwards.
     */
    public void deisotope(IOFunctions.IOConsumer<AlignedFeatures> deleteFunctionForFeatures) throws IOException {
        {
            // collect all isotopes
            final HashSet<AdductNode> toDel = new HashSet<>();
            final ListIterator<AdductNode> singletonIter = singletons.listIterator();
            for (AdductNode u : singletons) {
                u.collectIsotopes(toDel);
            }
            for (List<AdductNode> us : subgraphs) {
                us.forEach(u->u.collectIsotopes(toDel));
            }
            // delete all isotopes
            {
                toDel.forEach(this::deleteNode);
                singletons.removeAll(toDel);
                for (List<AdductNode> sub : subgraphs) {
                    sub.removeAll(toDel);
                }
            }
            // call appropiate functions
            for (AdductNode u : toDel) {
                deleteFunctionForFeatures.accept(u.features);
            }

        }
        // repeat the same thing on networks
        {
            final ListIterator<List<AdductNode>> singletonIter = subgraphs.listIterator();
            while (singletonIter.hasNext()) {
                List<AdductNode> vs = singletonIter.next();
                Iterator<AdductNode> vi = vs.iterator();
                while (vi.hasNext()) {
                    AdductNode v = vi.next();
                    if (v.isIsotopeNode()) {
                        deleteNode(v);
                        deleteFunctionForFeatures.accept(v.features);
                        vi.remove();
                    }
                }
            }
        }
    }

    private void deleteNode(AdductNode v) {
        final Iterator<AdductEdge> e = v.getEdges().iterator();
        while (e.hasNext()) e.next().getOther(v).removeEdgeTo(v);
    }

    public Set<PrecursorIonType> assignNetworksAndAdductsToFeatures(JobManager manager, SubnetworkResolver resolver, int charge, IOFunctions.IOConsumer<AlignedFeatures> updateRoutineForFeatures,
                                                   IOFunctions.IOFunction<de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork, Long> updateRoutineForNetworks,
                                                   IOFunctions.IOFunction<de.unijena.bioinf.ms.persistence.model.core.feature.AlignedFeatures, Long> feature2compoundRoutine) throws IOException {
        final ArrayList<BasicJJob<HashMap<AdductNode, AdductAssignment>>> jobs = new ArrayList<>();
        for (List<AdductNode> subgraph : subgraphs) {
            jobs.add(manager.submitJob(new BasicJJob<HashMap<AdductNode, AdductAssignment>>() {
                @Override
                protected HashMap<AdductNode, AdductAssignment> compute() throws Exception {
                    AdductNode[] nodes = subgraph.toArray(AdductNode[]::new);
                    AdductAssignment[] assignments = resolver.resolve(adductManager, nodes, charge);
                    HashMap<AdductNode, AdductAssignment> assignmentMap = new HashMap<>();

                    for (int i = 0; i < nodes.length; i++) {
                        if (assignments != null) {
                            assignmentMap.put(nodes[i], assignments[i]);
                        }
                    }
                    return assignmentMap;
                }
            }));
        }
        Set<PrecursorIonType> detectedAdducts = new HashSet<>();

        // DEBUG
        HashMap<String, Integer> uniqueIonModeCounter = new HashMap<>();
        HashMap<String, Double> adductCounter = new HashMap<>();
        int ambigous = 0; int networkNodes=0; int unambigous=0;
        // DEBUG END

        ListIterator<BasicJJob<HashMap<AdductNode, AdductAssignment>>> iter = jobs.listIterator();
        while (iter.hasNext()) {
            HashMap<AdductNode, AdductAssignment> map = iter.next().takeResult();
            // insert networks into project space
            Map<AdductNode, de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork> networks = addAdductNetworksToFeatures(map);
            for (de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork network : Set.copyOf(networks.values())) {
                long id = updateRoutineForNetworks.apply(network);
                if (network.getNetworkId()!=id) {
                    network.setNetworkId(id);
                }
            }
            // insert adduct information
            for (AdductNode node : networks.keySet()) {
                AlignedFeatures feature = node.features;
                addAssignmentsToFeature(node, map.get(node));
                feature.setAdductNetworkId(networks.get(node).getNetworkId());
                // insert compounds
                feature2compoundRoutine.apply(feature);
            }
            // finally update all compounds in database
            for (AdductNode node : map.keySet()) {
                updateRoutineForFeatures.accept(node.features);
                // collect all detected adducts as return value
                node.features.getDetectedAdducts().getDetectedAdductsStr().map(DetectedAdduct::getAdduct)
                        .filter(Objects::nonNull)
                        .forEach(detectedAdducts::add);
            }

            // DEBUG
            for (AdductNode node : map.keySet()) {
                List<PrecursorIonType> allAdducts = node.features.getDetectedAdducts().getAllAdducts();
                allAdducts.forEach(x -> adductCounter.put(x.toString(), adductCounter.getOrDefault(x.toString(), 0d) + 1d / allAdducts.size()));
                List<Ionization> list = allAdducts.stream().filter(x -> x.hasNeitherAdductNorInsource()).map(x -> x.getIonization()).distinct().toList();
                if (list.size() == 1) {
                    ++unambigous;
                    uniqueIonModeCounter.put(list.get(0).toString(), uniqueIonModeCounter.getOrDefault(list.get(0).toString(),0)+1);
                } else {
                    ++ambigous;
                }
                ++networkNodes;
            }
            // DEBUG END
            iter.set(null); // free memory
        }
        // also process all singleton nodes
        for (AdductNode node : singletons) {
            AlignedFeatures f = node.features;
            if (f.getDetectedAdducts() == null) {
                f.setDetectedAdducts(DetectedAdducts.emptySingleton(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN));
            } else {
                f.getDetectedAdducts().addEmptySource(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN);
            }
            f.setAdductNetworkId(null);
            feature2compoundRoutine.apply(f);
            updateRoutineForFeatures.accept(f);
            // collect all detected adducts as return value
            f.getDetectedAdducts().getDetectedAdductsStr().map(DetectedAdduct::getAdduct)
                    .filter(Objects::nonNull)
                    .forEach(detectedAdducts::add);
        }
        // DEBUG
        /*
        System.out.println("##########################################");
        System.out.println("Of " + (singletons.size() + networkNodes) + " features, " + singletons.size() + " are singletons and " + networkNodes +
                " features are part of an adduct network. " + ambigous + " nodes have ambigous annotations, " +  unambigous + " have unambigous annotation.");
        System.out.println(uniqueIonModeCounter);
        System.out.println("------");
        System.out.println(adductCounter);
        System.out.println("##########################################");
        */
        return detectedAdducts;

    }

    /*
    public void assign(JobManager manager, SubnetworkResolver resolver, int charge, IOFunctions.IOConsumer<Compound> updateRoutine) {
        final ArrayList<BasicJJob<Object>> jobs = new ArrayList<>();
        for (List<AdductNode> subgraph : subgraphs) {
            jobs.add(manager.submitJob(new BasicJJob<Object>() {
                @Override
                protected Void compute() throws Exception {
                    AdductNode[] nodes = subgraph.toArray(AdductNode[]::new);
                    AdductAssignment[] assignments = resolver.resolve(adductManager, nodes, charge);
                    HashMap<AdductNode, AdductAssignment> assignmentMap = new HashMap<>();

                    for (int i = 0; i < nodes.length; i++)
                        if (assignments != null)
                            assignmentMap.put(nodes[i], assignments[i]);

                    final HashSet<AdductNode> visited = new HashSet<>();
                    for (AdductNode node : nodes)
                        if (!visited.contains(node))
                            updateRoutine.accept(extractCompound(assignmentMap, visited, node, 0.5));
                    return null;
                }
            }));
        }
        jobs.add(manager.submitJob(new BasicJJob<Object>() {
            @Override
            protected Void compute() throws Exception {
                for (AdductNode node : singletons) {
                    updateRoutine.accept(singletonCompound(node));
                }
                return null;
            }
        }));
        jobs.forEach(JJob::takeResult);
    }
     */

    private Map<AdductNode, de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork> addAdductNetworksToFeatures(HashMap<AdductNode, AdductAssignment> map) {
        // we first have to split the nodes into subnetworks AFTER knowing about their assignments (as some incompatible edges might
        // split the graph into connection components
        Map<AdductNode, de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork> networks = new HashMap<>();
        Map<AdductNode, de.unijena.bioinf.ms.persistence.model.core.networks.AdductNode> translate = new HashMap<>();

        for (AdductNode node : map.keySet()) {
            if (!networks.containsKey(node)) {
                // generate new network
                de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork network = new de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork();
                network.setEgdes(new ArrayList<>());
                network.setNodes(new ArrayList<>());
                // spread over all connected nodes
                spreadAdductNetworks(node, network, translate, map, networks);
            }
        }
        return networks;

    }

    private void spreadAdductNetworks(AdductNode node, de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork network, Map<AdductNode, de.unijena.bioinf.ms.persistence.model.core.networks.AdductNode> translate, HashMap<AdductNode, AdductAssignment> map, Map<AdductNode, de.unijena.bioinf.ms.persistence.model.core.networks.AdductNetwork> networks) {
        de.unijena.bioinf.ms.persistence.model.core.networks.AdductNode newNode = new de.unijena.bioinf.ms.persistence.model.core.networks.AdductNode();
        newNode.setTraceId(node.features.getTraceReference().stream().mapToLong(TraceRef::getTraceId).findFirst().orElse(-1));
        newNode.setAlignedFeatureId(node.features.getAlignedFeatureId());
        newNode.setPossibleAdducts(Arrays.stream(map.get(node).getIonTypes()).map(x -> x.toPrecursorIonType().orElse(null)).toArray(PrecursorIonType[]::new));
        newNode.setAdductProbabilities(MatrixUtils.double2float(map.get(node).getProbabilities()));
        newNode.setMz(node.getMass());
        translate.put(node, newNode);

        networks.put(node, network);
        network.getNodes().add(newNode);

        for (AdductEdge e : node.getEdges()) {

            AdductNode leftNode = e.left, rightNode = e.right;

            if (!map.containsKey(e.getLeft()) || !map.containsKey(e.getRight())) continue;
            if (translate.containsKey(e.getLeft()) && translate.containsKey(e.getRight())) continue;
            boolean compatible = false;
            IonType cleft=null,cright=null;
            outer:
            for (KnownMassDelta delta : e.getExplanations()) {
                for (IonType left : map.get(e.getLeft()).getIonTypes()) {
                    for (IonType right : map.get(e.getRight()).getIonTypes()) {
                        if (delta.isCompatible(left,right)) {
                            compatible=true;
                            cleft=left;
                            cright=right;
                            break outer;
                        }
                    }
                }
            }
            if (/*compatible*/ true) {
                // add edge
                de.unijena.bioinf.ms.persistence.model.core.networks.AdductEdge edge = new de.unijena.bioinf.ms.persistence.model.core.networks.AdductEdge();
                edge.setPvalue(e.pvalue);
                edge.setMergedCorrelation(e.interSampleCorrelation);
                edge.setRepresentativeCorrelation(e.interSampleCorrelationRepresentative);
                edge.setMs2cosine(e.ms2score);
                edge.setIntensityRatioScore(e.extraSampleCorrelation);
                edge.setLeftFeatureId(leftNode.getFeature().getAlignedFeatureId());
                edge.setRightFeatureId(rightNode.getFeature().getAlignedFeatureId());

                String adductLabel=null, insourceLabel=null;
                for (KnownMassDelta d : e.getExplanations()) {
                    if (d instanceof LossRelationship) {
                        insourceLabel = (((LossRelationship) d).formula).toString();
                    } else if (compatible && d instanceof AdductRelationship) {
                        if (d.isCompatible(cleft,cright)) {
                            adductLabel = simplifyEdgeName(((AdductRelationship) d).left, ((AdductRelationship) d).right);
                        }
                    }
                }
                if (insourceLabel!=null) edge.setLabel(insourceLabel);
                else if (adductLabel!=null) edge.setLabel(adductLabel);
                else edge.setLabel("");
                networks.get(node).getEgdes().add(edge);
                // attach new node
                spreadAdductNetworks(e.getOther(node), network, translate, map, networks);
            }
        }
    }

    private String simplifyEdgeName(PrecursorIonType left, PrecursorIonType right) {
        if (left.getMultimereCount()!=right.getMultimereCount()) {
            return left.toString() + " -> " + right.toString();
        }
        if (!left.getIonization().equals(right.getIonization())) {
            if (left.getModification().equals(right.getModification())) {
                return left.getIonization().getAtoms().toString() + (left.getCharge()>0 ? "+" : "-") + " -> " + right.getIonization().getAtoms().toString() + (right.getCharge()>0 ? "+" : "-");
            } else return left.toString() + " -> " + right.toString();
        }
        return shortForm(left) + " -> " + shortForm(right);
    }

    private String shortForm(PrecursorIonType type) {
        String ad = type.getAdductAndIons().toString() + (type.getCharge()>0 ? "+" : "-");
        String insource = type.getInSourceFragmentation().toString();
        if (insource.isEmpty()) return ad;
        else return ad + " - " + insource;
    }

    private void addAssignmentsToFeature(@NotNull AdductNode adductNode, @NotNull AdductAssignment assignment){
        final AlignedFeatures feature = adductNode.features;
        final List<DetectedAdduct> pas = new ArrayList<>(assignment.toPossibleAdducts(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN));
        if (!pas.isEmpty()) {
            DetectedAdducts detectedAdducts = feature.getDetectedAdducts();
            if (detectedAdducts == null) {
                detectedAdducts = new DetectedAdducts();
                feature.setDetectedAdducts(detectedAdducts);
            }
            detectedAdducts.addAll(pas);
        }
    }
    private String toDot(AdductNode node) {
        return toDot(spread(node, new BitSet()));
    }
    private String toDot(Map<AdductNode, AdductAssignment> assignments) {
        List<AdductNode> subgraph = new ArrayList<>(assignments.keySet());
        StringBuilder buffer = new StringBuilder();
        buffer.append("strict digraph {\n");
        for (AdductNode node : subgraph) {
            buffer.append("v").append(node.index).append("[label=\"").append(String.format(Locale.US, "%s\n%.2f", assignments.get(node).mostLikelyAdduct().toString(), node.getMass())).append("\"]\n");
        }
        for (AdductNode node : subgraph) {
            for (AdductEdge edge : node.edges) {
                if (edge.getLeft()==node) {
                    buffer.append("v").append(node.index).append(" -> ").append("v").append(edge.getRight().index);
                    buffer.append("[label=\"").append(String.format(Locale.US, "%.2f", edge.getRight().getMass()-edge.getLeft().getMass()));
                    for (KnownMassDelta d : edge.getExplanations()) {
                        if (d.isCompatible(assignments.get(edge.getLeft()).mostLikelyAdduct(), assignments.get(edge.getRight()).mostLikelyAdduct())) {
                            if (!(d instanceof AdductRelationship)) {
                                buffer.append("\n").append(d.toString());
                                break;
                            }
                        }
                    }
                    buffer.append("\"]\n");
                }
            }
        }
        buffer.append("}\n");
        return buffer.toString();
    }

    private String toDot(List<AdductNode> subgraph) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("strict digraph {\n");
        for (AdductNode node : subgraph) {
            buffer.append("v").append(node.index).append("[label=\"").append(String.format(Locale.US, "%.2f", node.getMass())).append("\"]\n");
        }
        for (AdductNode node : subgraph) {
            for (AdductEdge edge : node.edges) {
                if (edge.getLeft()==node) {
                    buffer.append("v").append(node.index).append(" -> ").append("v").append(edge.getRight().index)
                            .append(" [label=<").append((int)Math.round(edge.getLeft().getMass()-edge.getRight().getMass())).append("<BR/><FONT POINT-SIZE=\"5\">")
                            .append(edge.getExplanations()[0].toString().replace("-->", "→")).append("</FONT>").append(">]\n");
                }
            }
        }
        buffer.append("}\n");
        return buffer.toString();
    }

    private Compound extractCompound(Map<AdductNode, AdductAssignment> assignments, Set<AdductNode> visited, AdductNode seed, double probabilityThreshold) {
        if (!assignments.containsKey(seed) || assignments.get(seed).likelyUnknown() || assignments.get(seed).probabilityOfMostLikelyAdduct() < probabilityThreshold)
            return singletonCompound(seed);

        ArrayList<AdductNode> compound = new ArrayList<>();
        ArrayList<CorrelatedIonPair> pairs = new ArrayList<>();
        HashSet<AdductNode> exclusion = new HashSet<>();
        compound.add(seed);
        visited.add(seed);
        addAssignmentsToFeature(seed, assignments.get(seed));
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
                    addAssignmentsToFeature(v, assignments.get(v));
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
                            Float.isFinite(uv.interSampleCorrelation) ? Double.valueOf(uv.interSampleCorrelation) : null,
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
            compound.get(0).features.getDetectedAdducts().addAll(
                    DetectedAdduct.builder()
                            .adduct(PrecursorIonType.unknown(compound.get(0).getFeature().getCharge()))
                            .score(0.5d).source(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN)
                            .build());
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
        if (f.getDetectedAdducts() == null) {
            f.setDetectedAdducts(DetectedAdducts.emptySingleton(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN));
        } else {
            f.getDetectedAdducts().addEmptySource(de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts.Source.LCMS_ALIGN);
        }

        return Compound.singleton(f);
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
        if (D instanceof AdductRelationship) {
            if (((AdductRelationship)D).isMultimere()) return CorrelatedIonPair.Type.MULTIMERE;
            else if (((AdductRelationship)D).isInSource()) return CorrelatedIonPair.Type.INSOURCE;
            else return CorrelatedIonPair.Type.ADDUCT;
        }
        if (D instanceof LossRelationship) return CorrelatedIonPair.Type.INSOURCE;
        if (D instanceof UnknownLossRelationship) return CorrelatedIonPair.Type.INSOURCE;
        if (D instanceof IsotopeRelationship) return CorrelatedIonPair.Type.ISOTOPE;
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
                    double ms2value = Float.isNaN(e.ms2score) ? 0f : -(e.ms2score - 0.25)*8;
                    e.pvalue = (float) (score + ms2value);
                    ++count;
                }
            }
        }
        double minimum=Math.max(3,Math.log(pValueStats.medianDensity()));
        double correction = minimum;
        int above5=0, aboveCorrect=0;
        for (AdductNode node : rtOrderedNodes) {
            for (AdductEdge e : node.getEdges()){
                if (e.getLeft()==node) {
                    if (e.getScore()>=5) ++above5;
                    if (e.getScore()>=(correction)) ++aboveCorrect;
                    // correct p-value score
                    e.pvalue += (float)minimum;
                }
            }
        }
        deleteEdgesWithLowPvalue(3);
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

    abstract static class PValueVariable {
        private IntArrayList counts;
        private double[] pvalues;
        public abstract int getBinFor(double value);

        public PValueVariable() {
            this.counts = new IntArrayList();
        }

        public void done() {
            int count = counts.intStream().sum();
            int cumsum=1;
            double base = Math.log(count+1);
            double pbase=base;
            pvalues = new double[counts.size()+1];
            pvalues[pvalues.length-1] = Math.log(cumsum)-base;
            for (int i=counts.size()-1; i >= 0; --i) {
                cumsum += counts.getInt(i);
                double logprob = Math.log(cumsum) - base;
                pvalues[i] = logprob;
            }
        }

        public double pvalue(double value) {
            if (Double.isFinite(value)) {
                int b = getBinFor(value);
                if (b >= counts.size()) return pvalues[pvalues.length-1];
                else return pvalues[b];
            } else return 0;
        }

        public void add(double value) {
            if (!Double.isFinite(value)) return;
            int b = getBinFor(value);
            while (b >= counts.size()) counts.add(0);
            counts.set(b, counts.getInt(b)+1);
        }
        public IntArrayList getCounts() {
            return counts;
        }
    }
    static class ProbabilityValue extends PValueVariable {
        private static double[] corbins = new double[]{0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.75, 0.8, 0.85, 0.9, 0.92, 0.94, 0.96, 0.97, 0.98, 0.99, 0.995};

        public int getBinFor(double score) {
            int index = Arrays.binarySearch(corbins, score);
            if (index >= 0) return index;
            else return -(index +1);
        }
    }

    static class CorrelationValue {
        private final int threshold;
        private final ProbabilityValue below, above;
        public CorrelationValue(int threshold) {
            this.threshold = threshold;
            this.below = new ProbabilityValue();
            this.above = new ProbabilityValue();
        }

        public void done() {
            below.done();
            above.done();
        }

        public ProbabilityValue get(int datapoints) {
            if (datapoints < threshold) {
                return below;
            } else return above;
        }
    }

    class PValueStats {
        private CorrelationValue merged, representative, extra;
        private ProbabilityValue rt, ms2;
        private IntArrayList density;

        public PValueStats() {
            merged = new CorrelationValue(8);
            representative = new CorrelationValue(8);
            extra = new CorrelationValue(6);
            rt = new ProbabilityValue();
            ms2 = new ProbabilityValue();
            this.density = new IntArrayList();
        }

        /**
         * Statistic about how many compounds are within a retention time window in average
         */
        public void addDensity(int densityEstimate) {
            density.add(densityEstimate);
        }

        public float medianDensity() {
            density.sort(null);
            return density.getInt(density.size()/2);
        }
        public float averageDensity() {
            return (float)density.intStream().average().orElse(0);
        }

        private void done() {
            merged.done();
            representative.done();
            extra.done();
            rt.done();
            ms2.done();
        }

        public double logPvalue(AdductEdge edge) {
            return merged.get(edge.interSampleCorrelationCount).pvalue(edge.interSampleCorrelation) +
                    representative.get(edge.interSampleCorrelationRepresentativeCount).pvalue(edge.interSampleCorrelationRepresentative)
                    + extra.get(edge.extraSampleCorrelationCount).pvalue(edge.extraSampleCorrelation)
                    + rt.pvalue(edge.rtScore) + ms2.pvalue(edge.ms2score);
        }

        public void add(AdductEdge edge) {
            if (Double.isFinite(edge.interSampleCorrelation)) merged.get(edge.interSampleCorrelationCount).add(edge.interSampleCorrelation);
            if (Double.isFinite(edge.interSampleCorrelationRepresentative)) representative.get(edge.interSampleCorrelationRepresentativeCount).add(edge.interSampleCorrelationRepresentative);
            if (Double.isFinite(edge.extraSampleCorrelation)) extra.get(edge.extraSampleCorrelationCount).add(edge.extraSampleCorrelation);
            if (Double.isFinite(edge.rtScore)) rt.add(edge.rtScore);
            if (Double.isFinite(edge.ms2score)) ms2.add(edge.ms2score);
        }

    }

}
