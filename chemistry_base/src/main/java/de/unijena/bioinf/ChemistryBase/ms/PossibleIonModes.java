package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.*;

/**
 * Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown, SIRIUS will use this
 * object and compute trees for all ion types with probability > 0.
 * If probability is unknown, you can assign a constant to each ion type.
 */
public class PossibleIonModes {

    public static PossibleIonModes deterministic(PrecursorIonType precursorIonType) {
        final PossibleIonModes a = new PossibleIonModes();
        a.add(precursorIonType, 1);
        a.disableGuessFromMs1();
        return a;
    }

    public static PossibleIonModes useAlwaysProtonationButAllowMs1Detection(int charge) {
        final PossibleIonModes a = new PossibleIonModes();
        final PeriodicTable t = PeriodicTable.getInstance();
        if (charge > 0) {
            a.add(t.ionByName("[M+H]+").getIonization(), 1);
            a.add(t.ionByName("[M+Na]+").getIonization(), 0);
            a.add(t.ionByName("[M+K]+").getIonization(), 0);
        } else {
            a.add(t.ionByName("[M-H]-").getIonization(), 1);
            a.add(t.ionByName("[M+Cl]-").getIonization(), 0);
            a.add(t.ionByName("[M+Br]-").getIonization(), 0);
        }
        a.enableGuessFromMs1();
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
        a.enableGuessFromMs1();
        return a;
    }

    public static class ProbabilisticIonization {
        public final Ionization ionMode;
        public final double probability;

        private ProbabilisticIonization(Ionization ionMode, double probability) {
            this.ionMode = ionMode;
            this.probability = probability;
        }

        @Override
        public String toString() {
            return ionMode.toString() + "=" + probability;
        }
    }

    public static enum GuessingMode {DISABLED, SELECT, ADD_IONS;
        public boolean isEnabled(){
            return this.equals(SELECT) || this.equals(ADD_IONS);
        }

    };
    protected static final GuessingMode DEFAULT_ENABLED_GUESSING_MODE = GuessingMode.ADD_IONS;

    protected List<ProbabilisticIonization> ionTypes;//todo i think class is currently not save for having ionization multiple times in the list
    protected double totalProb;
    protected GuessingMode GuessingModeFromMs1;


    public PossibleIonModes(PossibleIonModes pi) {
        this.ionTypes = new ArrayList<>();
        for (ProbabilisticIonization i : pi.ionTypes)
            this.ionTypes.add(new ProbabilisticIonization(i.ionMode, i.probability));
        this.totalProb = pi.totalProb;
        this.GuessingModeFromMs1 = pi.GuessingModeFromMs1;
    }

    public PossibleIonModes() {
        this.ionTypes = new ArrayList<>();
        this.GuessingModeFromMs1 = DEFAULT_ENABLED_GUESSING_MODE;
    }

    public boolean isGuessFromMs1Enabled() {
        return GuessingModeFromMs1.isEnabled();
    }

    public void setGuessFromMs1(GuessingMode mode) {
        this.GuessingModeFromMs1 = mode;
    }

    public void enableGuessFromMs1WithCommonIonModes(int charge) {
        if (!isGuessFromMs1Enabled()) setGuessFromMs1(DEFAULT_ENABLED_GUESSING_MODE);
        final PossibleIonModes pm = PossibleIonModes.useAlwaysProtonationButAllowMs1Detection(charge);
        for (ProbabilisticIonization pa : pm.ionTypes) {
            if (getProbabilityFor(pa.ionMode)<=0) takeMaxProbability(pa);
        }
    }
    public void enableGuessFromMs1(){
        setGuessFromMs1(DEFAULT_ENABLED_GUESSING_MODE);
    }
    public void disableGuessFromMs1() {
        setGuessFromMs1(GuessingMode.DISABLED);
    }

    public GuessingMode getGuessingMode() {
        return GuessingModeFromMs1;
    }

    protected void takeMaxProbability(ProbabilisticIonization pi){
        final ListIterator<ProbabilisticIonization> iter = ionTypes.listIterator();
        while (iter.hasNext()) {
            final ProbabilisticIonization ion = iter.next();
            if (ion.ionMode.equals(pi.ionMode)) {
                if (ion.probability>=pi.probability) return;
                totalProb -= ion.probability;
                iter.set(pi);
                totalProb += pi.probability;
                return;
            }
        }
        ionTypes.add(pi);
        totalProb += pi.probability;
        return;
    }

    public void add(ProbabilisticIonization ionMode) {
        add(ionMode.ionMode,ionMode.probability);
    }

    public boolean add(PrecursorIonType ionType, double probability) {
        final ListIterator<ProbabilisticIonization> iter = ionTypes.listIterator();
        while (iter.hasNext()) {
            final ProbabilisticIonization ion = iter.next();
            if (ion.ionMode.equals(ionType.getIonization())) {
                totalProb -= ion.probability;
                iter.set(new ProbabilisticIonization(ionType.getIonization(),probability));
                totalProb += probability;
                return false;
            }
        }
        ionTypes.add(new ProbabilisticIonization(ionType.getIonization(), probability));
        totalProb += probability;
        return true;
    }

