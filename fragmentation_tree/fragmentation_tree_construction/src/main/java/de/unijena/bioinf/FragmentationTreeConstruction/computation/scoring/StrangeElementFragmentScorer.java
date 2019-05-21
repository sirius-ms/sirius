package de.unijena.bioinf.FragmentationTreeConstruction.computation.scoring;

import de.unijena.bioinf.ChemistryBase.algorithm.ParameterHelper;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnkownElementException;
import de.unijena.bioinf.ChemistryBase.data.DataDocument;
import de.unijena.bioinf.sirius.ProcessedInput;
import de.unijena.bioinf.sirius.ProcessedPeak;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;

public class StrangeElementFragmentScorer implements DecompositionScorer<Element[]>{

    protected HashSet<MolecularFormula> knownFragments;
    protected double penalty, bonus;
    protected double minMass;

    @Override
    public <G, D, L> void importParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        this.penalty = document.getDoubleFromDictionary(dictionary, "penalty");
        this.bonus = document.getDoubleFromDictionary(dictionary, "score");
        this.minMass = document.getDoubleFromDictionary(dictionary,"minMass");
        this.knownFragments = new HashSet<>();
        final L dd = document.getListFromDictionary(dictionary, "whiteset");
        for (int i=0, n = document.sizeOfList(dd); i  < n; ++i) {
            try {
                knownFragments.add(MolecularFormula.parse(document.getStringFromList(dd, i)));
            } catch (UnkownElementException e) {
                LoggerFactory.getLogger(CommonFragmentsScore.class).warn("Cannot parse Formula. Skipping!", e);
            }
        }
    }

    @Override
    public <G, D, L> void exportParameters(ParameterHelper helper, DataDocument<G, D, L> document, D dictionary) {
        document.addToDictionary(dictionary,"penalty", penalty);
        document.addToDictionary(dictionary, "score", bonus);
        document.addToDictionary(dictionary, "minMass", minMass);
        final L dic = document.newList();
        for (MolecularFormula f : knownFragments) document.addToList(dic ,f.toString());

    }

    @Override
    public Element[] prepare(ProcessedInput input) {
        final ArrayList<Element> specialElements = new ArrayList<>();
        final PeriodicTable t = PeriodicTable.getInstance();
        final Element C = t.getByName("C");
        final Element H = t.getByName("H");
        final Element N = t.getByName("N");
        final Element O = t.getByName("O");
        for (Element e : input.getExperimentInformation().
                getAnnotationOrDefault(FormulaConstraints.class).getChemicalAlphabet().getElements()) {
            if (e == C || e == H || e == N || e == O) continue;
            specialElements.add(e);
        }
        return specialElements.toArray(new Element[specialElements.size()]);
    }

    @Override
    public double score(MolecularFormula formula, Ionization ion, ProcessedPeak peak, ProcessedInput input, Element[] precomputed) {
        if (knownFragments.contains(formula)) return bonus;
        else if (formula.getMass() >= minMass){
            for (Element e : precomputed) {
                if (formula.numberOf(e)>0) {
                    return penalty;
                }
            }
        }
        return 0d;
    }
}
