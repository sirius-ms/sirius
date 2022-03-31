package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.JacksonDocument;
import org.openscience.cdk.exception.InvalidSmilesException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class CombinatorialSubtreeCalculatorJsonReader {

    public static CombinatorialSubtree readTreeFromJson(Reader reader) throws IOException, UnknownElementException, InvalidSmilesException {
        JacksonDocument json = new JacksonDocument();
        JsonNode docRoot = json.fromReader(reader);
        JsonNode treeNode = json.getFromDictionary(docRoot, "tree");
        return CombinatorialSubtreeJsonReader.treeFromJson(treeNode, json);
    }

    public static ArrayList<String> getCuttedBondsFromJson(Reader reader) throws IOException {
        JacksonDocument json = new JacksonDocument();
        JsonNode docRoot = json.fromReader(reader);
        JsonNode treeNode = json.getFromDictionary(docRoot, "tree");
        JsonNode edgesNode = json.getFromDictionary(treeNode, "edges");
        ArrayList<String> cuttedBondsBySpecificName = new ArrayList<>();

        for(JsonNode edgeNode : edgesNode){
            addBondToList(cuttedBondsBySpecificName, edgeNode.get("cut1"), json);
            addBondToList(cuttedBondsBySpecificName, edgeNode.get("cut2"), json);
        }

        return cuttedBondsBySpecificName;
    }

    private static void addBondToList(ArrayList<String> cuttedBonds, JsonNode cutNode, JacksonDocument json){
        if(!cutNode.isNull()){
            String bondName = json.getString(cutNode.get("bondNameSpecific"));
            cuttedBonds.add(bondName);
        }
    }
}
