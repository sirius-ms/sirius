package matching.datastructures;

import org.openscience.cdk.exception.NoSuchAtomException;

/**
 * <p>
 * An object of this class SideChain represents a side chain or more correctly a substituent.
 * </p>
 * <p>
 * This means that a SideChain object is a substructure of a molecule which is connected to the rest of this
 * molecule by only one single bond. Thus, there is only one atom in this substructure that links
 * this substituent to the rest of the molecule by this "bridge". We call this atom "bridge atom" or "bridge node" of
 * the considered substituent.
 * </p>
 * <p>
 * An object of this class consists of an {@link AtomContainerE} which represents this molecular structure and
 * a pointer to the bridge node contained in this container.
 * </p>
 */
public class SideChain {

    /**
     * An {@link AtomContainerE} which represents this side chain (more correctly, substituent).
     */
    private AtomContainerE sideChain;

    /**
     * An {@link AtomE} which is contained in this {@link #sideChain}.<br>
     * This is the only atom which connects this molecular structure with the rest of a molecule that contains
     * this side chain.
     */
    private AtomE bridgeNode;

    /**
     * Constructs a new SideChain object with a given {@link AtomContainerE}
     * and a given bridge node of class {@link AtomE}.<br>
     * Note that {@code bridgeNode} has to be contained in {@code sideChain}.
     *
     * @param sideChain the given side chain
     * @param bridgeNode the associated bridge node
     */
    public SideChain(AtomContainerE sideChain, AtomE bridgeNode){
        if(sideChain.contains(bridgeNode)){
            this.sideChain = sideChain;
            this.bridgeNode = bridgeNode;
        }else{
            throw new NoSuchAtomException("The given atom is not contained in the given AtomContainer");
        }
    }

    /**
     * Returns the {@link AtomContainerE} of this object which represents the molecular structure.
     *
     * @return the molecular structure {@link #sideChain} of this SideChain object
     */
    public AtomContainerE getSideChain(){
        return this.sideChain;
    }

    /**
     * Returns the {@link AtomE} of this object which represents the bridge node of this considered side chain.
     *
     * @return the bridge node {@link #bridgeNode} of this SideChain object
     */
    public AtomE getBridgeNode(){
        return this.bridgeNode;
    }
}
