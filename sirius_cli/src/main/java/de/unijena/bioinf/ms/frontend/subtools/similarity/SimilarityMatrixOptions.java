/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.frontend.subtools.similarity;

import de.unijena.bioinf.ms.frontend.subtools.PreprocessingJob;
import de.unijena.bioinf.ms.frontend.subtools.Provide;
import de.unijena.bioinf.ms.frontend.subtools.RootOptions;
import de.unijena.bioinf.ms.frontend.subtools.StandaloneTool;
import de.unijena.bioinf.ms.properties.ParameterConfig;
import de.unijena.bioinf.projectspace.ProjectSpaceManager;
import de.unijena.bioinf.projectspace.ProjectSpaceManagerFactory;
import picocli.CommandLine;

import java.io.File;

@CommandLine.Command(name = "similarity",  description = "<STANDALONE> Computes the similarity between all compounds in the dataset and outputs a matrix of similarities. %n %n",  versionProvider = Provide.Versions.class, mixinStandardHelpOptions = true, showDefaultValues = true)
public class SimilarityMatrixOptions implements StandaloneTool<SimilarityMatrixWorkflow> {
    @CommandLine.Option(names = {"--digits","--precision","-p"},
            description = {"Specify number of digits used for values in the distance matrix. -1 -> full length Double value."}, defaultValue="-1")
    protected int digits;

    @CommandLine.Option(names = "--ftalign",
            description = {"Compute fragmentation tree alignments between all compounds in the dataset."})
    protected boolean useAlignment;

    @CommandLine.Option(names = "--ftblast",
            description = {"Compute fragmentation tree alignments between all compounds in the dataset, incorporating the given fragmentation tree library. The similarity is not the raw alignment score, but the correlation of the scores."})
    protected File useFtblast;

    @CommandLine.Option(names = "--tanimoto",
            description = {"compute fingerprint similarity between all compounds in the dataset"})
    protected boolean useTanimoto;

    @CommandLine.Option(names = "--tanimoto-canopus",
            description = {"compute canopus fingerprint similarity between all compounds in the dataset"})
    protected boolean useCanopus;

    @CommandLine.Option(names = "--cosine",
            description = {"Compute spectral cosine similarity between all compounds in the dataset"})
    protected boolean useCosine;

    @CommandLine.Option(names = "--modified-cosine",
            description = {"Compute spectral cosine similarity between all compounds in the dataset"})
    protected boolean useModifiedCosine;

    @CommandLine.Option(names = "--minpeaks",defaultValue = "0",
            description = {"For cosine: when less than K peaks are matching, set cosine to zero."})
    protected int useMinPeaks;



    @CommandLine.Option(names = {"--directory", "-d"}, defaultValue = ".", description = "Directory to store the matrices.")
    protected File outputDirectory;

    @CommandLine.Option(names = {"--numpy", "--matrix"},
            description = "Write as tab separated file with a comment line containing the row/col names (numpy compatible). Otherwise, write as tab separated file with row and column names.")
    public boolean numpy;

    private final ProjectSpaceManagerFactory<?> projectSpaceManagerFactory;

    public SimilarityMatrixOptions(ProjectSpaceManagerFactory<?> projectSpaceManagerFactory) {
        this.projectSpaceManagerFactory = projectSpaceManagerFactory;
    }

    @Override
    public SimilarityMatrixWorkflow makeWorkflow(RootOptions<?> rootOptions, ParameterConfig config) {
        return new SimilarityMatrixWorkflow((PreprocessingJob<? extends ProjectSpaceManager>) rootOptions.makeDefaultPreprocessingJob(), projectSpaceManagerFactory, this, config);
    }
}
