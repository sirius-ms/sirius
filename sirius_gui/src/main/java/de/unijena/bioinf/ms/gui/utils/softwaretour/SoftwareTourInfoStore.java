package de.unijena.bioinf.ms.gui.utils.softwaretour;

import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfo.LocationHorizontal;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfo.LocationVertical;
/**
 * This be convention holds the necessary component descriptions for the software tour. However, this is not enforced,
 * so there is no guarantee that the software tour information is not just provided directly where the class is instantiated.
 */
public class SoftwareTourInfoStore {




    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Software tour entry points ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String MainFrameTourKey = "de.unijena.bioinf.sirius.ui.tutorial.mainFrame";
    public static final String BatchComputeTourKey = "de.unijena.bioinf.sirius.ui.tutorial.computeDialog";

    public static final String DatabaseSearchTabTourKey = "de.unijena.bioinf.sirius.ui.tutorial.databaseTab";
    public static final String EpimetheusTabTourKey = "de.unijena.bioinf.sirius.ui.tutorial.epimetheusTab";



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Tour elements ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Main window tour /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////SiriusToolbar
    public static final SoftwareTourInfo ComputeAllButton = new SoftwareTourInfo("Here you can start computations on all features currently displayed in the feature list.", 10, LocationHorizontal.CENTER, LocationVertical.BELOW_BOTTOM);

    ////left feature list
    public static final SoftwareTourInfo CompoundListView = new SoftwareTourInfo( "This is the feature list. It lists all features that pass the current filter.", 1, LocationHorizontal.RIGHT_SPACE,  LocationVertical.CENTER);
    public static final SoftwareTourInfo OpenFilterPanelButton = new SoftwareTourInfo("This open the filter dialog.<br>Here, you can filter the features by m/z, RT, quality and more.<br>You can even filter based on results such as database affiliation.", 5, LocationHorizontal.RIGHT_SPACE, LocationVertical.TOP_TOP_ALIGN);


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Batch compute tour /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Note: we could add a separate tour for the advanced mode. But probably advance users don't need it?
    public static final SoftwareTourInfo BatchCompute_PresetDropDown = new SoftwareTourInfo("Presets specify the parameters of your workflow", 1, LocationHorizontal.LEFT_ALIGN_TO_RIGHT, LocationVertical.BELOW_BOTTOM);
    //todo check if tour description and tool tip should be identical, or if one should be more comprehensive
    public static final SoftwareTourInfo BatchCompute_Formula = new SoftwareTourInfo("Activate to compute molecular formulas.",  2, LocationHorizontal.RIGHT_SPACE, LocationVertical.CENTER);
    public static final SoftwareTourInfo BatchCompute_ZODIAC = new SoftwareTourInfo("Activate to optimize molecular formula annotations.<br>" +
                                                                    "This reranks molecular formula annotations from the previous step based on similarities between compounds in the whole dataset.<br>" +
                                                                    "This does not generate new annotations. Please, first read documentation for prerequisites for this method.",  2, LocationHorizontal.RIGHT_SPACE, LocationVertical.CENTER);
    public static final SoftwareTourInfo BatchCompute_FingerprintCanopus = new SoftwareTourInfo("Activate to predict molecular fingerprints and compounds classes.",  2, LocationHorizontal.RIGHT_SPACE, LocationVertical.CENTER);
    public static final SoftwareTourInfo BatchCompute_Fingerblast = new SoftwareTourInfo("Activate to perform structure database search.",  2, LocationHorizontal.RIGHT_SPACE, LocationVertical.CENTER);
    public static final SoftwareTourInfo BatchCompute_MsNovelist = new SoftwareTourInfo("Activate to annotate molecular structures independent of a database.<br>Molecular structures are generated based on the predicted molecular fingerprint.", 2, LocationHorizontal.LEFT_SPACE_TO_LEFT, LocationVertical.CENTER);


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Database search tour /////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static final SoftwareTourInfo DatabaseSearch_CSIScore = new SoftwareTourInfo("The CSI:FingerID score is used to rank the molecular structure candidates.<br>It is a log-likelihood. The best possible score is 0 the worst -infinity.", 1, LocationHorizontal.LEFT_ALIGN_TO_RIGHT, LocationVertical.BELOW_BOTTOM);
    public static final SoftwareTourInfo DatabaseSearch_Rank = new SoftwareTourInfo("This is the rank of the structure hit. If multiple top candidates are highlighted green this means that this are structurally highly similar - the confidence score would consider any of this as top hit.<br>Green-highlighted rows in other result tabs all link to these top hits.", 2, LocationHorizontal.RIGHT_SPACE, LocationVertical.CENTER);
    public static final SoftwareTourInfo DatabaseSearch_Source = new SoftwareTourInfo("The structure hit can be found in the displayed databases. Even database not selected for search are displayed.", 3, LocationHorizontal.CENTER, LocationVertical.BELOW_BOTTOM);



    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Epimetheus tour /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static final SoftwareTourInfo Epimetheus_SpectralVisualization = new SoftwareTourInfo("This panel allows you to manually validate molecular structure hits by assessing potential fragment explanations.<br>This does not consider rearrangements", 2, LocationHorizontal.CENTER, LocationVertical.ON_TOP);
}
