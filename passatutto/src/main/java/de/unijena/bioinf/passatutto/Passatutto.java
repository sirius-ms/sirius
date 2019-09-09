package de.unijena.bioinf.passatutto;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleSpectrum;
import de.unijena.bioinf.jjobs.BasicJJob;

public class Passatutto {

    public static Decoy createDecoyByRerootingTree(FTree inputTree, PrecursorIonType adduct) {
        final FTree decoyTree = new RerootingTreeMethod().randomlySelectRerootedTree(inputTree).tree;
        updatePeaks(decoyTree);
        final SimpleSpectrum spec = tree2spectrum(decoyTree, adduct);
        return new Decoy(spec, adduct.getIonization().addToMass(inputTree.getRoot().getFormula().getMass()), decoyTree);

    }

    private static void updatePeaks(FTree decoyTree) {
        final FragmentAnnotation<AnnotatedPeak> pk = decoyTree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        final FragmentAnnotation<Peak> pk2 = decoyTree.getOrCreateFragmentAnnotation(Peak.class);
        for (Fragment f : decoyTree) {
            AnnotatedPeak a = pk.get(f);
            double mz = f.getIonization().addToMass(a.getMolecularFormula().getMass());
            AnnotatedPeak b = new AnnotatedPeak(a.getMolecularFormula(), mz, mz, a.getRelativeIntensity(), f.getIonization(), new Peak[0], new CollisionEnergy[0], new int[0]);
            pk.set(f,b);
            pk2.set(f, new SimplePeak(b.getMass(),b.getRelativeIntensity()));
        }
    }

    private static SimpleSpectrum tree2spectrum(FTree decoyTree, PrecursorIonType adduct) {
        final SimpleMutableSpectrum buf = new SimpleMutableSpectrum();
        final FragmentAnnotation<AnnotatedPeak> ano = decoyTree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
        for (Fragment f : decoyTree) {
            final AnnotatedPeak pk = ano.get(f);
            buf.addPeak(pk.getMass(), pk.getRelativeIntensity());
        }
        return new SimpleSpectrum(buf);
    }


    public static JJob makePassatuttoJob(FTree inputTree, PrecursorIonType adduct) {
        return new Passatutto.JJob(inputTree, adduct);
    }


    public static class JJob extends BasicJJob<Decoy> {

        final FTree inputTree;
        final PrecursorIonType adduct;

        public JJob(FTree inputTree, PrecursorIonType adduct) {
            super(JobType.CPU);
            this.inputTree = inputTree;
            this.adduct = adduct;
        }

        @Override
        protected Decoy compute() throws Exception {
            return createDecoyByRerootingTree(inputTree, adduct);
        }
    }
}
