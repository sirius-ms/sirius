package de.unijena.bioinf.FragmentationTreeConstruction.model;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
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
public class PossibleAdductTypes {

    public static PossibleAdductTypes deterministic(PrecursorIonType precursorIonType) {
        final PossibleAdductTypes a = new PossibleAdductTypes();
        a.add(precursorIonType,1);
        return a;
    }

    public static class Adduct {
        protected PrecursorIonType ionType;
        protected double probability;

        private Adduct(PrecursorIonType ionType, double probability) {
            this.ionType = ionType;
            this.probability = probability;
        }

        public PrecursorIonType getIonType() {
            return ionType;
        }

        public double getProbability() {
            return probability;
        }
    }

    protected List<Adduct> ionTypes;
    protected double totalProb;

    public PossibleAdductTypes() {
        this.ionTypes = new ArrayList<>();
    }

    public void add(PrecursorIonType ionType, double probability) {
        ionTypes.add(new Adduct(ionType, probability));
        totalProb += probability;
    }
    public void add(Ionization ionType, double probability) {
        add(PrecursorIonType.getPrecursorIonType(ionType), probability);
    }

    public List<Adduct> getAdductTypes() {
        return ionTypes;
    }

    public boolean hasPositiveCharge() {
        for (Adduct a : ionTypes)
            if (a.getIonType().getCharge()>0)
                return true;
        return false;
    }
    public boolean hasNegativeCharge() {
        for (Adduct a : ionTypes)
            if (a.getIonType().getCharge()<0)
                return true;
        return false;
    }

    public double getProbabilityFor(PrecursorIonType ionType) {
        if (ionTypes.isEmpty()) return 0d;
        for (Adduct a : ionTypes) {
            if (a.getIonType().equals(ionType)) return a.probability/totalProb;
        }
        return 0d;
    }
    public double getProbabilityFor(Ionization ionType) {
        if (ionTypes.isEmpty()) return 0d;
        double prob=0d;
        for (Adduct a : ionTypes) {
            if (a.getIonType().getIonization().equals(ionType)) prob += a.probability;
        }
        return prob/totalProb;
    }

    public List<PrecursorIonType> getPrecursorIonTypes() {
        final ArrayList<PrecursorIonType> ions = new ArrayList<>(ionTypes.size());
        for (Adduct a : ionTypes) ions.add(a.ionType);
        return ions;
    }
    public List<Ionization> getIonModes() {
        final Set<Ionization> ions = new HashSet<>(ionTypes.size());
        for (Adduct a : ionTypes) ions.add(a.ionType.getIonization());
        return new ArrayList<>(ions);
    }

}
