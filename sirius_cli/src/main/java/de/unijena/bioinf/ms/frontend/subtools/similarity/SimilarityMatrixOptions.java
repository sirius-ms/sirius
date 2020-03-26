package de.unijena.bioinf.ms.frontend.subtools.similarity;

import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "similarity",  description = "<STANDALONE> Computes the similarity between all compounds in the dataset and outputs a matrix of similarities.",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SimilarityMatrixOptions implements StandaloneTool<SimilarityMatrixWorkflow> {

    @CommandLine.Option(names = "--ftalign",
            description = {"Compute fragmentation tree alignments between all compounds in the dataset."})
    protected boolean useAlignment;

    @CommandLine.Option(names = "--ftblast",
            description = {"Compute fragmentation tree alignments between all compounds in the dataset, incorporating the given fragmentation tree library. The similarity is not the raw alignment score, but the correlation of the scores."})
    protected File useFtblast;

    @CommandLine.Option(names = "--tanimoto",
            description = {"compute fingerprint similarity between all compounds in the dataset"})
    protected boolean useTanimoto;

    @CommandLine.Option(names = "--cosine",
            description = {"Compute spectral cosine similarity between all compounds in the dataset"})
    protected boolean useCosine;

    @CommandLine.Option(names = {"--directory", "-d"}, defaultValue = ".", description = "Directory to store the matrices.")
    protected File outputDirectory;

    @CommandLine.Option(names = {"--numpy", "--matrix"},
            description = "Write as tab separated file with a comment line containing the row/col names (numpy compatible). Otherwise, write as tab separated file with row and column names.")
    public boolean numpy;


    @Override
    public SimilarityMatrixWorkflow makeWorkflow(RootOptions<?,?,?> rootOptions, ParameterConfig config) {
        return new SimilarityMatrixWorkflow((PreprocessingJob<ProjectSpaceManager>) rootOptions.makeDefaultPreprocessingJob(), this, config);
    }
}
