package de.unijena.bioinf.fingerid;

import com.google.common.collect.Iterables;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.ChemistryBase.ms.MS2MassDeviation;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.model.Whiteset;
import de.unijena.bioinf.chemdb.*;
import de.unijena.bioinf.jjobs.BasicJJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * retrieves a {@link Whiteset} of {@link MolecularFormula}s based on the given {@link SearchableDatabase}
 */
public class FormulaWhiteListJob extends BasicJJob<Whiteset> {
    private final List<SearchableDatabase> searchableDatabases;
    private final RestWithCustomDatabase searchDB;
    //job parameter
    private final boolean onlyOrganic;
    private final boolean annotate;

    //experiment parameter
    private final Ms2Experiment experiment;
    private final Deviation massDev;


    /*public FormulaWhiteListJob(RestWithCustomDatabase searchDB, List<SearchableDatabase> searchableDatabases, Ms2Experiment experiment) {
        this(searchDB, searchableDatabases, experiment, false);
    }*/

    public FormulaWhiteListJob(RestWithCustomDatabase searchDB, List<SearchableDatabase> searchableDatabases, Ms2Experiment experiment, boolean onlyOrganic, boolean annotateResult) {
        this(searchDB, searchableDatabases, experiment, experiment.getAnnotationOrThrow(MS2MassDeviation.class).allowedMassDeviation, onlyOrganic, annotateResult);
    }

    public FormulaWhiteListJob(RestWithCustomDatabase searchDB, List<SearchableDatabase> searchableDatabases, Ms2Experiment experiment, Deviation massDev, boolean onlyOrganic, boolean annotateResult) {
        super(JobType.WEBSERVICE);
        this.massDev = massDev;
        this.searchableDatabases = searchableDatabases;
        this.experiment = experiment;
        this.annotate = annotateResult;
        this.searchDB = searchDB;
        this.onlyOrganic = onlyOrganic;
    }

    @Override
    protected Whiteset compute() throws Exception {
        PrecursorIonType ionType = experiment.getPrecursorIonType();
        PrecursorIonType[] allowedIons;
        if (ionType.isIonizationUnknown()) {
            allowedIons = Iterables.toArray(PeriodicTable.getInstance().getIonizations(ionType.getCharge()), PrecursorIonType.class);
        } else {
            allowedIons = new PrecursorIonType[]{ionType};
        }

        final Set<MolecularFormula> formulas = searchDB.loadMolecularFormulas(experiment.getIonMass(), massDev, allowedIons, searchableDatabases)
                .stream().map(FormulaCandidate::getFormula).filter(f -> !onlyOrganic || f.isCHNOPSBBrClFI())
                .collect(Collectors.toSet());

        final Whiteset whiteset = Whiteset.of(formulas);
        if (annotate)
            experiment.setAnnotation(Whiteset.class, whiteset);

        return whiteset;
    }

    /*private List<MolecularFormula> searchInOnlineDB(final RESTDatabase db, PrecursorIonType[] allowedIons) throws ChemicalDatabaseException {
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
    }*/

    /*public static Whiteset searchFormulasInBackround(double ppm, boolean onlyOrganic, SearchableDatabase searchableDatabase, Ms2Experiment ex) {
        FormulaWhiteListJob j = new FormulaWhiteListJob(new Deviation(ppm), onlyOrganic, searchableDatabase, ex, false);
        SiriusJobs.getGlobalJobManager().submitJob(j);
        return j.getResult();
    }*/
}
