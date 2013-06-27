package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.MassDecomposer.cli.Main;

public class DecompCommand implements Command {
    @Override
    public String getDescription() {
        return "Decompose the given mass.\n" + Main.USAGE;
    }

    @Override
    public String getName() {
        return "decomp";
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
