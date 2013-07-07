package de.unijena.bioinf.GCMSAnalysis.cli;

import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

public interface Options {

    @Option(longName = "help", shortName = "h", helpRequest = true, description = "")
    public boolean getHelp();

    @Unparsed
    public String getInputPath();

    //molecular ion peak options
    @Option(shortName = "mp", defaultValue = "known", description = "Molecular ion peak option: known|unknown " +
            "\nIs the molecule peak known or unknown? " +
            "\nIf the molecule is unknown, information from the input file is ignored. \nUnknown by default.")
    public String getMPKnown();

    /*
    @Option(shortName = "pm", defaultToNull = true, description = "--pm <double> \nThe mass of the molecule peak. \nOnly works in combination with -mp known.")
    public Double getParentMass();

    @Option(shortName = "pmd", defaultToNull = true, description = "Molecular formula of the molecular ion. \nOnly works in combination with -mp known.")
    public String getMolecularIonFormula();
    */

    //algorithm options
    @Option(shortName = "peaks", defaultToNull = true, description = "-peaks <int> \n"+"\tNumber of Peaks to score. \n"+"\t For formula identification this is a combination of:\n" +
            "\t\t the <int> most intense peaks\n" +
            "\t\t the <int> best peaks with mass*log(relIntensity)\n" +
            "\t\t the <int> best peaks with mass*log(relIntensity) int he upper mass range\n"
            //+ "	 For tree computation this is only the <int> most intense peaks." //todo that's no longer done
            )
    public Integer getPeaksToScore();


    @Option(shortName = "iso", description = "Remove isotope peaks.")
    public boolean isRemoveIsotopePeaks();

    @Option(shortName = "filter", defaultValue = "0.0", description = "Remove peaks with relative intensity below <double> %.")
    public double getFilterLowIntensity();

    @Option(shortName = "forest", description = "Compute a forest (rooted in dummy node).")
    public boolean isForest();

    @Option(shortName = "ilp", description = "Use the ILP for computation.\n " +
            "\tRecommended for full tree computation with known parent.\n" +
            "\tNot recommended for molecular formula identification.")
    public boolean isILP();


    //alphabet options
    @Option(shortName = "elh", description = "Add all halogens (F, Br, I, Cl) to the alphabet.")
    public boolean isAddHalogens();

    @Option(shortName = "elc", description = "Add Cl to the alphabet.")
    public boolean isAddCl();

    @Option(shortName = "derivate", description = "Add both derivatization groups (Tms, Pfb) to the alphabet.")
    public boolean isAddDerivate();

    @Option(shortName = "tms", description = "Only add TMS derivatization.")
    public boolean isAddTms();

    @Option(shortName = "pfb", description = "Allow only PFB derivatization.")
    public boolean isAddPfb();

    //output options
    @Option(shortName = "trees", defaultValue = "best", description = "Number of output trees: <int>|all|best. (Default: 1)")
    public String getOutputSize();

    @Option(shortName = "o", longName = "outDir", defaultToNull = true, description = "Specify the output folder.")
    public String getOutputDir();



}
