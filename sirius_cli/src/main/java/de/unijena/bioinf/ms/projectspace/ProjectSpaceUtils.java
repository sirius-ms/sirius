package de.unijena.bioinf.ms.projectspace;

import de.unijena.bioinf.sirius.ExperimentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProjectSpaceUtils {
    protected static Logger logger = LoggerFactory.getLogger(ProjectSpaceUtils.class);

    // TODO: implement merge?
    public static DirectoryWriter.WritingEnvironment getWorkspaceWritingEnvironmentForSirius(String value) throws IOException {
        try {
            if (value.equals("-")) {
                return new SiriusWorkspaceWriter(System.out);
            } else {
                return new SiriusWorkspaceWriter(new FileOutputStream(new File(value)));
            }
        } catch (FileNotFoundException e) {
            throw new IOException("Cannot write into " + value + ". The given file name might already exists.");
        }
    }

    public static DirectoryWriter.WritingEnvironment getWorkspaceWritingEnvironmentForDirectoryOutput(String value) throws IOException {
        final File root = new File(value);
        if (root.exists()) {
            throw new IOException("Cannot create directory " + root.getName() + ". File already exist.");
        }
        root.mkdirs();
        return new SiriusFileWriter(root);
    }


    public static DirectoryReader.ReadingEnvironment getWorkspaceReadingEnvironmentForSirius(String value) throws IOException {
        try {
            return new SiriusWorkspaceReader(new File(value));
        } catch (IOException e) {
            throw new IOException("Cannot read " + value + ":\n" + e.getMessage());
        }
    }

    public static DirectoryReader.ReadingEnvironment getWorkspaceReadingEnvironmentForDirectoryOutput(String value) {
        final File root = new File(value);
        return new SiriusFileReader(root);
    }


    public static ProjectWriter getDirectoryOutputWriter(String directoryOutputPath, ReaderWriterFactory readerWriterFactory) throws IOException {
        final ProjectWriter pw;
        if ((new File(directoryOutputPath)).exists()) {
            try {
                checkForValidProjectDirectory(directoryOutputPath);
                pw = new ProjectSpaceMerger(readerWriterFactory, directoryOutputPath.toString(), false);
            } catch (IOException e) {
                throw new IOException("Cannot merge project " + directoryOutputPath + ". Maybe the specified directory is not a valid SIRIUS workspace. You can still specify a new not existing filename to create a new workspace.\n" + e.getMessage(), e);
            }
        } else {
            try {
                pw = readerWriterFactory.getDirectoryOutputWriter(directoryOutputPath, ProjectSpaceUtils.getWorkspaceWritingEnvironmentForDirectoryOutput(directoryOutputPath));
            } catch (IOException e) {
                throw new IOException("Cannot write into " + directoryOutputPath + ":\n" + e.getMessage(), e);
            }
        }
        return pw;
    }

    public static ProjectWriter getSiriusOutputWriter(String sirius, ReaderWriterFactory readerWriterFactory) throws IOException {
        final ProjectWriter pw;
        if (sirius.equals("-")) {
            pw = readerWriterFactory.getSiriusOutputWriter(sirius, ProjectSpaceUtils.getWorkspaceWritingEnvironmentForSirius(sirius));
        } else if (new File(sirius).exists()) {
            try {
                pw = new ProjectSpaceMerger(readerWriterFactory, sirius, true);
            } catch (IOException e) {
                throw new IOException("Cannot merge " + sirius + ". The specified file might be no valid SIRIUS workspace. You can still specify a new not existing filename to create a new workspace.");
            }
        } else {
            pw = readerWriterFactory.getSiriusOutputWriter(sirius, ProjectSpaceUtils.getWorkspaceWritingEnvironmentForSirius(sirius));
        }
        return pw;
    }


    public static ProjectWriterInfo getProjectWriter(String directoryOutputPath, String siriusOutputPath, ReaderWriterFactory readerWriterFactory) throws IOException {
        List<ProjectWriter> writers = new ArrayList<>();
        int numberOfWrittenExperiments = 0;
        if (directoryOutputPath!=null){
            ProjectWriter pw = getDirectoryOutputWriter(directoryOutputPath, readerWriterFactory);
            writers.add(pw);
            if (pw instanceof ProjectSpaceMerger)
                numberOfWrittenExperiments = Math.max(numberOfWrittenExperiments,((ProjectSpaceMerger)pw).getNumberOfWrittenExperiments());
        }
        if (siriusOutputPath!=null){
            ProjectWriter pw = getSiriusOutputWriter(siriusOutputPath, readerWriterFactory);
            writers.add(pw);
            if (pw instanceof ProjectSpaceMerger)
                 numberOfWrittenExperiments = Math.max(numberOfWrittenExperiments,((ProjectSpaceMerger)pw).getNumberOfWrittenExperiments());
        }

        final ProjectWriter projectWriter;
        if (writers.size() > 1) {
            projectWriter =  new MultipleProjectWriter(writers.toArray(new ProjectWriter[writers.size()]));
        } else if (writers.size() > 0) {
            projectWriter =  writers.get(0);
        } else {
            projectWriter = new ProjectWriter() {
                @Override
                public void writeExperiment(ExperimentResult result) throws IOException {
                    // dummy stub
                }

                @Override
                public void close() throws IOException {
                    // dummy stub
                }
            };
        }

        return new ProjectWriterInfo(projectWriter, numberOfWrittenExperiments);
    }


    private static void checkForValidProjectDirectory(String output) throws IOException {
        final File f = new File(output);
        if (!f.exists()) return;
        if (!f.isDirectory()) throw new IOException("Expect a directory name. But " + output + " is an existing file.");
        final Pattern pat = Pattern.compile("Sirius", Pattern.CASE_INSENSITIVE);
        boolean empty = true;
        for (File g : f.listFiles()) {
            empty = false;
            if (g.getName().equalsIgnoreCase("version.txt")) {
                for (String line : com.google.common.io.Files.readLines(g, Charset.forName("UTF-8"))) {
                    if (pat.matcher(line).find()) return;
                }
            }
        }
        if (!empty)
            throw new IOException("Given directory is not a valid SIRIUS workspace. Please specify an empty directory or existing SIRIUS workspace!");
    }

    public static class ProjectWriterInfo {
        private ProjectWriter projectWriter;
        private int numberOfWrittenExperiments;

        protected ProjectWriterInfo(ProjectWriter projectWriter, int numberOfWrittenExperiments) {
            this.projectWriter = projectWriter;
            this.numberOfWrittenExperiments = numberOfWrittenExperiments;
        }

        public ProjectWriter getProjectWriter() {
            return projectWriter;
        }

        public int getNumberOfWrittenExperiments() {
            return numberOfWrittenExperiments;
        }
    }
}
