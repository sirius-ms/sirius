package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AdductManager {

    private List<IonType> precursorTypes;

    private MassMap<KnownMassDelta> massDeltas;

    public AdductManager() {
        this.precursorTypes = new ArrayList<>();
        this.massDeltas = null;
    }

    public void addAdducts(Set<PrecursorIonType> precursorIonTypes) {
        this.massDeltas=null;
        this.precursorTypes.addAll(precursorIonTypes.stream().map(IonType::new).toList());
    }

    public void buildMassDifferences() {
        this.massDeltas = new MassMap<>(500);
        for (IonType left : precursorTypes) {
            for (IonType right : precursorTypes) {
                final double massDifference = right.ionType.getModificationMass() - left.ionType.getModificationMass();
                if (Math.abs(massDifference) > 1e-3) {
                    massDeltas.put(massDifference, new AdductRelationship(left, right));
                }
            }
        }
    }

    public List<KnownMassDelta> retrieveMassDeltas(double massDifference, Deviation deviation) {
        return massDeltas.retrieveAll(massDifference, deviation);
    }


}
