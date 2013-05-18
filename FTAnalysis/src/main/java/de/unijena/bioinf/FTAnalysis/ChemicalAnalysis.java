package de.unijena.bioinf.FTAnalysis;


public class ChemicalAnalysis {
    /*
    public static void main(String... args) {
        final PeriodicTable P = PeriodicTable.getInstance();
        final File f = new File("/home/kai/data/analysis/compounds.csv");
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(1e-4, new ChemicalAlphabet(P.getAllByName("C", "H", "N", "O", "P", "F", "I", "Na")));
        final HashMap<Element, Interval> bounds = new HashMap<Element, Interval>();
        bounds.put(P.getByName("P"), new Interval(0, 8));
        bounds.put(P.getByName("F"), new Interval(0, 8));
        bounds.put(P.getByName("I"), new Interval(0, 8));
        bounds.put(P.getByName("Na"), new Interval(0, 1));
        try {
            BufferedReader r = new BufferedReader(new FileReader(f));
            r.readLine();
            final StringBuilder buffer = new StringBuilder();
            final BufferedWriter w = new BufferedWriter(new FileWriter(new File("/home/kai/data/analysis/decomps.csv")));
            w.write("formula,rdbe,he2c,hy2c\n");
            while (r.ready()) {
                final String line = r.readLine();
                if (line != null) {
                    String[] rows = line.split(",");
                    MolecularFormula formula = MolecularFormula.parse(rows[1]);
                    double mass = Double.parseDouble(rows[4]);
                    final List<MolecularFormula> list = decomposer.decomposeToFormulas(mass, bounds, new FormulaConstraints());
                    for (MolecularFormula g : list) {
                        buffer.append(g.toString()).append(",").append(g.rdbe()).append(",").append(g.hetero2CarbonRatio()).append(",").append(g.hydrogen2CarbonRatio()).append("\n");
                    }
                }
                w.write(buffer.toString());
                buffer.delete(0, buffer.length());
            }
            w.close();
            r.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }
    */

}
