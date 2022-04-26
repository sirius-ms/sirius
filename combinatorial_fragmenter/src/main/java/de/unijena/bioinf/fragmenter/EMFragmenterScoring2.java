package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;

public class EMFragmenterScoring2 implements CombinatorialFragmenterScoring{

    private static final String[] BondNames = new String[]{
            "N-Se", "N.amide-N.planar3", "N.sp2-N.thioamide", "O.planar3-P.ate", "Si.sp3-C.sp", "N.thioamide-N.sp2", "C-As", "C.sp2-S.onyl", "N.planar3:N.sp2", "O.planar3-N.amide", "C.sp2-S.planar3", "I=O", "C:O", "S.3-C.sp", "C-Se", "N.sp2-N.amide", "C.sp2-N.thioamide", "C.sp2-O.planar3", "C-Br", "C.sp2-P.ate", "N:O", "C-I", "N:S", "N-Cl", "O.planar3-S.onyl", "I.sp3d2.3-C.sp3", "C.sp2-N.sp2", "C-B", "As-O", "C.sp2-S.inyl", "P.ate-C.sp3", "P=C", "C:S", "C.sp2-N.plus.sp2", "C.sp2:N.sp2", "C.sp2-N.planar3", "N-N", "P.anium=O.sp2", "C:N", "N.sp2-O.sp3", "N.planar3-C.sp3", "N.plus-O.minus", "P-S", "C-P", "C.sp2-Si.sp3", "C.sp2-S.3", "As=O", "C.sp3-Br", "O.sp3-N.plus.sp2", "C-Cl", "C.sp2:N.planar3", "S:N", "N.sp3-N.sp2", "P-C", "I-O", "O-I", "N.amide-N.sp3", "N:N", "C.sp3-N.thioamide", "N=N", "C.sp2-N.sp3", "C-S", "N.amide-N.sp2", "P-N", "N.thioamide-C.sp2", "C.sp2:N.plus.sp2", "C.sp3-P.ate", "N.amide-N.amide", "N-O", "S-S", "S:C", "C.sp2-O.sp3", "Si-O", "P.ate-N.sp3", "S-N", "C=S", "C.sp2-F", "N-P", "C.sp3-Cl", "S.planar3-C.sp2", "C.sp2-N.amide", "N-S", "C.sp2-C.sp", "S.onyl-N.amide", "C-N", "P.ate-C.sp2", "N.amide-O.sp3", "C.sp2:N.amide", "N.thioamide-C.sp3", "N.sp3-N.sp3", "N.sp3-C.sp3", "S-Cl", "C-F", "O-Si", "S=N", "P=N", "C=N", "N.amide-C.sp2", "C.sp2-C.sp2", "N.sp3-P.ate", "C.sp3-S.inyl", "S.inyl=O.sp2", "C.sp3-S.3", "As-C", "N=O", "C-O", "S.3-C.sp3", "C.sp2:O.plus.sp2", "C.sp3-N.sp2", "S-P", "O:N", "S.inyl-C.sp3", "C.sp3-F", "N.sp2-N.sp3", "C.sp3-S.onyl", "N.plus.sp2-C.sp3", "O.planar3-N.sp2", "Si-C", "S.inyl-O.sp3", "P=S", "I.5-C.sp2", "C#N", "Si.sp3-C.sp3", "O.plus.sp2:C.sp2", "N-C", "O.planar3-C.sp2", "C.sp2-C.sp3", "N.sp2:C.sp2", "B-O", "N=C", "N.plus-C.sp3", "N.amide-S.onyl", "N.sp2-C.sp3", "C.sp3-C.sp", "S-C", "N.sp2:N.planar3", "N.sp3-N.amide", "C.sp2=N.plus.sp2", "C.sp-Si.sp3", "N:C", "C=O", "C.sp3-N.amide", "S=S", "C.sp3-N.planar3", "C.sp3-O.sp3", "O-O", "S.onyl-C.sp3", "C#C", "C.sp3-C.sp2", "N.planar3-C.sp2", "N.planar3:C.sp2", "S-F", "C-C", "C.sp3-N.plus.sp2", "O-P", "C=C", "S.3-C.sp2", "C:C", "N.sp2-C.sp2", "O:C", "O.sp3-N.sp2", "S=O", "N.amide-C.sp3", "O.planar3:C.sp2", "P-F", "C.sp3-C.sp3", "C.sp3-N.plus", "S.onyl=O.sp2", "C.sp-C.sp3", "S.inyl-C.sp2", "O.sp3-C.sp3", "P-O", "O-C", "Se-C", "O-S", "P=O", "N.sp3-C.sp2", "N.plus.sp2:C.sp2", "S.onyl-C.sp2", "N.amide:C.sp2", "O.sp3-N.amide", "C-Si", "N.plus.sp2=C.sp2", "S-O", "S.onyl-O.sp3", "O-N", "O.sp3-C.sp2", "C.sp3-Si.sp3", "O-B", "C.sp-C.sp2", "I-C", "*~*", "N=P", "F-P", "Cl-N", "O-As", "O=As", "O=I", "F-B", "B-F", "Se-Se", "Cl-S", "O=Se", "Se=O", "Se-N", "N=S", "B-C", "N#N", "C=P", "F-S", "S=P", "I-C.sp2", "O=S", "S=C", "N#C", "Cl-C", "O=P", "F-C", "O.minus-N.plus", "O.minus-N.plus.sp2", "O=N", "Br-C.sp3", "Br-C.sp2", "Br-C", "N.plus.sp2-C.sp2", "O=C"
    };
    private static final double[] BondScores = new double[]{
            -0.001, -0.01035624587572856, -0.04430742416544236, -0.0850107897083487, -0.21085336531489315, -0.22184874961635637, -0.22596531581118853, -0.22953331552100503, -0.24176378428434706, -0.2455126678141498, -0.2642358129580592, -0.2653144433974467, -0.26560770063540345, -0.26626788940476925, -0.278753600952829, -0.28172484046859453, -0.28240625245584106, -0.28459097564478897, -0.2848832043729947, -0.28961419072764955, -0.29107733570953276, -0.2961567497035101, -0.2979894151094185, -0.3010299956639812, -0.3010299956639812, -0.3010299956639812, -0.3093636387217806, -0.31231100607367024, -0.31439395722196267, -0.3150334229809615, -0.32021331203632064, -0.3409015077016735, -0.3419661979732691, -0.34488578222678634, -0.34981457637516045, -0.3608368893979614, -0.3618712423617866, -0.36797678529459443, -0.37357217404310816, -0.3736364309644388, -0.37711174892824534, -0.38233493534146157, -0.38706265521296507, -0.39537333817604986, -0.3979400086720376, -0.39933649917747904, -0.40205657486686974, -0.40722371914797145, -0.40748532657826797, -0.40869480656186075, -0.41307774815301623, -0.41426197273903076, -0.4192091486249845, -0.4197543964074207, -0.42596873227228116, -0.42596873227228116, -0.428057209885486, -0.4301341045717023, -0.43015937525322456, -0.4325745106412413, -0.44057211281613673, -0.44122269872147113, -0.44288764804730557, -0.45096108547128394, -0.4534555136660733, -0.45731286136610994, -0.4691523250483871, -0.4745438318318518, -0.47723629199784073, -0.4774221168597886, -0.4818500640896991, -0.5010271438139433, -0.5064808720487198, -0.5071966843971494, -0.5134091148057911, -0.5135939519661449, -0.5182103014122342, -0.5192603971601848, -0.5242018389221275, -0.5269383436149315, -0.531670574872092, -0.5360912026635893, -0.5413728372228448, -0.5453021869310097, -0.5635901570099187, -0.5647975292090197, -0.5748757881336622, -0.57752661441754, -0.5906027174158215, -0.5920757704213615, -0.6016334042523007, -0.6020599913279624, -0.6035925366615896, -0.6038758594029894, -0.6084001693589813, -0.6127838567197355, -0.6148956677144725, -0.6201944262833624, -0.6211229902265754, -0.6221512000988484, -0.6239198273127646, -0.6243363860391147, -0.6328125115717474, -0.6450946235531642, -0.645624466977306, -0.6519220082743122, -0.6736006516012493, -0.6779034566750108, -0.6823458638477505, -0.6865607463541988, -0.6915695230206425, -0.6932216827592989, -0.6934017195867906, -0.7086572717391505, -0.7093079752856194, -0.7100259132674878, -0.7101541712885325, -0.7120971527053714, -0.713210443450629, -0.7191733904243057, -0.7269987279362623, -0.7337879273613025, -0.7347527796476381, -0.7369929389396352, -0.7429848304912077, -0.7431754929592862, -0.7440612220981371, -0.7592043839691467, -0.7637931003367765, -0.7700882000074267, -0.7717848408856608, -0.7776585736638718, -0.7842790647855418, -0.7876935862251858, -0.7907191541164673, -0.7913894396419558, -0.7923916894982539, -0.809460684995823, -0.8129133566428556, -0.8135494355572696, -0.8264016773397583, -0.8266410963433888, -0.8325089127062363, -0.8477961214126284, -0.8507047373969268, -0.8527848686805478, -0.8772644484595302, -0.8812320280795192, -0.8995492782391128, -0.9086730348824855, -0.9109385596529075, -0.9164539485499251, -0.9239054315762341, -0.9273585617188803, -0.9548653419539813, -0.9582601593187262, -0.9634708200704978, -0.965232441124466, -1.0245347829379374, -1.0539502919714958, -1.0755162919612193, -1.0765053166513825, -1.076806654678227, -1.0775215500967497, -1.0881360887005513, -1.0905301498447173, -1.0988135886204116, -1.1086033202379477, -1.162398038611996, -1.1638197610179466, -1.1687661043890454, -1.1718697898972805, -1.2554279117062817, -1.278753600952829, -1.2838410603229669, -1.2874496626854786, -1.3072832948042838, -1.3631779024128257, -1.3715961625605932, -1.5077688474530984, -1.5212646571197774, -1.6083477151670096, -1.6419695977020592, -1.7119758543517558, -1.7546610152443152, -1.777662681613436, -1.8682770987301485, -1.965201701025912, -2.075546961392531, -2.1827785602798455, -2.968716377466786, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3
    };


