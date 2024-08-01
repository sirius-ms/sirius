package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.databind.JsonNode;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.utils.UnknownElementException;
import de.unijena.bioinf.ChemistryBase.data.JacksonDocument;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

public class CombinatorialSubtreeJsonReader {

    public static CombinatorialSubtree treeFromJson(Reader reader) throws IOException, UnknownElementException, InvalidSmilesException {
        final JacksonDocument json = new JacksonDocument();
        final JsonNode docRoot = json.fromReader(reader);
        return treeFromJson(docRoot, json);
    }

    public static CombinatorialSubtree treeFromJson(@NotNull JsonNode docRoot,@NotNull JacksonDocument json) throws IOException, UnknownElementException, InvalidSmilesException {
        // First: collect all the data from the JSON document
        final SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        final String smiles = json.getStringFromDictionary(docRoot, "molecule");
        final String mf = json.getStringFromDictionary(docRoot, "molecularFormula");
        MolecularGraph molecule = new MolecularGraph(MolecularFormula.parse(mf), parser.parseSmiles(smiles));

        // Create a hashmap which maps a Bitset to the corresponding CombinatorialNode (as JsonNode),
        // and which maps a Bitset to a set of JsonNode-Edges whose source fragment correspond to the given BitSet:
        final JsonNode nodes = json.getFromDictionary(docRoot, "nodes");
        final JsonNode edges = json.getFromDictionary(docRoot, "edges");
        HashMap<BitSet, JsonNode> bitset2Fragment = new HashMap<>(nodes.size());
        HashMap<BitSet, ArrayList<JsonNode>> bitset2EdgeList = new HashMap<>();

        for(int i = 0; i < nodes.size(); i++){
            final JsonNode node = nodes.get(i);
            BitSet bitSet = getBitSetFromJsonNode(json.getFromDictionary(node, "bitset"));
            bitset2Fragment.put(bitSet, node);
        }
        for(int i = 0; i < edges.size(); i++){
            final JsonNode edge = edges.get(i);
            BitSet bitSet = getBitSetFromJsonNode(json.getFromDictionary(edge, "sourceBitset"));
            bitset2EdgeList.computeIfAbsent(bitSet, k -> new ArrayList<JsonNode>()).add(edge);
        }

        /* Now, build the CombinatorialSubtree with the computed hashmaps.
         * How? For each node, contained in the tree, request all JsonNode-Edges with the same 'sourceBitset'.
         * These JsonNode-Edges represent all outgoing edges of the current node of interest.
         * Then, for each of these JsonNode-Edges, compute the 'targetBitset' and get the corresponding CombinatorialFragment.
         * Attach this fragment to the node.
         */
        CombinatorialSubtree tree = new CombinatorialSubtree(molecule);
        ArrayDeque<CombinatorialNode> nodesToProcess = new ArrayDeque<>(); // set of nodes that are already contained in the tree
        nodesToProcess.addLast(tree.getRoot());

        while(!nodesToProcess.isEmpty()){
            CombinatorialNode node = nodesToProcess.pollFirst();
            ArrayList<JsonNode> outgoingEdges = bitset2EdgeList.get(node.fragment.bitset);

            if(outgoingEdges != null){
                for(JsonNode edge : outgoingEdges){
                    BitSet targetBitset = getBitSetFromJsonNode(json.getFromDictionary(edge, "targetBitset"));
                    final JsonNode targetNode = bitset2Fragment.get(targetBitset);
                    BitSet targetDisconnectedRings = getBitSetFromJsonNode(json.getFromDictionary(targetNode, "disconnectedRings"));
                    MolecularFormula targetMf = MolecularFormula.parse(json.getStringFromDictionary(targetNode, "molecularFormula"));
                    CombinatorialFragment targetFragment = new CombinatorialFragment(molecule, targetBitset, targetMf, targetDisconnectedRings, json.getBooleanFromDictionary(targetNode, "innerNode"), 0f);

                    float fragmentScore = json.getFromDictionary(targetNode, "fragmentScore").floatValue();
                    float edgeScore = json.getFromDictionary(edge, "edgeScore").floatValue();
                    IBond cut1 = getBondFromJsonNode(molecule, json.getFromDictionary(edge, "cut1"), json);
                    IBond cut2 = getBondFromJsonNode(molecule, json.getFromDictionary(edge, "cut2"), json);

                    CombinatorialNode newNode = tree.addFragment(node, targetFragment, cut1, cut2, fragmentScore, edgeScore);
                    nodesToProcess.addLast(newNode);
                }
            }
        }

        return tree;
    }

    private static BitSet getBitSetFromJsonNode(JsonNode jsonNode){
        long[] longs = new long[jsonNode.size()];
        for(int i = 0; i < jsonNode.size(); i++){
            longs[i] = jsonNode.get(i).asLong();
        }
        return BitSet.valueOf(longs);
    }

    private static IBond getBondFromJsonNode(MolecularGraph molecule, JsonNode jsonNode, JacksonDocument json) throws IOException{
        if (jsonNode.isNull()) return null;

        final int idxFirstAtom = json.getFromDictionary(jsonNode, "indexFirstAtom").asInt();
        final int idxSecondAtom = json.getFromDictionary(jsonNode, "indexSecondAtom").asInt();
        final IAtom firstAtom = molecule.molecule.getAtom(idxFirstAtom);
        final IAtom secondAtom = molecule.molecule.getAtom(idxSecondAtom);
        return molecule.molecule.getBond(firstAtom, secondAtom);
    }
}
