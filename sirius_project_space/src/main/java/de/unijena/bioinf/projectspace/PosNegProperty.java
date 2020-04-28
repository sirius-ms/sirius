package de.unijena.bioinf.projectspace;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import org.jetbrains.annotations.NotNull;

public abstract class PosNegProperty<Data> implements ProjectSpaceProperty {
    protected final Data positive;
    protected final Data negative;

    protected PosNegProperty(Data positive, Data negative) {
        this.positive = positive;
        this.negative = negative;
    }


    public Data getPositive() {
        return positive;
    }

    public Data getNegative() {
        return negative;
    }

    public Data getByIonType(@NotNull PrecursorIonType ionType) {
        return getByCharge(ionType.getCharge());
    }

    public Data getByCharge(int charge) {
        return charge < 0 ? negative : positive;
    }
}