    private final static TObjectDoubleHashMap<String> name2score;
    static {
        name2score = new TObjectDoubleHashMap<>();
        for (int j=0; j < BondNames.length; ++j) {
            name2score.put(BondNames[j],BondScores[j]);
        }
    }

    public static double scoreFor(IBond b) {
        final String specific = DirectedBondTypeScoring.bondNameSpecific(b,true);
        if (name2score.containsKey(specific)) return name2score.get(specific);
        final String generic =  DirectedBondTypeScoring.bondNameGeneric(b,true);
        if (name2score.containsKey(generic)) return name2score.get(generic);
        return name2score.get("*~*");
    }

    protected static double rearrangementScore = -0.5d;
    protected static double peakScore = 4d;

    private double[] bondScoresLeft,bondScoresRight;
    private TObjectDoubleHashMap<MolecularFormula> fragmentScores;

    public EMFragmenterScoring2(MolecularGraph graph, FTree tree) {
        this.bondScoresLeft = new double[graph.bonds.length];
        this.bondScoresRight = new double[graph.bonds.length];
        this.fragmentScores = tree==null ? null : new TObjectDoubleHashMap<>(tree.numberOfVertices(), 0.75f, 0);
        if (tree!=null) {
            FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
            for (Fragment f : tree) {
                final double intensityScore = ano.get(f).getRelativeIntensity();
                fragmentScores.adjustOrPutValue(f.getFormula().withoutHydrogen(), intensityScore, intensityScore);
            }
        }
        final double wildcard = name2score.get("*~*");
        for (int i=0; i < bondScoresLeft.length; ++i) {
            IBond b = graph.bonds[i];

            {
                String name = DirectedBondTypeScoring.bondNameSpecific(b, true);
                if (name2score.containsKey(name)) {
                    bondScoresLeft[i] = name2score.get(name)  ;
                } else {
                    name = DirectedBondTypeScoring.bondNameGeneric(b, true);
                    if (name2score.containsKey(name)) {
                        bondScoresLeft[i] = name2score.get(name)   ;
                    } else {
                        bondScoresLeft[i] = wildcard;
                    }
                }
            }
            {
                String name = DirectedBondTypeScoring.bondNameSpecific(b, false);
                if (name2score.containsKey(name)) {
                    bondScoresRight[i] = name2score.get(name)   ;
                } else {
                    name = DirectedBondTypeScoring.bondNameGeneric(b, false);
                    if (name2score.containsKey(name)) {
                        bondScoresRight[i] = name2score.get(name)    ;
                    } else {
                        bondScoresRight[i] = wildcard;
                    }
                }
            }
            if (bondScoresLeft[i]>=0) bondScoresLeft[i] = -1e-3;
            if (bondScoresRight[i]>=0) bondScoresRight[i] = -1e-3;
        }

    }

