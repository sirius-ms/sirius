/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.babelms.json;

import com.google.common.collect.HashMultimap;
import com.google.gson.*;
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.data.JSONDocumentType;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;
import de.unijena.bioinf.ms.annotations.DataAnnotation;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class FTJsonReader implements Parser<FTree> {

    /*
    TODO: currently, only Peak and Ionization are parsed from input
     */

    protected final HashMap<String, MolecularFormula> formulaCache;

    public FTJsonReader() {
        this.formulaCache = new HashMap<>();
    }

    public FTJsonReader(@Nullable  HashMap<String, MolecularFormula> formulaCache) {
        this.formulaCache = formulaCache==null ? new HashMap<>()  : formulaCache;
    }

    public MolecularFormula formula(String formula) {
        return formulaCache.computeIfAbsent(formula, MolecularFormula::parseOrThrow);
    }

    @Deprecated
    public FTree parse(BufferedReader reader) throws IOException {
        return parse(reader, null);
    }

    public FTree parse(BufferedReader reader, URL source) throws IOException {
        final StringBuilder buffer = new StringBuilder(1024);
        String line = null;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append('\n');
        }
        if (buffer.length() == 0) return null;
        return treeFromJsonString(buffer.toString(), source);
    }

    public FTree treeFromJsonString(String jsonString, URL source) {
        final DescriptorRegistry registry = DescriptorRegistry.getInstance();
        final JSONDocumentType JSONdoc = new JSONDocumentType();
        final JsonParser r = new JsonParser();
        final JsonObject json = r.parse(jsonString).getAsJsonObject();
        double score = 0d;
        double scoreBoost = 0d;
        final JsonArray fragments = json.getAsJsonArray("fragments");
        final HashMap<MolecularFormula, FragmentInfo> fragmentByFormulaMap = new HashMap<>(fragments.size());
        final TIntObjectHashMap<FragmentInfo> fragmentByIdMap = new TIntObjectHashMap<>();
//        final TObjectIntHashMap<Fragment> treeFragmentToIdMap = new TObjectIntHashMap<>();
        final TIntIntHashMap treeFragmentIdToIdMap = new TIntIntHashMap();
        for (int k = 0; k < fragments.size(); ++k) {
            final JsonObject fragment = fragments.get(k).getAsJsonObject();
            final int id = Integer.parseInt(fragment.get("id").getAsString());
            final MolecularFormula vertex = formula(fragment.get("molecularFormula").getAsString());
            final Ionization vertexIon = PrecursorIonType.getPrecursorIonType(fragment.get("ion").getAsString()).getIonization();
//            fragmentByFormulaMap.put(vertex, new Object[]{fragment, vertexIon});
            fragmentByFormulaMap.put(vertex, new FragmentInfo(id, vertex, vertexIon, fragment));
            fragmentByIdMap.put(id, new FragmentInfo(id, vertex, vertexIon, fragment));
        }

        final FragmentInfo root = getRootInfo(json.get("root"), fragmentByFormulaMap, fragmentByIdMap);
        final FTree tree = new FTree(root.formula, root.ionization);
        treeFragmentIdToIdMap.put(tree.getRoot().getVertexId(), root.id); //todo is root always id 0??


//        final HashMap<MolecularFormula, JsonObject> incomingLossMap = new HashMap<>();
        final HashMap<FragmentInfo, JsonObject> incomingLossMap = new HashMap<>();
//        final HashMultimap<MolecularFormula, MolecularFormula> edges = HashMultimap.create();
//        final HashMultimap<FragmentInfo, FragmentInfo> edges = HashMultimap.create();
        final HashMultimap<Integer, Integer> edges = HashMultimap.create();
        final JsonArray losses = json.get("losses").getAsJsonArray();
        for (int k = 0; k < losses.size(); ++k) {
            final JsonObject loss = losses.get(k).getAsJsonObject();

            boolean byId = false;
            try {
                final JsonElement lossSource = loss.get("source");
                final JsonElement lossTarget = loss.get("target");
                if (lossSource.isJsonPrimitive() && lossTarget.isJsonPrimitive()
                        && ((JsonPrimitive)lossSource).isNumber() && ((JsonPrimitive)lossTarget).isNumber()){
                    final int a = loss.get("source").getAsInt();
                    final int b = loss.get("target").getAsInt();
                    final FragmentInfo bInfo = fragmentByIdMap.get(b);
                    edges.put(fragmentByIdMap.get(a).id, bInfo.id);
                    incomingLossMap.put(bInfo, loss);
                    byId = true;
                }
            } catch (UnsupportedOperationException e) {

            }

            if (!byId) {
                //this is for backwards compatibility, from now on we use ids to map
                final MolecularFormula a = formula(loss.get("source").getAsString()),
                        b = formula(loss.get("target").getAsString());

                final FragmentInfo bInfo = fragmentByFormulaMap.get(b);
                edges.put(fragmentByFormulaMap.get(a).id, bInfo.id);
                incomingLossMap.put(bInfo, loss);
            }

        }


        final ArrayDeque<Fragment> stack = new ArrayDeque<Fragment>();
        stack.push(tree.getRoot());
        while (!stack.isEmpty()) {
            final Fragment u = stack.pollFirst();
            final int id = treeFragmentIdToIdMap.get(u.getVertexId());
//            for (MolecularFormula child : edges.get(u.getFormula())) {
//                Ionization ion = (Ionization) fragmentByFormulaMap.get(child)[1];
            for (int childId : edges.get(id)) {
                FragmentInfo child = fragmentByIdMap.get(childId);
                Ionization ion = child.ionization;
                final Fragment v = tree.addFragment(u, child.formula, ion);
                treeFragmentIdToIdMap.put(v.getVertexId(), childId);
                stack.push(v);
                if (incomingLossMap.get(child).has("score"))
                    v.getIncomingEdge().setWeight(incomingLossMap.get(child).get("score").getAsDouble() + child.jsonObject.get("score").getAsDouble());
            }
        }

//        final ArrayDeque<Fragment> stack = new ArrayDeque<Fragment>();
//        stack.push(tree.getRoot());
//        while (!stack.isEmpty()) {
//            final Fragment u = stack.pollFirst();
////            for (MolecularFormula child : edges.get(u.getFormula())) {
////                Ionization ion = (Ionization) fragmentByFormulaMap.get(child)[1];
//            for (int childId : edges.get(u.getVertexId())) {
//                FragmentInfo child = fragmentByIdMap.get(childId);
//                Ionization ion = child.ionization;
//                final Fragment v = tree.addFragment(u, child.formula, ion);
//                stack.push(v);
//                if (incomingLossMap.get(child).has("score"))
//                    v.getIncomingEdge().setWeight(incomingLossMap.get(child).get("score").getAsDouble());
//            }
//        }

        {
            final JsonObject treeAnnotations = json.get("annotations").getAsJsonObject();
            final String[] keywords = getKeyArray(treeAnnotations);
            final Descriptor[] descriptors = registry.getByKeywords(FTree.class, keywords);
            for (Descriptor<DataAnnotation> descriptor : descriptors) {
                final DataAnnotation annotation = descriptor.read(JSONdoc, treeAnnotations);
                if (annotation != null) {
                    tree.setAnnotation(descriptor.getAnnotationClass(), annotation);
                }
            }
            if (treeAnnotations.has("nodeBoost")) {
                scoreBoost = treeAnnotations.get("nodeBoost").getAsDouble();
            }
        }
        double rootScore = 0d;
        for (Fragment f : tree.getFragments()) {
//            final Object[] objects = fragmentByFormulaMap.get(f.getFormula());
//            final JsonObject jsonfragment = (JsonObject)objects[0];
            final FragmentInfo info = fragmentByIdMap.get(treeFragmentIdToIdMap.get(f.getVertexId()));
            final JsonObject jsonfragment = info.jsonObject;

            final String[] keywords = getKeyArray(jsonfragment);
            final Descriptor[] descriptors = registry.getByKeywords(Fragment.class, keywords);
            for (Descriptor<DataAnnotation> descriptor : descriptors) {
                final DataAnnotation annotation = descriptor.read(JSONdoc, jsonfragment);
                if (annotation != null) {
                    FragmentAnnotation<DataAnnotation> fano = tree.getOrCreateFragmentAnnotation(descriptor.getAnnotationClass());
                    fano.set(f, annotation);
                }
            }

            if (jsonfragment.has("score")) {
                score += (jsonfragment.get("score").getAsDouble());
                if (f.getFormula().equals(root.formula)) {
                    rootScore = score;
                }
            }
        }

        for (Loss l : tree.losses()) {
//            final JsonObject jsonloss = incomingLossMap.get(l.getTarget().getFormula());
            final JsonObject jsonloss = incomingLossMap.get(fragmentByIdMap.get(treeFragmentIdToIdMap.get(l.getTarget().getVertexId())));
            final String[] keywords = getKeyArray(jsonloss);
            final Descriptor[] descriptors = registry.getByKeywords(Loss.class, keywords);
            for (Descriptor<DataAnnotation> descriptor : descriptors) {
                final DataAnnotation annotation = descriptor.read(JSONdoc, jsonloss);
                if (annotation != null) {
                    LossAnnotation<DataAnnotation> lano = tree.getOrCreateLossAnnotation(descriptor.getAnnotationClass());
                    lano.set(l, annotation);
                }
            }
            if (jsonloss.has("score"))
                score += jsonloss.get("score").getAsDouble();
        }

        if (source != null) tree.setAnnotation(DataSource.class, new DataSource(source));
        tree.normalizeStructure();
        tree.setTreeWeight(score);
        tree.setRootScore(rootScore);
        return tree;
    }

    private FragmentInfo getRootInfo(JsonElement rootElement, HashMap<MolecularFormula, FragmentInfo> fragmentByFormulaMap, TIntObjectHashMap<FragmentInfo> fragmentByIdMap) {
        try {
            if (rootElement.isJsonPrimitive() && ((JsonPrimitive)rootElement).isNumber()){
                final int id = rootElement.getAsInt();
                FragmentInfo fragmentInfo  = fragmentByIdMap.get(id);
                if (fragmentInfo==null) throw new RuntimeException("Cannot determine root fragment");
                return fragmentInfo;
            }
        } catch (UnsupportedOperationException e) {

        }

        //this is for backwards compatibility, from now on we use ids to map
        final MolecularFormula f = formula(rootElement.getAsString());
        final FragmentInfo rInfo = fragmentByFormulaMap.get(f);
        if (rInfo==null) throw new RuntimeException("Cannot determine root fragment");

        return rInfo;
    }

    public static String[] getKeyArray(JsonObject object) {
        final Set<Map.Entry<String, JsonElement>> entrySet = object.entrySet();
        final String[] a = new String[entrySet.size()];
        Iterator<Map.Entry<String, JsonElement>> it = entrySet.iterator();
        for (int i = 0; i < a.length; i++) {
            a[i] = it.next().getKey();
        }
        return a;
    }


    private class FragmentInfo {
        int id;
        MolecularFormula formula;
        Ionization ionization;
        JsonObject jsonObject;

        public FragmentInfo(int id, MolecularFormula formula, Ionization ionization, JsonObject jsonObject) {
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
