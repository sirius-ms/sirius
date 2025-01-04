package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class IonType {

    private static Set<PrecursorIonType> FREQUENT_ION_TYPES = Set.of(

    );
    private static Set<PrecursorIonType> POSSIBLE_ION_TYPES = Set.of(
            PrecursorIonType.getPrecursorIonType("[M+NH4]+"),
            PrecursorIonType.getPrecursorIonType("[M-H2O+H]+")
    );


    /**
     * I have to think more about this, but for the moment, we just use some rule of thumb.
     */
    public enum Frequency {
        // always trust FREQUENT adducts
        FREQUENT, // plain ionizations and NH4+
        // only trust POSSIBLE adducts if you see a direct edge
        POSSIBLE, // water loss and similar stuff, multimeres
        // only trust UNLIKELY adducts if you see at least two edges
        UNLIKELY; // other weird adducts

        // if not trust, add plain ionization as fallback
    }

    protected PrecursorIonType ionType;
    @Deprecated protected float multimere; // 0 = unknown
    @Deprecated protected MolecularFormula insource; // null = unknown

    public IonType(PrecursorIonType ionType, float multimere, MolecularFormula insource) {
        this.ionType = ionType;
        this.multimere = multimere;
        this.insource = insource;
    }

    public Frequency getAdductFrequency() {
        if (ionType.getMultimereCount()!=1) {
            if (ionType.getModification().isEmpty()) return Frequency.POSSIBLE;
            else return Frequency.UNLIKELY;
        } else {
            if (ionType.isPlainProtonationOrDeprotonation()) return Frequency.FREQUENT;
            if (FREQUENT_ION_TYPES.contains(ionType)) return Frequency.FREQUENT;
            if (POSSIBLE_ION_TYPES.contains(ionType)) return Frequency.POSSIBLE;
            return Frequency.UNLIKELY;
        }
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

        //These kind of in-source fragments should not exist at all. However, the can occur due to ambiguous adduct relation direction.
        //For now we just filter them out if they occur.
        if (!insource.isAllPositiveOrZero()) {
            IonType.log.warn("In Source fragments with negative element amount are not supported. Ignoring this adduct.");
            return Optional.empty();
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
