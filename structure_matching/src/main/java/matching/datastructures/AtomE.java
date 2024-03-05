package matching.datastructures;

import lombok.Getter;
import org.openscience.cdk.Atom;
import org.openscience.cdk.interfaces.*;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import java.util.Map;

/**
 * An AtomE object is an object of {@link Atom} which has three additional attributes.<br>
 * It can be marked in two several ways and a non-negative integer value can be assigned to this atom.
 */
public class AtomE implements IAtom {

    private IAtom atom;

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
    @Getter
    private int depth;

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
     * @param atom IAtomType to copy information from
     */
    public AtomE(IAtom atom){
        this.atom = atom;
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
        this.depth = depth;
    }

    public IAtom getOrgAtom(){
        return this.atom;
    }

    @Override
    public void setCharge(Double charge) {
        this.atom.setCharge(charge);
    }

    @Override
    public Double getCharge() {
        return this.atom.getCharge();
    }

    @Override
    public void setImplicitHydrogenCount(Integer hydrogenCount) {
        this.atom.setImplicitHydrogenCount(hydrogenCount);
    }

    @Override
    public Integer getImplicitHydrogenCount() {
        return this.atom.getImplicitHydrogenCount();
    }

    @Override
    public void setPoint2d(Point2d point2d) {
        this.atom.setPoint2d(point2d);
    }

    @Override
    public void setPoint3d(Point3d point3d) {
        this.atom.setPoint3d(point3d);
    }

    @Override
    public void setFractionalPoint3d(Point3d point3d) {
        this.atom.setFractionalPoint3d(point3d);
    }

    @Override
    public void setStereoParity(Integer stereoParity) {
        this.atom.setStereoParity(stereoParity);
    }

    @Override
    public Point2d getPoint2d() {
        return this.atom.getPoint2d();
    }

    @Override
    public Point3d getPoint3d() {
        return this.atom.getPoint3d();
    }

    @Override
    public Point3d getFractionalPoint3d() {
        return this.atom.getFractionalPoint3d();
    }

    @Override
    public Integer getStereoParity() {
        return this.atom.getStereoParity();
    }

    @Override
    public IAtomContainer getContainer() {
        return this.atom.getContainer();
    }

    @Override
    public int getIndex() {
        return this.atom.getIndex();
    }

    @Override
    public Iterable<IBond> bonds() {
        return this.atom.bonds();
    }

    @Override
    public int getBondCount() {
        return this.atom.getBondCount();
    }

    @Override
    public IBond getBond(IAtom atom) {
        return this.atom.getBond(atom);
    }

    @Override
    public boolean isAromatic() {
        return this.atom.isAromatic();
    }

    @Override
    public void setIsAromatic(boolean arom) {
        this.atom.setIsAromatic(arom);
    }

    @Override
    public boolean isInRing() {
        return this.atom.isInRing();
    }

    @Override
    public void setIsInRing(boolean ring) {
        this.atom.setIsInRing(ring);
    }

    @Override
    public int getMapIdx() {
        return this.atom.getMapIdx();
    }

    @Override
    public void setMapIdx(int mapidx) {
        this.atom.setMapIdx(mapidx);
    }

    @Override
    public void addListener(IChemObjectListener col) {
        this.atom.addListener(col);
    }

    @Override
    public int getListenerCount() {
        return this.atom.getListenerCount();
    }

    @Override
    public void removeListener(IChemObjectListener col) {
        this.atom.removeListener(col);
    }

    @Override
    public void setNotification(boolean bool) {
        this.atom.setNotification(bool);
    }

    @Override
    public boolean getNotification() {
        return this.atom.getNotification();
    }

    @Override
    public void notifyChanged() {
        this.atom.notifyChanged();
    }

    @Override
    public void notifyChanged(IChemObjectChangeEvent evt) {
        this.atom.notifyChanged(evt);
    }

    @Override
    public void setProperty(Object description, Object property) {
        this.atom.setProperty(description, property);
    }

    @Override
    public void removeProperty(Object description) {
        this.atom.removeProperty(description);
    }

    @Override
    public <T> T getProperty(Object description) {
        return this.atom.getProperty(description);
    }

    @Override
    public <T> T getProperty(Object description, Class<T> c) {
        return this.atom.getProperty(description, c);
    }

