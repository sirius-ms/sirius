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
import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.*;
import de.unijena.bioinf.babelms.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FTJsonReader implements Parser<FTree> {

    /*
    TODO: currently, only Peak and Ionization are parsed from input
     */

    @Deprecated
    public FTree parse(BufferedReader reader) throws IOException {
        return parse(reader,null);
    }

    @Override
    public FTree parse(BufferedReader reader, URL source) throws IOException {
        final StringBuilder buffer = new StringBuilder(1024);
        String line=null;
        while ((line= reader.readLine())!=null) {
            buffer.append(line).append('\n');
        }
        reader.close();
        try {
            final JSONObject json = new JSONObject(buffer.toString());
            final FTree tree = new FTree(MolecularFormula.parse(json.getString("molecularFormula")));
            final JSONArray fragments = json.getJSONArray("fragments");
            final HashMap<MolecularFormula, JSONObject> fragmentMap = new HashMap<MolecularFormula, JSONObject>(fragments.length());
            for (int k=0; k < fragments.length(); ++k) {
                final JSONObject fragment = fragments.getJSONObject(k);
                final MolecularFormula vertex = MolecularFormula.parse(fragment.getString("molecularFormula"));
                fragmentMap.put(vertex, fragment);
            }

            final HashMap<MolecularFormula, JSONObject> incomingLossMap = new HashMap<MolecularFormula, JSONObject>();
            final HashMultimap<MolecularFormula, MolecularFormula> edges = HashMultimap.create();
            final JSONArray losses = json.getJSONArray("losses");
            for (int k=0; k < losses.length(); ++k) {
                final JSONObject loss = losses.getJSONObject(k);
                final MolecularFormula a = MolecularFormula.parse(loss.getString("source")),
                        b = MolecularFormula.parse(loss.getString("target"));
                edges.put(a,b);
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
                        v.getIncomingEdge().setWeight(incomingLossMap.get(child).getDouble("score"));
                }
            }

            // add annotations
            final FragmentAnnotation<Peak> peakAno = tree.getOrCreateFragmentAnnotation(Peak.class);
            final List<FragmentAnnotation<? extends Object>> fragmentannotations = Arrays.asList(
                    tree.getOrCreateFragmentAnnotation(Ionization.class),
                    tree.getOrCreateFragmentAnnotation(Score.class )
            );
            for (Fragment f : tree.getFragments()) {
                final JSONObject jsonfragment = fragmentMap.get(f.getFormula());
                for (FragmentAnnotation<? extends Object> g : fragmentannotations) {
                    ((FragmentAnnotation<Object>)g).set(f, FTSpecials.readSpecialAnnotation(jsonfragment, g.getAnnotationType()));
                }
                // read peak data
                if (jsonfragment.has("mz") && jsonfragment.has("intensity")) {
                    peakAno.set(f, new Peak(jsonfragment.getDouble("mz"), jsonfragment.getDouble("intensity")));
                }
            }

            for (Loss l : tree.losses()) {
                final JSONObject jsonloss = incomingLossMap.get(l.getTarget().getFormula());
                for (FragmentAnnotation<? extends Object> g : lossAnnotations) {
                    ((FragmentAnnotation<Object>)g).set(f, FTSpecials.readSpecialAnnotation(jsonfragment, g.getAnnotationType()));
                }
                // read peak data
                if (jsonfragment.has("mz") && jsonfragment.has("intensity")) {
                    peakAno.set(f, new Peak(jsonfragment.getDouble("mz"), jsonfragment.getDouble("intensity")));
                }
            }

            // add tree annotation
            tree.addAnnotation(Ionization.class, FTSpecials.readSpecialAnnotation(json.getJSONObject("annotations"), Ionization.class));
            tree.addAnnotation(PrecursorIonType.class, FTSpecials.readSpecialAnnotation(json.getJSONObject("annotations"), PrecursorIonType.class));
            tree.addAnnotation(RecalibrationFunction.class, FTSpecials.readSpecialAnnotation(json.getJSONObject("annotations"), RecalibrationFunction.class));
            tree.addAnnotation(TreeScoring.class, FTSpecials.readSpecialAnnotation(json.getJSONObject("annotations"), TreeScoring.class));
            if (source != null) tree.addAnnotation(URL.class, source);
            return tree;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }
}
