package de.unijena.bioinf.fragmenter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class CombinatorialSubtreeJsonWriter {

    public static String treeToJsonString(CombinatorialSubtree tree) throws CDKException, IOException {
        StringWriter writer = new StringWriter();
        tree2Json(tree, writer);
        return writer.toString();
    }

    public static void writeTreeToFile(CombinatorialSubtree tree, File file) throws IOException, CDKException {
        try(BufferedWriter writer = Files.newBufferedWriter(file.toPath(), Charset.defaultCharset())) {
            tree2Json(tree, writer);
        }
    }

    public static void tree2Json(final CombinatorialSubtree tree,final Writer writer) throws IOException, CDKException {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        JsonGenerator jsonGenerator = factory.createGenerator(writer);
        tree2Json(tree, jsonGenerator);
        jsonGenerator.close();
    }

    public static void tree2Json(final CombinatorialSubtree tree,final JsonGenerator jsonGenerator) throws IOException, CDKException {
        jsonGenerator.useDefaultPrettyPrinter();
        jsonGenerator.writeStartObject();

        final IAtomContainer molecule = tree.getRoot().fragment.parent.getMolecule();
        final SmilesGenerator smiGen = new SmilesGenerator(SmiFlavor.Generic);
        final int[] atomOrder = new int[molecule.getAtomCount()];

        jsonGenerator.writeStringField("molecule", smiGen.create(molecule, atomOrder));
        jsonGenerator.writeStringField("molecularFormula", tree.getRoot().getFragment().getFormula().toString());
        jsonGenerator.writeNumberField("treeScore", tree.getScore());

        // Now, write a list of all nodes (including the root) into the JSON format:
        final HashMap<CombinatorialNode, Integer> subtreeSizes = tree.getSubtreeSizes();
        jsonGenerator.writeFieldName("nodes");
        jsonGenerator.writeStartArray();
        writeCombinatorialNodeToJson(tree.getRoot(), atomOrder,subtreeSizes.get(tree.getRoot()), jsonGenerator);
        for(CombinatorialNode node : tree.getNodes()) writeCombinatorialNodeToJson(node, atomOrder,subtreeSizes.get(node), jsonGenerator);
        jsonGenerator.writeEndArray();

        // Now, write a list of all edges into the JSON format:
        final List<CombinatorialEdge> edges = tree.getEdgeList();
        jsonGenerator.writeFieldName("edges");
        jsonGenerator.writeStartArray();
        for(CombinatorialEdge edge : edges) writeCombinatorialEdgeToJson(edge, atomOrder, jsonGenerator);
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
        jsonGenerator.flush();
    }

    // order[i] := index of atom i in the new AtomContainer
    private static BitSet permutate(BitSet bitSet, int[] order){
        BitSet newBitSet = new BitSet(bitSet.length());
        for(int i = 0; i < order.length; i++){
            newBitSet.set(order[i], bitSet.get(i));
        }
        return newBitSet;
    }

    private static void writeBitSetToJson(String fieldName, BitSet bitSet, JsonGenerator jsonGenerator) throws IOException {
        long[] longs = bitSet.toLongArray();
        jsonGenerator.writeFieldName(fieldName);
        jsonGenerator.writeStartArray();
        for(int i = 0; i < longs.length; i++){
            jsonGenerator.writeNumber(longs[i]);
        }
        jsonGenerator.writeEndArray();
    }

    private static void writeCombinatorialNodeToJson(CombinatorialNode node, int[] order, int subtreeSize, JsonGenerator jsonGenerator) throws IOException{
        final BitSet bitSet = (node.fragment.isInnerNode()) ? permutate(node.fragment.bitset, order) : node.fragment.bitset;
        final CombinatorialNode parentNode = (node.incomingEdges.size() == 1) ? node.incomingEdges.get(0).source : null;
        final BitSet parentBitSet = (parentNode == null) ? null : permutate(parentNode.fragment.bitset, order);

        jsonGenerator.writeStartObject();
        writeBitSetToJson("bitset", bitSet, jsonGenerator);
        writeBitSetToJson("disconnectedRings", node.fragment.disconnectedRings, jsonGenerator);
        if(parentBitSet != null){
            writeBitSetToJson("parentBitset", parentBitSet, jsonGenerator);
        }else{
            jsonGenerator.writeNullField("parentBitSet");
        }
        jsonGenerator.writeStringField("smiles", node.fragment.toSMILES());
        jsonGenerator.writeStringField("molecularFormula", node.fragment.getFormula().toString());
        jsonGenerator.writeNumberField("fragmentScore", node.fragmentScore);
        jsonGenerator.writeNumberField("totalScore", node.totalScore);
        jsonGenerator.writeNumberField("depth", node.depth);
        jsonGenerator.writeNumberField("subtreeSize", subtreeSize);
        jsonGenerator.writeNumberField("bondbreaks", node.bondbreaks);
        jsonGenerator.writeBooleanField("innerNode", node.fragment.isInnerNode());
        jsonGenerator.writeEndObject();
    }

    private static void writeCombinatorialEdgeToJson(CombinatorialEdge edge, int[] order, JsonGenerator jsonGenerator) throws IOException {
        CombinatorialNode sourceNode = edge.source;
        CombinatorialNode targetNode = edge.target;
        jsonGenerator.writeStartObject();
        writeBitSetToJson("sourceBitset", permutate(sourceNode.fragment.bitset, order), jsonGenerator);
        writeBitSetToJson("targetBitset", (targetNode.fragment.isInnerNode()) ? permutate(targetNode.fragment.bitset, order) : targetNode.fragment.bitset, jsonGenerator);
        writeBondToJson("cut1", edge.getCut1(), edge.getDirectionOfFirstCut(), order, jsonGenerator);
        writeBondToJson("cut2", edge.getCut2(), edge.getDirectionOfSecondCut(), order, jsonGenerator);
        jsonGenerator.writeNumberField("edgeScore", edge.getScore());
        jsonGenerator.writeBooleanField("directionCut1", edge.getDirectionOfFirstCut());
        jsonGenerator.writeBooleanField("directionCut2", edge.getDirectionOfSecondCut());

        if (!edge.target.fragment.isInnerNode()) {
            // outout hydrogen rearrangements
            final int hydrogenRearrangements =edge.source.fragment.hydrogenRearrangements(edge.target.fragment.getFormula());
            jsonGenerator.writeNumberField("hydrogenRearrangements", hydrogenRearrangements);
        }

        jsonGenerator.writeEndObject();
    }

    private static void writeBondToJson(String fieldName, IBond bond, boolean cutDirection, int[] order, JsonGenerator jsonGenerator) throws IOException{
        if(bond != null) {
            jsonGenerator.writeFieldName(fieldName);
            jsonGenerator.writeStartObject();
            jsonGenerator.writeNumberField("indexFirstAtom", order[bond.getAtom(0).getIndex()]);
            jsonGenerator.writeNumberField("indexSecondAtom", order[bond.getAtom(1).getIndex()]);
            jsonGenerator.writeStringField("firstAtomSymbol", bond.getAtom(0).getSymbol());
            jsonGenerator.writeStringField("secondAtomSymbol", bond.getAtom(1).getSymbol());
            jsonGenerator.writeStringField("bondNameSpecific", DirectedBondTypeScoring.bondNameSpecific(bond, cutDirection));
            jsonGenerator.writeStringField("bondNameGeneric", DirectedBondTypeScoring.bondNameGeneric(bond, cutDirection));
            jsonGenerator.writeEndObject();
        }else{
            jsonGenerator.writeNullField(fieldName);
        }
    }
}
