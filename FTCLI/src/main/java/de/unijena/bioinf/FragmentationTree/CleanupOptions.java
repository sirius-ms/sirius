package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.sirius.cli.BasicOptions;
import de.unijena.bioinf.sirius.cli.ProfileOptions;

import java.io.File;
import java.util.List;

/**
 * Created by kaidu on 12/5/13.
 */
public interface CleanupOptions extends ProfileOptions, BasicOptions {

    public static enum NOISE_FILTER {
        EXPLAINABLE, EXPLAINED;
    }

    @Option(shortName = "f", description = "molecular formula of the compound", defaultToNull = true)
    public String getFormula();

    @Option(description = "recalibrate the spectrum using the annotated peaks from the fragmentation tree")
    public boolean getRecalibrate();

    @Option(description = "EXPLAINABLE: delete all peaks with no explanation for the parent formula\nEXPLAINED: delete all peaks which are not contained in the tree", defaultToNull = true)
    public NOISE_FILTER getFilter();

    @Unparsed
    public List<File> getFiles();

    @Option(shortName = "t", defaultValue = "cleanedUp", description = "target directory for the output data")
    public File getTarget();

}
