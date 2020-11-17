package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;

public class DirectedBondTypeScoring {

    private static final String[] BondNames = new String[]{
            "C.sp3-C.sp3", "C.sp2-C.sp3", "C.sp3-C.sp2", "C.sp2-C.sp2", "C.sp-C.sp3", "C.sp3-C.sp", "C.sp-C.sp2", "C.sp2-C.sp", "C-C", "C.sp3-O.sp3", "C.sp2-O.sp3", "C.sp2-O.planar3", "C-O", "C:C", "C.sp2-N.amide", "C.sp3-N.amide", "C.sp3-N.sp3", "C.sp3-N.plus", "C.sp2-N.sp3", "C.sp3-N.planar3", "C.sp2-N.planar3", "C.sp3-N.sp2", "C.sp2-N.thioamide", "C.sp2-N.sp2", "C.sp2-N.plus.sp2", "C.sp3-N.thioamide", "C.sp3-N.plus.sp2", "C-N", "C=O", "N.amide-C.sp2", "N.amide-C.sp3", "N.sp3-C.sp3", "N.plus-C.sp3", "N.sp3-C.sp2", "N.planar3-C.sp3", "N.planar3-C.sp2", "N.sp2-C.sp3", "N.thioamide-C.sp2", "N.sp2-C.sp2", "N.plus.sp2-C.sp2", "N.thioamide-C.sp3", "N.plus.sp2-C.sp3", "N-C", "O.sp3-C.sp3", "O.sp3-C.sp2", "O.planar3-C.sp2", "O-C", "C=C", "C.sp2:N.sp2", "C.sp2:N.planar3", "C.sp2:N.plus.sp2", "C.sp2:N.amide", "C.sp2-Cl", "C.sp3-Cl", "C.sp3-S.3", "C.sp2-S.onyl", "C.sp2-S.3", "C.sp3-S.onyl", "C.sp2-S.planar3", "C.sp3-S.inyl", "C.sp2-S.inyl", "C-S", "C-F", "N.sp2:C.sp2", "N.planar3:C.sp2", "N.plus.sp2:C.sp2", "N.amide:C.sp2", "O-P", "C.sp2-Br", "C.sp3-Br", "C-Br", "S.3-C.sp3", "S.onyl-C.sp2", "S.3-C.sp2", "S.onyl-C.sp3", "S.planar3-C.sp2", "S.inyl-C.sp3", "S.inyl-C.sp2", "S-C", "N.plus.sp2-O.minus", "N.amide-O.sp3", "N.sp2-O.sp3", "N.plus-O.minus", "N-O", "N=C", "N.amide-N.sp2", "N.sp2-N.amide", "N.sp3-N.amide", "N.amide-N.sp3", "N.amide-N.amide", "N.sp3-N.sp3", "N.thioamide-N.sp2", "N.sp2-N.thioamide", "N.sp2-N.sp3", "N.sp3-N.sp2", "N-N", "C:S", "P-O", "C.sp2:O.planar3", "C.sp2:O.plus.sp2", "C=N", "N.sp2:N.planar3", "N.planar3:N.sp2", "N.sp2:N.sp2", "N:N", "N.sp3-S.onyl", "N.amide-S.onyl", "N.planar3-S.onyl", "N-S", "S:C", "C-I", "S.onyl=O.sp2", "S.inyl=O.sp2", "S=O", "N=O", "S.onyl-N.sp3", "S.onyl-N.amide", "S.onyl-N.planar3", "S-N", "C=S", "P=O", "C#N", "P-C", "C-P", "O:C", "N=N", "S-S", "N:O", "C#C", "P-S", "O-S", "S-O", "P-N", "P=S", "O-O", "O.minus-N.plus.sp2", "O.sp3-N.amide", "O.sp3-N.sp2", "O.minus-N.plus", "O-N", "N-P", "O:N", "S-P", "Si-C", "S:N", "N:S", "C-Si", "N#C", "I-C", "O=C", "S=P", "O=P", "S=C", "O=S", "F-C", "Br-C", "Cl-C", "O=N", "*~*"
    };
    private static final double[] BondScores = new double[]{
            0.1163996434698896, // C.sp3-C.sp3
            0.20796339514275722, // C.sp2-C.sp3
            0.3080497081030584, // C.sp3-C.sp2
            0.4316393739291627, // C.sp2-C.sp2
            0.12892162046951372, // C.sp-C.sp3
            0.5630763309559895, // C.sp3-C.sp
            0.05803679155796643, // C.sp-C.sp2
            1.089347763535745, // C.sp2-C.sp
            0.17036086089508393, // C-C
            0.4464396418620415, // C.sp3-O.sp3
            0.6145430722742283, // C.sp2-O.sp3
            0.4364217677296092, // C.sp2-O.planar3
            0.5019984281864255, // C-O
            0.13945115557191637, // C:C
            0.6101917049189554, // C.sp2-N.amide
            0.37831581946478404, // C.sp3-N.amide
            0.6397169543552775, // C.sp3-N.sp3
            0.1224867763112264, // C.sp3-N.plus
            0.8222939821914764, // C.sp2-N.sp3
            0.5972927006920319, // C.sp3-N.planar3
            0.5579551144481417, // C.sp2-N.planar3
            0.8154596411817978, // C.sp3-N.sp2
            0.8267979613131784, // C.sp2-N.thioamide
            0.5898910571209063, // C.sp2-N.sp2
            1.956723112614512, // C.sp2-N.plus.sp2
            0.4778293887139036, // C.sp3-N.thioamide
            0.548421299642146, // C.sp3-N.plus.sp2
            0.5284585499064091, // C-N
            0.5203502334852799, // C=O
            0.5586336331319339, // N.amide-C.sp2
            0.18429826162647578, // N.amide-C.sp3
            0.23899251763891893, // N.sp3-C.sp3
            0.01935645203515353, // N.plus-C.sp3
            0.10518967337576782, // N.sp3-C.sp2
            0.947915198147142, // N.planar3-C.sp3
            0.22199396486044037, // N.planar3-C.sp2
            0.3767332807155728, // N.sp2-C.sp3
            0.6466200000783103, // N.thioamide-C.sp2
            0.2123755264232725, // N.sp2-C.sp2
            0.007308624910387287, // N.plus.sp2-C.sp2
            0.3982791273583044, // N.thioamide-C.sp3
            0.5851256137164536, // N.plus.sp2-C.sp3
            0.30157807952671195, // N-C
            0.1608222494474937, // O.sp3-C.sp3
            0.06252452705410255, // O.sp3-C.sp2
            0.20360758093067977, // O.planar3-C.sp2
            0.12960185259657708, // O-C
            0.1849131527808372, // C=C
            0.36095398095920406, // C.sp2:N.sp2
            0.3137804010840882, // C.sp2:N.planar3
            0.32151459262750254, // C.sp2:N.plus.sp2
            0.25212872701457173, // C.sp2:N.amide
            1.3113238279732398, // C.sp2-Cl
            0.9485218772321721, // C.sp3-Cl
            0.5254832877310242, // C.sp3-S.3
            1.5609310885802299, // C.sp2-S.onyl
            0.8604120934155205, // C.sp2-S.3
            0.41021521696742186, // C.sp3-S.onyl
            0.6820001149931441, // C.sp2-S.planar3
            0.8441222502345104, // C.sp3-S.inyl
            1.001656584584825, // C.sp2-S.inyl
            0.7630411019221899, // C-S
            1.0249779826077312, // C-F
            0.16634365942312326, // N.sp2:C.sp2
            0.0960011256727613, // N.planar3:C.sp2
            0.06603960904348764, // N.plus.sp2:C.sp2
            0.05597614587340614, // N.amide:C.sp2
            0.1933779429696095, // O-P
            2.1910275890995057, // C.sp2-Br
            1.4394196434949622, // C.sp3-Br
            2.130961115124311, // C-Br
            0.26037277087892646, // S.3-C.sp3
            0.1015805771063019, // S.onyl-C.sp2
            0.3472003397349047, // S.3-C.sp2
            0.2126757531872082, // S.onyl-C.sp3
            0.3696153219958329, // S.planar3-C.sp2
            0.3246425264550265, // S.inyl-C.sp3
            0.5760161179698217, // S.inyl-C.sp2
            0.25361831448554717, // S-C
            0.6803809930429694, // N.plus.sp2-O.minus
            0.49371094848766045, // N.amide-O.sp3
            1.7854064646378711, // N.sp2-O.sp3
            1.429767424634602, // N.plus-O.minus
            0.8449967226456752, // N-O
            0.45136558608505634, // N=C
            0.563322626476605, // N.amide-N.sp2
            0.741327872458616, // N.sp2-N.amide
            0.45367255803836704, // N.sp3-N.amide
            0.8540025101651212, // N.amide-N.sp3
            0.6805652453531918, // N.amide-N.amide
            1.041032281583197, // N.sp3-N.sp3
            0.5004205053042993, // N.thioamide-N.sp2
            1.1261322389236632, // N.sp2-N.thioamide
            0.5682828708911385, // N.sp2-N.sp3
            1.229377577802775, // N.sp3-N.sp2
            0.798725545556798, // N-N
            0.5107937626415862, // C:S
            0.07516904096760611, // P-O
            0.414453089957252, // C.sp2:O.planar3
            0.13101041867489235, // C.sp2:O.plus.sp2
            0.33255531681177963, // C=N
            0.26167229900431593, // N.sp2:N.planar3
            0.4281124637466392, // N.planar3:N.sp2
            0.31342591869293374, // N.sp2:N.sp2
            0.3333972165749702, // N:N
            0.9271967757624104, // N.sp3-S.onyl
            0.33245806401969835, // N.amide-S.onyl
            1.1261839579809099, // N.planar3-S.onyl
            0.831912679887203, // N-S
            0.34843429762697853, // S:C
            2.323272838820448, // C-I
            0.18883732987979676, // S.onyl=O.sp2
            0.7990362291248378, // S.inyl=O.sp2
            0.22133136570441903, // S=O
            0.6413410419101986, // N=O
            0.5646526034318516, // S.onyl-N.sp3
            0.6615400892582333, // S.onyl-N.amide
            0.3144542772861358, // S.onyl-N.planar3
            0.5888475883001202, // S-N
            0.7700957741399164, // C=S
            0.08234571452704621, // P=O
            0.46091994257094476, // C#N
            0.7934540946896999, // P-C
            0.7521291662858364, // C-P
            0.06534430317721392, // O:C
            0.7421790291885503, // N=N
            0.5135273683547494, // S-S
            0.5355624585164865, // N:O
            0.18031164473853142, // C#C
            1.1133527035653288, // P-S
            0.2132171711834622, // O-S
            0.16983079827625722, // S-O
            0.855815884273331, // P-N
            0.4683138401559452, // P=S
            0.510751551606934, // O-O
            0.0025401880497421896, // O.minus-N.plus.sp2
            0.03707200038580247, // O.sp3-N.amide
            0.171141975308642, // O.sp3-N.sp2
            0.029940119760479042, // O.minus-N.plus
            0.03132126437721339, // O-N
            0.6053339243498818, // N-P
            0.2139312415835477, // O:N
            0.4914276485788112, // S-P
            0.6005411255411174, // Si-C
            0.5627538029100532, // S:N
            0.43679185563114126, // N:S
            0.09523809523809523, // C-Si
            0.0057943810904742985, // N#C
            0.011502029769959404, // I-C
            4.4057398143001256e-05, // O=C
            0.021052631578947368, // S=P
            0.00047106460600958164, // O=P
            0.004170141784820684, // S=C
            0.000676132521974307, // O=S
            0.000531575590048905, // F-C
            0.0019692792437967705, // Br-C
            0.0005243838489774515, // Cl-C
            0.0022644927536231885, // O=N
            0.21973559490104003 // *~*
    };


