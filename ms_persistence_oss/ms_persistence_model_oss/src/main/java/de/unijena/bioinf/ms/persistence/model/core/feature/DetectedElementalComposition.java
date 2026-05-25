package de.unijena.bioinf.ms.persistence.model.core.feature;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import de.unijena.bioinf.ChemistryBase.chem.Element;
import de.unijena.bioinf.ChemistryBase.ms.DetectedElements;
import de.unijena.bioinf.ChemistryBase.ms.PossibleElement;
import jakarta.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@ToString
@Getter
@Setter
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.ANY, setterVisibility = JsonAutoDetect.Visibility.ANY)
public class DetectedElementalComposition {

    private Element[] elements;
    private float[] logits;
    @Nullable private Integer[] upperbounds;

    public DetectedElementalComposition() {
    }

    public DetectedElementalComposition(PossibleElement[] preds) {
        this.elements = new Element[preds.length];
        this.logits = new float[preds.length];
        this.upperbounds = new Integer[preds.length];
        boolean noUpperbound=true;
        for (int k=0; k < preds.length; ++k) {
            this.elements[k] = preds[k].getElement();
            this.logits[k] = preds[k].getLogit();
            this.upperbounds[k] = preds[k].getUpperbound().orElse(null);
            if (this.upperbounds[k]!=null) noUpperbound=false;
        }
        if (noUpperbound) this.upperbounds = null;
    }

    public DetectedElementalComposition(Element[] elements, float[] logits, Integer[] upperbounds) {
        this.elements = elements;
        this.logits = logits;
        this.upperbounds = upperbounds;
    }
}
