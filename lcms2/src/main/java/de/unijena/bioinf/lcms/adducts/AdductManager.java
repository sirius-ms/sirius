package de.unijena.bioinf.lcms.adducts;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.AdductSettings;
import de.unijena.bioinf.ChemistryBase.ms.utils.MassMap;
import de.unijena.bioinf.ms.properties.PropertyManager;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.*;

public class AdductManager {

    private List<PrecursorIonType> precursorTypes;

    private List<PrecursorIonType> multimereIonTypes;

    private MassMap<KnownMassDelta> massDeltas;
    private MassMap<AdductRelationship> multimereDeltas;
    private IntOpenHashSet decoys;

    private Set<MolecularFormula> losses;

    public AdductManager(int polarity) {
        this.precursorTypes = new ArrayList<>();
        this.massDeltas = new MassMap<>(500);
        this.decoys = initDecoys(polarity);
        this.losses = new HashSet<>();
        this.multimereIonTypes = new ArrayList<>();
        final AdductSettings settings = PropertyManager.DEFAULTS.createInstanceWithDefaults(AdductSettings.class);
        add(settings.getDetectable(polarity), Set.of(MolecularFormula.parseOrThrow("H2O"), MolecularFormula.parseOrThrow("C2H3N"), MolecularFormula.parseOrThrow("CO2"), MolecularFormula.parseOrThrow("CH2O2"), MolecularFormula.parseOrThrow("C2H2"),
                MolecularFormula.parseOrThrow("C6H12O6")));
    }

    private IntOpenHashSet initDecoys(int polarity) {
        double[] Decoys = (polarity>0) ? DecoysPositive : DecoysNegative;
        final IntOpenHashSet set = new IntOpenHashSet(Decoys.length);
        for (double decoy : Decoys) {
            set.add((int)Math.round(10*decoy));
        }
        return set;
    }

    public void add(Set<PrecursorIonType> ionTypes, Set<MolecularFormula> losses) {
        // split adducts into Adducts, Insource and Multimere
        Set<PrecursorIonType> adducts = new HashSet<>(), insource = new HashSet<>(), multimeres = new HashSet<>();
        for (PrecursorIonType ionType : ionTypes) {
            if (ionType.isMultimere()) multimeres.add(ionType);
            else if (!ionType.getAdduct().isEmpty()) adducts.add(ionType);
            else if (!ionType.getInSourceFragmentation().isEmpty()) insource.add(ionType);
            else if (ionType.hasNeitherAdductNorInsource() && !ionType.isIntrinsicalCharged()) adducts.add(ionType);
        }
        this.precursorTypes.addAll(adducts);
        this.precursorTypes.addAll(insource); // no longer treat insources and adducts differently
        this.losses.addAll(losses);
        //precursorTypes.removeIf(x->this.losses.contains(x.getAdduct()));
        this.multimereIonTypes.addAll(multimeres);
        buildMassDifferences();
    }


    public void buildMassDifferences() {
        this.massDeltas = new MassMap<>(500);

        for (MolecularFormula loss : losses) {
            massDeltas.put(loss.getMass(), new LossRelationship(loss));
            massDeltas.put(-loss.getMass(), new LossRelationship(loss.negate()));
        }

        final Deviation dev = new Deviation(5);

        for (PrecursorIonType left : precursorTypes) {
            for (PrecursorIonType right : precursorTypes) {
                final double massDifference = right.getModificationMass() - left.getModificationMass();
                if (Math.abs(massDifference) > 1e-3) {
                    massDeltas.put(massDifference, new AdductRelationship(left, right));
                }
            }
        }

        multimereDeltas = new MassMap<>(500);
        for (PrecursorIonType a : multimereIonTypes) {
            for (PrecursorIonType b : multimereIonTypes) {
                multimereDeltas.put(a.getModificationMass()-2*b.getModificationMass(), new AdductRelationship(b.withMultimere(1), a.withMultimere(2)));
            }
        }
    }

    public Optional<AdductRelationship> checkForMultimere(double largerMass, double smallerMass, Deviation dev) {
        final double delta = largerMass - 2*smallerMass;
        for (AdductRelationship r : multimereDeltas.retrieveAll(delta, dev)) {
            return Optional.of(r);
        }
        return Optional.empty();
    }

    /**
     * If one explanation is a loss, always return the loss. Other adduct types can be resolved later
     */
    public List<KnownMassDelta> retrieveMassDeltasWithNoAmbiguity(double massDifference, Deviation deviation) {
        List<KnownMassDelta> deltas = massDeltas.retrieveAll(massDifference, deviation);
        Optional<KnownMassDelta> loss = deltas.stream().filter(x->x instanceof LossRelationship).findFirst();
        if (loss.isPresent()) {
            deltas.removeIf(x->x instanceof AdductRelationship && ((AdductRelationship) x).left.getIonization().equals(((AdductRelationship) x).right.getIonization()));
            for (int i=0; i < deltas.size(); ++i) {
                if (deltas.get(i) instanceof LossRelationship) deltas.set(i, ((LossRelationship) deltas.get(i)).coveringAdduct());
            }
        }
        return deltas;
    }

