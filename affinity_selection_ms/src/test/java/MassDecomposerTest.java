import combinatorial_molecule_library_design.MassDecomposer;

public class MassDecomposerTest {

    public static void main(String[] args){
        int[][] bbMasses = new int[][]{{2,3,7,10},{2,3,7,10},{2,3,7,10},{2,3,7,10}};
        MassDecomposer massDecomposer = new MassDecomposer(bbMasses);
        System.out.println(massDecomposer.numberOfMoleculesForIntegerMass(5));
        System.out.println(massDecomposer.numberOfMoleculesForIntegerMass(10));
        System.out.println(massDecomposer.numberOfMoleculesForIntegerMass(20));
        System.out.println(massDecomposer.numberOfMoleculesForIntegerMass(10));
    }
}
