package de.unijena.bioinf.ChemistryBase.fp;

import java.util.Locale;

public class ClassyfireProperty extends MolecularProperty {

    protected final int chemOntId;
    protected final String name;
    protected final String description;
    protected final int parentId;

    protected ClassyfireProperty parent;

    public ClassyfireProperty(int chemOntId, String name, String description, int parentId) {
        this.chemOntId = chemOntId;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
    }

    void setParent(ClassyfireProperty parent) {
        this.parent = parent;
    }

    public String getChemontIdentifier() {
        return String.format(Locale.US, "CHEMONT:%07d", chemOntId);
    }

    public int getChemOntId() {
        return chemOntId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getParentId() {
        return parentId;
    }

    public ClassyfireProperty getParent() {
        return parent;
    }
}
