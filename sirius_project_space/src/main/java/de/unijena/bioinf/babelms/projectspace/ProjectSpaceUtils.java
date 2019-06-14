package de.unijena.bioinf.babelms.projectspace;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;
import java.util.regex.Pattern;

public class ProjectSpaceUtils {
    public static DirectoryWriter.WritingEnvironment getWorkspaceWritingEnvironmentForSirius(String value) throws IOException {
        try {
            if (value.equals("-")) {
                return new SiriusZipFileWriter(System.out);
            } else {
                return new SiriusZipFileWriter(new FileOutputStream(new File(value)));
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
            return new SiriusZipFileReader(new File(value));
        } catch (IOException e) {
            throw new IOException("Cannot read " + value + ":\n" + e.getMessage());
        }
    }

    public static DirectoryReader.ReadingEnvironment getWorkspaceReadingEnvironmentForDirectoryOutput(String value) {
        final File root = new File(value);
        return new SiriusFileReader(root);
    }




    private static void checkForValidProjectDirectory(String output) throws IOException {
        final File f = new File(output);
        if (!f.exists()) return;
        if (!f.isDirectory()) throw new IOException("Expect a directory name. But " + output + " is an existing file.");
        final Pattern pat = Pattern.compile("Sirius", Pattern.CASE_INSENSITIVE);
        boolean empty = true;
        for (File g : Objects.requireNonNull(f.listFiles())) {
            empty = false;
            if (g.getName().equalsIgnoreCase("version.txt")) {
                for (String line : Files.readAllLines(g.toPath(), Charset.forName("UTF-8"))) {
                    if (pat.matcher(line).find()) return;
                }
            }
        }
        if (!empty)
            throw new IOException("Given directory is not a valid SIRIUS workspace. Please specify an empty directory or existing SIRIUS workspace!");
    }


}
