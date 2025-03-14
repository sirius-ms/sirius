package de.unijena.bioinf.ms.gui.utils.softwaretour;

import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfo.LocationHorizontal;
import de.unijena.bioinf.ms.gui.utils.softwaretour.SoftwareTourInfo.LocationVertical;
/**
 * This holds the necessary component descriptions for the software tour. However, this is not enforced,
 * so there is no guarantee that the software tour information is not just provided directly where the class is instantiated.
 */
public class SoftwareTourInfoStore {

    public static final String TOUR_ELEMENT_PROPERTY_KEY = "SIRIUS software tour element";

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Software tour entry points ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static final String MainFrameTourKey = "de.unijena.bioinf.sirius.ui.tutorial.mainFrame";
    public static final String BatchComputeTourKey = "de.unijena.bioinf.sirius.ui.tutorial.computeDialog";

    public static final String DatabaseSearchTabTourKey = "de.unijena.bioinf.sirius.ui.tutorial.databaseTab";
    public static final String DeNovoStructuresTabTourKey = "de.unijena.bioinf.sirius.ui.tutorial.deNovoStructuresTab";
    public static final String EpimetheusTabTourKey = "de.unijena.bioinf.sirius.ui.tutorial.epimetheusTab";


    //INCLUDE ALL TO TOUR KEYS HERE!
    public static final String[] AllTourKeys = new String[] {MainFrameTourKey, BatchComputeTourKey, DatabaseSearchTabTourKey, EpimetheusTabTourKey};


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
    public static final SoftwareTourInfo Log = new SoftwareTourInfo("This provides information, warnings and error on all jobs, including import and annotation tasks.<br>Information on individual jobs that failed can also be specifically selected using the \"Jobs\" panel.", 10, LocationHorizontal.RIGHT_ALIGN, LocationVertical.BELOW_BOTTOM);

    ////left feature list
    public static final SoftwareTourInfo CompoundListView = new SoftwareTourInfo( "This is the feature list. It displays all features that pass the current filter.<br>The right-click menu allows to sort or perform computations on a set of selected features.", 2, LocationHorizontal.RIGHT_SPACE,  LocationVertical.CENTER);
    public static final SoftwareTourInfo OpenFilterPanelButton = new SoftwareTourInfo("This opens the filter dialog.<br>Here, you can filter the features by m/z, RT, quality and more.<br>You can even filter based on results such as database affiliation of structure hits.", 3, LocationHorizontal.RIGHT_SPACE, LocationVertical.TOP_TOP_ALIGN);


    ///landing page
    public static final SoftwareTourInfo LandingPage_AccountInfo = new SoftwareTourInfo( "Create an account or log in to an existing to use the full feature set of SIRIUS.", 1, LocationHorizontal.LEFT_ALIGN_TO_RIGHT,  LocationVertical.BELOW_BOTTOM); //todo is this worth a tutorial message?
    public static final SoftwareTourInfo LandingPage_WebConnectionInfo = new SoftwareTourInfo( "See if your SIRIUS client established a connection to the webservice.<br>If you encounter general connection issues, please refer to the documentation and check with your IT department.", 1, LocationHorizontal.RIGHT_ALIGN,  LocationVertical.BELOW_BOTTOM);
    public static final SoftwareTourInfo LandingPage_GetStartedInfo = new SoftwareTourInfo( "Get started with tutorials and documentation.<br>Get an overview and in-depth information to best practices and parameters.", 1, LocationHorizontal.RIGHT_SPACE,  LocationVertical.CENTER);
    public static final SoftwareTourInfo LandingPage_CommunityInfo = new SoftwareTourInfo( "Interact with other SIRIUS users, ask question or report bugs.", 1, LocationHorizontal.LEFT_SPACE_TO_LEFT,  LocationVertical.CENTER);


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
    public static final SoftwareTourInfo DatabaseSearch_CSIScore = new SoftwareTourInfo("The CSI:FingerID score is used to rank the molecular structure candidates.<br>It is a log-likelihood. The best possible score is 0 the worst -infinity.", 1, LocationHorizontal.LEFT_ALIGN_TO_RIGHT, LocationVertical.BELOW_BOTTOM, DatabaseSearchTabTourKey);
    public static final SoftwareTourInfo DatabaseSearch_Rank = new SoftwareTourInfo("This is the rank of the structure hit.<br>If multiple top candidates are highlighted green this means they are structurally highly similar - the confidence score would consider any of this as top hit.<br>Green-highlighted rows in other result tabs all link to these top hits.", 2, LocationHorizontal.RIGHT_SPACE, LocationVertical.CENTER, DatabaseSearchTabTourKey);
    public static final SoftwareTourInfo DatabaseSearch_Source = new SoftwareTourInfo("The structure hit can be found in the displayed databases. Even databases that have not been selected for search are displayed.", 3, LocationHorizontal.CENTER, LocationVertical.BELOW_BOTTOM, DatabaseSearchTabTourKey);

    public static final SoftwareTourInfo DatabaseSearch_Substructures = new SoftwareTourInfo("Each square represents a molecular property (substructure) from the fingerprint that is present in the candidate.<br>Green squares agree with the predicted fingerprint.<br>Pink squares disagree with the predicted fingerprint.<br>Larger squares correspond to properties that can be well predicted (high predictor quality).", 4, LocationHorizontal.LEFT_ALIGN_TO_RIGHT, LocationVertical.BELOW_BOTTOM, DatabaseSearchTabTourKey);

    public static final SoftwareTourInfo DatabaseSearch_DatabaseFilter = new SoftwareTourInfo("Filter candidate structure by database...", 10, LocationHorizontal.LEFT_ALIGN_TO_RIGHT, LocationVertical.BELOW_BOTTOM, DatabaseSearchTabTourKey);
    public static final SoftwareTourInfo DatabaseSearch_SubstructureFilter = new SoftwareTourInfo("... or by a substructure you selected via the \"substructures\" squares", 10, LocationHorizontal.LEFT_ALIGN_TO_RIGHT, LocationVertical.BELOW_BOTTOM, DatabaseSearchTabTourKey);
    public static final SoftwareTourInfo DatabaseSearch_TextFilter = new SoftwareTourInfo("... or by text, e.g. name, molecular formula or inchi key", 10, LocationHorizontal.LEFT_ALIGN_TO_RIGHT, LocationVertical.BELOW_BOTTOM, DatabaseSearchTabTourKey);


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// De Novo structures tour /////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static final SoftwareTourInfo DeNovo_Source = new SoftwareTourInfo("The de novo structures view is very similar to the database structures view.<br>A \"De Novo\" label as part of the sources indicates that this structure was generated de novo.<br>De novo generated structures can also be contained in databases.", 1, LocationHorizontal.CENTER, LocationVertical.BELOW_BOTTOM, DeNovoStructuresTabTourKey);
    public static final SoftwareTourInfo DeNovo_Filter = new SoftwareTourInfo("By default, database structures are shown.<br>Toggle here to (un-)hide those.", 2, LocationHorizontal.RIGHT_ALIGN, LocationVertical.BELOW_BOTTOM, DeNovoStructuresTabTourKey);

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //// Epimetheus tour /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static final SoftwareTourInfo Epimetheus_SpectralVisualization = new SoftwareTourInfo("This panel allows you to manually validate molecular structure hits by assessing potential fragment explanations.<br>This does not consider rearrangements", 2, LocationHorizontal.CENTER, LocationVertical.ON_TOP);
}
