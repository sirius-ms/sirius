package de.unijena.bioinf.ChemistryBase.ms;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * A prediction for a single element in a molecular formulas elemental composition. A prediction consists of an element
 * and a logit for its probability.
 * Additionally, a prediction might contain lower and upperbounds for the presence of this element. There is no
 * meaningful way of merging different predictions with different bounds. If we merge element predictions from different
 * sources together then we use the min and max of the bounds over all predictions with positive logit.
 */
public class PossibleElement {

    private final Element element;
    private final float logit;

    private final byte lowerbound, upperbound; // -1 if bound is absent

    public static FormulaConstraints toFormulaConstraints(PossibleElement[] elements, float epsilon) {
        final FormulaConstraints fc = new FormulaConstraints(new ChemicalAlphabet(Arrays.stream(elements).map(x->x.element).toArray(Element[]::new)));
        for (final PossibleElement e : elements) {
            if (e.logit>=epsilon) {
                e.getLowerbound().ifPresent(l->fc.setLowerbound(e.element,l));
                e.getUpperbound().ifPresent(u->fc.setUpperbound(e.element,u));
            } else {
                fc.setBound(e.element, 0,0);
            }
        }
        return fc;
    }

    /**
     * @param elements array of possible elements
     * @return merge all PossibleElement with same element such that their logits are summed up
     */
    public static PossibleElement[] merge(PossibleElement[] elements, float epsilon) {
        elements = elements.clone();
        Arrays.sort(elements, Comparator.comparingInt(e->e.element.getId()));
        float[] logits = new float[elements.length];
        byte[] lowerbounds = new byte[elements.length];
        byte[] upperbounds = new byte[elements.length];
        Element[] elems = new Element[elements.length];
        logits[0] = elements[0].logit;
        elems[0] = elements[0].element;
        lowerbounds[0] = logits[0]>=epsilon ? elements[0].lowerbound : -1;
        upperbounds[0] = logits[0]>=epsilon ? elements[0].upperbound : -1;
        int count = 0;
        for (int i=1; i < elements.length; ++i) {
            if (elements[i].element.getId()==elements[count].element.getId()) {
                logits[count] += elements[i].logit;
                if (elements[i].logit>=epsilon) {
                    if (elements[i].lowerbound>=epsilon) {
                        lowerbounds[count] = (byte)(lowerbounds[count]<0 ? elements[i].lowerbound : Math.min(elements[i].lowerbound, lowerbounds[count]));
                    }
                    if (elements[i].upperbound>=epsilon) {
                        upperbounds[count] = (byte)(upperbounds[count]<0 ? elements[i].upperbound : Math.max(elements[i].upperbound, upperbounds[count]));
                    }
                }
            } else {
                ++count;
                logits[count] = elements[i].logit;
                elems[count] = elements[i].element;
                lowerbounds[count] = logits[count]>=epsilon ? elements[count].lowerbound : -1;
                upperbounds[count] = logits[count]>=epsilon ? elements[count].upperbound : -1;
            }
        }
        PossibleElement[] out = new PossibleElement[count+1];
        for (int i=0; i <= count; ++i) {
            out[i] = new PossibleElement(elems[i], logits[i], lowerbounds[i], upperbounds[i]);
        }
        return out;
    }

    public PossibleElement(Element element, float logit, int lowerbound, int upperbound) {
        this.element = element;
        this.logit = logit;
        this.lowerbound = (byte)lowerbound;
        this.upperbound = (byte)upperbound;
    }

    public PossibleElement(Element element, float logit) {
        this(element,logit,-1,-1);
    }

    public Element getElement() {
        return element;
    }

    public float getLogit() {
        return logit;
    }

    public Optional<Integer> getUpperbound() {
        return upperbound>=0 ? Optional.of((int) upperbound) : Optional.empty();
    }

    public Optional<Integer> getLowerbound() {
        return lowerbound>=0 ? Optional.of((int) lowerbound) : Optional.empty();
    }

    public String toString() {
        return element.getSymbol() + ":" + String.valueOf(logit);
    }
    public static PossibleElement fromString(String str) {
        String[] pt = str.split(":");
        return new PossibleElement(PeriodicTable.getInstance().getByName(pt[0]), Float.parseFloat(pt[1]));
    }
}
