package matching.datastructures;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * A SideChainList is a set/list of SideChain objects.<br>
 * </p>
 * <p>
 * If for each SideChain object contained in this list all occurrences are to be removed from a given molecule,
 * then it would be better to rank that list according to the complexity of the contained side chains.<br>
 * That means for two side chains G1 and G2 when it will be iterated through this list starting from index 0:<br>
 *     If G2 is a substructure of G1, then the index of G2 in this list has to be greater than the index of G1.
 * </p>
 */
public class SideChainList {

    /**
     * An {@link ArrayList} that represents this list of {@link SideChain} objects.
     */
    private ArrayList<SideChain> sideChains;

    /**
     * Constructs an empty list of side chains.
     */
    public SideChainList(){
        this.sideChains = new ArrayList<SideChain>();
    }

    /**
     * Constructs a new SideChainList object that contains the elements of the given SideChainList object.
     *
     * @param sideChainList the given SideChainList object
     */
    public SideChainList(SideChainList sideChainList){
        this.sideChains = new ArrayList<SideChain>(sideChainList.sideChains);
    }

    /**
     * Constructs a new SideChainList object that contains the elements of the given collection.
     *
     * @param collection the {@link Collection} that contains {@link SideChain} objects
     */
    public SideChainList(Collection<SideChain> collection){
        this.sideChains = new ArrayList<SideChain>(collection);
    }

    /**
     * Adds the specified {@link SideChain} to the end of this list.
     *
     * @param sc the {@link SideChain} to be appended to this list
     */
    public void add(SideChain sc){
        this.sideChains.add(sc);
    }

    /**
     * Inserts the specified {@link SideChain} at the specified position in this list.
     * Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified {@link SideChain} is to be inserted
     * @param sc {@link SideChain} to be inserted
     */
    public void add(int index, SideChain sc){
        this.sideChains.add(index, sc);
    }

    /**
     * Returns the {@link SideChain} at the specified position {@code index} in this list.
     *
     * @param index index of the {@link SideChain} to return
     * @return the {@link SideChain} at the specified position in this list
     */
    public SideChain get(int index){
        return this.sideChains.get(index);
    }

    /**
     * Returns the number of {@link SideChain} objects contained in this list.
     *
     * @return the number of {@link SideChain} objects in this list
     */
    public int size(){
        return this.sideChains.size();
    }

    /**
     * Removes all of the {@link SideChain} objects from this list.
     * The list will be empty after this call returns.
     */
    public void clear(){
        this.sideChains.clear();
    }
}
