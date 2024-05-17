package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class IonType {

    protected PrecursorIonType ionType;
    protected float multimere; // 0 = unknown
    protected MolecularFormula insource; // null = unknown

    public IonType(PrecursorIonType ionType, float multimere, MolecularFormula insource) {
        this.ionType = ionType;
        this.multimere = multimere;
        this.insource = insource;
    }

    public IonType withMultimere(float multimere) {
        return new IonType(ionType,multimere,insource);
    }

    public IonType withInsource(MolecularFormula insource) {
        return new IonType(ionType,multimere,insource);
    }

    public IonType multiplyMultimere(float multiplicator) {
        return new IonType(ionType, multimere*multiplicator, insource);
    }

    public IonType addInsource(MolecularFormula insource) {
        return new IonType(ionType, multimere, this.insource.add(insource));
    }

    public PrecursorIonType getIonType() {
        return ionType;
    }

    public float getMultimere() {
        return multimere;
    }

    public MolecularFormula getInsource() {
        return insource;
    }

    public Optional<PrecursorIonType> toPrecursorIonType() {
        if ((multimere==0 || multimere==1) && insource==null || insource.isEmpty())
            return Optional.of(ionType);
        if (multimere!=0 && multimere<1) {
            throw new IllegalArgumentException();
        }
        if (multimere>1) {
            throw new UnsupportedOperationException("Have to implement multimere support first");
        }
        return Optional.of(ionType.substituteInsource(ionType.getInSourceFragmentation().add(insource)));

    }

    @Override
    public String toString() {
        PrecursorIonType adduct = ionType;
        if (insource!=null && !insource.isEmpty()) {
            adduct = adduct.substituteInsource(insource.add(adduct.getInSourceFragmentation()));
        }
        if ((multimere==0 || multimere==1)) {
            return adduct.toString();
        } else {
            return String.format(Locale.US, "Multimere(%.1f)", multimere) + " of " + adduct.toString();
        }


    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IonType ionType1 = (IonType) o;
        return multimere == ionType1.multimere && Objects.equals(ionType, ionType1.ionType) && Objects.equals(insource, ionType1.insource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ionType, multimere, insource);
    }
}
