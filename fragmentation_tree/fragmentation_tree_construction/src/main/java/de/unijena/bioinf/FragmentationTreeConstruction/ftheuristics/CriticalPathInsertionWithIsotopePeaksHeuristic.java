package de.unijena.bioinf.FragmentationTreeConstruction.ftheuristics;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;

import java.util.*;

public class CriticalPathInsertionWithIsotopePeaksHeuristic extends CriticalPathInsertionHeuristic {

    public CriticalPathInsertionWithIsotopePeaksHeuristic(FGraph graph) {
        super(graph);
    }

    @Override
    protected FTree buildSolution() {
        if (usedColorList.size()<=0) {
            Fragment bestFrag = null;
            for (Fragment f : graph.getRoot().getChildren()) {
                if (bestFrag==null || bestFrag.getIncomingEdge().getWeight() < f.getIncomingEdge().getWeight() ) {
                    bestFrag = f;
                }
            }
            final FTree t = new FTree(bestFrag.getFormula(), bestFrag.getIonization());
            t.setTreeWeight(bestFrag.getIncomingEdge().getWeight());
            return t;
        }
        selectedEdges.addAll(color2Edge.valueCollection());
        selectedEdges.sort(Comparator.comparingInt(a -> a.getTarget().getColor()));
        // find root
        for (int i=0; i < selectedEdges.size(); ++i) {
            if (selectedEdges.get(i).getSource()==graph.getRoot()) {
                if (i>0) {
                    Loss swapped = selectedEdges.get(0);
                    selectedEdges.set(0, selectedEdges.get(i));
                    selectedEdges.set(i, swapped);
                }
                break;
            }
        }


        Fragment target = selectedEdges.get(0).getTarget();
        final FTree tree = new FTree(target.getFormula(), target.getIonization());

        final ArrayList<Loss> isoStack = new ArrayList<>();
        final FragmentAnnotation<IsotopicMarker> marker = graph.getFragmentAnnotationOrNull(IsotopicMarker.class);

        final HashMap<MolecularFormula, Fragment> fragmentsByFormula = new HashMap<>();
        fragmentsByFormula.put(tree.getRoot().getFormula(), tree.getRoot());
        double score = selectedEdges.get(0).getWeight();
        for (int i=1; i < selectedEdges.size(); ++i) {
            final Loss L = selectedEdges.get(i);

            if (marker!=null && marker.get(L.getTarget())!=null) {
                isoStack.add(L);
                continue;
            }

            final Fragment f = tree.addFragment(fragmentsByFormula.get(L.getSource().getFormula()), L.getTarget());
            f.getIncomingEdge().setWeight(L.getWeight());
            f.setPeakId(L.getTarget().getPeakId());
            fragmentsByFormula.put(f.getFormula(), f);
            score += L.getWeight();
        }

        if (isoStack.size()>0) {
            final HashMap<MolecularFormula, List<Loss>> isos = new HashMap<>(isoStack.size());
            for (int i=isoStack.size()-1; i >= 0; --i) {
                Loss L = isoStack.get(i);
                Fragment u = L.getSource();
                while (marker.get(u)!=null) {
                    u = u.getParent();
                }
                isos.putIfAbsent(u.getFormula(), new ArrayList<>());
                isos.get(u.getFormula()).add(L);
            }
            for (Map.Entry<MolecularFormula, List<Loss>> entry : isos.entrySet()) {
                List<Loss> xs = entry.getValue();
                xs.sort(Comparator.comparingInt(u -> -u.getTarget().getColor()));
                Fragment init = fragmentsByFormula.get(entry.getKey());
                for (int i=0;i<xs.size(); ++i) {
                    double weight = xs.get(i).getWeight();
                    score += weight;
                    init = tree.addFragment(init, MolecularFormula.emptyFormula(), PrecursorIonType.unknown().getIonization());
                    init.getIncomingEdge().setWeight(weight);
                    init.setPeakId(xs.get(i).getTarget().getPeakId());
                }
            }
        }

        tree.setTreeWeight(score);
        return tree;
    }

}
