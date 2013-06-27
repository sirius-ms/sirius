package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.FragmentationTree.Main;

public class TreeCommand implements Command{

    @Override
    public String getDescription() {
        return "computes fragmentation trees for given input spectra";
    }

    @Override
    public String getName() {
        return "tree";
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
