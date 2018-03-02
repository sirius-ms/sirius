package de.unijena.bioinf.fingerid;

import de.unijena.bioinf.sirius.projectspace.*;

public class FingeridProjectSpaceFactory implements ReaderWriterFactory {
    @Override
    public ProjectWriter getSiriusOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new FingerIdResultWriter(env);

    }

    @Override
    public ProjectWriter getDirectoryOutputWriter(String sirius, DirectoryWriter.WritingEnvironment env) {
        return new FingerIdResultWriter(env);
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
