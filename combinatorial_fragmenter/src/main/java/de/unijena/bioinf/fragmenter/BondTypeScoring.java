package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;

public class BondTypeScoring {

    private static String[] bondTypes = new String[]{
            "C.sp3-C.sp3","C.sp2-C.sp3","C.sp2-C.sp2","C.sp-C.sp3","C.sp-C.sp2","C-C","C-O","C:C","C=O","C.sp2-N.amide","C.sp3-N.amide","C.sp3-N.sp3","C.sp3-N.plus","C.sp2-N.sp3","C.sp3-N.planar3","C.sp2-N.planar3","C.sp3-N.sp2","C.sp2-N.thioamide","C.sp2-N.sp2","C.sp2-N.plus.sp2","C.sp3-N.thioamide","C.sp3-N.plus.sp2","C-N","C=C","C.sp2:N.sp2","C.sp2:N.planar3","C.sp2:N.plus.sp2","C.sp2:N.amide","C.sp2-Cl","C.sp3-Cl","O-P","C-F","C.sp3-S.3","C.sp2-S.onyl","C.sp2-S.3","C.sp3-S.onyl","C.sp2-S.planar3","C.sp3-S.inyl","C.sp2-S.inyl","C-S","Br-C.sp2","Br-C.sp3","Br-C","C=N","C:S","C:O","N.sp3-S.onyl","N.amide-S.onyl","N.planar3-S.onyl","N-S","C-I","N:N","N.plus.sp2-O.minus","N.amide-O.sp3","N.sp2-O.sp3","N.plus-O.minus","N-O","N=O","O.sp2=S.onyl","O.sp2=S.inyl","O=P","C=S","C#N","N.amide-N.sp2","N.amide-N.sp3","N.sp2-N.thioamide","N.amide-N.amide","N.sp3-N.sp3","N.sp2-N.sp3","N-N","C.sp3-P.ate","C.sp2-P.ate","C-P","O-S","N:O","P-S","N=N","S-S","C#C","N-P","N:S","P=S","O-O","C-Si","*~*"
    };

    private static double[] bondScores = new double[]{
            -1.422691773010865,-0.644928976497275,-0.13473420218185225,-0.36272335178337217,0.13930694717745318,-1.0492569363888102,-0.45023141787405796,-1.2529819150187451,-0.6495518503866872,0.16673356602353478,-0.5468024913666523,-0.13210965532493157,-1.9955628897122573,-0.06149961421256729,0.4465242950426763,-0.23685527745902957,0.22119476407975766,0.39031341982342027,-0.2120463224103844,0.6648983491630347,-0.146348039912203,0.10145961317402538,-0.1769301422636504,-0.9834198318016604,-0.6269190393907034,-0.8550989663013292,-0.9089103685740376,-1.243691370728539,0.26399662648708294,-0.05350211320903136,-1.315419132196136,0.0215265871831425,-0.22512309360658964,0.5017693681274681,0.19119500740554404,-0.48587969425798666,0.049550255603367996,0.1453048922438568,0.43579463014472625,0.02417704785354305,0.7800234147165065,0.36188710066745017,0.7523308303691928,-0.22789892039458,-0.14701581855204252,-0.7423384902986149,0.39805351520776167,-0.009431844176429548,0.3318288595709655,0.35065777293063044,0.8491761852645763,-0.40486637760118993,-0.392581236759917,-0.6512351661610059,0.660377695626171,0.35427028832347207,-0.14135527606992473,-0.4524552806358209,-1.6631257194788598,-0.21960768800407188,-2.489670330089028,-0.259605165529807,-0.773806625114072,0.26753076605955395,0.25919766954331713,0.47559099364388757,0.28733485512104145,0.6954667130831772,0.5617262417036548,0.4685220192849586,0.4472725523401184,0.366509391105333,0.43179348270557566,-0.9794634143510622,-0.29417408051785404,0.46579118267249225,0.37579899056083277,0.023430161966290212,-1.024446528990568,0.366400448671523,-0.03283272261568555,-0.749140123154636,-0.019599762726626734,-0.40076836289636775,-0.12329888989174126
    };

    private final static TObjectDoubleHashMap<String> name2score;
    static {
        name2score = new TObjectDoubleHashMap<>();
        double mx = Arrays.stream(bondScores).max().getAsDouble();
        for (int j=0; j < bondScores.length; ++j) name2score.put(bondTypes[j],bondScores[j]-mx);
    }

    public CombinatorialFragmenterScoring getScoringFor(MolecularGraph graph, FTree tree) {
        return new BondTypeScoringImpl(graph, tree);
    }

    public static String bondNameGeneric(IBond b) {
        return explainBondBy(b, new String[]{
           b.getAtom(0).getSymbol()  ,b.getAtom(1).getSymbol()
        });
    }

    public static String bondNameSpecific(IBond b) {
        return explainBondBy(b, new String[]{
                b.getAtom(0).getAtomTypeName()  ,b.getAtom(1).getAtomTypeName()
        });
    }

    private static String explainBondBy(IBond b, String[] atomLabels) {
        Arrays.sort(atomLabels);
        String s = atomLabels[0];
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
        s += atomLabels[1];
        return s;
    }

    protected static class BondTypeScoringImpl implements CombinatorialFragmenterScoring {

        private double[] bondScores;
        private TObjectDoubleHashMap<MolecularFormula> fragmentScores;

        public BondTypeScoringImpl(MolecularGraph graph, FTree tree) {
            FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);

            this.bondScores = new double[graph.bonds.length];
            this.fragmentScores = new TObjectDoubleHashMap<>(tree.numberOfVertices(), 0.75f, 0);
            for (Fragment f : tree) {
                final double intensityScore = ano.get(f).getRelativeIntensity() ;
                fragmentScores.adjustOrPutValue(f.getFormula().withoutHydrogen(), intensityScore,intensityScore);
            }
            final double wildcard = name2score.get("*~*");
            for (int i=0; i < bondScores.length; ++i) {
                IBond b = graph.bonds[i];
                String name = bondNameSpecific(b);
                if (name2score.containsKey(name)) {
                    bondScores[i] = name2score.get(name);
                } else {
                    name = bondNameGeneric(b);
                    if (name2score.containsKey(name)) {
                        bondScores[i] = name2score.get(name);
                    } else {
                        bondScores[i] = wildcard;
                    }
                }
            }

        }

        @Override
        public double scoreBond(IBond bond, boolean direction) {
            return bondScores[bond.getIndex()];
        }

        @Override
        public double scoreFragment(CombinatorialNode fragment) {
            return fragmentScores.get(fragment.fragment.getFormula().withoutHydrogen());
        }

        public double scoreEdge(CombinatorialEdge edge){
            return scoreBond(edge.getCut1(),edge.getDirectionOfFirstCut()) + (edge.getCut2() != null ? scoreBond(edge.getCut2(),edge.getDirectionOfSecondCut()) : 0);
        }
    }

}
