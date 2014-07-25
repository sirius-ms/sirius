package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

import java.io.File;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 20.08.13
 * Time: 01:29
 * To change this template use File | Settings | File Templates.
 */
public interface SSPSBasicOptions extends EvalBasicOptions {

    @Option(shortName = "F")
    public boolean isNoFingerprint();

    @Option(shortName = "t", defaultValue = "ssps.csv")
    public File getTarget();

    @Option(shortName = "k", defaultValue = "10")
    public int getK();

    @Unparsed()
    public List<File> getInput();

}
