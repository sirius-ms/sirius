package matching.datastructures;

import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.AtomRef;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.silent.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.Bond;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * An AtomContainerE object is an object of class {@link AtomContainer} and represents an molecule
 * with explicit hydrogen atoms. In addition, each atom contained in this AtomContainerE is an object
 * of type {@link AtomE}. That means that every atom can be marked in two several ways
 * ({@link AtomE#color} and {@link AtomE#marked}) and that a non-negative integer value can be assigned to every atom.<br>
 * An object of this class is convenient for a search algorithm, like Breadth-First Search (BFS).
 * </p>
 */
public class AtomContainerE implements IAtomContainer {

    private IAtomContainer container;

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
        this.container = container;
        this.convertImplicitToExplicitHydrogens();
        Iterable<IAtom> atoms = this.atoms();
        for(IAtom atom : atoms){
            AtomE atomE = new AtomE(atom);
            AtomContainerManipulator.replaceAtomByAtom(this.container, atom, atomE);
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
        this.container = new AtomContainer(container.getContainer());
    }

    private void convertImplicitToExplicitHydrogens(){
        int size = this.getAtomCount();

        for(int i = 0; i < size; i++){
            int hCount = this.getAtom(i).getImplicitHydrogenCount();
            this.getAtom(i).setImplicitHydrogenCount(0);

            for(int j = 0; j < hCount; j++){
                Atom hydrogen = new Atom("H");
                hydrogen.setImplicitHydrogenCount(0);
                this.addAtom(hydrogen);
                this.addBond(new Bond(this.getAtom(i), hydrogen));
            }
        }
    }

    public IAtomContainer getContainer(){
        return this.container;
    }


    @Override
    public void addStereoElement(IStereoElement element) {
        this.container.addStereoElement(element);
    }

    @Override
    public void setStereoElements(List<IStereoElement> elements) {
        this.container.setStereoElements(elements);
    }

    @Override
    public Iterable<IStereoElement> stereoElements() {
        return this.container.stereoElements();
    }

    @Override
    public void setAtoms(IAtom[] atoms) {
        this.container.setAtoms(atoms);
    }

    @Override
    public void setBonds(IBond[] bonds) {
        this.container.setBonds(bonds);
    }

    @Override
    public void setAtom(int idx, IAtom atom) {
        this.container.setAtom(idx, atom);
    }

    @Override
    public IAtom getAtom(int idx) {
        return this.unbox(this.container.getAtom(idx));
    }

    @Override
    public IBond getBond(int idx) {
        return this.container.getBond(idx);
    }

    @Override
    public ILonePair getLonePair(int idx) {
        return this.container.getLonePair(idx);
    }

    @Override
    public ISingleElectron getSingleElectron(int idx) {
        return this.container.getSingleElectron(idx);
    }

    @Override
    public Iterable<IAtom> atoms() {
       return new Iterable<>() {
           @NotNull
           @Override
           public Iterator<IAtom> iterator() {
               return new AtomIterator(container.atoms().iterator());
           }
       };
    }

    private class AtomIterator implements Iterator<IAtom>{

        private Iterator<IAtom> orgIterator;

        public AtomIterator(Iterator<IAtom> iterator){
            this.orgIterator = iterator;
        }
        @Override
        public boolean hasNext() {
            return this.orgIterator.hasNext();
        }

        @Override
        public IAtom next() {
            return unbox(this.orgIterator.next());
        }
    }

    @Override
    public Iterable<IBond> bonds() {
        return this.container.bonds();
    }

    @Override
    public Iterable<ILonePair> lonePairs() {
        return this.container.lonePairs();
    }

    @Override
    public Iterable<ISingleElectron> singleElectrons() {
        return this.container.singleElectrons();
    }

    @Override
    public Iterable<IElectronContainer> electronContainers() {
        return this.container.electronContainers();
    }

    @Override
    public IAtom getFirstAtom() {
        return this.unbox(this.container.getFirstAtom());
    }

    @Override
    public IAtom getLastAtom() {
        return this.unbox(this.container.getLastAtom());
    }

    @Override
    public int getAtomNumber(IAtom atom) {
        return this.container.getAtomNumber(atom);
    }

    @Override
    public int getBondNumber(IAtom atom1, IAtom atom2) {
        return this.container.getBondNumber(atom1, atom2);
    }

    @Override
    public int getBondNumber(IBond bond) {
        return this.container.getBondNumber(bond);
    }

    @Override
    public int getLonePairNumber(ILonePair lonePair) {
        return this.container.getLonePairNumber(lonePair);
    }

    @Override
    public int getSingleElectronNumber(ISingleElectron singleElectron) {
        return this.container.getSingleElectronNumber(singleElectron);
    }

    @Override
    public int indexOf(IAtom atom) {
        return this.container.indexOf(atom);
    }

    @Override
    public int indexOf(IBond bond) {
        return this.container.indexOf(bond);
    }

    @Override
    public int indexOf(ISingleElectron electron) {
        return this.container.indexOf(electron);
    }

    @Override
    public int indexOf(ILonePair pair) {
        return this.container.indexOf(pair);
    }

    @Override
    public IElectronContainer getElectronContainer(int number) {
        return this.container.getElectronContainer(number);
    }

    @Override
    public IBond getBond(IAtom atom1, IAtom atom2) {
        return this.container.getBond(atom1, atom2);
    }

    @Override
    public int getAtomCount() {
        return this.container.getAtomCount();
    }

    @Override
    public int getBondCount() {
        return this.container.getBondCount();
    }

    @Override
    public int getLonePairCount() {
        return this.container.getLonePairCount();
    }

    @Override
    public int getSingleElectronCount() {
        return this.container.getSingleElectronCount();
    }

    @Override
    public int getElectronContainerCount() {
        return this.container.getElectronContainerCount();
    }

    @Override
    public List<IAtom> getConnectedAtomsList(IAtom atom) {
        return new ArrayList(this.container.getConnectedAtomsList(atom).stream().map(this::unbox).toList());
    }

    @Override
    public List<IBond> getConnectedBondsList(IAtom atom) {
        return this.container.getConnectedBondsList(atom);
    }

    @Override
    public List<ILonePair> getConnectedLonePairsList(IAtom atom) {
        return this.container.getConnectedLonePairsList(atom);
    }

    @Override
    public List<ISingleElectron> getConnectedSingleElectronsList(IAtom atom) {
        return this.container.getConnectedSingleElectronsList(atom);
    }

    @Override
    public List<IElectronContainer> getConnectedElectronContainersList(IAtom atom) {
        return this.container.getConnectedElectronContainersList(atom);
    }

    @Override
    public int getConnectedAtomsCount(IAtom atom) {
        return this.container.getConnectedAtomsCount(atom);
    }

    @Override
    public int getConnectedBondsCount(IAtom atom) {
        return this.container.getConnectedBondsCount(atom);
    }

    @Override
    public int getConnectedBondsCount(int idx) {
        return this.container.getConnectedBondsCount(idx);
    }

    @Override
    public int getConnectedLonePairsCount(IAtom atom) {
        return this.container.getConnectedLonePairsCount(atom);
    }

    @Override
    public int getConnectedSingleElectronsCount(IAtom atom) {
        return this.container.getConnectedSingleElectronsCount(atom);
    }

    @Override
    public double getBondOrderSum(IAtom atom) {
        return this.container.getBondOrderSum(atom);
    }

    @Override
    public IBond.Order getMaximumBondOrder(IAtom atom) {
        return this.container.getMaximumBondOrder(atom);
    }

    @Override
    public IBond.Order getMinimumBondOrder(IAtom atom) {
        return this.container.getMinimumBondOrder(atom);
    }

    @Override
    public void add(IAtomContainer atomContainer) {
        this.container.add(atomContainer);
    }

    @Override
    public void addAtom(IAtom atom) {
        this.container.addAtom(atom);
    }

    @Override
    public void addBond(IBond bond) {
        this.container.addBond(bond);
    }

    @Override
    public void addLonePair(ILonePair lonePair) {
        this.container.addLonePair(lonePair);
    }

    @Override
    public void addSingleElectron(ISingleElectron singleElectron) {
        this.container.addSingleElectron(singleElectron);
    }

    @Override
    public void addElectronContainer(IElectronContainer electronContainer) {
        this.container.addElectronContainer(electronContainer);
    }

    @Override
    public void remove(IAtomContainer atomContainer) {
        this.container.remove(atomContainer);
    }

    @Override
    public void removeAtomOnly(int position) {
        this.container.removeAtomOnly(position);
    }

    @Override
    public void removeAtomOnly(IAtom atom) {
        this.container.removeAtomOnly(atom);
    }

    @Override
    public IBond removeBond(int position) {
        return this.container.removeBond(position);
    }

    @Override
    public IBond removeBond(IAtom atom1, IAtom atom2) {
        return this.container.removeBond(atom1, atom2);
    }

    @Override
    public void removeBond(IBond bond) {
        this.container.removeBond(bond);
    }

    @Override
    public ILonePair removeLonePair(int position) {
        return this.container.removeLonePair(position);
    }

    @Override
    public void removeLonePair(ILonePair lonePair) {
        this.container.removeLonePair(lonePair);
    }

    @Override
    public ISingleElectron removeSingleElectron(int position) {
        return this.container.getSingleElectron(position);
    }

    @Override
    public void removeSingleElectron(ISingleElectron singleElectron) {
        this.container.removeSingleElectron(singleElectron);
    }

    @Override
    public IElectronContainer removeElectronContainer(int position) {
        return this.container.removeElectronContainer(position);
    }

    @Override
    public void removeElectronContainer(IElectronContainer electronContainer) {
        this.container.removeElectronContainer(electronContainer);
    }

    @Override
    public void removeAtom(IAtom atom) {
        this.container.removeAtom(atom);
    }

    @Override
    public void removeAtom(int pos) {
        this.container.removeAtom(pos);
    }

    @Override
    public void removeAtomAndConnectedElectronContainers(IAtom atom) {
        this.container.removeAtomAndConnectedElectronContainers(atom);
    }

    @Override
    public void removeAllElements() {
        this.container.removeAllElements();
    }

    @Override
    public void removeAllElectronContainers() {
        this.container.removeAllElectronContainers();
    }

    @Override
    public void removeAllBonds() {
        this.container.removeAllBonds();
    }

    @Override
    public void addBond(int atom1, int atom2, IBond.Order order, IBond.Stereo stereo) {
        this.container.addBond(atom1, atom2, order, stereo);
    }

    @Override
    public void addBond(int atom1, int atom2, IBond.Order order) {
        this.container.addBond(atom1, atom2, order);
    }

    @Override
    public void addLonePair(int atomID) {
        this.container.addLonePair(atomID);
    }

    @Override
    public void addSingleElectron(int atomID) {
        this.container.addSingleElectron(atomID);
    }

    @Override
    public boolean contains(IAtom atom) {
        return this.container.contains(atom);
    }

    @Override
    public boolean contains(IBond bond) {
        return this.container.contains(bond);
    }

    @Override
    public boolean contains(ILonePair lonePair) {
        return this.container.contains(lonePair);
    }

    @Override
    public boolean contains(ISingleElectron singleElectron) {
        return this.container.contains(singleElectron);
    }

    @Override
    public boolean contains(IElectronContainer electronContainer) {
        return this.container.contains(electronContainer);
    }

    @Override
    public boolean isEmpty() {
        return this.container.isEmpty();
    }

    @Override
    public String getTitle() {
        return this.container.getTitle();
    }

    @Override
    public void setTitle(String title) {
        this.container.setTitle(title);
    }

    @Override
    public void addListener(IChemObjectListener col) {
        this.container.addListener(col);
    }

    @Override
    public int getListenerCount() {
        return this.container.getListenerCount();
    }

    @Override
    public void removeListener(IChemObjectListener col) {
        this.container.removeListener(col);
    }

    @Override
    public void setNotification(boolean bool) {
        this.container.setNotification(bool);
    }

    @Override
    public boolean getNotification() {
        return this.container.getNotification();
    }

    @Override
    public void notifyChanged() {
        this.container.notifyChanged();
    }

    @Override
    public void notifyChanged(IChemObjectChangeEvent evt) {
        this.container.notifyChanged(evt);
    }

    @Override
    public void setProperty(Object description, Object property) {
        this.container.setProperty(description, property);
    }

    @Override
    public void removeProperty(Object description) {
        this.container.removeProperty(description);
    }

    @Override
    public <T> T getProperty(Object description) {
        return this.container.getProperty(description);
    }

    @Override
    public <T> T getProperty(Object description, Class<T> c) {
        return this.container.getProperty(description, c);
    }

    @Override
    public Map<Object, Object> getProperties() {
        return this.container.getProperties();
    }

    @Override
    public String getID() {
        return this.container.getID();
    }

    @Override
    public void setID(String identifier) {
        this.container.setID(identifier);
    }

    @Override
    public void setFlag(int mask, boolean value) {
        this.container.setFlag(mask, value);
    }

    @Override
    public boolean getFlag(int mask) {
        return this.container.getFlag(mask);
    }

    @Override
    public void setProperties(Map<Object, Object> properties) {
        this.container.setProperties(properties);
    }

    @Override
    public void addProperties(Map<Object, Object> properties) {
        this.container.addProperties(properties);
    }

    @Override
    public void setFlags(boolean[] newFlags) {
        this.container.setFlags(newFlags);
    }

    @Override
    public boolean[] getFlags() {
        return this.container.getFlags();
    }

    @Override
    public Number getFlagValue() {
        return this.container.getFlagValue();
    }

    @Override
    public IChemObjectBuilder getBuilder() {
        return this.container.getBuilder();
    }

    @Override
    public void stateChanged(IChemObjectChangeEvent event) {
        this.container.stateChanged(event);
    }

    @Override
    public AtomContainerE clone() throws CloneNotSupportedException{
        IAtomContainer clonedContainer = this.container.clone();
        return new AtomContainerE(clonedContainer);
    }

    private IAtom unbox(IAtom atom){
        IAtom unpackedAtom = atom;
        while(unpackedAtom instanceof AtomRef){
            unpackedAtom = ((AtomRef) unpackedAtom).deref();
        }
        return unpackedAtom;
    }
}
