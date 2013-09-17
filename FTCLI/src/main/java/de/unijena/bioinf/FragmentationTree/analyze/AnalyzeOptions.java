package de.unijena.bioinf.FragmentationTree.analyze;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;
import de.unijena.bioinf.sirius.cli.BasicOptions;
import de.unijena.bioinf.sirius.cli.BasicProfileOptions;

import java.io.File;
import java.util.List;

public interface AnalyzeOptions extends BasicOptions, BasicProfileOptions {

    @Option(shortName = "n", longName = {"number", "formula"}, defaultValue = "")
    public SelectionOption getNumber();

    @Unparsed
    public List<String> getInput();

    @Option(shortName = "t", defaultValue = ".")
    public File getTarget();
}
