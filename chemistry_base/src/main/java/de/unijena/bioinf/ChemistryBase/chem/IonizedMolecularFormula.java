package de.unijena.bioinf.ChemistryBase.chem;

import java.util.Objects;

public class IonizedMolecularFormula {
    private final MolecularFormula formula;
    private final Ionization ionization;

    public IonizedMolecularFormula(MolecularFormula formula, Ionization ionization) {
        this.formula = formula;
        this.ionization = ionization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IonizedMolecularFormula that = (IonizedMolecularFormula) o;
        return Objects.equals(formula, that.formula) &&
                Objects.equals(ionization, that.ionization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(formula, ionization);
    }
}
