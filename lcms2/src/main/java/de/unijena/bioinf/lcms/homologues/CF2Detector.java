package de.unijena.bioinf.lcms.homologues;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.PossibleElement;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.lcms.adducts.ProjectSpaceTraceProvider;
import de.unijena.bioinf.ms.persistence.model.core.feature.*;
import de.unijena.bioinf.sirius.elementdetection.TransformerElementDetector;
import de.unijena.bioinf.sirius.elementdetection.transformer.TransformerPrediction;
import de.unijena.bionf.fastcosine.FastCosine;
import de.unijena.bionf.fastcosine.SearchPreparedMergedSpectrum;
import de.unijena.bionf.fastcosine.SearchPreparedSpectrum;
import de.unijena.bionf.spectral_alignment.SpectralSimilarity;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.*;

public class CF2Detector {
    // tags.pfas:sample

    private ProjectSpaceTraceProvider provider;

    public CF2Detector(ProjectSpaceTraceProvider provider) {
        this.provider = provider;
    }

    /**
     * Detects all features that are part of a CF2 homologue series
     * @param features
     * @return IDs of features that have homologue series
     */
    public LongOpenHashSet detectPFASSeries(AlignedFeatures[] features) {
        Arrays.sort(features, Comparator.comparingDouble((AlignedFeatures x)->x.getDataQuality().getScore()).thenComparing(AbstractFeature::getApexIntensity).reversed());
        final MolecularFormula CF2 = MolecularFormula.parseOrThrow("CF2");

        // step 1: merge features with similar m/z
        MassMap<Node> nodes = new MassMap<>(1987);
        {
            final Deviation dev = new Deviation(3);
            for (AlignedFeatures f : features) {
                Optional<Node> node = nodes.retrieveClosest(f.getAverageMass(), dev);
                if (node.isPresent()) {
                    node.get().addFeature(f);
                } else {
                    Node n = new Node();
                    n.addFeature(f);
                    nodes.put(n.mz, n);
                }
            }
        }
        // step 2: search for multiples of CF2. We allow up to 2 missing elements in the series
        final Deviation dev = new Deviation(5);
        for (Node node : nodes) {
            for (int i=1; i < 3; ++i) {
                List<Node> nextNodes = nodes.retrieveAll(node.mz + CF2.getMass()*i, dev);
                node.nextNeighbors.addAll(nextNodes);
                nextNodes.forEach(Node::setPartOfSeries);
                if (!nextNodes.isEmpty()) break;
            }
        }
        List<Node> seeds = new ArrayList<>();
        for (Node node : nodes) {
            if (!node.nextNeighbors.isEmpty() && !node.isPartOfSeries()) {
                seeds.add(node);
            }
        }
        final FastCosine fastCosine = new FastCosine();
        final TransformerElementDetector detector = new TransformerElementDetector();
        final int MAYBE_FLUORINE_FLAG = 4;
        final int FLUORINE_FLAG = 2;
        LongOpenHashSet pfas = new LongOpenHashSet();
        for (Node s : seeds) {
            List<Node> compl = s.completeSeries();
            for (Node n : compl) {
                for (AlignedFeatures f : n.features) {
                    Optional<SimpleSpectrum> isotopes = provider.getIsotopes(f);
                    if (isotopes.isPresent()) {
                        Optional<TransformerPrediction> predict = detector.getPredictor().predict(isotopes.get(), 0);
                        if (predict.isPresent()) {
                            if (predict.get().getPolyFluorinatedLogit()>=0) {
                               n.flag |= FLUORINE_FLAG;
                            }
                            if (predict.get().getPolyFluorinatedLogit()>=-1) {
                                n.flag |= MAYBE_FLUORINE_FLAG;
                            }
                        }
                    }
                }
            }

            int length = s.length();
            int fls = (int)compl.stream().filter(x->(x.flag&FLUORINE_FLAG)!=0 ).count();
            if (fls <= 0 && length < 4) continue;
            List<AlignedFeatures> ms2 = compl.stream().flatMap(x->x.features.stream()).filter(AbstractAlignedFeatures::isHasMsMs).toList();
            List<SearchPreparedSpectrum> specs = ms2.stream().map(x-> fastCosine.prepareQuery(x.getAverageMass(), provider.getMsMsSpectrumOf(x).get())).toList();
            List<Node> ms2Nodes = ms2.stream().map(x->{Node n = new Node(); n.features.add(x); return n;}).toList();
            for (int i=0; i < ms2.size(); ++i) {
                final SearchPreparedSpectrum l = specs.get(i);
                for (int j=0; j < i; ++j) {
                    final SearchPreparedSpectrum r = specs.get(j);
                    SpectralSimilarity spectralSimilarity = fastCosine.fastModifiedCosine(l, r);
                    if (Math.abs(l.getParentMass()-r.getParentMass()) > 0.25) {
                        final float cosine = (float)(spectralSimilarity.similarity - Math.max(0,6-spectralSimilarity.sharedPeaks)*0.05);
                        if (cosine>=0.65) {
                            ms2Nodes.get(i).nextNeighbors.add(ms2Nodes.get(j));
                            ms2Nodes.get(j).nextNeighbors.add(ms2Nodes.get(i));
                        }
                    }
                }
            }
            final Element F = PeriodicTable.getInstance().getByName("F");
            // check if we have a series
            for (Node m : ms2Nodes) {
                List<Node> series = m.completeSeries();
                if (series.size()>=2) {
                    for (Node n : series) {
                        if (n.features.isEmpty() || pfas.contains(n.features.get(0).getAlignedFeatureId())) continue;
                        for (AlignedFeatures f : n.features) {
                            if (f.getDetectedElements()==null) f.setDetectedElements(new DetectedElements());
                            f.getDetectedElements().addDetectedElements(de.unijena.bioinf.ChemistryBase.ms.DetectedElements.singleton(
                                    de.unijena.bioinf.ChemistryBase.ms.DetectedElements.Source.HOMOLOGUE_SERIES,
                                    new PossibleElement(F, 3f)
                            ));
                            pfas.add(f.getAlignedFeatureId());
                        }
                    }
                }
            }
        }
        return pfas;
    }

    private static class Node {
        private List<AlignedFeatures> features;
        private double mzsum, mz;
        private List<Node> nextNeighbors;
        private byte flag;

        public Node() {
            this.features = new ArrayList<>();
            this.nextNeighbors = new ArrayList<>();
        }

        public int length() {
            if (nextNeighbors.isEmpty()) return 1;
            else return 1 +nextNeighbors.stream().mapToInt(x->x.length()).max().orElse(0);
        }

        List<Node> completeSeries() {
            List<Node> stack = new ArrayList<>();
            HashSet<Node> visited = new HashSet<>();
            stack.add(this);
            visited.add(this);
            int k=0;
            while (k < stack.size()) {
                Node n = stack.get(k);
                for (int i=0; i < n.nextNeighbors.size(); ++i) {
                    if (visited.add(n.nextNeighbors.get(i))) {
                        stack.add(n.nextNeighbors.get(i));
                    }
                }
                ++k;
            }
            return stack;
        }

        public void addFeature(AlignedFeatures features) {
            this.features.add(features);
            this.mzsum += features.getAverageMass();
            this.mz = mzsum/this.features.size();
        }

        public void setPartOfSeries() {
            this.flag |= 1;
        }

        public boolean isPartOfSeries() {
            return (flag & 1) == 1;
        }
    }

}
