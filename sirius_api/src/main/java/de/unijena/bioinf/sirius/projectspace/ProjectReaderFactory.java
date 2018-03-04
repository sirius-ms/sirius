package de.unijena.bioinf.sirius.projectspace;

public interface ProjectReaderFactory {

    public ProjectReader getSiriusOutputReader(String sirius, DirectoryReader.ReadingEnvironment env);

    public ProjectReader getDirectoryOutputReader(String sirius, DirectoryReader.ReadingEnvironment env);
}
