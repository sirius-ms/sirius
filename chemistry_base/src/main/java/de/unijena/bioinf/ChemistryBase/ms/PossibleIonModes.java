package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, SIRIUS will use this
 * object and compute trees for all ion types with probability > 0.
 * If probability is unknown, you can assign a constant to each ion type.
 */
public class PossibleIonModes {

    public static PossibleIonModes deterministic(PrecursorIonType precursorIonType) {
        final PossibleIonModes a = new PossibleIonModes();
        a.add(precursorIonType, 1);
        return a;
    }

    public static PossibleIonModes defaultFor(int charge) {
        final PossibleIonModes a = new PossibleIonModes();
        final PeriodicTable t = PeriodicTable.getInstance();
        if (charge > 0) {
            a.add(t.ionByName("[M+H]+").getIonization(), 0.95);
            a.add(t.ionByName("[M+Na]+").getIonization(), 0.03);
            a.add(t.ionByName("[M+K]+").getIonization(), 0.02);
        } else {
            a.add(t.ionByName("[M-H]-").getIonization(), 0.95);
            a.add(t.ionByName("[M+Cl]-").getIonization(), 0.03);
            a.add(t.ionByName("[M+Br]-").getIonization(), 0.02);
        }
        return a;
    }

    public static class ProbabilisticIonization {
        protected Ionization ionType;
        protected double probability;

        private ProbabilisticIonization(Ionization ionType, double probability) {
            this.ionType = ionType;
            this.probability = probability;
        }

        public Ionization getIonMode() {
            return ionType;
        }

        public double getProbability() {
            return probability;
        }
    }

    protected List<ProbabilisticIonization> ionTypes;
    protected double totalProb;

    public PossibleIonModes(PossibleIonModes pi) {
        this.ionTypes = new ArrayList<>();
        for (ProbabilisticIonization i : pi.ionTypes)
            this.ionTypes.add(new ProbabilisticIonization(i.ionType,i.probability));
        this.totalProb = pi.totalProb;
    }

    public PossibleIonModes() {
        this.ionTypes = new ArrayList<>();
    }

    public void add(PrecursorIonType ionType, double probability) {
        ionTypes.add(new ProbabilisticIonization(ionType.getIonization(), probability));
        totalProb += probability;
    }

    public void add(Ionization ionType, double probability) {
        add(PrecursorIonType.getPrecursorIonType(ionType), probability);
    }
    public void add(Ionization ionType) {
        double minProb = Double.POSITIVE_INFINITY;
        for (ProbabilisticIonization pi : ionTypes)
            if (pi.probability>0)
                minProb = Math.min(pi.probability, minProb);
        if (Double.isInfinite(minProb)) minProb = 1d;
        add(PrecursorIonType.getPrecursorIonType(ionType), minProb);
    }

    public List<ProbabilisticIonization> ProbabilisticIonizations() {
        return ionTypes;
    }

    public boolean hasPositiveCharge() {
        for (ProbabilisticIonization a : ionTypes)
            if (a.getIonMode().getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (ProbabilisticIonization a : ionTypes)
            if (a.getIonMode().getCharge() < 0)
                return true;
        return false;
    }

    public double getProbabilityFor(PrecursorIonType ionType) {
        if (ionTypes.isEmpty()) return 0d;
        for (ProbabilisticIonization a : ionTypes) {
            if (a.getIonMode().equals(ionType.getIonization())) return a.probability / totalProb;
        }
        return 0d;
    }

    public double getProbabilityFor(Ionization ionType) {
        if (ionTypes.isEmpty()) return 0d;
        double prob = 0d;
        for (ProbabilisticIonization a : ionTypes) {
            if (a.getIonMode().equals(ionType)) prob += a.probability;
        }
        return prob / totalProb;
    }

    public List<Ionization> getIonModes() {
        final Set<Ionization> ions = new HashSet<>(ionTypes.size());
        for (ProbabilisticIonization a : ionTypes) ions.add(a.getIonMode());
        return new ArrayList<>(ions);
    }
}
