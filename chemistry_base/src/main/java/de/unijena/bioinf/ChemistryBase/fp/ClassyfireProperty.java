/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.ChemistryBase.fp;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

public class ClassyfireProperty extends MolecularProperty {

    @Getter
    protected final int chemOntId;
    @Getter
    protected final String name;
    @Getter
    protected final String description;
    @Getter
    protected final int parentId;
    /**
     * if a compound has two classes, it's main class is the class with higher priority,
     * while the other class becomes its alternative class
     */
    @Getter
    protected final int priority;
    protected int fixedPriority;
    // altPriority is an geometric mean between priority ranks and frequency ranks. Thus, "large" compound
    // classes like peptides are ranked down because they are so frequent
    protected float altPriority;
    @Getter
    protected int level;
    @Getter
    protected ClassyfireProperty parent;

    public ClassyfireProperty(int chemOntId, String name, String description, int parentId, int priority, float altPriority) {
        this.chemOntId = chemOntId;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.priority = priority;
        this.fixedPriority=-1;
        this.level=-1;
        this.altPriority = altPriority;
    }

    public int getFixedPriority() {
        if (fixedPriority<0) __getFixedPriority();
        return fixedPriority;
    }

    public float getAltPriority() {
        if (fixedPriority < 0) {
            __getFixedPriority();
        }
        return altPriority;
    }

    /*
        strangely, sometimes a subclass has a lower priority than the superclass..
         */
    private void __getFixedPriority() {
        int prio = priority;
        float prio2 = altPriority;
        ClassyfireProperty node = this;
        while (node.parent!=null) {
            node = node.parent;
            prio = Math.max(prio,node.priority);
            prio2 = Math.max(prio2, node.altPriority);
        }
        this.fixedPriority = prio;
        this.altPriority = prio2;
    }

    void setParent(ClassyfireProperty parent) {
        this.parent = parent;
    }

    public String getChemontIdentifier() {
        return String.format(Locale.US, "CHEMONT:%07d", chemOntId);
    }

    /**
     *
     * @param includeChemicalEntity if true, includes the true root of the ontology, "chemical entity" which is not predicted by CANOPUS.
     * @return the path from root to this node (inclusive)
     */
    public ClassyfireProperty[] getLineageRootToNode(boolean includeChemicalEntity) {
        ArrayList<ClassyfireProperty> prop = getAncestorsList(true, includeChemicalEntity);
        return prop.reversed().toArray(ClassyfireProperty[]::new);
    }

    /**
     *
     * @param includeChemicalEntity if true, includes the true root of the ontology, "chemical entity" which is not predicted by CANOPUS.
     * @return the path from this node (inclusive) to root
     */
    public ClassyfireProperty[] getLineageNodeToRoot(boolean includeChemicalEntity) {
        ArrayList<ClassyfireProperty> prop = getAncestorsList(true, includeChemicalEntity);
        return prop.toArray(ClassyfireProperty[]::new);
    }

    /**
     *
     * @param includeChemicalEntity if true, includes the true root of the ontology, "chemical entity" which is not predicted by CANOPUS.
     * @return all ancestors of this class, from direct parent up to the root
     */
    public ClassyfireProperty[] getAncestors(boolean includeChemicalEntity) {
        return getAncestorsList(false, includeChemicalEntity).toArray(ClassyfireProperty[]::new);
    }

    /**
     *
     * @param includeThisNode includes the current node in the list
     * @param includeChemicalEntity if true, includes the true root of the ontology, "chemical entity" which is not predicted by CANOPUS.
     * @return all ancestors of this class, from direct parent up to the root
     */
    private ArrayList<ClassyfireProperty> getAncestorsList(boolean includeThisNode, boolean includeChemicalEntity) {
        ArrayList<ClassyfireProperty> prop = new ArrayList<>();
        if (includeThisNode) prop.add(this);
        ClassyfireProperty node = this;
        while (node.parent!=null) {
            node=node.parent;
            prop.add(node);
        }
        if (!includeChemicalEntity) {
            assert prop.get(prop.size()-1).name.equals("Chemical entities");
            prop.remove(prop.size()-1);
        }
        return prop;
    }

    /**
     * Compares two compound classes based on how descriptive they are. The "larger" compound class is always the
     * one which describes a larger structure or a more specific concept.
     */
    public static class CompareCompoundClassDescriptivity implements Comparator<ClassyfireProperty> {

        @Override
        public int compare(ClassyfireProperty o1, ClassyfireProperty o2) {
            if ((o1.getFixedPriority() > o2.getFixedPriority()) || (o1.getFixedPriority() == o2.getFixedPriority() && o1.level > o2.level)) return 1;
            if ((o2.getFixedPriority() > o1.getFixedPriority()) || (o2.getFixedPriority() == o1.getFixedPriority() && o2.level > o1.level)) return -1;
            return 0;
        }
    }
    public static class CompareCompoundClassDescriptivityConsideringFrequency implements Comparator<ClassyfireProperty> {

        @Override
        public int compare(ClassyfireProperty o1, ClassyfireProperty o2) {
            if ((o1.getAltPriority() > o2.getAltPriority()) || (o1.getAltPriority() == o2.getAltPriority() && o1.level > o2.level)) return 1;
            if ((o2.getAltPriority() > o1.getAltPriority()) || (o2.getAltPriority() == o1.getAltPriority() && o2.level > o1.level)) return -1;
            return 0;
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
