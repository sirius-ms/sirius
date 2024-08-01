
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

package de.unijena.bioinf.babelms.dot;

import de.unijena.bioinf.ChemistryBase.chem.Ionization;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.data.DataSource;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.SimplePeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.graphUtils.tree.PreOrderTraversal;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated //todo do we still use this? binary format does not save/read fragment ionization
public class FTDotReader implements Parser<FTree> {

    private static final Pattern PEAK_PATTERN = Pattern.compile("(\\d+(?:\\.\\d*)?) Da, (\\d+(?:\\.\\d*)?) %");
    private static final Pattern CE_PATTERN = Pattern.compile("cE: \\[(.+)\\]");
    private static final Pattern SCORE_PATTERN = Pattern.compile("(Compound)?Score:(\\d+)");

    @Override
    public FTree parse(BufferedReader reader, URI source) throws IOException {
        final Graph g = DotParser.parseGraph(reader);
        final FragmentPropertySet rootSet = new FragmentPropertySet(g.getRoot().getProperties());
        final FTree tree = new FTree(rootSet.formula, rootSet.ion);
        final FragmentAnnotation<AnnotatedPeak> peakAno = tree.addFragmentAnnotation(AnnotatedPeak.class);

        Peak peak = rootSet.peak;
        CollisionEnergy[] energy = rootSet.collisionEnergies;
        peakAno.set(tree.getRoot(), new AnnotatedPeak(rootSet.formula, peak.getMass(), 0d, peak.getIntensity(), rootSet.ion, new Peak[]{peak}, rootSet.collisionEnergies, null));


        new PreOrderTraversal<Vertex>(g.getRoot(), g.getTreeAdapter()).call(new PreOrderTraversal.Call<Vertex, Fragment>() {
            @Override
            public Fragment call(Fragment parentResult, Vertex node) {
                if (parentResult == null) return tree.getRoot();
                final FragmentPropertySet set = new FragmentPropertySet(node.getProperties());
                final Fragment f = tree.addFragment(parentResult, set.formula, set.ion);
                peakAno.set(f, new AnnotatedPeak(set.formula, set.peak.getMass(), 0d, set.peak.getIntensity(), set.ion, new Peak[]{set.peak}, set.collisionEnergies, null));
                return f;
            }
        });
        if (source != null) tree.setAnnotation(DataSource.class, new DataSource(source));
        return tree;
    }

    @Override
    public FTree parse(InputStream inputStream, URI source) throws IOException {
        return parse(FileUtils.ensureBuffering(new InputStreamReader(inputStream)), source);
    }

    public static class FragmentPropertySet {
        private MolecularFormula formula;
        private Ionization ion;
        private Peak peak;
        private CollisionEnergy[] collisionEnergies;
        private TObjectDoubleHashMap scores;
        private HashMap<String, String> properties;
        private Double score;
        private Double compoundScore;

        public FragmentPropertySet(Map<String, String> properties) {
            this.properties = new HashMap<String, String>(properties);
            this.scores = new TObjectDoubleHashMap();
            final String label = properties.remove("label");
            final String[] infos = label.split("\\\\n");
            String[] fpart = infos[0].split(" ", 2);
            this.formula = MolecularFormula.parseOrNull(fpart[0]);
            this.ion = PrecursorIonType.getPrecursorIonType("[M " + fpart[1].substring(0,fpart[1].length()-1) + "]" + fpart[1].charAt(fpart[1].length()-1)).getIonization();
            {
                final Matcher m = PEAK_PATTERN.matcher(infos[1]);
                m.find();
                this.peak = new SimplePeak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)) / 100d);
            }
            for (int x = 2; x < infos.length; ++x) {
                final String info = infos[x];
                if (info.startsWith("cE:")) {
                    final Matcher m = CE_PATTERN.matcher(info);
                    if (m.find()) {
                        final String[] energies = m.group(1).split(",\\s*");
                        this.collisionEnergies = new CollisionEnergy[energies.length];
                        int k = 0;
                        for (String ce : energies) collisionEnergies[k++] = CollisionEnergy.fromString(ce);
                        continue;
                    }
                } else if (info.startsWith("CompoundScore:")) {
                    this.compoundScore = Double.parseDouble(info.substring(info.indexOf(':') + 1));
                } else if (info.startsWith("Score:")) {
                    this.score = Double.parseDouble(info.substring(info.indexOf(':') + 1));
                } else {
                    int pos = info.indexOf('=');
                    if (pos >= 0) {
                        scores.put(info.substring(0, pos).trim(), Double.parseDouble(info.substring(pos + 1)));
                    } else {
                        final int pos2 = info.indexOf(':');
                        if (pos2 >= 0)
                            this.properties.put(info.substring(0, pos2).trim(), info.substring(pos2 + 1).trim());
                    }
                }
            }
        }
    }
}
