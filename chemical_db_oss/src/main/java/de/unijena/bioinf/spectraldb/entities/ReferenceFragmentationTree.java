package de.unijena.bioinf.spectraldb.entities;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.Spectrums;
import de.unijena.bioinf.ms.annotations.TreeAnnotation;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceFragmentationTree {

    @Id
    private long uuid;

    /**
     * fragments, ordered by mass!
     */
    private ReferenceFragment[] fragments;

    public FTree asFTree() {
        ReferenceFragment root = getRoot();
        final FTree tree = new FTree(root.getFormula(), root.getIonType().getIonization());
        tree.setAnnotation(PrecursorIonType.class, root.getIonType());
        final HashMap<ReferenceFragment, Fragment> map = new HashMap<>();
        FragmentAnnotation<AnnotatedPeak> ano = tree.getOrCreateFragmentAnnotation(AnnotatedPeak.class);
        map.put(root, tree.getRoot());
        double maxint = Arrays.stream(fragments).mapToDouble(ReferenceFragment::getNormalizedIntensity).max().orElse(1d);
        if (maxint==0) maxint=1d;
        ano.set(tree.getRoot(), new AnnotatedPeak(root.getFormula(), root.getMz()==null ? root.exactMass() : root.getMz(),
                root.exactMass(), root.getNormalizedIntensity()/maxint, root.getIonType().getIonization(),
                new Peak[0], new CollisionEnergy[0], new int[0]));
        for (int i=fragments.length-2; i >= 0; --i) {
            ReferenceFragment f = fragments[i];
            Fragment g = tree.addFragment(map.get(fragments[f.getParentIndex()]), f.getFormula(), f.getIonType().getIonization());
            if (f.getMz()==null) {
                ano.set(g, new AnnotatedPeak(f.getFormula(), f.exactMass(), f.exactMass(), 0d, f.getIonType().getIonization(), new Peak[0], new CollisionEnergy[0], new int[0]));
            } else {
                ano.set(g, new AnnotatedPeak(f.getFormula(), f.getMz(), f.getMz(), f.getNormalizedIntensity()/maxint, f.getIonType().getIonization(), new Peak[0], new CollisionEnergy[0], new int[0]));
            }
            map.put(f, g);
        }
        return tree;
    }

    public static ReferenceFragmentationTree from(FTree tree, MergedReferenceSpectrum merged) {
        HashMap<Fragment, ReferenceFragment> map = new HashMap<>();
        Deviation dev = new Deviation(10);
        FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrNull(AnnotatedPeak.class);
        List<ReferenceFragment> orderedByMass = new ArrayList<>();
        Fragment root = tree.getRoot();
        SimpleSpectrum peaks = new SimpleSpectrum(merged.getQuerySpectrum());
        for (Fragment f : tree.getFragments()) {
            ReferenceFragment g = new ReferenceFragment();
            g.setMz(ano!=null ? ano.get(f).getMass() : null);
            g.setIntensity(ano!=null ? (float)(ano.get(f).getRelativeIntensity()) : null);
            g.setFormula(f.getFormula());
            g.setLossFormula(f.isRoot() ? null : f.getIncomingEdge().getFormula());
            g.setRootLossFormula(f.isRoot() ? null : root.getFormula().subtract(f.getFormula()));
            g.setIonType(PrecursorIonType.getPrecursorIonType(f.getIonization()));
            g.setNormalizedIntensity(g.getIntensity()!=null ? g.getIntensity() : 0);
            // find peak in merged spectrum
            int index = Spectrums.mostIntensivePeakWithin(peaks, g.getMz()!=null ? g.getMz() : g.getIonType().neutralMassToPrecursorMass(g.getFormula().getMass()), dev);
            g.setPeakIndex(index);
            map.put(f,g);
            orderedByMass.add(g);
        }
        orderedByMass.sort(Comparator.comparingDouble(ReferenceFragment::exactMass));
        for (int i=0; i < orderedByMass.size(); ++i) orderedByMass.get(i).setIndex(i);
        for (Map.Entry<Fragment, ReferenceFragment> entry : map.entrySet()) {
            Fragment f = entry.getKey();
            if (!f.isRoot()) entry.getValue().setParentIndex(map.get(f.getParent()).getIndex());
        }
        ReferenceFragmentationTree rtree = new ReferenceFragmentationTree();
        rtree.setFragments(orderedByMass.toArray(ReferenceFragment[]::new));
        return rtree;
    }

    public ReferenceFragment getRoot() {
        return fragments[fragments.length-1];
    }

}
