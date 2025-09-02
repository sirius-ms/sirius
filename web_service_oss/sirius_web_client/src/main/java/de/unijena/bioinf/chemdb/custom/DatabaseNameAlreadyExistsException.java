package de.unijena.bioinf.chemdb.custom;

public class DatabaseNameAlreadyExistsException extends IllegalStateException{
    public DatabaseNameAlreadyExistsException(String name) {
        super("Datasource with name '" + name + "' already exists.");
    }
}
