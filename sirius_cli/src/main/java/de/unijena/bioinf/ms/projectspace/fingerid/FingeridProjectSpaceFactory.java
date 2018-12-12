package de.unijena.bioinf.ms.projectspace.fingerid;


import de.unijena.bioinf.ms.projectspace.*;

public class FingeridProjectSpaceFactory implements ReaderWriterFactory {
    private FilenameFormatter filenameFormatter;

    public FingeridProjectSpaceFactory(FilenameFormatter filenameFormatter) {
        this.filenameFormatter = filenameFormatter;
    }

    @Override
    public ProjectWriter getSiriusOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new FingerIdResultWriter(env, filenameFormatter);

    }

    @Override
    public ProjectWriter getDirectoryOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new FingerIdResultWriter(env, filenameFormatter);
    }

    @Override
    public ProjectReader getSiriusOutputReader(String sirius, DirectoryReader.ReadingEnvironment env) {
        return new FingerIdResultReader(env);
    }

    @Override
    public ProjectReader getDirectoryOutputReader(String sirius, DirectoryReader.ReadingEnvironment env) {
        return new FingerIdResultReader(env);
    }
}
