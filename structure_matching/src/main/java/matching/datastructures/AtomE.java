package matching.datastructures;

import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.IElement;

/**
 * An AtomE object is an object of {@link Atom} which has three additional attributes.<br>
 * It can be marked in two several ways and a non-negative integer value can be assigned to this atom.
 */
public class AtomE extends Atom {

    /**
     * A boolean value to assign this object a particular state.<br>
     * This value can be used for Breadth-First Search (BFS) where all nodes are either black or white.
     */
    private boolean color;

    /**
     * A boolean value to assign this object a particular state.<br>
     */
    private boolean marked;

    /**
     * A non-negative integer value which can be assigned to this object.<br>
     * This value can be used to determine the distance from the start atom to this atom in BFS.
     * Or it can be used to determine when this atom was considered in a recursive procedure.
     */
    private int depth;

    /**
     * Constructs an completely unset atom.<br>
     * {@link #color} and {@link #marked} are set to {@code false} and {@link #depth} is set to {@code -1}.
     */
    public AtomE(){
        super();
        this.color = false;
        this.marked = false;
        this.depth = -1;
    }

    /**
     * Constructs an isotope by copying the symbol, atomic number, flags, identifier, exact mass, natural abundance,
     * mass number, maximum bond order, bond order sum, van der Waals and covalent radii, formal charge, hybridization,
     * electron valency, formal neighbour count and atom type name from the given IAtomType.<br>
     * It does not copy the listeners and properties. If the element is an instance of IAtom,
     * then the 2D, 3D and fractional coordinates, partial atomic charge,
     * hydrogen count and stereo parity are copied too.<br>
     * The additional attributes {@link #color} and {@link #marked} are set to {@code false} and {@link #depth}
     * is set to {@code -1}.
     *
     * @param element IAtomType to copy information from
     */
    public AtomE(IElement element){
        super(element);
        this.color = false;
        this.marked = false;
        this.depth = -1;
    }

    /**
     * Create a new atom with of the specified element.<br>
     * The additional attributes {@link #color} and {@link #marked} are set to {@code false} and {@link #depth}
     * is set to {@code -1}.
     *
     * @param elem atomic number
     */
    public AtomE(int elem){
        super(elem);
        this.color = false;
        this.marked = false;
        this.depth = -1;
    }

    /**
     * Create a new atom with the specified element and hydrogen count.<br>
     * The additional attributes {@link #color} and {@link #marked} are set to {@code false} and {@link #depth}
     * is set to {@code -1}.
     *
     * @param elem atomic number
     * @param hcnt hydrogen count
     */
    public AtomE(int elem, int hcnt){
        super(elem, hcnt);
        this.color = false;
        this.marked = false;
        this.depth = -1;
    }

    /**
     * Create a new atom with the specified element, hydrogen count, and formal charge.<br>
     * The additional attributes {@link #color} and {@link #marked} are set to {@code false} and {@link #depth}
     * is set to {@code -1}.
     *
     * @param elem atomic number
     * @param hcnt hydrogen count
     * @param fchg formal charge
     */
    public AtomE(int elem, int hcnt, int fchg){
        super(elem, hcnt, fchg);
        this.color = false;
        this.marked = false;
        this.depth = -1;
    }

    /**
     * Constructs an Atom from a string containing an element symbol and optionally the atomic mass,
     * hydrogen count, and formal charge.<br>
     * The symbol grammar allows easy construction from common symbols, for example:
     * <ul>
     *      {@code new Atom("NH+");   // nitrogen cation with one hydrogen}<br>
     *      {@code new Atom("OH");    // hydroxy}<br>
     *      {@code new Atom("O-");    // oxygen anion}<br>
     *      {@code new Atom("13CH3"); // methyl isotope 13}<br>
     *          <br>
     *      {@code atom := {mass}? {symbol} {hcnt}? {fchg}?}<br>
     *      {@code mass := \d+}<br>
     *      {@code hcnt := 'H' \d+}<br>
     *      {@code fchg := '+' \d+? | '-' \d+?}<br>
     * </ul>
     * The additional attributes {@link #color} and {@link #marked} are set to {@code false} and {@link #depth}
     * is set to {@code -1}.
     *
     * @param symbol string with the mandatory element symbol
     */
    public AtomE(String symbol){
        super(symbol);
        this.color = false;
        this.marked = false;
        this.depth = -1;
    }

    /**
     * Sets {@link #color} to the given boolean value.
     *
     * @param color the boolean value that {@link #color} will be set to
     */
    public void setColor(boolean color){
        this.color = color;
    }

    /**
     * Sets {@link #marked} to the given boolean value.
     *
     * @param marked the boolean value that {@link #marked} will be set to
     */
    public void setMarked(boolean marked){
        this.marked = marked;
    }

    /**
     * Returns the value of {@link #color}.
     *
     * @return value of {@link #color}
     */
    public boolean getColor(){
        return this.color;
    }

    /**
     * Returns the value of {@link #marked}.
     *
     * @return value of {@link #marked}
     */
    public boolean getMarked(){
        return this.marked;
    }

    /**
     * Sets {@link #depth} with the given non-negative integer value.
     *
     *
     * @param depth a non-negative integer value that {@link #depth} will be set to
     */
    public void setDepth(int depth){
        if(depth < 0 ){
            throw new RuntimeException("The given parameter is not valid.");
        }else{
            this.depth = depth;
        }
    }

    /**
     * Returns the value of {@link #depth}.
     *
     * @return value of {@link #depth}
     */
    public int getDepth(){
        return this.depth;
    }

}
