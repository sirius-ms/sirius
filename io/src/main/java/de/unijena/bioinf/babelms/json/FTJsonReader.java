
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schilller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

package de.unijena.bioinf.babelms.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.data.JSONDocumentType;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;

public class FTJsonReader implements Parser<FTree> {

    /*
    TODO: currently, only Peak and Ionization are parsed from input
     */

    protected final HashMap<String, MolecularFormula> formulaCache;

    private Reader consumedReader = null;

    public FTJsonReader() {
        this.formulaCache = new HashMap<>();
    }

    public FTJsonReader(@Nullable HashMap<String, MolecularFormula> formulaCache) {
        this.formulaCache = formulaCache == null ? new HashMap<>() : formulaCache;
    }

    public MolecularFormula formula(String formula) {
        return formulaCache.computeIfAbsent(formula, MolecularFormula::parseOrThrow);
    }

    @Override
    public FTree parse(InputStream inputStream, URI source) throws IOException {
        return parse(FileUtils.ensureBuffering(new InputStreamReader(inputStream)), source);
    }

    @Deprecated
    public FTree parse(BufferedReader reader) throws IOException {
        return parse(reader, null);
    }

    public FTree parse(BufferedReader reader, URI source) throws IOException {
        return treeFromJson(reader, source);
    }

    public FTree treeFromJsonString(String reader, URI source) throws IOException {
        return treeFromJson(new StringReader(reader), source);
    }

    public FTree treeFromJson(Reader reader, URI source) throws IOException {
        if (reader == consumedReader) {
            return null;
        }
        final JsonNode docRoot = new ObjectMapper().readTree(reader);
        consumedReader = reader;
        return treeFromJson(docRoot, source);
    }

