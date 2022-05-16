package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.jjobs.BasicJJob;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class AnnotateFragmentationTree {

    private final FTree tree;
    private final MolecularGraph graph;
    private final CombinatorialFragmenterScoring scoring;

    private ArrayList<Entry> entries;

    public AnnotateFragmentationTree(FTree tree, MolecularGraph molecule, CombinatorialFragmenterScoring scoring) {
        this.tree = tree;
        this.graph = molecule;
        this.scoring = scoring;
    }

    public void run() {
        try {
            call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ArrayList<Entry> call() throws Exception {
        entries = new Job(tree, graph, scoring).withTimeLimit(120000).call();
        return entries;
    }

    public Job makeJJob() {
        return new Job(tree, graph, scoring);
    }


    public static class Job extends BasicJJob<ArrayList<Entry>> {
        private final FTree tree;
        private final MolecularGraph graph;
        private final CombinatorialFragmenterScoring scoring;

        public Job(FTree tree, MolecularGraph graph, CombinatorialFragmenterScoring scoring) {
            this.tree = tree;
            this.graph = graph;
            this.scoring = scoring;
        }

        @Override
        protected ArrayList<Entry> compute() throws Exception {
            final PriorizedFragmenter fragmenter = new PriorizedFragmenter(graph, scoring);

            final HashMap<MolecularFormula, List<Fragment>> formulas = new HashMap<>();
            for (Fragment f : tree.getFragmentsWithoutRoot()) {
                formulas.computeIfAbsent(f.getFormula().withoutHydrogen(), (x) -> new ArrayList<>()).add(f);
            }

            final HashMap<Fragment, CombinatorialNode> bestMatch = new HashMap<>();
            final HashMap<Fragment, CombinatorialNode> secondBestMatch = new HashMap<>();

            final ArrayList<Entry> entries = new ArrayList<>();
            while (fragmenter.nextFragment() != null) {
                checkForInterruption();
                final int remaining = tree.numberOfVertices() - bestMatch.size() - 1;
                if (remaining == 0) break;
                CombinatorialNode f = fragmenter.currentFragment;

                final boolean match = formulas.containsKey(f.fragment.getFormula().withoutHydrogen());
                if (((match || f.totalScore >= -5)) && (f.getBondbreaks() < 10))
                    fragmenter.acceptFragmentForFragmentation();
                if (match) {
                    if (insertBestMatching(bestMatch, formulas, f, secondBestMatch)) {
                        //System.out.println(f.getFragment().toSMILES() + "\t" + f.totalScore + "\t" + f.getBondbreaks() + "\t" + f.fragment.getFormula() + "\t" + match + "\t" + remaining);
                        fragmenter.acceptFragmentForFragmentation();
                    }
                }
            }

            for (Map.Entry<Fragment, CombinatorialNode> n : bestMatch.entrySet()) {
                checkForInterruption();
                final Entry e = new Entry(graph, n.getValue(), n.getKey());
                if (secondBestMatch.containsKey(n.getKey())) {
                    e.nextBest = new Entry(graph, secondBestMatch.get(n.getKey()), n.getKey());
                }
                entries.add(e);
            }

            return entries;
        }

        public String getJson() {
            final StringWriter w = new StringWriter();
            try {
                writeJson(tree, graph, result(), w);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return w.toString();
        }
    }


    private static boolean insertBestMatching(HashMap<Fragment, CombinatorialNode> mapping, HashMap<MolecularFormula, List<Fragment>> formulas, CombinatorialNode node, HashMap<Fragment, CombinatorialNode> secondBest) {
        final Iterator<Fragment> iterator = formulas.get(node.fragment.getFormula().withoutHydrogen()).stream().sorted(Comparator.comparingInt(
                x -> node.fragment.hydrogenRearrangements(x.getFormula())
        )).iterator();
        while (iterator.hasNext()) {
            Fragment possibleMatch = iterator.next();
            //double rearrangementPenalty = Math.pow(10,Math.abs(f.fragment.hydrogenRearrangements(possibleMatch.getFormula()))-3)/3d;
            if (mapping.get(possibleMatch)==null) {
                mapping.put(possibleMatch,node);
                return true;
            }else if (mapping.get(possibleMatch).totalScore < node.totalScore || (mapping.get(possibleMatch).totalScore == node.totalScore && Math.abs(mapping.get(possibleMatch).fragment.hydrogenRearrangements(possibleMatch.getFormula())) > Math.abs(node.fragment.hydrogenRearrangements(possibleMatch.getFormula())))) {
                CombinatorialNode previousMatch = mapping.get(possibleMatch);
                mapping.put(possibleMatch, node);
                insertBestMatching(mapping,formulas,previousMatch,secondBest);
                return true;
            } else {
                // try another one
            }
        }
        if (secondBest!=null) {
            // ensure that second best does not contain same stuff
            Set<CombinatorialNode> xs = new HashSet<>();
            for (CombinatorialNode n : mapping.values()) xs.add(n);
            for (Fragment key : secondBest.keySet().toArray(Fragment[]::new)){
                if (xs.contains(secondBest.get(key))) secondBest.remove(key);
            }
            insertBestMatching(secondBest,formulas,node,null);
        }
        return false;
    }

    public FTree getTree() {
        return tree;
    }

    public MolecularGraph getGraph() {
        return graph;
    }

    public CombinatorialFragmenterScoring getScoring() {
        return scoring;
    }

    public ArrayList<Entry> getEntries() {
        return entries;
    }

    public String getJson() {
        final StringWriter w = new StringWriter();
        try {
            writeJson(tree, graph, entries, w);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return w.toString();
    }



    public static void writeJson(FTree tree, MolecularGraph graph, List<Entry> entries, Writer out) throws IOException {
        final JsonGenerator G = new JsonFactory().createGenerator(out);
        G.writeStartArray();
        double totalScore = entries.stream().mapToDouble(x->x.totalScore).sum();
        FragmentAnnotation<AnnotatedPeak> peak = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for (Entry entry : entries) {
            G.writeStartObject();
            G.writeStringField("formula", entry.formula);
            G.writeNumberField("peakmass", peak.get(entry.bestMatchingFragment).getMass());
            G.writeNumberField("totalScore", totalScore);
            G.writeNumberField("score", entry.totalScore);
            // write bonds and atoms
            G.writeArrayFieldStart("atoms");
            Set<IAtom> atoms = new HashSet<>();
            for (IAtom atom : entry.fragment) {
                G.writeNumber(atom.getIndex());
                atoms.add(atom);
            }
            G.writeEndArray();
            G.writeArrayFieldStart("bonds");
            for (IBond bond : graph.getBonds()) {
                if (atoms.contains(bond.getAtom(0)) && atoms.contains(bond.getAtom(1))) {
                    G.writeNumber(bond.getIndex());
                }
            }
            G.writeEndArray();
            G.writeArrayFieldStart("cuts");
            for (IBond bond : entry.bondsToCut) {
                G.writeNumber(bond.getIndex());
            }
            G.writeEndArray();
            G.writeEndObject();
        }
        G.writeEndArray();
        G.flush();
    }


    public static class Entry {
        private IBond[] bondsToCut;
        private IAtom[] fragment;
        private IAtom[] hydrogenCarriers;
        private double score, totalScore;
        private int h;
        protected String formula;
        protected Fragment bestMatchingFragment;
        protected Entry nextBest;
        public Entry(MolecularGraph graph, CombinatorialNode node, Fragment treeFragment) {
            final List<CombinatorialEdge> optimalPathToRoot = node.getOptimalPathToRoot();
            final List<IBond> bonds = new ArrayList<>(optimalPathToRoot.size());
            List<IAtom> hydrogenCarriers = new ArrayList<>();
            for (CombinatorialEdge e : optimalPathToRoot) {
                bonds.add(e.cut1);
                if (e.cut2!=null) bonds.add(e.cut2);
                hydrogenCarriers.addAll(Arrays.asList(e.getAtomsOfFragment()));
            }
            this.hydrogenCarriers = hydrogenCarriers.toArray(IAtom[]::new);
            this.bondsToCut = bonds.toArray(IBond[]::new);
            this.fragment = node.getFragment().getAtoms();
            this.score = node.score;
            this.totalScore = node.totalScore;
            this.bestMatchingFragment = treeFragment;
            this.h = bestMatchingFragment==null ? 0 : node.getFragment().hydrogenRearrangements(bestMatchingFragment.getFormula());
            this.formula = bestMatchingFragment == null ? node.getFragment().getFormula().toString() : bestMatchingFragment.getFormula().toString();//node.getFragment().formula.toString();
        }
    }

}
