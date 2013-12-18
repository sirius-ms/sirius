package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.IsotopePatternAnalysis.isogencli.Main;

public class IsogenCommand implements Command {
    @Override
    public String getDescription() {
        return "computes a theoretical isotope pattern for the given molecular formula.";
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
