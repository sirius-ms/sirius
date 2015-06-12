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
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.babelms.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;

public class FTJsonReader implements Parser<FTree> {
    @Override
    public FTree parse(BufferedReader reader) throws IOException {
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
                    v.getIncomingEdge().setWeight(incomingLossMap.get(child).getDouble("score"));
                }
            }
            return tree;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }
}