    private final static TObjectDoubleHashMap<String> name2score;
    static {
        name2score = new TObjectDoubleHashMap<>();
        double max = Math.log(Arrays.stream(BondScores).max().orElse(1d));
        for (int j=0; j < BondNames.length; ++j) name2score.put(BondNames[j],Math.log(BondScores[j])-max);
    }


    public static String bondNameGeneric(IBond b, boolean fromLeftToRight) {
        return explainBondBy(b, b.getAtom(0).getSymbol()  ,b.getAtom(1).getSymbol(), fromLeftToRight);
    }

    public static String bondNameSpecific(IBond b, boolean fromLeftToRight) {
        return explainBondBy(b, b.getAtom(0).getAtomTypeName()  ,b.getAtom(1).getAtomTypeName(), fromLeftToRight);
    }

    private static String explainBondBy(IBond b, String labelA, String labelB, boolean fromLeftToRight) {
        if (!fromLeftToRight) {
            String c = labelA;
            labelA = labelB;
            labelB = c;
        }
        String s = labelA;
        if (b.isAromatic()) {
            s += ":";
        } else {
            switch (b.getOrder()) {
                case SINGLE:
                    s += "-";
                    break;
                case DOUBLE:
                    s += "=";
                    break;
                case TRIPLE:
                    s += "#";
                    break;
                default:
                    s += "?";
            }
        }
        s += labelB;
        return s;
    }

