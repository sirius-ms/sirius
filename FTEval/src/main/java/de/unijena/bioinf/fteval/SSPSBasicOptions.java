package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 20.08.13
 * Time: 01:29
 * To change this template use File | Settings | File Templates.
 */
public interface SSPSBasicOptions extends EvalBasicOptions{

    @Option(shortName = "F")
    public boolean isNoFingerprint();

}