    /**
     * score for cutting the bond. direction is true when the bond goes from fragment to loss and
     * false when the bond goes from loss to fragment
     * @return
     */
    @Override
    public double scoreBond(IBond bond, boolean direction) {
        return (direction ? bondScoresLeft[bond.getIndex()] : bondScoresRight[bond.getIndex()]);
    }

    public double peakIntensityScore(float peakIntensity) {
        if (peakIntensity > 0.1) return 1;
        if (peakIntensity > 0.01) return 0.5;
        return 0;
    }

    @Override
    public double scoreFragment(CombinatorialNode fragment){
        if(fragment.fragment.isInnerNode()){
            return fragment.depth * -0.05;
        }else{
            return terminalScore(fragment);
        }
    }

    public double terminalScore(CombinatorialNode fragment) {
        return peakIntensityScore(fragment.fragment.peakIntensity) + 4d;
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge){
        CombinatorialFragment sourceFragment = edge.source.fragment;
        CombinatorialFragment targetFragment = edge.target.fragment;

        if(targetFragment.isInnerNode()){
            return scoreBond(edge.getCut1(),edge.getDirectionOfFirstCut()) + (edge.getCut2() != null ? scoreBond(edge.getCut2(),edge.getDirectionOfSecondCut()) : 0);
        }else{
            int hydrogenDiff = Math.abs(sourceFragment.hydrogenRearrangements(targetFragment.getFormula()));
            if(hydrogenDiff == 0){
                return 0;
            }else{
                double score = hydrogenDiff * rearrangementScore;
                return (Double.isNaN(score) || Double.isInfinite(score)) ? (-1.0E6) : score;
            }
        }
    }
}