    public CombinatorialFragmenterScoring getScoringFor(MolecularGraph graph, FTree tree){
        return new Impl(graph,tree);
    }



    protected static class Impl implements CombinatorialFragmenterScoring {

        private double[] bondScoresLeft,bondScoresRight;
        private TObjectDoubleHashMap<MolecularFormula> fragmentScores;

        public Impl(MolecularGraph graph, FTree tree) {
            FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);

            this.bondScoresLeft = new double[graph.bonds.length];
            this.bondScoresRight = new double[graph.bonds.length];
            this.fragmentScores = new TObjectDoubleHashMap<>(tree.numberOfVertices(), 0.75f, 0);
            for (Fragment f : tree) {
                final double intensityScore = ano.get(f).getRelativeIntensity() ;
                fragmentScores.adjustOrPutValue(f.getFormula().withoutHydrogen(), intensityScore,intensityScore);
            }
            final double wildcard = name2score.get("*~*");
            for (int i=0; i < bondScoresLeft.length; ++i) {
                IBond b = graph.bonds[i];
                {
                    String name = bondNameSpecific(b, true);
                    if (name2score.containsKey(name)) {
                        bondScoresLeft[i] = name2score.get(name);
                    } else {
                        name = bondNameGeneric(b, true);
                        if (name2score.containsKey(name)) {
                            bondScoresLeft[i] = name2score.get(name);
                        } else {
                            bondScoresLeft[i] = wildcard;
                        }
                    }
                }
                {
                    String name = bondNameSpecific(b, false);
                    if (name2score.containsKey(name)) {
                        bondScoresRight[i] = name2score.get(name);
                    } else {
                        name = bondNameGeneric(b, false);
                        if (name2score.containsKey(name)) {
                            bondScoresRight[i] = name2score.get(name);
                        } else {
                            bondScoresRight[i] = wildcard;
                        }
                    }
                }
            }

        }

        @Override
        public double scoreBond(IBond bond, boolean direction) {
            return (direction ? bondScoresLeft[bond.getIndex()] : bondScoresRight[bond.getIndex()]);
        }

        @Override
        public double scoreFragment(CombinatorialFragment fragment) {
            return fragmentScores.get(fragment.getFormula());
        }
    }

}
