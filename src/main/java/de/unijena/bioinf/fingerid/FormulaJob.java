package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.chemdb.RestWithCustomDatabase;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeState;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.SearchableDatabase;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.utils.NetUtils;

import java.util.List;

/**
 * retrieves {@link FingerprintCandidate}s for a given {@link MolecularFormula}
 */
public class FormulaJob extends BasicJJob<RestWithCustomDatabase.CandidateResult> {

    protected final MolecularFormula formula;
    protected final RestWithCustomDatabase searchDatabase;
    protected final List<SearchableDatabase> dbs;
    protected final PrecursorIonType ionType;
    protected final boolean includeRestAllDb;


    public FormulaJob(MolecularFormula formula, RestWithCustomDatabase searchDatabase, List<SearchableDatabase> dbs, PrecursorIonType precursorIonType, boolean includeRestAllDb) {
        super(JobType.WEBSERVICE);
        this.formula = formula;
        this.searchDatabase = searchDatabase;
        this.dbs = dbs;
        this.ionType = precursorIonType;
        this.includeRestAllDb = includeRestAllDb;
    }

    @Override
    protected RestWithCustomDatabase.CandidateResult compute() throws Exception {
        return NetUtils.tryAndWait(() -> {
            final RestWithCustomDatabase.CandidateResult result = searchDatabase.loadCompoundsByFormula(formula, dbs, includeRestAllDb);


            final CompoundCandidateChargeState chargeState = CompoundCandidateChargeState.getFromPrecursorIonType(ionType);
            if (chargeState != CompoundCandidateChargeState.NEUTRAL_CHARGE) {
                final MolecularFormula hydrogen = MolecularFormula.parseOrThrow("H");
                final RestWithCustomDatabase.CandidateResult protonated = searchDatabase.loadCompoundsByFormula(
                        ionType.getCharge() > 0 ? formula.subtract(hydrogen) : formula.add(hydrogen),
                        dbs, includeRestAllDb);

                result.merge(protonated);
            }

            return result;
        }, this::checkForInterruption);
    }
}
