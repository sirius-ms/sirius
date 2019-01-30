package de.unijena.bioinf.ms.io.projectspace;

public interface ProjectReaderFactory {

    public ProjectReader getSiriusOutputReader(String sirius, DirectoryReader.ReadingEnvironment env);

    public ProjectReader getDirectoryOutputReader(String sirius, DirectoryReader.ReadingEnvironment env);
}
