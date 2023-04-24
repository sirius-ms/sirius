package combinatorial_molecule_library_design;

public class NumCandidatesEvaluator implements CMLEvaluator{

    private double relDev;
    private Normalization numCandidatesNormalization;

    public NumCandidatesEvaluator(double ppm, Normalization numCandidatesNormalization){
        this.relDev = ppm * 1e-6;
        this.numCandidatesNormalization = numCandidatesNormalization;
    }

    public NumCandidatesEvaluator(double ppm){
        this(ppm, x -> x);
    }

    @Override
    public double evaluate(int[][] bbMasses) {
        double averageNumCandidatesPerMass = CMLUtils.getAverageNumCandidatesPerMass(bbMasses);
        int totalNumMolecules = CMLUtils.getTotalNumberOfMolecules(bbMasses);
        return totalNumMolecules - numCandidatesNormalization.normalize(averageNumCandidatesPerMass);
    }

    public double getRelDeviation(){
        return this.relDev;
    }
}
