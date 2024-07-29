package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.JacksonDocument;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class CombinatorialSubtreeCalculatorJsonReader {

    public static CombinatorialSubtree readTreeFromJson(JsonNode docRoot, JacksonDocument json) throws UnknownElementException, IOException, InvalidSmilesException {
        JsonNode treeNode = json.getFromDictionary(docRoot, "tree");
        return CombinatorialSubtreeJsonReader.treeFromJson(treeNode, json);
    }

    public static CombinatorialSubtree readTreeFromJson(Reader reader) throws IOException, UnknownElementException, InvalidSmilesException {
        JacksonDocument json = new JacksonDocument();
        JsonNode docRoot = json.fromReader(reader);
        return readTreeFromJson(docRoot, json);
    }

    public static ArrayList<Integer> getHydrogenRearrangements(JsonNode docRoot, JacksonDocument json) throws IOException {
        JsonNode assignments = json.getFromDictionary(docRoot, "assignments");
        ArrayList<Integer> listOfHydrogenRearrangements = new ArrayList<>();

        for(JsonNode assignment : assignments){
            JsonNode assignedFragments = json.getFromDictionary(assignment, "assignedFragments");
            for(JsonNode assignedFragment : assignedFragments){
                int hydrogenRearrangements = (int) json.getIntFromDictionary(assignedFragment, "hydrogenRearrangements");
                listOfHydrogenRearrangements.add(hydrogenRearrangements);
            }
        }

        return listOfHydrogenRearrangements;
    }

    public static ArrayList<Integer> getHydrogenRearrangements(Reader reader) throws IOException {
        JacksonDocument json = new JacksonDocument();
        JsonNode docRoot = json.fromReader(reader);
        return getHydrogenRearrangements(docRoot, json);
    }

    public static IAtomContainer getMolecule(Reader reader) throws CDKException, IOException {
        JacksonDocument json = new JacksonDocument();
        JsonNode docRoot = json.fromReader(reader);
        return getMolecule(docRoot, json);
    }

    public static IAtomContainer getMolecule(JsonNode docRoot, JacksonDocument json) throws CDKException {
        SmilesParser smilesParser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        String smiles = json.getStringFromDictionary(docRoot, "smiles");

        IAtomContainer molecule = smilesParser.parseSmiles(smiles);
        AtomContainerManipulator.percieveAtomTypesAndConfigureUnsetProperties(molecule);
        Aromaticity.cdkLegacy().apply(molecule);

        return molecule;
    }

    public static MolecularGraph getMolecularGraph(Reader reader) throws IOException, CDKException, UnknownElementException {
        JacksonDocument json = new JacksonDocument();
        JsonNode docRoot = json.fromReader(reader);
        return getMolecularGraph(docRoot, json);
    }

    public static MolecularGraph getMolecularGraph(JsonNode docRoot, JacksonDocument json) throws UnknownElementException, CDKException {
        IAtomContainer molecule = getMolecule(docRoot, json);
        String molecularFormulaStr = json.getStringFromDictionary(docRoot, "molecularFormula");
        MolecularFormula molecularFormula = MolecularFormula.parse(molecularFormulaStr);
        return new MolecularGraph(molecularFormula, molecule);
    }



}
