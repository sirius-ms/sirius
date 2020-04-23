package de.unijena.bioinf.canopus;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.math.ParetoDistribution;
import de.unijena.bioinf.ChemistryBase.ms.*;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.ms.ft.Loss;
import de.unijena.bioinf.ChemistryBase.ms.utils.SimpleMutableSpectrum;
import de.unijena.bioinf.fingerid.Prediction;
import de.unijena.bioinf.sirius.Sirius;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class DecoySpectrumGenerator {

    protected final Prediction csi;
    protected final int N;
    protected final Random random;
    protected final MolecularFormula[] losses;
    protected final Sirius sirius;

    // distributions
    protected ParetoDistribution noiseIntensity;

    public DecoySpectrumGenerator(Prediction csi) {
        this.csi = csi;
        this.N = csi.getFingerid().numberOfTrainingData();
        this.random = new Random();
        this.sirius = new Sirius("qtof");
        final ArrayList<MolecularFormula> losses = new ArrayList<>();
        final HashMap<MolecularFormula, MolecularFormula> fpool = new HashMap<>();
        for (FTree tree : csi.getFingerid().getTrainingTrees()) {
            for (Loss l : tree.losses()) {
                fpool.putIfAbsent(l.getFormula(),l.getFormula());
                losses.add(fpool.get(l.getFormula()));
            }
            // randomly add path of length 2
            final int fi = random.nextInt(tree.numberOfVertices()-2)+2;
            for (int i=fi; i < tree.numberOfVertices(); ++i) {
                Fragment q = tree.getFragmentAt(i);
                if (!q.isRoot() && !q.getParent().isRoot()) {
                    MolecularFormula path = q.getIncomingEdge().getFormula().add(q.getParent().getIncomingEdge().getFormula());
                    fpool.putIfAbsent(path,path);
                    losses.add(path);
                    break;
                }
            }
        }
        this.losses = losses.toArray(new MolecularFormula[losses.size()]);
    }

    public Ms2Experiment drawExperiment() {
        final int size = csi.getFingerid().getTrainingSpectra()[randomInstance()].size();
        final double precursorIon = csi.getFingerid().getPrecursorMz()[randomInstance()];
        final SimpleMutableSpectrum spec = new SimpleMutableSpectrum(size);
        spec.addPeak(precursorIon, randomSignal());
        for (int i=0; i < size/2; ++i) {
            spec.addPeak(randomPeak(precursorIon-1));
        }
        // randomly add losses to spectrum
        for (int i=0; i < (size+1)/2; ++i) {
            final int rpeak = random.nextInt(spec.size());
            final MolecularFormula l = randomLoss();
            double newmz = spec.getMzAt(rpeak)-l.getMass();
            if (newmz < 50) {
                newmz = spec.getMzAt(rpeak)+l.getMass();
            }
            spec.addPeak(newmz, randomSignal());
        }
        // we are done! compute tree
        final MutableMs2Experiment exp = new MutableMs2Experiment();
        exp.setPrecursorIonType(PrecursorIonType.getPrecursorIonType("[M+H]+"));
        exp.setIonMass(precursorIon);
        exp.getMs2Spectra().add(new MutableMs2Spectrum(spec, precursorIon, CollisionEnergy.none(), 2));
        return exp;
    }

    private MolecularFormula randomLoss() {
        return losses[random.nextInt(losses.length)];
    }

    private Peak randomPeak(double maxMz) {
        while (true) {
            final FTree tree = csi.getFingerid().getTrainingTrees()[randomInstance()];
            int k = random.nextInt(tree.numberOfEdges())+1;
            final FragmentAnnotation<Peak> pa = tree.getFragmentAnnotationOrThrow(Peak.class);
            for (int i=k; k < tree.numberOfVertices(); ++i) {
                final Fragment f = tree.getFragmentAt(i);
                if (pa.get(f)!=null) {
                    return pa.get(f);
                }
            }
        }
    }


    private double randomSignal() {
        while (true) {
            final FTree tree = csi.getFingerid().getTrainingTrees()[randomInstance()];
            int k = random.nextInt(tree.numberOfEdges())+1;
            final FragmentAnnotation<Peak> pa = tree.getFragmentAnnotationOrThrow(Peak.class);
            for (int i=k; k < tree.numberOfVertices(); ++i) {
                final Fragment f = tree.getFragmentAt(i);
                if (pa.get(f)!=null) {
                    return pa.get(f).getIntensity();
                }
            }
        }
    }

    public int randomInstance() {
        return random.nextInt(N);
    }
}
