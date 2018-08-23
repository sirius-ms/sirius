package de.unijena.bioinf.fingerid.jjobs;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeLayer;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeState;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.SearchStructureByFormula;
import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.List;

/**
 * retrieves {@FingerprintCandidate}s for a given {@MolecularFormula}
 */
public class FormulaJob extends BasicJJob<List<FingerprintCandidate>> {

    protected final MolecularFormula formula;
    protected final SearchStructureByFormula searchDatabase;
    protected final PrecursorIonType ionType;

    public FormulaJob(MolecularFormula formula, SearchStructureByFormula searchDatabase, PrecursorIonType precursorIonType) {
        super(JobType.REMOTE);
        this.formula = formula;
        this.searchDatabase = searchDatabase;
        this.ionType = precursorIonType;
    }

    @Override
    protected List<FingerprintCandidate> compute() throws Exception {
        final CompoundCandidateChargeState chargeState = CompoundCandidateChargeState.getFromPrecursorIonType(ionType);
        if (chargeState != CompoundCandidateChargeState.NEUTRAL_CHARGE) {
            final List<FingerprintCandidate> intrinsic = searchDatabase.lookupStructuresAndFingerprintsByFormula(formula);
            intrinsic.removeIf((f)->!f.hasChargeState(CompoundCandidateChargeLayer.Q_LAYER, chargeState));
            // all intrinsic formulas have to contain a p layer?
            final MolecularFormula hydrogen = MolecularFormula.parse("H");
            final List<FingerprintCandidate> protonated = searchDatabase.lookupStructuresAndFingerprintsByFormula(ionType.getCharge()>0 ? formula.subtract(hydrogen) : formula.add(hydrogen));
            protonated.removeIf((f)->!f.hasChargeState(CompoundCandidateChargeLayer.P_LAYER, chargeState));

            intrinsic.addAll(protonated);
            return intrinsic;
        } else {
            final List<FingerprintCandidate> candidates = searchDatabase.lookupStructuresAndFingerprintsByFormula(formula);
            candidates.removeIf((f)->!f.hasChargeState(CompoundCandidateChargeLayer.P_LAYER, CompoundCandidateChargeState.NEUTRAL_CHARGE));
            return candidates;
        }
    }
}
