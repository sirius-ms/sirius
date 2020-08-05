
package fragtreealigner.domainobjects.chem.structure;

import fragtreealigner.domainobjects.chem.basics.Element;
import fragtreealigner.domainobjects.graphs.Graph;
import fragtreealigner.domainobjects.util.Coordinate;
import fragtreealigner.util.Session;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.element.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class MolecularStructure extends Graph<MolecularStructureAtom, MolecularStructureBond> {
	public MolecularStructure(Session session) {
		super(session);
		this.isDirected = false;
	}

	public MolecularStructureAtom addAtom(String label) {
		MolecularStructureAtom atom = new MolecularStructureAtom(label);
		super.addNode(atom, label);
		return atom;
	}
	
	public MolecularStructureAtom addAtom(String label, Element elementType, int hydrogenCount, Coordinate coordinate) {
		MolecularStructureAtom atom = new MolecularStructureAtom(label, elementType, hydrogenCount, coordinate);
		super.addNode(atom, label);
		return atom;	
	}
	
	@Override
	public MolecularStructureBond connect(MolecularStructureAtom atom1, MolecularStructureAtom atom2) {
		MolecularStructureBond bond = new MolecularStructureBond(atom1, atom2);
		super.connect(bond);
		return bond;
	}
	
	public MolecularStructureBond connect(MolecularStructureAtom atom1, MolecularStructureAtom atom2, String label, BondType bondType) {
		MolecularStructureBond bond = new MolecularStructureBond(atom1, atom2, label, bondType);
		super.connect(bond);
		return bond;
	}
	
	public static MolecularStructure convertFromCml(CMLMolecule cmlMolecule, Session session) {
		Map<String, Integer> mapBondTypes = new HashMap<String, Integer>();
	    mapBondTypes.put("1", 0);
	    mapBondTypes.put("s", 0);
	    mapBondTypes.put("S", 0);
	    mapBondTypes.put("2", 1);
	    mapBondTypes.put("d", 1);
	    mapBondTypes.put("D", 1);
	    mapBondTypes.put("3", 2);
	    mapBondTypes.put("t", 2);
	    mapBondTypes.put("T", 2);
	    mapBondTypes.put("a", 4);
	    mapBondTypes.put("A", 4);

		MolecularStructure molStructure = new MolecularStructure(session);
		CMLAtomArray cmlAtomArray = cmlMolecule.getAtomArray();
		CMLBondArray cmlBondArray = cmlMolecule.getBondArray();
		
		if (cmlAtomArray != null) {
			for (CMLAtom cmlAtom : cmlAtomArray.getAtomElements()) {
				Element elementType = session.getParameters().elementTable.getElement(cmlAtom.getElementType());
				molStructure.addAtom(cmlAtom.getId(), elementType, cmlAtom.getHydrogenCount(), new Coordinate(cmlAtom.getX2(), cmlAtom.getY2()));
			}
			for (CMLBond cmlBond : cmlBondArray.getBondElements()) {
				molStructure.connect(molStructure.getNodeByName(cmlBond.getAtomRefs2()[0]), molStructure.getNodeByName(cmlBond.getAtomRefs2()[1]), cmlBond.getId(), BondType.values()[mapBondTypes.get(cmlBond.getOrder())]);
			}
		}
		return molStructure;
	}
	
	public static MolecularStructure readFromCml(BufferedReader reader, Session session) {
		Document doc = null;
		try {
			doc = new CMLBuilder().build(reader);
		} catch (ValidityException e) {
			System.out.println("File is not a valid CML file: " + e);
			e.printStackTrace();
		} catch (ParsingException e) {
			System.out.println("File is not a valid CML file: " + e);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("The following IOError occured: " + e);
			e.printStackTrace();
		}
		CMLElement rootElement = (CMLElement)doc.getRootElement();
		
		CMLMolecule cmlMolecule = null;
		if (rootElement.getLocalName().equals("molecule")) {
			cmlMolecule = (CMLMolecule)rootElement;
		} else {
			System.err.println("No molecule found in file.");
			//TODO Search through whole CML file
		}
		return convertFromCml(cmlMolecule, session);
	}
}
