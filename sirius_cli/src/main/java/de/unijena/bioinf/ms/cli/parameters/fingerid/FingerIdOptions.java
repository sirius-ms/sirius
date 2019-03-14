package de.unijena.bioinf.ms.cli.parameters.fingerid;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.fingerid.predictor_types.UserDefineablePredictorType;
import de.unijena.bioinf.ms.cli.parameters.DefaultParameterOptionLoader;
import de.unijena.bioinf.ms.cli.parameters.InstanceJob;
import de.unijena.bioinf.ms.cli.parameters.Provide;
import de.unijena.bioinf.ms.cli.parameters.sirius.SiriusOptions;
import de.unijena.bioinf.sirius.Sirius;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This is for CSI:FingerID specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "fingerid", aliases = {"F"}, description = "Identify molecular structure for each compound Individually using CSI:FingerID.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class FingerIdOptions  implements Callable<InstanceJob.Factory> {
    private SiriusOptions siriusOptions;
    private Sirius siriusAPI; //todo fill me
    public final static String CONSIDER_ALL_FORMULAS = "all";

    protected final DefaultParameterOptionLoader defaultConfigOptions;

    public FingerIdOptions(DefaultParameterOptionLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }

    @Option(names = "-d", description = "search formulas in given database: all, pubchem, bio, kegg, hmdb")
    public String database;

    @Option(names = {"--fingerid-db", "--fingerid_db", "--fingeriddb"}, description = "search structure in given database. By default the same database for molecular formula search is also used for structure search. If no database is used for molecular formula search, PubChem is used for structure search. Accepts also a filepath to a valid database directory.")
    public String fingerIdDb;

    @Option(names = {"--webservice-info", "-W"}, description = "information about connection of CSI:FingerID Webservice")
    public boolean fingeridInfo;

    @Option(names = {"--fingerid-predictors", "-P"}, description = "Predictors used to search structure with CSI:FingerID")
    public List<UserDefineablePredictorType> getPredictors = Collections.singletonList(UserDefineablePredictorType.CSI_FINGERID);

    @Option(names = "--generate-custom-db", description = "EXPERIMENTAL FEATURE: generate a custom compound database. Ignore all other options. Import compounds from all given files. Usage: sirius --generate-custom-db [DATABASENAME] [INPUTFILE1] [INPUTFILE2] ... ")
    public String generatingCompoundDatabase;

    /*
    @Option(description = "output predicted fingerprint")
    public File getPredict();
    */

    protected Set<MolecularFormula> getFormulaWhitesetWithDB(Ms2Experiment experiment) {
        //todo this should be a remote job we should just annotate that we want to recompute the white list
        /*final String dbOptName = database.toLowerCase();
        FingerIdInstanceProcessor fingerIdInstanceProcessor = new FingerIdInstanceProcessor();
        //todo may create extra DB class
        final HashMap<String, Long> aliasMap = fingerIdInstanceProcessor.getDatabaseAliasMap();
        final SearchableDatabase searchableDatabase = fingerIdInstanceProcessor.getDatabase();
        final long flag;

        if (aliasMap.containsKey(dbOptName)) {
            flag = aliasMap.get(dbOptName).longValue() == DatasourceService.Sources.BIO.flag ? 0 : aliasMap.get(dbOptName);
        } else {
            flag = 0L;
        }
        final Deviation dev;
        if (siriusOptions.ppmMax != null) dev = new Deviation(siriusOptions.ppmMax);
        else
            dev = siriusAPI.getMs2Analyzer().getDefaultProfile().getAllowedMassDeviation();
        final Set<PrecursorIonType> allowedIonTypes = new HashSet<>();
        if (experiment.getPrecursorIonType() == null || experiment.getPrecursorIonType().isIonizationUnknown()) {
            allowedIonTypes.addAll(experiment.getAnnotation(PossibleAdducts.class).getAdducts());
        } else {
            allowedIonTypes.add(experiment.getPrecursorIonType());
        }
        final FormulaConstraints allowedAlphabet;
        if (siriusOptions.elements != null) allowedAlphabet = siriusOptions.elements;
        else
            allowedAlphabet = new FormulaConstraints("CHNOPSBBrClIF"); //todo can we move this to args parsing as default

        List<List<FormulaCandidate>> candidates = new ArrayList<>();
        try {
            if (searchableDatabase.searchInBio()) {
                try (final RESTDatabase db = WebAPI.INSTANCE.getRESTDb(BioFilter.ONLY_BIO, fingerIdInstanceProcessor.bioDatabase.getDatabasePath())) {
                    candidates.addAll(db.lookupMolecularFormulas(experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                }
            }
            if (searchableDatabase.searchInPubchem()) {
                try (final RESTDatabase db = WebAPI.INSTANCE.getRESTDb(BioFilter.ONLY_NONBIO, fingerIdInstanceProcessor.pubchemDatabase.getDatabasePath())) {
                    candidates.addAll(db.lookupMolecularFormulas(experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
                }
            }
            if (searchableDatabase.isCustomDb()) {
                candidates.addAll(fingerIdInstanceProcessor.getFileBasedDb(searchableDatabase).lookupMolecularFormulas(experiment.getIonMass(), dev, allowedIonTypes.toArray(new PrecursorIonType[allowedIonTypes.size()])));
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(this.getClass()).error("Connection to database fails. Probably our webservice is currently offline. You can still use SIRIUS in offline mode - you just have to remove the database flags -d or --database because database search is not available in offline mode.", e);
            System.exit(1);
            return null;
        }

        final HashSet<MolecularFormula> allowedSet = new HashSet<>();
        for (List<FormulaCandidate> fc : candidates) {
            for (FormulaCandidate f : fc) {
                final long bitset = f.getBitset();
                if (flag == 0 || (bitset & flag) != 0)
                    if (allowedAlphabet.isSatisfied(f.getFormula()))
                        allowedSet.add(f.getFormula());
            }
        }
        return allowedSet;*/
        return null;
    }

    @Override
    public InstanceJob.Factory call() throws Exception {
        return FingeridSubToolJob::new;
    }

  /*  @Override
    public void setParamatersToExperiment(MutableMs2Experiment experiment) {
        if (!database.toLowerCase().equals("all")) {
            Set<MolecularFormula> whiteSet = getFormulaWhitesetWithDB(experiment);
            if (whiteSet != null)
                Sirius.setFormulaSearchList(experiment, whiteSet);
        }
    }*/
}
