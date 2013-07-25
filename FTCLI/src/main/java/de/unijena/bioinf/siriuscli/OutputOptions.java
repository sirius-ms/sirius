package de.unijena.bioinf.siriuscli;

import com.lexicalscope.jewel.cli.Option;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 25.07.13
 * Time: 12:47
 * To change this template use File | Settings | File Templates.
 */
public interface OutputOptions {

    @Option(description = "Compute only trees with higher score than <value>", defaultToNull =true)
    public Double getLowerbound();

}
