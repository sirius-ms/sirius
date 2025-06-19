package de.unijena.bioinf.sirius.elementdetection.transformer;

import de.unijena.bioinf.ChemistryBase.chem.ChemicalAlphabet;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;

import java.util.HashMap;

public class TransformerPrediction {

    private final int monoisotopicPeak;
    private final boolean largest;
    private final Element[] predictableElements;
    private final float[] logits,probabilities;
    private final float chnopfOdd;
    private final float fluorinated;

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i < predictableElements.length; ++i) {
            buf.append(predictableElements[i]);
            buf.append(": ");
            buf.append(String.format("%.2f", probabilities[i]*100));
            buf.append(" %");
            buf.append("\n");
        }
        buf.append("CHNOPF odds: ").append(chnopfOdd).append("\n");
        buf.append("PFAS: ").append(String.format("%.2f",100d*Activation.SIGMOID.apply(fluorinated))).append(" %\n");
        return buf.toString();
    }

    TransformerPrediction(int monoisotopicPeak, boolean largest, Element[] predictableElements, float[] logits, float chnopfOdd, float fluorinated) {
        this.monoisotopicPeak = monoisotopicPeak;
        this.largest = largest;
        this.predictableElements = predictableElements;
        this.logits = logits;
        this.chnopfOdd = chnopfOdd;
        this.fluorinated = fluorinated;
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

    public boolean isLargest() {
        return largest;
    }

    private static Element F = PeriodicTable.getInstance().getByName("F");

    public FormulaConstraints getConstraints() {
        final HashMap<Element, Integer> elements = new HashMap<>(10);
        if (Activation.SIGMOID.apply(fluorinated)>=0.33) {
            elements.put(F, Integer.MAX_VALUE);
        }
        for (int k=0; k < predictableElements.length; ++k) {
            if (probabilities[k]>0.33) {
                elements.put(predictableElements[k], (predictableElements[k].getSymbol().equals("Cl") || predictableElements[k].getSymbol().equals("Br") )? 2
                        : (predictableElements[k].getSymbol().equals("S") ? Integer.MAX_VALUE : 1));
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