    public List<KnownMassDelta> retrieveMassDeltas(double massDifference, Deviation deviation) {
        return massDeltas.retrieveAll(massDifference, deviation);
    }

    public boolean hasDecoy(double massDifference) {
        return decoys.contains((int)Math.round(massDifference*10));
    }

    private static final double[] DecoysNegative=new double[]{
            7.0, 8.0, 9.0, 11.0, 12.0, 13.0, 16.0, 19.0, 20.0, 21.0, 23.0, 25.0, 26.0, 27.0, 29.0, 30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 37.0, 39.0, 40.0, 41.0, 43.0, 45.0, 47.0, 48.0, 49.0, 50.0, 51.0, 52.0, 53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0, 61.0, 63.0, 64.0, 65.0, 5.1, 6.1, 7.1, 8.1, 9.1, 10.1, 11.1, 12.1, 13.1, 14.1, 15.1, 16.1, 17.1, 18.1, 19.1, 20.1, 21.1, 22.1, 23.1, 24.1, 25.1, 26.1, 27.1, 28.1, 29.1, 30.1, 31.1, 32.1, 33.1, 34.1, 35.1, 36.1, 37.1, 38.1, 39.1, 40.1, 41.1, 42.1, 43.1, 44.1, 45.1, 46.1, 47.1, 48.1, 49.1, 50.1, 51.1, 52.1, 53.1, 54.1, 55.1, 56.1, 57.1, 58.1, 59.1, 60.1, 61.1, 62.1, 63.1, 64.1, 65.1, 5.9, 6.9, 7.9, 8.9, 9.9, 10.9, 11.9, 12.9, 13.9, 14.9, 15.9, 16.9, 17.9, 18.9, 20.9, 21.9, 22.9, 23.9, 24.9, 25.9, 26.9, 27.9, 28.9, 29.9, 30.9, 31.9, 32.9, 34.9, 35.9, 36.9, 37.9, 38.9, 39.9, 40.9, 41.9, 42.9, 44.9, 45.9, 46.9, 47.9, 48.9, 49.9, 50.9, 51.9, 52.9, 53.9, 54.9, 55.9, 56.9, 58.9, 59.9, 60.9, 62.9, 63.9, 64.9, 65.9
    } ;

    private static final double[] DecoysPositive=new double[]{
            7.0, 9.0, 11.0, 12.0, 13.0, 15.0, 20.0, 21.0, 23.0, 25.0, 26.0, 27.0, 28.0, 30.0, 31.0, 32.0, 33.0, 34.0, 35.0, 36.0, 37.0, 39.0, 40.0, 42.0, 43.0, 44.0, 45.0, 47.0, 48.0, 49.0, 50.0, 51.0, 52.0, 53.0, 54.0, 55.0, 56.0, 57.0, 58.0, 59.0, 60.0, 61.0, 62.0, 63.0, 64.0, 65.0, 5.1, 6.1, 7.1, 8.1, 9.1, 10.1, 11.1, 12.1, 13.1, 14.1, 15.1, 16.1, 17.1, 18.1, 19.1, 20.1, 21.1, 22.1, 23.1, 24.1, 25.1, 26.1, 27.1, 28.1, 29.1, 30.1, 31.1, 32.1, 33.1, 34.1, 35.1, 36.1, 37.1, 38.1, 39.1, 40.1, 41.1, 42.1, 43.1, 44.1, 45.1, 46.1, 47.1, 48.1, 49.1, 50.1, 51.1, 52.1, 53.1, 54.1, 55.1, 56.1, 57.1, 58.1, 59.1, 60.1, 61.1, 62.1, 63.1, 64.1, 65.1, 5.9, 6.9, 7.9, 8.9, 9.9, 10.9, 11.9, 12.9, 13.9, 14.9, 15.9, 16.9, 17.9, 18.9, 19.9, 21.9, 22.9, 23.9, 24.9, 25.9, 26.9, 27.9, 28.9, 29.9, 30.9, 31.9, 32.9, 33.9, 34.9, 35.9, 36.9, 37.9, 38.9, 39.9, 40.9, 41.9, 42.9, 43.9, 44.9, 45.9, 46.9, 47.9, 48.9, 49.9, 50.9, 51.9, 52.9, 53.9, 54.9, 55.9, 56.9, 57.9, 58.9, 59.9, 60.9, 61.9, 62.9, 63.9, 64.9, 65.9
    } ;


    public static void main(String[] args) {
        {
        AdductManager adductManager = new AdductManager(1);
            for (double d : DecoysPositive ) {
                List<KnownMassDelta> xs = adductManager.retrieveMassDeltas(d, new Deviation(10, 0.05));
                if (xs.size()>0) {
                    xs.forEach(x->System.out.println(d + " => " + x.toString()));
                }
            }
        }
        {
            AdductManager adductManager = new AdductManager(-1);
            for (double d : DecoysNegative ) {
                List<KnownMassDelta> xs = adductManager.retrieveMassDeltas(d, new Deviation(10, 0.05));
                if (xs.size()>0) {
                    xs.forEach(x->System.out.println(d + " => " + x.toString()));
                }
            }
        }
    }

}
