package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.FragmentationTree.CleanupSpectrum;
import de.unijena.bioinf.FragmentationTree.Main;

/**
 * Created by kaidu on 12/5/13.
 */
public class CleanupCommand implements Command {
    @Override
    public String getDescription() {
        return "idealize spectrum";
    }

    @Override
    public String getName() {
        return "cleanup";
    }

    @Override
    public void run(String[] args) {
        CleanupSpectrum.main(args);
    }

    @Override
    public String getVersion() {
        return Main.VERSION_STRING;
    }
}
