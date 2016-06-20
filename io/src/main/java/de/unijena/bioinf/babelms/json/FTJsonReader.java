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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.babelms.descriptor.Descriptor;
import de.unijena.bioinf.babelms.descriptor.DescriptorRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class FTJsonReader implements Parser<FTree> {

    /*
    TODO: currently, only Peak and Ionization are parsed from input
     */

    @Deprecated
    public FTree parse(BufferedReader reader) throws IOException {
        return parse(reader, null);
    }

    @Override
    public FTree parse(BufferedReader reader, URL source) throws IOException {
        final StringBuilder buffer = new StringBuilder(1024);
        String line = null;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append('\n');
        }
        if (buffer.length() == 0) return null;

        final DescriptorRegistry registry = DescriptorRegistry.getInstance();
        final JSONDocumentType JSONdoc = new JSONDocumentType();


        final JsonParser r = new JsonParser();
        final JsonObject json = r.parse(buffer.toString()).getAsJsonObject();
        final FTree tree = new FTree(MolecularFormula.parse(json.get("molecularFormula").getAsString()));
        final JsonArray fragments = json.getAsJsonArray("fragments");
        final HashMap<MolecularFormula, JsonObject> fragmentMap = new HashMap<>(fragments.size());
        for (int k = 0; k < fragments.size(); ++k) {
            final JsonObject fragment = fragments.get(k).getAsJsonObject();
            final MolecularFormula vertex = MolecularFormula.parse(fragment.get("molecularFormula").getAsString());
            fragmentMap.put(vertex, fragment);
        }

        final HashMap<MolecularFormula, JsonObject> incomingLossMap = new HashMap<>();
        final HashMultimap<MolecularFormula, MolecularFormula> edges = HashMultimap.create();
        final JsonArray losses = json.get("losses").getAsJsonArray();
        for (int k = 0; k < losses.size(); ++k) {
            final JsonObject loss = losses.get(k).getAsJsonObject();
            final MolecularFormula a = MolecularFormula.parse(loss.get("source").getAsString()),
                    b = MolecularFormula.parse(loss.get("target").getAsString());
            edges.put(a, b);
            incomingLossMap.put(b, loss);
        }
        final ArrayDeque<Fragment> stack = new ArrayDeque<Fragment>();
        stack.push(tree.getRoot());
        while (!stack.isEmpty()) {
            final Fragment u = stack.pollFirst();
            for (MolecularFormula child : edges.get(u.getFormula())) {
                final Fragment v = tree.addFragment(u, child);
                stack.push(v);
                if (incomingLossMap.get(child).has("score"))
                    v.getIncomingEdge().setWeight(incomingLossMap.get(child).get("score").getAsDouble());
            }
        }

        {
            final JsonObject treeAnnotations = json.get("annotations").getAsJsonObject();
            final String[] keywords = getKeyArray(treeAnnotations);
            final Descriptor[] descriptors = registry.getByKeywords(FTree.class, keywords);
            for (Descriptor<Object> descriptor : descriptors) {
                final Object annotation = descriptor.read(JSONdoc, treeAnnotations);
                if (annotation != null) {
                    tree.addAnnotation(descriptor.getAnnotationClass(), annotation);
                }
            }
        }

        for (Fragment f : tree.getFragments()) {
            final JsonObject jsonfragment = fragmentMap.get(f.getFormula());
            final String[] keywords = getKeyArray(jsonfragment);
            final Descriptor[] descriptors = registry.getByKeywords(Fragment.class, keywords);
            for (Descriptor<Object> descriptor : descriptors) {
                final Object annotation = descriptor.read(JSONdoc, jsonfragment);
                if (annotation != null) {
                    FragmentAnnotation<Object> fano = tree.getOrCreateFragmentAnnotation(descriptor.getAnnotationClass());
                    fano.set(f, annotation);
                }
            }
        }

        for (Loss l : tree.losses()) {
            final JsonObject jsonloss = incomingLossMap.get(l.getTarget().getFormula());
            final String[] keywords = getKeyArray(jsonloss);
            final Descriptor[] descriptors = registry.getByKeywords(Loss.class, keywords);
            for (Descriptor<Object> descriptor : descriptors) {
                final Object annotation = descriptor.read(JSONdoc, jsonloss);
                if (annotation != null) {
                    LossAnnotation<Object> lano = tree.getOrCreateLossAnnotation(descriptor.getAnnotationClass());
                    lano.set(l, annotation);
                }
            }
        }

        if (source != null) tree.addAnnotation(URL.class, source);
        return tree;
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


}
