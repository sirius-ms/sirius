package de.unijena.bioinf.ChemistryBase.ms;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.IonMode;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ms.annotations.ProcessedInputAnnotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

//import de.unijena.bioinf.sirius.ionGuessing.IonGuessingMode;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, SIRIUS will use this
 * object and compute trees for all ion types with probability &gt; 0.
 * If probability is unknown, you can assign a constant to each ion type.
 */
public class PossibleIonModes implements ProcessedInputAnnotation {

    public static PossibleIonModes uniformlyDistributed(Iterable<IonMode> ionModes) {
        return new PossibleIonModes(Iterables.transform(ionModes,u->new ProbabilisticIonization(u,1d)));
    }

    public static PossibleIonModes deterministic(IonMode ionType) {
        return new PossibleIonModes(Collections.singleton(new ProbabilisticIonization(ionType,1d)));
    }

    public static class ProbabilisticIonization {
        public final IonMode ionMode;
        public final double probability;

        private ProbabilisticIonization(IonMode ionMode, double probability) {
            this.ionMode = ionMode;
            this.probability = probability;
        }

        @Override
        public String toString() {
            return ionMode.toString() + "=" + probability;
        }
    }



    protected final HashMap<IonMode, ProbabilisticIonization> ionTypes;
    protected final double totalProb;

    protected PossibleIonModes() {
        this.totalProb = 0d;
        this.ionTypes = new HashMap<>();
    }

    public PossibleIonModes(Iterable<ProbabilisticIonization> ions) {
        this.ionTypes = new HashMap<>();
        double acum=0d;
        for (ProbabilisticIonization ion : ions) {
            ionTypes.compute(ion.ionMode, (k,v)->v==null ? ion : new ProbabilisticIonization(k,ion.probability+v.probability));
            acum += ion.probability;
        }
        this.totalProb = acum;
    }


    public List<ProbabilisticIonization> probabilisticIonizations() {
        return new ArrayList<>(ionTypes.values());
    }

    public boolean hasPositiveCharge() {
        for (ProbabilisticIonization a : ionTypes.values())
            if (a.ionMode.getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (ProbabilisticIonization a : ionTypes.values())
            if (a.ionMode.getCharge() < 0)
                return true;
        return false;
    }

    public double getProbabilityFor(PrecursorIonType ionType) {
        return getProbabilityFor(ionType.getIonization());
    }

    public double getProbabilityFor(Ionization ionType) {
        return ionTypes.getOrDefault(ionType,ZERO).probability/totalProb;
    }

    public List<Ionization> getIonModesWithProbabilityAboutZero() {
        return ionTypes.values().stream().filter(x->x.probability>0).map(u->u.ionMode).collect(Collectors.toList());
    }

    public List<PrecursorIonType> getIonModesWithProbabilityAboutZeroAsPrecursorIonType() {
        return ionTypes.values().stream().filter(x->x.probability>0).map(u->PrecursorIonType.getPrecursorIonType(u.ionMode)).collect(Collectors.toList());
    }

    public List<Ionization> getIonModes() {
        return new ArrayList<>(ionTypes.keySet());
    }

    public List<PrecursorIonType> getIonModesAsPrecursorIonType() {
        return ionTypes.keySet().stream().map(PrecursorIonType::getPrecursorIonType).collect(Collectors.toList());
    }


    @Override
    public String toString() {
        return ionTypes.toString();
    }

    private static ProbabilisticIonization ZERO = new ProbabilisticIonization(null,0d);
}
