package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.Smiles;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.babelms.MsIO;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;
import de.unijena.bioinf.sirius.Sirius;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TestUi2 {

    public static void main(String[] args) {
        try {

            final Ms2Experiment experiment = MsIO.readExperimentFromFile(new File("/home/kaidu/data/ms/evaluation_set_122/mona_442.ms")).next();
            final FTree tree = new Sirius().compute(experiment,experiment.getMolecularFormula()).getTree();//MsIO.readTreeFromFile(new File("/home/kaidu/temp/example.json"));

            final IAtomContainer M = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(experiment.getAnnotation(Smiles.class).get().smiles);
            final MolecularGraph graph = new MolecularGraph(M);

            final CombinatorialGraph comb = new CombinatorialFragmenter(graph).createCombinatorialFragmentationGraph((x)->x.bondbreaks<4);
            System.out.println(comb.nodes.size());
            final Set<MolecularFormula> insilicoFormulas = comb.nodes.stream().map(x -> x.fragment.getFormula()).collect(Collectors.toSet());
            System.out.println(insilicoFormulas.toString());
            System.out.println(insilicoFormulas.size());

            int matchable = 0;
            for (Fragment f : tree) {
                if (insilicoFormulas.contains(f.getFormula().withoutHydrogen()))
                    ++matchable;
            }
            System.out.println(matchable + " / " + tree.numberOfVertices() + " fragments COULD be matched");


            matchTreeAndGraph(graph, comb, tree);
            int matched = 0;
            final FragmentAnnotation<SmilesList> sm = tree.getOrCreateFragmentAnnotation(SmilesList.class);
            for (Fragment f : tree) {
                //sm.set(f, new SmilesList(new LinkedHashSet<String>(Arrays.asList(sm.get(f, SmilesList::empty).smiles)).toArray(String[]::new)));
                if (sm.get(f,SmilesList::empty).smiles.length>0)
                    ++matched;
            }
            System.out.println(matched + " / " +  tree.numberOfVertices() + " fragments were matched");

            DescriptorRegistry.getInstance().put(Fragment.class, SmilesList.class, new Descriptor<SmilesList>() {
                @Override
                public String[] getKeywords() {
                    return new String[]{"smiles"};
                }

                @Override
                public Class<SmilesList> getAnnotationClass() {
                    return SmilesList.class;
                }

                @Override
                public <G, D, L> SmilesList read(DataDocument<G, D, L> document, D dictionary) {
                    List<String> smiles = new ArrayList<>();
                    if (document.hasKeyInDictionary(dictionary,"smiles")) {
                        final L list = document.getListFromDictionary(dictionary, "smiles");
                        for (int i=0, n = document.sizeOfList(list); i < n; ++i) {
                            smiles.add(document.getStringFromList(list,i));
                        }
                    }
                    return new SmilesList(smiles.toArray(String[]::new));
                }

                @Override
                public <G, D, L> void write(DataDocument<G, D, L> document, D dictionary, SmilesList annotation) {
                    L list = document.newList();
                    for (String smile : annotation.smiles) document.addToList(list,smile);
                    document.addListToDictionary(dictionary,"smiles", list);
                }
            });

            new FTJsonWriter().writeTreeToFile(new File("/home/kaidu/temp/annotated.json"), tree);




        } catch (InvalidSmilesException | IOException e) {
            e.printStackTrace();
        }

    }

    private static class SmilesList implements TreeAnnotation {
        private String[] smiles;

        private static SmilesList EMPTY = new SmilesList();

        public static SmilesList empty() {
            return EMPTY;
        }

        public SmilesList(String... smiles) {
            this.smiles = smiles;
        }

        public SmilesList join(String... other) {
            return join(new SmilesList(other));
        }

        public SmilesList join(SmilesList other) {
            final String[] strings = Arrays.copyOf(this.smiles, this.smiles.length + other.smiles.length);
            System.arraycopy(other.smiles, 0, strings, smiles.length, other.smiles.length);
            return new SmilesList(strings);
        }
    }

    private static void matchTreeAndGraph(MolecularGraph mol, CombinatorialGraph graph, FTree tree) {

    //match(graph.root, tree.getRoot(), tree.getOrCreateFragmentAnnotation(SmilesList.class));
        final FragmentAnnotation<SmilesList> ano = tree.getOrCreateFragmentAnnotation(SmilesList.class);
        ano.set(tree.getRoot(), new SmilesList(mol.asFragment().toSMILES()));
        match(tree.getRoot(), new HashSet<>(Arrays.asList(graph.root)), ano);
    }

    private static void match(Fragment fragment, HashSet<CombinatorialNode> fragments, FragmentAnnotation<SmilesList> ano) {
        for (Fragment child : fragment.getChildren()) {
            final HashSet<CombinatorialNode> candidates = new HashSet<>();
            for (CombinatorialNode candidate : fragments) {
                search(candidates, child.getFormula().withoutHydrogen(), candidate,0);
            }
            final ArrayList<CombinatorialNode> candidateList = new ArrayList<>(candidates);
            candidateList.sort(Comparator.comparingDouble((x)->x.score));
            if (candidateList.size()>1)
                candidateList.removeIf(x->x.score > 2*candidateList.get(0).score);
            LinkedHashSet<String> smiles = new LinkedHashSet<>();
            for (CombinatorialNode n : candidateList) {
                smiles.add(n.fragment.toSMILES());
            }
            ano.set(fragment, new SmilesList(smiles.toArray(String[]::new)));
            System.out.println("match " + smiles.size() + " SMILES to " + child.getFormula());
            System.out.println(candidateList.stream().map(x->x.fragment.toSMILES() + " (" + x.score + ")").collect(Collectors.joining(", ")));
            match(child, candidates, ano);
        }
    }
    private static void search(HashSet<CombinatorialNode> candidates, MolecularFormula withoutHydrogen, CombinatorialNode candidate, double deletionPenalty) {
        for (CombinatorialEdge e : candidate.outgoingEdges) {
            final MolecularFormula g = e.target.fragment.getFormula().subtract(withoutHydrogen);
            if (g.isEmpty()) {
                candidates.add(e.target);
            } else if (g.isAllPositiveOrZero()) {
                double cost = e.target.score +deletionPenalty;
                if (cost >= 100) return;
                search(candidates, withoutHydrogen, e.target, e.target.score + deletionPenalty + 20);
            }
        }
    }

}
