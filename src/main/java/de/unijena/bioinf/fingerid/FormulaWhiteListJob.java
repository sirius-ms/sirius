package de.unijena.bioinf.fingerid;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.WebAPI;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * retrieves a {@Whiteset} of {@MolecularFormula}s based on the given {@SearchableDatabase}
 */
public class FormulaWhiteListJob extends BasicJJob<Whiteset> {
    private final WebAPI webAPI;
    private final SearchableDatabase searchableDatabase;

    //job parameter
    private final boolean onlyOrganic;
    private final boolean annotate;

    //experiment parameter
    private final Ms2Experiment experiment;
    private final Deviation massDev;


    public FormulaWhiteListJob(WebAPI api, SearchableDatabase searchableDatabase, Ms2Experiment experiment) {
        this(api, searchableDatabase, experiment, false);
    }

    public FormulaWhiteListJob(WebAPI api, SearchableDatabase searchableDatabase, Ms2Experiment experiment, boolean annotateResult) {
        this(api, searchableDatabase, experiment, experiment.getAnnotationOrThrow(MS2MassDeviation.class).allowedMassDeviation, true, annotateResult);
    }

    public FormulaWhiteListJob(WebAPI api, SearchableDatabase searchableDatabase, Ms2Experiment experiment, Deviation massDev, boolean onlyOrganic, boolean annotateResult) {
        super(JobType.WEBSERVICE);
        this.massDev = massDev;
        this.onlyOrganic = onlyOrganic;
        this.searchableDatabase = searchableDatabase;
        this.experiment = experiment;
        this.annotate = annotateResult;
        this.webAPI = api;
    }

    @Override
    protected Whiteset compute() throws Exception {
        final Set<MolecularFormula> formulas = new HashSet<>();

        PrecursorIonType ionType = experiment.getPrecursorIonType();
        PrecursorIonType[] allowedIons;
        if (ionType.isIonizationUnknown()) {
            allowedIons = Iterables.toArray(PeriodicTable.getInstance().getIonizations(ionType.getCharge()), PrecursorIonType.class);
        } else {
            allowedIons = new PrecursorIonType[]{ionType};
        }

        if (searchableDatabase.isCustomDb()) {
            for (List<FormulaCandidate> fc : new FilebasedDatabase(webAPI.getCDKMaskedFingerprintVersion(ionType.getCharge()), searchableDatabase.getDatabasePath()).lookupMolecularFormulas(experiment.getIonMass(), massDev, allowedIons)) {
                formulas.addAll(getFromCandidates(fc));
            }
        }

        if (searchableDatabase.searchInBio()) {
            try (final RESTDatabase db = webAPI.getRESTDb(BioFilter.ONLY_BIO, null)) {
                formulas.addAll(searchInOnlineDB(db, allowedIons));
            }
        }

        if (searchableDatabase.searchInPubchem()) {
            try (final RESTDatabase db = webAPI.getRESTDb(searchableDatabase.searchInBio() ? BioFilter.ONLY_NONBIO : BioFilter.ALL, null)) {
                formulas.addAll(searchInOnlineDB(db, allowedIons));
            }
        }

        Whiteset whiteset = Whiteset.of(formulas);
        if (annotate) {
            experiment.setAnnotation(Whiteset.class, whiteset);
        }

        return whiteset;
    }

    private List<MolecularFormula> searchInOnlineDB(final RESTDatabase db, PrecursorIonType[] allowedIons) throws ChemicalDatabaseException {
        final List<MolecularFormula> formulas = new ArrayList<>();
        for (List<FormulaCandidate> fc : db.lookupMolecularFormulas(experiment.getIonMass(), massDev, allowedIons)) {
            formulas.addAll(getFromCandidates(fc));
        }
        return formulas;
    }

    private List<MolecularFormula> getFromCandidates(List<FormulaCandidate> fc) {
        final List<MolecularFormula> formulas = new ArrayList<>();
        for (FormulaCandidate f : fc) {
            if (onlyOrganic) {
                if (f.getFormula().isCHNOPSBBrClFI())
                    formulas.add(f.getFormula());
            } else {
                formulas.add(f.getFormula());
            }
        }
        return formulas;
    }

    /*public static Whiteset searchFormulasInBackround(double ppm, boolean onlyOrganic, SearchableDatabase searchableDatabase, Ms2Experiment ex) {
        FormulaWhiteListJob j = new FormulaWhiteListJob(new Deviation(ppm), onlyOrganic, searchableDatabase, ex, false);
        SiriusJobs.getGlobalJobManager().submitJob(j);
        return j.getResult();
    }*/
}
