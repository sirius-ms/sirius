
package de.unijena.bioinf.ftalign;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.utils.FileUtils;
import de.unijena.bioinf.counting.Weighting;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeightingReader {

    private static Pattern Regexp = Pattern.compile("(.+?),(\\d+(?:\\.\\d*)?)");

    public Weighting parseCSV(File csv) throws IOException {
        Reader r = null;
        try {
            r = new FileReader(csv);
            return parseCSV(r);
        } finally {
            if (r != null) r.close();
        }

    }

    public Weighting<Fragment> parseCSV(Reader csvReader) throws IOException {
        final TObjectDoubleHashMap<MolecularFormula> map = new TObjectDoubleHashMap<MolecularFormula>();
        double defaultScore = Double.NaN;
        final BufferedReader reader = FileUtils.ensureBuffering(csvReader);
        {
            final String line = reader.readLine();
            if (line == null) return new LossWeighting(map, 1);
            // may ignore first line and treat it as header
            final Matcher h = Regexp.matcher(line);
            if (h.find()) {
                final String f = h.group(1);
                if (f.equals("*")) defaultScore=Double.parseDouble(h.group(2));
                else MolecularFormula.parseAndExecute(f, formula -> map.put(formula, Double.parseDouble(h.group(2))));
            }
        }
        while (reader.ready()) {
            final String l = reader.readLine();
            if (l.isEmpty()) continue;
            final Matcher m = Regexp.matcher(l);
            if (m.find()) {
                final String f = m.group(1);
                if (f.equals("*")) defaultScore=Double.parseDouble(m.group(2));
                else  MolecularFormula.parseAndExecute(f, fomula -> map.put(fomula, Double.parseDouble(m.group(2))));
            } else throw new IOException("No valid csv file");
        }
        if (Double.isNaN(defaultScore)) throw new IOException("Can't find default score for unseen losses. Please provide a row '*,<score>'");
        return new LossWeighting(map, defaultScore);
    }

    private static class LossWeighting implements Weighting<Fragment> {
        private final TObjectDoubleHashMap<MolecularFormula> formulas;
        private final double defaultScore;

        LossWeighting(TObjectDoubleHashMap<MolecularFormula> map, double defaultScore) {
            this.formulas = new TObjectDoubleHashMap<MolecularFormula>(map.size(), 0.75f, defaultScore);
            this.formulas.putAll(map);
            this.defaultScore = defaultScore;
        }

        @Override
        public double weight(Fragment u, Fragment v) {
            final MolecularFormula formula = u.getIncomingEdge().getFormula();
            return formulas.get(formula);
        }
    }

}
