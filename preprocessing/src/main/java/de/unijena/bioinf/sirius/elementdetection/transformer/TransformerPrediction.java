package de.unijena.bioinf.sirius.elementdetection.transformer;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.util.HashMap;

public class TransformerPrediction {

    private static final float PROB_THRESHOLD = 0.33f;

    private final int monoisotopicPeak;
    private final Element[] predictableElements;
    private final float[] logits,probabilities;
    private final float chnopfOdd;
    private final float fluorinated;
    private final float patternLogit, patternProb;

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(monoisotopicPeak + "-th peak (" + String.format("%.2f %%\t%.4f)\n", patternProb*100, patternLogit));
        for (int i=0; i < predictableElements.length; ++i) {
            buf.append(predictableElements[i]);
            buf.append(": ");
            buf.append(String.format("%.2f %% \t(%.4f)", probabilities[i]*100, logits[i]));
            buf.append("\n");
        }
        buf.append("CHNOPF odds: ").append(chnopfOdd).append("\n");
        buf.append("PFAS: ").append(String.format("%.2f",100d*Activation.SIGMOID.apply(fluorinated))).append(" %\n");
        return buf.toString();
    }

    TransformerPrediction(int monoisotopicPeak, float patternLogit, Element[] predictableElements, float[] logits, float chnopfOdd, float fluorinated) {
        this.monoisotopicPeak = monoisotopicPeak;
        this.predictableElements = predictableElements;
        this.logits = logits;
        this.chnopfOdd = chnopfOdd;
        this.fluorinated = fluorinated;
        this.patternLogit = patternLogit;
        this.patternProb = Activation.SIGMOID.apply(patternLogit);
        this.probabilities = new float[logits.length];
        for (int i=0; i < probabilities.length; ++i) probabilities[i] = Activation.SIGMOID.apply(logits[i]);
    }


    public float[] getLogits() {
        return logits;
    }

    public Element[] getDetectableElements() {
        return predictableElements;
    }

    public float[] getProbabilities() {
        return probabilities;
    }

    public float getLogOddForCHNOPF() {
        return chnopfOdd;
    }

    public float getPolyFluorinatedLogit() {
        return fluorinated;
    }

    public int getMonoisotopicPeak() {
        return monoisotopicPeak;
    }

    private static Element F = PeriodicTable.getInstance().getByName("F");

    public boolean hasAnyPredictions() {
        for (int i=0; i < probabilities.length; ++i) {
            if (probabilities[i] >= PROB_THRESHOLD) return true;
        }
        return false;
    }

    public FormulaConstraints getConstraints() {
        final HashMap<Element, Integer> elements = new HashMap<>(10);
        if (Activation.SIGMOID.apply(fluorinated)>=PROB_THRESHOLD) {
            elements.put(F, Integer.MAX_VALUE);
        }
        for (int k=0; k < predictableElements.length; ++k) {
            if (probabilities[k]>=PROB_THRESHOLD) {
                elements.put(predictableElements[k], (predictableElements[k].getSymbol().equals("Cl") ? 5 : (predictableElements[k].getSymbol().equals("Br") )? 3
                        : (predictableElements[k].getSymbol().equals("S") ? Integer.MAX_VALUE : 1)));
            }
        }
        final ChemicalAlphabet alphabet = new ChemicalAlphabet(elements.keySet().toArray(new Element[elements.size()]));
        final FormulaConstraints constraints = new FormulaConstraints(alphabet);
        for (Element e : elements.keySet()) {
            constraints.setUpperbound(e, elements.get(e));
        }
        return constraints;
    }
}
