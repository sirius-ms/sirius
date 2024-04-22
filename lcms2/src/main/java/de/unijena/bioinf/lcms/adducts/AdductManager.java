package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdductManager {

    private List<IonType> precursorTypes;

    private MassMap<KnownMassDelta> massDeltas;
    private IntOpenHashSet decoys;

    private Set<MolecularFormula> losses;

    public AdductManager() {
        this.precursorTypes = new ArrayList<>();
        this.massDeltas = null;
        this.decoys = initDecoys();
        this.losses = new HashSet<>();
    }

    private IntOpenHashSet initDecoys() {
        final IntOpenHashSet set = new IntOpenHashSet(Decoys.length);
        for (double decoy : Decoys) {
            set.add((int)Math.round(10*decoy));
        }
        return set;
    }

    public void addAdducts(Set<PrecursorIonType> precursorIonTypes) {
        this.massDeltas=null;
        this.precursorTypes.addAll(precursorIonTypes.stream().map(IonType::new).toList());
    }

    public void addLoss(MolecularFormula lossFormula) {
        losses.add(lossFormula);
        this.massDeltas = null;
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
        for (MolecularFormula loss : losses) {
            massDeltas.put(loss.getMass(), new LossRelationship(loss));
            massDeltas.put(-loss.getMass(), new LossRelationship(loss.negate()));
        }
    }

    public List<KnownMassDelta> retrieveMassDeltas(double massDifference, Deviation deviation) {
        if (massDeltas==null) buildMassDifferences();
        return massDeltas.retrieveAll(massDifference, deviation);
    }

    public boolean hasDecoy(double massDifference) {
        return decoys.contains((int)Math.round(massDifference*10));
    }

    private static final double[] Decoys=new double[]{
            7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0, 26.0, 27.0, 28.0, 29.0, 30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 36.0, 37.0, 38.0, 39.0, 40.0, 41.0, 42.0, 43.0, 44.0, 45.0, 46.0, 47.0, 48.0, 49.0, 50.0, 51.0, 52.0, 53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0, 60.0, 61.0, 62.0, 63.0, 64.0, 65.0, 5.1, 6.1, 7.1, 8.1, 9.1, 10.1, 11.1, 12.1, 13.1, 14.1, 15.1, 16.1, 17.1, 18.1, 19.1, 20.1, 21.1, 22.1, 23.1, 24.1, 25.1, 26.1, 27.1, 28.1, 29.1, 30.1, 31.1, 32.1, 33.1, 34.1, 35.1, 36.1, 37.1, 38.1, 39.1, 40.1, 41.1, 42.1, 43.1, 44.1, 45.1, 46.1, 47.1, 48.1, 49.1, 50.1, 51.1, 52.1, 53.1, 54.1, 55.1, 56.1, 57.1, 58.1, 59.1, 60.1, 61.1, 62.1, 63.1, 64.1, 65.1, 5.9, 6.9, 7.9, 8.9, 9.9, 10.9, 11.9, 12.9, 13.9, 14.9, 15.9, 16.9, 17.9, 18.9, 19.9, 20.9, 21.9, 22.9, 23.9, 24.9, 25.9, 26.9, 27.9, 28.9, 29.9, 30.9, 31.9, 32.9, 33.9, 34.9, 35.9, 36.9, 37.9, 38.9, 39.9, 40.9, 41.9, 42.9, 43.9, 44.9, 45.9, 46.9, 47.9, 48.9, 49.9, 50.9, 51.9, 52.9, 53.9, 54.9, 55.9, 56.9, 57.9, 58.9, 59.9, 60.9, 61.9, 62.9, 63.9, 64.9, 65.9
    } ;

}
