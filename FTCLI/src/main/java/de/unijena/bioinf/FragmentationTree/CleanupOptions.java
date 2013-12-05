package de.unijena.bioinf.FragmentationTree;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.sirius.cli.BasicOptions;
import de.unijena.bioinf.sirius.cli.ProfileOptions;

import java.io.File;
import java.util.List;

/**
 * Created by kaidu on 12/5/13.
 */
public interface CleanupOptions extends ProfileOptions, BasicOptions {

    public static enum MZ {
        RECALIBRATE, IDEALIZE, KEEP;
    }

    public static enum NOISE {
        POSSIBLE, EXPLAINED;
    }

    @Option(shortName = "f", description = "molecular formula of the compound", defaultToNull = true)
    public String getFormula();

    @Option(shortName = "m", description = "how to change the masses: recalibrate, idealize or keep the original mzs", defaultValue = "IDEALIZE")
    public MZ getMz();

    @Option(shortName = "n", description = "which peaks should be contained in the spectrum: all peaks for which a " +
            "POSSIBLE molecular formula exists or all peaks that are EXPLAINED in the tree.", defaultValue = "EXPLAINED")
    public NOISE getPeakFilter();

    @Unparsed
    public List<File> getFiles();

    @Option(shortName = "t", defaultValue = ".", description = "target directory for the output data")
    public File getTarget();

}
