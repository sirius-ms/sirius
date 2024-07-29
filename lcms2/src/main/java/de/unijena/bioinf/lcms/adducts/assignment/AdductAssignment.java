package de.unijena.bioinf.lcms.adducts.assignment;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.DetectedAdducts;
import de.unijena.bioinf.ChemistryBase.ms.PossibleAdducts;
import de.unijena.bioinf.lcms.adducts.IonType;
import de.unijena.bioinf.lcms.adducts.IonType;
import de.unijena.bioinf.ms.persistence.model.core.feature.DetectedAdduct;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.*;

public class AdductAssignment {

    private IonType[]  ionTypes;
    private double[] probabilities;

    public static AdductAssignment merge(int charge, AdductAssignment[] assignments, double[] probabilities) {
        Object2DoubleOpenHashMap<IonType> map = new Object2DoubleOpenHashMap<>();
        IonType unknown = new IonType(PrecursorIonType.unknown(charge), 1f, MolecularFormula.emptyFormula());
        for (int k=0; k < assignments.length; ++k) {
            final double prob = probabilities[k];
            if (assignments[k].isUnknown()) {
                map.merge(unknown, prob, (K,V)-> prob + V);
            } else {
                for (int j=0; j < assignments[k].ionTypes.length; ++j) {
                    double prob2 = prob * assignments[k].probabilities[j];
                    map.merge(assignments[k].ionTypes[j], prob2, (K,V)->V+prob2);
                }
            }
        }
        double sum = map.values().doubleStream().sum();
        final IonType[] ions = map.keySet().stream().filter(x->map.getDouble(x)>0).sorted(Comparator.comparingDouble(x->-map.getDouble(x))).toArray(IonType[]::new);
        final double[] probs = Arrays.stream(ions).mapToDouble(x->map.getDouble(x)/sum).toArray();
        return new AdductAssignment(ions, probs);
    }

    public AdductAssignment(IonType[] ionTypes, double[] probabilities) {
        this.ionTypes = ionTypes;
        this.probabilities = probabilities;
    }

    public IonType mostLikelyAdduct() {
        return ionTypes.length==0 ? null : ionTypes[0];
    }

    public double probabilityOfMostLikelyAdduct() {
        return probabilities.length==0 ? 0d : probabilities[0];
    }

    public List<DetectedAdduct> toPossibleAdducts(DetectedAdducts.Source source) {
        ArrayList<DetectedAdduct> adducts = new ArrayList<>();
        for (int i=0; i < ionTypes.length; ++i) {
            final double p = probabilities[i];
            if (p >0) {
                ionTypes[i].toPrecursorIonType().ifPresent(x->adducts.add(DetectedAdduct.builder().adduct(x).score(p).source(source).build()));
            }
        }
        return adducts;
    }

    public boolean isUnknown() {
        return ionTypes.length==0;
    }
    public boolean likelyUnknown() {
        return ionTypes.length==0 || ionTypes[0].getIonType().isIonizationUnknown();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int k=0; k < ionTypes.length; ++k) {
            buf.append(ionTypes[k].toString());
            if (probabilities[k]<1) buf.append(" ").append((int)(probabilities[k]*100)).append(" %");
            if (k+1 < ionTypes.length) buf.append(", ");
        }
        return buf.toString();
    }

}
