package de.unijena.bioinf.FragmentationTreeConstruction.computation.decomposing;

import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.FragmentationTreeConstruction.model.MSExperimentInformation;
import de.unijena.bioinf.MassDecomposer.ChemicalValidator;
import de.unijena.bioinf.MassDecomposer.Chemistry.ChemicalAlphabet;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.MassDecomposer.MassDecomposer;
import de.unijena.bioinf.MassDecomposer.ValenceBoundary;

import java.util.*;

/**
 * @author Kai Dührkop
 */
public class RoundRobinDecomposer implements Decomposer<RoundRobinDecomposer.Algorithm> {

    private final Map<Element, Interval> boundaries;
    private final int uncommonBoundary;
    private final ChemicalValidator validator;

    public static RoundRobinDecomposer withDefaultBoundaries(Map<Element, Interval> uncommonBoundary, int uncommonUpperbound, ChemicalValidator validator) {
        final Map<Element, Interval> boundaries = new HashMap<Element, Interval>(uncommonBoundary);
        final PeriodicTable table = PeriodicTable.getInstance();
        for (Element e : table.getAllByName("C", "H", "N", "O", "P", "S")) {
        	if (!boundaries.containsKey(e)) {
        		boundaries.put(e, new Interval(0, Integer.MAX_VALUE));
        	}
        }
        return new RoundRobinDecomposer(boundaries, uncommonUpperbound, validator);
    }

    public static RoundRobinDecomposer withDefaultBoundaries(int uncommonBoundary, ChemicalValidator validator) {
        return withDefaultBoundaries(Collections.<Element, Interval>emptyMap(),uncommonBoundary, validator);
    }

    public static RoundRobinDecomposer withDefaultBoundaries(int uncommonBoundary) {
        return withDefaultBoundaries(Collections.<Element, Interval>emptyMap(),uncommonBoundary, ChemicalValidator.getPermissiveThreshold());
    }

    public static RoundRobinDecomposer withDefaultBoundaries(Map<Element, Interval> boundaries, int uncommonBoundary) {
        return withDefaultBoundaries(boundaries, uncommonBoundary, ChemicalValidator.getPermissiveThreshold());
    }

    public RoundRobinDecomposer(Map<Element, Interval> boundaries, int uncommonBoundary, ChemicalValidator validator) {
        this.boundaries = boundaries;
        this.uncommonBoundary = uncommonBoundary;
        this.validator = validator;
    }

    public Algorithm initialize(ChemicalAlphabet alphabet, MSExperimentInformation informations) {
        final Deviation e = informations.getMassError();
        final MassDecomposer<Element> decomposer =
            new MassDecomposer<Element>(e.getPrecision(), e.getPpm(), e.getAbsolute(), alphabet);
        decomposer.setValidator(validator);
        synchronized (decomposer) {
            decomposer.init();
        }
        final Element[] elements = new Element[alphabet.size()];
        for (int i=0; i < elements.length; ++i) elements[i] = alphabet.get(i);
        final Map<Element, Interval> elementBoundary = alphabet.toMap();
        for (Element element : elements) {
            if (boundaries.containsKey(element)) elementBoundary.put(element, boundaries.get(element));
            else elementBoundary.put(element, new Interval(0, uncommonBoundary));
        }
        return new Algorithm(decomposer, new ValenceBoundary<Element>(alphabet),elementBoundary, alphabet);
    }

    @Override
    public boolean alphabetStillValid(Algorithm decomposer, ChemicalAlphabet alphabet) {
        return decomposer.alphabet.equals(alphabet);
    }

    public boolean isDecomposable(Algorithm algorithm, double mass, MSExperimentInformation info) {
        return algorithm.decomposer.maybeDecomposable(mass);
    }

    public List<MolecularFormula> decompose(Algorithm algorithm, double mass, MSExperimentInformation info) {
        if (mass <= 0d) return Collections.emptyList();
        final List<int[]> decompositions = algorithm.decomposer.decompose(mass, algorithm.boundary.getMapFor(mass, algorithm.uncommonBoundary));
        final ArrayList<MolecularFormula> formulas = new ArrayList<MolecularFormula>();
        final ChemicalAlphabet alphabet = algorithm.alphabet;
        for (int[] compomer : decompositions) {
            formulas.add(alphabet.decompositionToFormula(compomer));
            assert Math.abs(formulas.get(formulas.size()-1).getMass() - mass) <= (info.getMassError().absoluteFor(mass));
        }
        return formulas;
    }

    public static class Algorithm {
        private final MassDecomposer<Element> decomposer;
        private final ValenceBoundary<Element> boundary;
        private final Map<Element, Interval> uncommonBoundary;
        private final ChemicalAlphabet alphabet;

        private Algorithm(MassDecomposer<Element> decomposer, ValenceBoundary<Element> boundary, 
        		Map<Element, Interval> uncommonBoundary, ChemicalAlphabet alphabet) {
            this.decomposer = decomposer;
            this.boundary = boundary;
            this.alphabet = alphabet;
            this.uncommonBoundary = uncommonBoundary;
        }
    }

}
