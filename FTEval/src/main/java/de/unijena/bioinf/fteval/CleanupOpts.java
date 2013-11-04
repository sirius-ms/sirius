package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 10/10/13
 * Time: 6:40 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CleanupOpts extends EvalBasicOptions {

    @Option(shortName = "d", description = "delete files which tree contains less than <n> nodes", defaultValue = "0")
    public int getEdgeLimit();

}
