package de.unijena.bioinf.ms.frontend.subtools.fingerid;

import de.unijena.bioinf.ms.frontend.subtools.InstanceJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.config.DefaultParameterConfigLoader;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * This is for CSI:FingerID specific parameters.
 * <p>
 * They may be annotated to the MS2 Experiment
 *
 * @author Markus Fleischauer (markus.fleischauer@gmail.com)
 */
@CommandLine.Command(name = "fingerid", aliases = {"F"}, description = "Identify molecular structure for each compound Individually using CSI:FingerID.", defaultValueProvider = Provide.Defaults.class, versionProvider = Provide.Versions.class,  mixinStandardHelpOptions = true)
public class FingerIdOptions implements Callable<InstanceJob.Factory<FingeridSubToolJob>> {
    protected final DefaultParameterConfigLoader defaultConfigOptions;


    public FingerIdOptions(DefaultParameterConfigLoader defaultConfigOptions) {
        this.defaultConfigOptions = defaultConfigOptions;
    }



    @Option(names = {"--info", "--webservice-info"}, description = "information about connection of CSI:FingerID Webservice")
    public boolean fingeridInfo;

    @Option(names = {"-d", "--db ", "--fingerid-db", "--fingerid_db", "--fingeriddb"}, description = "search structure in given database. By default the same database for molecular formula search is also used for structure search. If no database is used for molecular formula search, PubChem is used for structure search.")/*Accepts also a filepath to a valid database directory.*/
    public void setDatabase(String name) throws Exception {
        defaultConfigOptions.changeOption("StructureSearchDB", name);
    }

    @Option(names = {"-p", "--structure-predictors"}, description = "Predictors used to search structures. Currently only CSI:FingerID is working.", hidden = true)
    public void setPredictors(List<String> predictors) throws Exception {
        defaultConfigOptions.changeOption("StructurePredictors", predictors);
    }

    // candidates
    @Option(names = {"-c", "--candidates"}, description = "Number of molecular structure candidates in the output.")
    public void setNumberOfCandidates(String value) throws Exception {
        defaultConfigOptions.changeOption("NumberOfStructureCandidates", value);
    }


    @Override
    public InstanceJob.Factory<FingeridSubToolJob> call() throws Exception {
        return FingeridSubToolJob::new;
    }



}
