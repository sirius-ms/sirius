package de.unijena.bioinf.fteval;

import com.lexicalscope.jewel.cli.Option;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kai
 * Date: 8/12/13
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AlignOpts extends EvalBasicOptions{

    @Option(shortName = "J")
    public boolean isNoMultijoin();

    @Option(shortName = "t", defaultValue = "matrix.csv")
    public String getTarget();

    @Option(shortName = "x", defaultToNull = true)
    public List<String> getXtra();

}
