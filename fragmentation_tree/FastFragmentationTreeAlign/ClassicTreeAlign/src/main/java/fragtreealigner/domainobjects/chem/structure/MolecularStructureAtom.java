
package fragtreealigner.domainobjects.chem.structure;

import fragtreealigner.domainobjects.chem.basics.Element;
import fragtreealigner.domainobjects.graphs.Node;
import fragtreealigner.domainobjects.util.Coordinate;

@SuppressWarnings("serial")
public class MolecularStructureAtom extends Node<MolecularStructureAtom, MolecularStructureBond> {
	private Element elementType;
	private int hydrogenCount;
	private Coordinate coordinate;

	public MolecularStructureAtom() {
		super();
	}
	
	public MolecularStructureAtom(String label) {
		this();
		this.label = label;
	}
	
	public MolecularStructureAtom(String label, Element elementType, int hydrogenCount, Coordinate coordinate) {
		this(label);
		this.elementType = elementType;
		this.hydrogenCount = hydrogenCount;
		this.coordinate = coordinate;
	}
	
	public MolecularStructureAtom(String label, Element elementType, int hydrogenCount, double xCoordinate, double yCoordinate) {
		this(label, elementType, hydrogenCount, new Coordinate(xCoordinate, yCoordinate));
	}
	
	public void setElementType(Element elementType) {
		this.elementType = elementType;
	}

	public Element getElementType() {
		return elementType;
	}

	public void setHydrogenCount(int hydrogenCount) {
		this.hydrogenCount = hydrogenCount;
	}

	public int getHydrogenCount() {
		return hydrogenCount;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}
	
	public void setCoordinate(double x, double y) {
		this.coordinate = new Coordinate(x, y);
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}
	
	public double getXCoordinate() {
		return coordinate.getX();
	}

	public double getYCoordinate() {
		return coordinate.getY();
	}
	
	@Override
	public String dotParams() {
		String dotParams = super.dotParams();
		dotParams += ", pos=\"" + (coordinate.getX() / 2) + "," + (coordinate.getY() / 2) + "\"" + ", shape=\"circle\", width=\"0.5\", height=\"0.5\"";
		return dotParams;		
	}
	
	@Override
	public String toString() {
		String output = "";
		output += elementType.getSymbol();// + "\\n";
//		output += hydrogenCount;
		return output;
	}
}