    public void add(String ionType, double probability) {
        add(PrecursorIonType.getPrecursorIonType(ionType), probability);
    }

    public void add(Ionization ionType, double probability) {
        add(PrecursorIonType.getPrecursorIonType(ionType), probability);
    }

    public void add(Ionization ionType) {
        add(PrecursorIonType.getPrecursorIonType(ionType), 1d);
    }

    public void add(PrecursorIonType[] ionTypes, double[] probabilities) {
        for (int i = 0; i < ionTypes.length; i++) {
            add(ionTypes[i], probabilities[i]);
        }
    }

    public void updateGuessedIons(PrecursorIonType[] ionTypes) {
        updateGuessedIons(ionTypes, null);
    }

    /**
     * use this method to update this {@link PossibleIonModes} after guessing from MS1.
     * Don't forget to set appropriate {@link GuessingMode}
     * @param ionTypes
     * @param probabilities
     */
    public void updateGuessedIons(PrecursorIonType[] ionTypes, double[] probabilities) {
        if (probabilities==null){
            probabilities = new double[ionTypes.length];
            Arrays.fill(probabilities, 1d);
        }

        if (GuessingModeFromMs1.equals(GuessingMode.ADD_IONS)){
            //adds new ions with their probabilities
            add(ionTypes, probabilities);
        } else if (GuessingModeFromMs1.equals(GuessingMode.SELECT)){
            //selects from known ion modes. no new modes allowed
            //set all probabilities to 0
            for (ProbabilisticIonization ionType : this.ionTypes) {
                add(new ProbabilisticIonization(ionType.ionMode, 0d));
            }
            //add new probabilities
            for (int i = 0; i < ionTypes.length; i++) {
                if (add(ionTypes[i],probabilities[i])){
                    throw new RuntimeException("Adding new ion mode is forbidden. It is only allowed to select known ion modes.");
                }

            }
        } else {
            throw new RuntimeException("guessing ionization is disabled");
        }
    }

    public List<ProbabilisticIonization> probabilisticIonizations() {
        return ionTypes;
    }

    public boolean hasPositiveCharge() {
        for (ProbabilisticIonization a : ionTypes)
            if (a.ionMode.getCharge() > 0)
                return true;
        return false;
    }

    public boolean hasNegativeCharge() {
        for (ProbabilisticIonization a : ionTypes)
            if (a.ionMode.getCharge() < 0)
                return true;
        return false;
    }

    public double getProbabilityFor(PrecursorIonType ionType) {
        if (ionTypes.isEmpty()) return 0d;
        for (ProbabilisticIonization a : ionTypes) {
            if (a.ionMode.equals(ionType.getIonization())) return a.probability / totalProb;
        }
        return 0d;
    }

    public double getProbabilityFor(Ionization ionType) {
        if (ionTypes.isEmpty()) return 0d;
        double prob = 0d;
        for (ProbabilisticIonization a : ionTypes) {
            if (a.ionMode.equals(ionType)) prob += a.probability;
        }
        return prob / totalProb;
    }

    public List<Ionization> getIonModesWithProbabilityAboutZero() {
        final Set<Ionization> ions = new HashSet<>(ionTypes.size());
        for (ProbabilisticIonization a : ionTypes)
            if (a.probability>0)
                ions.add(a.ionMode);
        return new ArrayList<>(ions);
    }

    public List<PrecursorIonType> getIonModesWithProbabilityAboutZeroAsPrecursorIonType() {
        final Set<PrecursorIonType> ions = new HashSet<>(ionTypes.size());
        for (ProbabilisticIonization a : ionTypes)
            if (a.probability>0)
                ions.add(PrecursorIonType.getPrecursorIonType(a.ionMode));
        return new ArrayList<>(ions);
    }

    public List<Ionization> getIonModes() {
        final Set<Ionization> ions = new HashSet<>(ionTypes.size());
        for (ProbabilisticIonization a : ionTypes) ions.add(a.ionMode);
        return new ArrayList<>(ions);
    }

    public List<PrecursorIonType> getIonModesAsPrecursorIonType() {
        final Set<PrecursorIonType> ions = new HashSet<>(ionTypes.size());
        for (ProbabilisticIonization a : ionTypes) ions.add(PrecursorIonType.getPrecursorIonType(a.ionMode));
        return new ArrayList<>(ions);
    }


    public static PossibleIonModes reduceTo(PossibleIonModes source, Collection<String> toKeep) {
        if (toKeep instanceof Set)
            return reduceTo(source, (Set<String>) toKeep);
        else
            return reduceTo(source, (new HashSet<>(toKeep)));

    }

    public static PossibleIonModes reduceTo(PossibleIonModes source, Set<String> toKeep) {
        PossibleIonModes nu = new PossibleIonModes();
        for (ProbabilisticIonization n : source.ionTypes) {
            if (toKeep.contains(n.ionMode.toString()))
                nu.add(n);
        }
        return nu;
    }

    @Override
    public String toString() {
        return ionTypes.toString();
    }
}
