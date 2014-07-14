package de.unijena.bioinf.babelms.dot;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.CollisionEnergy;
import de.unijena.bioinf.ChemistryBase.ms.Peak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import de.unijena.bioinf.babelms.Parser;
import de.unijena.bioinf.graphUtils.tree.PreOrderTraversal;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTDotReader implements Parser<FTree> {

    private static final Pattern PEAK_PATTERN = Pattern.compile("(\\d+(?:\\.\\d*)?) Da, (\\d+(?:\\.\\d*)?) %");
    private static final Pattern CE_PATTERN = Pattern.compile("cE: \\[(.+)\\]");
    private static final Pattern SCORE_PATTERN = Pattern.compile("(Compound)?Score:(\\d+)");

    @Override
    public FTree parse(BufferedReader reader) throws IOException {
        final Graph g = DotParser.parseGraph(reader);
        final FragmentPropertySet rootSet = new FragmentPropertySet(g.getRoot().getProperties());
        final FTree tree = new FTree(rootSet.formula);
        final FragmentAnnotation<Peak> peakAno = tree.addFragmentAnnotation(Peak.class);
        final FragmentAnnotation<CollisionEnergy[]> cesAno = tree.addFragmentAnnotation(CollisionEnergy[].class);
        final FragmentAnnotation<CollisionEnergy> ceAno = tree.addFragmentAnnotation(CollisionEnergy.class);
        peakAno.set(tree.getRoot(), rootSet.peak);
        cesAno.set(tree.getRoot(), rootSet.collisionEnergies);
        ceAno.set(tree.getRoot(), CollisionEnergy.mergeAll(rootSet.collisionEnergies));
        new PreOrderTraversal<Vertex>(g.getRoot(), g.getTreeAdapter()).<Fragment>call(new PreOrderTraversal.Call<Vertex, Fragment>() {
            @Override
            public Fragment call(Fragment parentResult, Vertex node) {
                if (parentResult == null) return tree.getRoot();
                final FragmentPropertySet set = new FragmentPropertySet(node.getProperties());
                final Fragment f = tree.addFragment(parentResult, set.formula);
                peakAno.set(f, set.peak);
                cesAno.set(f, set.collisionEnergies);
                ceAno.set(f, CollisionEnergy.mergeAll(set.collisionEnergies));
                return f;
            }
        });
        return tree;
    }

    public static class FragmentPropertySet {
        private MolecularFormula formula;
        private Peak peak;
        private CollisionEnergy[] collisionEnergies;
        private HashMap<String, Double> scores;
        private HashMap<String, String> properties;
        private Double score;
        private Double compoundScore;

        public FragmentPropertySet(Map<String, String> properties) {
            this.properties = new HashMap<String, String>(properties);
            final String label = properties.remove("label");
            final String[] infos = label.split("\\\\n");
            this.formula = MolecularFormula.parse(infos[0]);
            {
                final Matcher m = PEAK_PATTERN.matcher(infos[1]);
                m.find();
                this.peak = new Peak(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)));
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
