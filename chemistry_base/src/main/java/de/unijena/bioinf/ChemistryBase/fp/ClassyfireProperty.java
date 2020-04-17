package de.unijena.bioinf.ChemistryBase.fp;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Locale;

public class ClassyfireProperty extends MolecularProperty {

    protected final int chemOntId;
    protected final String name;
    protected final String description;
    protected final int parentId;
    /**
     * if a compound has two classes, it's main class is the class with higher priority,
     * while the other class becomes its alternative class
     */
    protected final int priority;
    protected int fixedPriority;
    protected int level;
    protected ClassyfireProperty parent;

    public ClassyfireProperty(int chemOntId, String name, String description, int parentId, int priority) {
        this.chemOntId = chemOntId;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.priority = priority;
        this.fixedPriority=-1;
        this.level=-1;
    }

    public int getPriority() {
        return priority;
    }

    public int getFixedPriority() {
        if (fixedPriority<0) fixedPriority = __getFixedPriority();
        return fixedPriority;
    }

    /*
        strangely, sometimes a subclass has a lower priority than the superclass..
         */
    private int __getFixedPriority() {
        int prio = priority;
        ClassyfireProperty node = this;
        while (node.parent!=null) {
            node = node.parent;
            prio = Math.max(prio,node.priority);
        }
        return prio;
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

    /*
        @return the path from root to this node (inclusive)
     */
    public ClassyfireProperty[] getLineage() {
        ArrayList<ClassyfireProperty> prop = new ArrayList<>();
        prop.add(this);
        ClassyfireProperty node = this;
        while (node.parent!=null) {
            node=node.parent;
            prop.add(node);
        }
        return Lists.reverse(prop).toArray(ClassyfireProperty[]::new);
    }

    /**
     * @return all ancestors of this class, from direct parent up to the root
     */
    public ClassyfireProperty[] getAncestors() {
        ArrayList<ClassyfireProperty> prop = new ArrayList<>();
        ClassyfireProperty node = this;
        while (node.parent!=null) {
            node=node.parent;
            prop.add(node);
        }
        return prop.toArray(ClassyfireProperty[]::new);
    }

    @Override
    public String toString() {
        return name;
    }
}