    public FTree treeFromJson(@NotNull final JsonNode docRoot, @Nullable URI source) {
        final DescriptorRegistry registry = DescriptorRegistry.getInstance();
        double score = 0d;
        double scoreBoost = 0d;

        final JsonNode fragments = docRoot.get("fragments");
        final HashMap<MolecularFormula, FragmentInfo> fragmentByFormulaMap = new HashMap<>(fragments.size());
        final TIntObjectHashMap<FragmentInfo> fragmentByIdMap = new TIntObjectHashMap<>();
//        final TObjectIntHashMap<Fragment> treeFragmentToIdMap = new TObjectIntHashMap<>();
        final TIntIntHashMap treeFragmentIdToIdMap = new TIntIntHashMap();
        for (int k = 0; k < fragments.size(); ++k) {
            final JsonNode fragment = fragments.get(k);
            final int id = fragment.get("id").asInt();
            final MolecularFormula vertex = MolecularFormula.parseOrThrow(fragment.get("molecularFormula").asText());
            final Ionization vertexIon = PrecursorIonType.getPrecursorIonType(fragment.get("ion").asText()).getIonization();
//            fragmentByFormulaMap.put(vertex, new Object[]{fragment, vertexIon});
            fragmentByFormulaMap.put(vertex, new FragmentInfo(id, vertex, vertexIon, fragment));
            fragmentByIdMap.put(id, new FragmentInfo(id, vertex, vertexIon, fragment));
        }

        final FragmentInfo root = getRootInfo(docRoot.get("root"), fragmentByFormulaMap, fragmentByIdMap);
        final FTree tree = new FTree(root.formula, root.ionization);
        treeFragmentIdToIdMap.put(tree.getRoot().getVertexId(), root.id); //todo is root always id 0??


        final HashMap<FragmentInfo, JsonNode> incomingLossMap = new HashMap<>();

        final Int2ObjectMap<IntArrayList> edges = new Int2ObjectOpenHashMap<>();
        final JsonNode losses = docRoot.get("losses");
        for (int k = 0; k < losses.size(); ++k) {
            final JsonNode loss = losses.get(k);

            boolean byId = false;
            try {
                final JsonNode lossSource = loss.get("source");
                final JsonNode lossTarget = loss.get("target");
                if (lossSource.isIntegralNumber() && lossTarget.isIntegralNumber()) {
                    final int a = loss.get("source").asInt();
                    final int b = loss.get("target").asInt();
                    final FragmentInfo bInfo = fragmentByIdMap.get(b);
                    edges.computeIfAbsent(fragmentByIdMap.get(a).id, key -> new IntArrayList()).add(bInfo.id);
                    incomingLossMap.put(bInfo, loss);
                    byId = true;
                }
            } catch (UnsupportedOperationException ignored) {}

            if (!byId) {
                //this is for backwards compatibility, from now on we use ids to map
                final MolecularFormula a = formula(loss.get("source").asText()),
                        b = formula(loss.get("target").asText());

                final FragmentInfo bInfo = fragmentByFormulaMap.get(b);
                edges.computeIfAbsent(fragmentByFormulaMap.get(a).id, key -> new IntArrayList()).add(bInfo.id);
                incomingLossMap.put(bInfo, loss);
            }

        }


        final ArrayDeque<Fragment> stack = new ArrayDeque<>();
        stack.push(tree.getRoot());
        while (!stack.isEmpty()) {
            final Fragment u = stack.pollFirst();
            final int id = treeFragmentIdToIdMap.get(u.getVertexId());
            if (edges.containsKey(id))
                edges.get(id).forEach(childId -> {
                    FragmentInfo child = fragmentByIdMap.get(childId);
                    Ionization ion = child.ionization;
                    final Fragment v = tree.addFragment(u, child.formula, ion);
                    treeFragmentIdToIdMap.put(v.getVertexId(), childId);
                    stack.push(v);
                    if (incomingLossMap.get(child).has("score"))
                        v.getIncomingEdge().setWeight(incomingLossMap.get(child).get("score").asDouble() + child.jsonObject.get("score").asDouble());
                });
        }

        {
            final JsonNode treeAnnotations = docRoot.get("annotations");
            final String[] keywords = getKeyArray(treeAnnotations);
            final Descriptor[] descriptors = registry.getByKeywords(FTree.class, keywords);
            for (Descriptor<DataAnnotation> descriptor : descriptors) {
                final DataAnnotation annotation = descriptor.read(new JSONDocumentType(), (ObjectNode) treeAnnotations);
                if (annotation != null) {
                    tree.setAnnotation(descriptor.getAnnotationClass(), annotation);
                }
            }
            if (treeAnnotations.has("nodeBoost")) {
                scoreBoost = treeAnnotations.get("nodeBoost").asDouble();
            }
        }
        double rootScore = 0d;
        for (Fragment f : tree.getFragments()) {
//            final Object[] objects = fragmentByFormulaMap.get(f.getFormula());
//            final JsonObject jsonfragment = (JsonObject)objects[0];
            final FragmentInfo info = fragmentByIdMap.get(treeFragmentIdToIdMap.get(f.getVertexId()));
            final JsonNode jsonfragment = info.jsonObject;

            final String[] keywords = getKeyArray(jsonfragment);
            final Descriptor[] descriptors = registry.getByKeywords(Fragment.class, keywords);
            for (Descriptor<DataAnnotation> descriptor : descriptors) {
                final DataAnnotation annotation = descriptor.read(new JSONDocumentType(), (ObjectNode)  jsonfragment);
                if (annotation != null) {
                    FragmentAnnotation<DataAnnotation> fano = tree.getOrCreateFragmentAnnotation(descriptor.getAnnotationClass());
                    fano.set(f, annotation);
                }
            }

            if (jsonfragment.has("score")) {
                score += (jsonfragment.get("score").asDouble());
                if (f.getFormula().equals(root.formula)) {
                    rootScore = score;
                }
            }
        }

        for (Loss l : tree.losses()) {
//            final JsonObject jsonloss = incomingLossMap.get(l.getTarget().getFormula());
            final JsonNode jsonloss = incomingLossMap.get(fragmentByIdMap.get(treeFragmentIdToIdMap.get(l.getTarget().getVertexId())));
            final String[] keywords = getKeyArray(jsonloss);
            final Descriptor[] descriptors = registry.getByKeywords(Loss.class, keywords);
            for (Descriptor<DataAnnotation> descriptor : descriptors) {
                final DataAnnotation annotation = descriptor.read(new JSONDocumentType(), (ObjectNode) jsonloss);
                if (annotation != null) {
                    LossAnnotation<DataAnnotation> lano = tree.getOrCreateLossAnnotation(descriptor.getAnnotationClass());
                    lano.set(l, annotation);
                }
            }
            if (jsonloss.has("score"))
                score += jsonloss.get("score").asDouble();
        }

        if (source != null) tree.setAnnotation(DataSource.class, new DataSource(source));
        tree.normalizeStructure();
        tree.setTreeWeight(score);
        tree.setRootScore(rootScore);
        return tree;
    }

    private FragmentInfo getRootInfo(JsonNode
                                             rootElement, HashMap<MolecularFormula, FragmentInfo> fragmentByFormulaMap, TIntObjectHashMap<FragmentInfo> fragmentByIdMap) {
        try {
            if (rootElement.isIntegralNumber()) {
                final int id = rootElement.asInt();
                FragmentInfo fragmentInfo = fragmentByIdMap.get(id);
                if (fragmentInfo == null) throw new RuntimeException("Cannot determine root fragment");
                return fragmentInfo;
            }
        } catch (UnsupportedOperationException ignored) {}

        //this is for backwards compatibility, from now on we use ids to map
        final MolecularFormula f = formula(rootElement.asText());
        final FragmentInfo rInfo = fragmentByFormulaMap.get(f);
        if (rInfo == null) throw new RuntimeException("Cannot determine root fragment");

        return rInfo;
    }

    public static String[] getKeyArray(JsonNode object) {
        final String[] fields = new String[object.size()];
        int k = 0;
        for (Iterator<String> fname = object.fieldNames(); fname.hasNext(); ) {
            fields[k++] = fname.next();
        }
        return fields;
    }


    private static class FragmentInfo {
        int id;
        MolecularFormula formula;
        Ionization ionization;
        JsonNode jsonObject;

        public FragmentInfo(int id, MolecularFormula formula, Ionization ionization, JsonNode jsonObject) {
            this.id = id;
            this.formula = formula;
            this.ionization = ionization;
            this.jsonObject = jsonObject;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FragmentInfo that = (FragmentInfo) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

}