    @Override
    public Map<Object, Object> getProperties() {
        return this.atom.getProperties();
    }

    @Override
    public String getID() {
        return this.atom.getID();
    }

    @Override
    public void setID(String identifier) {
        this.atom.setID(identifier);
    }

    @Override
    public void setFlag(int mask, boolean value) {
        this.atom.setFlag(mask, value);
    }

    @Override
    public boolean getFlag(int mask) {
        return this.atom.getFlag(mask);
    }

    @Override
    public void setProperties(Map<Object, Object> properties) {
        this.atom.setProperties(properties);
    }

    @Override
    public void addProperties(Map<Object, Object> properties) {
        this.atom.addProperties(properties);
    }

    @Override
    public void setFlags(boolean[] newFlags) {
        this.atom.setFlags(newFlags);
    }

    @Override
    public boolean[] getFlags() {
        return this.atom.getFlags();
    }

    @Override
    public Number getFlagValue() {
        return this.atom.getFlagValue();
    }

    @Override
    public IAtom clone() throws CloneNotSupportedException{
        AtomE atomCopy = new AtomE(this.atom.clone());
        atomCopy.setColor(this.color);
        atomCopy.setMarked(this.marked);
        atomCopy.setDepth(this.depth);
        return atomCopy;
    }

    @Override
    public void setAtomTypeName(String identifier) {
        this.atom.setAtomTypeName(identifier);
    }

    @Override
    public void setMaxBondOrder(IBond.Order maxBondOrder) {
        this.atom.setMaxBondOrder(maxBondOrder);
    }

    @Override
    public void setBondOrderSum(Double bondOrderSum) {
        this.atom.setBondOrderSum(bondOrderSum);
    }

    @Override
    public String getAtomTypeName() {
        return this.atom.getAtomTypeName();
    }

    @Override
    public IBond.Order getMaxBondOrder() {
        return this.atom.getMaxBondOrder();
    }

    @Override
    public Double getBondOrderSum() {
        return this.atom.getBondOrderSum();
    }

    @Override
    public void setFormalCharge(Integer charge) {
        this.atom.setFormalCharge(charge);
    }

    @Override
    public Integer getFormalCharge() {
        return this.atom.getFormalCharge();
    }

    @Override
    public void setFormalNeighbourCount(Integer count) {
        this.atom.setFormalNeighbourCount(count);
    }

    @Override
    public Integer getFormalNeighbourCount() {
        return this.atom.getFormalNeighbourCount();
    }

    @Override
    public void setHybridization(Hybridization hybridization) {
        this.atom.setHybridization(hybridization);
    }

    @Override
    public Hybridization getHybridization() {
        return this.atom.getHybridization();
    }

    @Override
    public void setCovalentRadius(Double radius) {
        this.atom.setCovalentRadius(radius);
    }

    @Override
    public Double getCovalentRadius() {
        return this.atom.getCovalentRadius();
    }

    @Override
    public void setValency(Integer valency) {
        this.atom.setValency(valency);
    }

    @Override
    public Integer getValency() {
        return this.atom.getValency();
    }

    @Override
    public void setNaturalAbundance(Double naturalAbundance) {
        this.atom.setNaturalAbundance(naturalAbundance);
    }

    @Override
    public void setExactMass(Double exactMass) {
        this.atom.setExactMass(exactMass);
    }

    @Override
    public Double getNaturalAbundance() {
        return this.atom.getNaturalAbundance();
    }

    @Override
    public Double getExactMass() {
        return this.atom.getExactMass();
    }

    @Override
    public Integer getMassNumber() {
        return this.atom.getMassNumber();
    }

    @Override
    public void setMassNumber(Integer massNumber) {
        this.atom.setMassNumber(massNumber);
    }

    @Override
    public Integer getAtomicNumber() {
        return this.atom.getAtomicNumber();
    }

    @Override
    public void setAtomicNumber(Integer atomicNumber) {
        this.atom.setMassNumber(atomicNumber);
    }

    @Override
    public String getSymbol() {
        return this.atom.getSymbol();
    }

    @Override
    public void setSymbol(String symbol) {
        this.atom.setSymbol(symbol);
    }

    @Override
    public IChemObjectBuilder getBuilder() {
        return this.atom.getBuilder();
    }
}
