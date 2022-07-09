package de.unijena.bioinf.lcms.ionidentity;

import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.lcms.LCMSProccessingInstance;
import de.unijena.bioinf.model.lcms.FragmentedIon;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.*;
import java.util.stream.Collectors;

public class AdductResolver {

    private final Set<PrecursorIonType> knownIonTypes;
    private double mainIonMz;
    private TreeMap<Long, AdductMassDifference> deltaMap;

    private TDoubleArrayList adductMzs;
    private TDoubleArrayList adductProbabilities;

    public AdductResolver(double mainIonMz, Set<PrecursorIonType> knownIonTypes) {
        this.knownIonTypes = knownIonTypes;
        this.mainIonMz = mainIonMz;
        this.deltaMap = AdductMassDifference.getAllDifferences(knownIonTypes);
        this.adductMzs = new TDoubleArrayList();
        this.adductProbabilities = new TDoubleArrayList();
    }

    public static void resolve(LCMSProccessingInstance inst, FragmentedIon ion) {
        final AdductResolver r = new AdductResolver(ion.getMass(), inst.getDetectableIonTypes().stream().filter(x->(x.getCharge()>0)==(ion.getPolarity()>0)).collect(Collectors.toSet()));
        ion.getAdducts().forEach(c->r.addAdduct(c.ion.getMass(), c.correlation.score));
        r.attachAdductRules(ion);
    }

    public void addAdduct(double adductMz, double adductProbability) {
        adductMzs.add(adductMz);
        adductProbabilities.add(adductProbability);
    }

    public List<AdductAssignment> getAssignments() {
        final HashMap<PrecursorIonType,PrecursorIonType>[] ionAssignments = new HashMap[adductMzs.size()];
        final HashSet<PrecursorIonType> allAssignments = new HashSet<>();
        PrecursorIonType proton = PrecursorIonType.getPrecursorIonType("[M + H]+");
        if (!knownIonTypes.contains(proton)) proton = PrecursorIonType.getPrecursorIonType("[M - H]-");
        for (int i=0; i < adductMzs.size(); ++i) {
            ionAssignments[i] = new HashMap<>();
            final AdductMassDifference adductMassDifference = deltaMap.get(AdductMassDifference.getkey(adductMzs.get(i) - mainIonMz));
            if (adductMassDifference==null) continue;
            for (int j=0; j < adductMassDifference.size(); ++j) {
                final PrecursorIonType l = adductMassDifference.getLeftAt(j);
                final PrecursorIonType r = adductMassDifference.getRightAt(j);
                ionAssignments[i].put(l, r);
                ////
                // type 1: [M + X + A] -> [M + X + B]
                if (l.getModification().equals(r.getModification()) && (!l.getModification().isEmpty())) {

                }
                // type 2: [M + X + A] -> [M + Y + A]
                else if (l.getIonization().equals(r.getIonization()) && (!l.getModification().isEmpty() || !r.getModification().isEmpty())) {
                    if (l.equals(proton) || r.equals(proton))  allAssignments.add(l);

                }
                // type 3: [M + X + A] -> [M + Y + B], [M + X] -> [M + Y]
                else {
                    allAssignments.add(l);
                }
            }
        }
        final ArrayList<AdductAssignment> assignments = new ArrayList<>();

        double probabilitySum = 0d;

        double priorProbability = 0.75d;
        for (int j=0; j < adductProbabilities.size(); ++j) priorProbability *= 1d-Math.min(1d-1e-3, Math.max(1e-3,adductProbabilities.getQuick(j)));

        probabilitySum += priorProbability;
        for (PrecursorIonType ionType : allAssignments) {
            double prob = 0.25d;
            int count = 0;
            List<PrecursorIonType> supTypes = new ArrayList<>();
            TDoubleArrayList supMzs = new TDoubleArrayList();
            for (int j=0; j < ionAssignments.length; ++j) {
                double probability = Math.min(1d-1e-3, Math.max(1e-3,adductProbabilities.getQuick(j)));
                if (ionAssignments[j].containsKey(ionType)) {
                    prob *= probability;
                    count++;
                    supMzs.add(adductMzs.get(j));
                    supTypes.add(ionAssignments[j].get(ionType));
                } else {
                    prob *= (1-probability);
                }
            }
            probabilitySum+=prob;
            assignments.add(new AdductAssignment(ionType, prob, count,supMzs.toArray(), supTypes.toArray(PrecursorIonType[]::new)));
        }
        assignments.sort(Comparator.comparingDouble(x->-x.probability));
        final double normf = probabilitySum;
        assignments.replaceAll(x->x.renorm(normf));
        return assignments;
    }

    public static class AdductAssignment {
        public final PrecursorIonType ionType;
        public final double probability;
        public final int supportedIons;
        public final double[] supportedIonMzs;
        public final PrecursorIonType[] supportedIonTypes;

        public AdductAssignment(PrecursorIonType ionType, double probability, int supportedIons,double[] supportedIonMzs,PrecursorIonType[] supportedIonTypes) {
            this.ionType = ionType;
            this.probability = probability;
            this.supportedIons = supportedIons;
            this.supportedIonMzs = supportedIonMzs;
            this.supportedIonTypes = supportedIonTypes;
        }

        public AdductAssignment renorm(double normFactor) {
            return new AdductAssignment(ionType,probability/normFactor, supportedIons,supportedIonMzs,supportedIonTypes);
        }
    }

    public void attachAdductRules(FragmentedIon ion) {
        final List<AdductAssignment> xs = getAssignments();
        if (xs.isEmpty()) return;
        double nullHyp = 1d;
        for (AdductAssignment x : xs) nullHyp -= x.probability;
        // case 1: one ion is dominantly chosen
        for (AdductAssignment x : xs) {
            if (x.probability >= 0.9) {
                // just chose this one
                ion.setDetectedIonType(x.ionType);
                ion.setPossibleAdductTypes(new HashSet<>(List.of(x.ionType)));
                return;
            }
        }
        // case 2: null hypothesis is above 50%
        if (nullHyp >= 0.5) {
            ion.setDetectedIonType(PrecursorIonType.unknown(ion.getPolarity()));
            return;
        }
        // case 3: attach all ions to the set. Add default ions, too, if nullHyp above 10%.
        HashSet<PrecursorIonType> ions = new HashSet<>();
        for (AdductAssignment x : xs) {
            if (x.probability>=0.05) {
                ions.add(x.ionType);
            }
        }
        if (nullHyp >= 0.10) {
            if (ion.getPolarity()>0) {
                ions.add(PrecursorIonType.getPrecursorIonType("[M+H]+"));
                ions.add(PrecursorIonType.getPrecursorIonType("[M+Na]+"));
            } else {
                ions.add(PrecursorIonType.getPrecursorIonType("[M-H]-"));
            }
        }
        ion.setDetectedIonType(PrecursorIonType.unknown(ion.getPolarity()));
        ion.setPossibleAdductTypes(ions);

    }

}
