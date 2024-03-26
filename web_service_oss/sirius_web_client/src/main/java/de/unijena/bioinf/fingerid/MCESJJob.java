package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.algorithm.scoring.Scored;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.jjobs.BasicDependentMasterJJob;
import de.unijena.bioinf.jjobs.JJob;
import matching.algorithm.MCESDist2;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.ArrayList;

/**
 * This job annotates the input list with MCES distance values to the top hit until the first hit is above the threshold.
 */
public class MCESJJob extends BasicDependentMasterJJob<Integer> {

    protected int mcesDistance;

    protected final ArrayList<Scored<FingerprintCandidate>> filteredScoredCandidates;

    private final static SmilesParser smiParser = new SmilesParser(SilentChemObjectBuilder.getInstance());


    public MCESJJob(int mcesDistance, ArrayList<Scored<FingerprintCandidate>> filteredScoredCandidate) {
        super(JobType.CPU);
        this.mcesDistance=mcesDistance;
        this.filteredScoredCandidates=filteredScoredCandidate;
    }

    @Override
    @NotNull
    protected Integer compute() throws Exception {
        checkForInterruption();
        int indexLastIncluded=0;
        if (filteredScoredCandidates.isEmpty())
            return 0;


        IAtomContainer mol1=smiParser.parseSmiles(filteredScoredCandidates.get(0).getCandidate().getSmiles());
        //Annotate top hit with distance 0
        filteredScoredCandidates.get(0).getCandidate().setMcesToTopHit(0d);


        for(int i=1;i< filteredScoredCandidates.size();i++){
            IAtomContainer mol2 = smiParser.parseSmiles(filteredScoredCandidates.get(i).getCandidate().getSmiles());

            MCESDist2 dist = new MCESDist2(mol1, mol2, MCESDist2.MatchingType.ELECTRON_PAIR_MOD);
            double distance = dist.compare();
            filteredScoredCandidates.get(i).getCandidate().setMcesToTopHit(distance);
            if (Double.isInfinite(distance)) {
                indexLastIncluded=i-1;
                break;
            }
        }


        return indexLastIncluded;
    }

    @Override
    public void handleFinishedRequiredJob(JJob required) {

    }
}
