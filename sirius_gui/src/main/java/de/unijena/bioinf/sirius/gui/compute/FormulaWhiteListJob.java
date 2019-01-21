package de.unijena.bioinf.sirius.gui.compute;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.jobs.SiriusJobs;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.fingerid.db.SearchableDatabase;
import de.unijena.bioinf.fingerid.webapi.WebAPI;
import de.unijena.bioinf.jjobs.BasicJJob;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


//todo this class should be in the cli, if we want to have the same funcionality there
public class FormulaWhiteListJob extends BasicJJob<Whiteset> {
    private final Deviation massDev;
    private final boolean onlyOrganic;
    private final SearchableDatabase searchableDatabase;
    private final Ms2Experiment experiment;

    private final boolean annotate;

    public FormulaWhiteListJob(Deviation massDev, boolean onlyOrganic, SearchableDatabase searchableDatabase, Ms2Experiment experiment, boolean annotateResult) {
        super(JobType.WEBSERVICE); //todo io or webservice
        this.massDev = massDev;
        this.onlyOrganic = onlyOrganic;
        this.searchableDatabase = searchableDatabase;
        this.experiment = experiment;
        this.annotate = annotateResult;
    }

    public FormulaWhiteListJob(Deviation massDev, boolean onlyOrganic, SearchableDatabase searchableDatabase, Ms2Experiment experiment) {
        this(massDev, onlyOrganic, searchableDatabase, experiment, true);
    }

    public FormulaWhiteListJob(double ppm, boolean onlyOrganic, SearchableDatabase searchableDatabase, Ms2Experiment experiment) {
        this(new Deviation(ppm), onlyOrganic, searchableDatabase, experiment, true);
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
            for (List<FormulaCandidate> fc : new FilebasedDatabase(MainFrame.MF.getCsiFingerId().getPredictor(ionType).getFingerprintVersion(), searchableDatabase.getDatabasePath()).lookupMolecularFormulas(experiment.getIonMass(), massDev, allowedIons)) {
                formulas.addAll(getFromCandidates(fc));
            }
        }

        if (searchableDatabase.searchInBio()) {
            try (final RESTDatabase db = WebAPI.INSTANCE.getRESTDb(BioFilter.ONLY_BIO, null)) {
                formulas.addAll(searchInOnlineDB(db, allowedIons));
            }
        }

        if (searchableDatabase.searchInPubchem()) {
            try (final RESTDatabase db = WebAPI.INSTANCE.getRESTDb(searchableDatabase.searchInBio() ? BioFilter.ONLY_NONBIO : BioFilter.ALL, null)) {
                formulas.addAll(searchInOnlineDB(db, allowedIons));
            }
        }

        Whiteset whiteset = new Whiteset(formulas);
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

    public static Whiteset searchFormulasInBackround(double ppm, boolean onlyOrganic, SearchableDatabase searchableDatabase, Ms2Experiment ex) {
        FormulaWhiteListJob j = new FormulaWhiteListJob(new Deviation(ppm), onlyOrganic, searchableDatabase, ex, false);
        SiriusJobs.getGlobalJobManager().submitJob(j);
        return j.getResult();
    }
}
