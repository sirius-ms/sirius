package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.IsotopePatternAnalysis.isogencli.Main;

public class IsogenCommand implements Command {
    @Override
    public String getDescription() {
        return "computes a theoretical isotope pattern for the given molecular formula.\nUsage:\n\tsirius isogen -i \"[M+H]+ -l 3 C6H12O6 > pattern.csv\"";
    }

    @Override
    public String getName() {
        return "isogen";
    }

    @Override
    public void run(String[] args) {
        Main.main(args);
    }

    @Override
    public String getVersion() {
        return Main.VERSION_STRING;
    }
}
