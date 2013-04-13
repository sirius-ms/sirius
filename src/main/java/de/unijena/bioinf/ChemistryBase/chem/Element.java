package de.unijena.bioinf.ChemistryBase.chem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.unijena.bioinf.ChemistryBase.chem.utils.IsotopicDistribution;


/**
 * Basic class for chemical elements. Instances of this class are read-only. So there is no possibility
 * to change their information.
 * Elements are immediate values. So two elements are equal if and only if they are the same object in memory.
 * So be careful when serializing elements! (Write them by their symbol and use the PeriodicTable to deserialize).
 */
public class Element implements Comparable<Element> {

    double mass;
    int nominalMass;
    private final int id, valence;
    private final String name, symbol;

    Element(int id, String name, String symbol, double mass, int valence) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.mass = mass;
        this.valence = valence;
        this.nominalMass = (int)Math.round(mass);
    }

    /**
     * returns the monoisotopic mass of the element.
     */
    public double getMass() {
        return mass;
    }

    /**
     * returns the maximal valence of this element. This is the number of bounds this element can
     * connect with other atoms.
     */
    public int getValence() {
        return valence;
    }

    /**
     * returns the rounded monoisotopic mass of this element.
     * @return
     */
    public int getIntegerMass() {
        return nominalMass;
    }

    /**
     * returns the full name of this element
     */
    public String getName() {
    	return name;
    }
    
    /**
     * returns the symbol of this element which is usually a capital letter and one or two
     * lower-case letters.
     */
    public String getSymbol() {
        return symbol;
    }

    public String toString() {
        return getSymbol();
    }

    /**
     * returns the id of this element. Usually, this is also the position of the element in the
     * periodic table. But the periodic table may contains also atoms like deuterium which has no
     * position. Nevertheless, also this elements have an id.
     * @return
     */
    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * compare two elements by their mass
     */
    @Override
    public int compareTo(Element element) {
        return getIntegerMass() - element.getIntegerMass();
    }
}
