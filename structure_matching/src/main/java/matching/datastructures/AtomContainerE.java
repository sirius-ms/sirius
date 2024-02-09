package matching.datastructures;

import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.silent.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;


/**
 * <p>
 * An AtomContainerE object is an object of class {@link AtomContainer} and represents an molecule
 * with explicit hydrogen atoms. In addition, each atom contained in this AtomContainerE is an object
 * of type {@link AtomE}. That means that every atom can be marked in two several ways
 * ({@link AtomE#color} and {@link AtomE#marked}) and that a non-negative integer value can be assigned to every atom.<br>
 * An object of this class is convenient for a search algorithm, like Breadth-First Search (BFS).
 * </p>
 */
public class AtomContainerE extends AtomContainer {

    /**
     * Constructs an empty AtomContainerE.
     */
    public AtomContainerE(){
        super();
    }

    /**
     * <p>
     * Constructs an AtomContainerE with a copy of the atoms and ElectronContainers of another AtomContainer.<br>
     * Note that this is a shallow copy, i.e. with the same ElectronContainer objects contained in the original
     * AtomContainer.
     * </p>
     * <p>
     * Every implicit hydrogen atom is added to this AtomContainer and each atom is an object of class {@link AtomE}.
     * </p>
     *
     * @param container the container which is transformed and copied into an AtomContainerE object
     */
    public AtomContainerE(IAtomContainer container){
        super(container);
        this.convertImplicitToExplicitHydrogens();

        for(IAtom atom : this.atoms()){
            AtomE atomE = new AtomE(atom);
            atomE.setImplicitHydrogenCount(0);
            AtomContainerManipulator.replaceAtomByAtom(this, atom, atomE);
        }
    }

    /**
     * Constructs an AtomContainerE with the copy of the atoms and ElectronContainer objects
     * of another AtomContainerE object.<br>
     * Note that the constructed object contains the same objects as in the given AtomContainerE.
     *
     * @param container the AtomContainerE to copy the atoms and ElectronContainers from
     */
    public AtomContainerE(AtomContainerE container){
        super(container);
    }

    private void convertImplicitToExplicitHydrogens(){
        int size = this.getAtomCount();

        for(int i = 0; i < size; i++){
            int hCount = this.atoms[i].getImplicitHydrogenCount();
            this.atoms[i].setImplicitHydrogenCount(0);

            for(int j = 0; j < hCount; j++){
                Atom hydrogen = new Atom("H");
                super.addAtom(hydrogen);
                this.addBond(new Bond(this.atoms[i], hydrogen));
            }
        }
    }

    /**
     * Adds a new atom to this container.<br>
     * The given {@link IAtom} object will not be contained in this container after calling this procedure
     * because this atom has to be transformed into an {@link AtomE} object. Therefore, {@link AtomE#AtomE(IElement)}
     * has to be called and a new {@link AtomE} object will be added.
     *
     * @param atom the {@link IAtom} which will be added to this container
     */
    @Override
    public void addAtom(IAtom atom){
        super.addAtom(new AtomE(atom));
    }

    /**
     * Adds a new atom to this container.<br>
     * This method differs to {@link #addAtom(IAtom)} because the given {@code atom} will be contained in
     * this AtomContainerE object.
     *
     * @param atom the {@link AtomE} which will be added to this container
     */
    public void addAtom(AtomE atom){
        super.addAtom(atom);
    }
}
