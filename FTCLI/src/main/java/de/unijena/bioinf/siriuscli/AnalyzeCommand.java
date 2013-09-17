package de.unijena.bioinf.siriuscli;

import de.unijena.bioinf.FragmentationTree.analyze.Analyzer;

/**
 * Created with IntelliJ IDEA.
 * User: kaidu
 * Date: 17.09.13
 * Time: 11:53
 * To change this template use File | Settings | File Templates.
 */
public class AnalyzeCommand implements Command {
    @Override
    public String getDescription() {
        return "analyze mass spectra";
    }

    @Override
    public String getName() {
        return "analyze";
    }

    @Override
    public void run(String[] args) {
        Analyzer.main(args);
    }

    @Override
    public String getVersion() {
        return Analyzer.VERSION_STRING;
    }
}
