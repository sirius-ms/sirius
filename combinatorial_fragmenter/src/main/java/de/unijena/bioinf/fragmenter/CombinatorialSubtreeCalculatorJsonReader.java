package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.JacksonDocument;
import org.openscience.cdk.exception.InvalidSmilesException;

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
        return CombinatorialSubtreeCalculatorJsonReader.readTreeFromJson(docRoot, json);
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
        return CombinatorialSubtreeCalculatorJsonReader.getHydrogenRearrangements(docRoot, json);
    }


}
