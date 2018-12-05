AlgorithmProfile = default
# Must be one of 'IGNORE', 'IF_NECESSARY', 'BRUKER_ONLY', 'BRUKER_IF_NECESSARY' or ALWAYS'
IsotopeHandlingMs2 =BRUKER_ONLY

# Must be either 'LOCAL' or GLOBAL'
NormalizationType =


IsolationWindow.width =2
IsolationWindow.shift =0

# FormulaConstraints contain all constraints which reduce the size of all possible
# decompositions of a mass.
# It consists of:
# - allowed elements
# - boundaries for elements
# - constraints for formulas
#
# Important: The RDBE Filter is always added to the FormulaConstraints! If you don't want so:
# either exlude it
# explicitly by calling constraints.getFilters().clear(); or create the constraints using
# the constructor
# new FormulaConstraints(alphabet, new ArrayList());
#
# But in application, you probably want the RDBE filter always active. If you just want to change
# its limit, set
# it explicitly by calling new FormulaConstraints(alphabet, Arrays.asList(new
# ValenceFilter(-4)));
FormulaConstraints.alphabet =CHNOP[5]S
FormulaConstraints.valenceFilter =-0.5

MS1MassDeviation.allowedMassDeviation =10.0
MS1MassDeviation.standardMassDeviation =10.0
MS1MassDeviation.massDifferenceDeviation =5.0

MS2MassDeviation.allowedMassDeviation =10.0
MS2MassDeviation.standardMassDeviation =10.0

MedianNoiseIntensity =0.015

NumberOfCandidates =5

NumberOfCandidatesPerIon =-1

PossibleAdductSwitches =[M+H]+:{[M+Na]+}

# Can be attached to a Ms2Experiment or ProcessedInput. If PrecursorIonType is unknown,
# CSI:FingerID will use this
# object and for all different adducts.
PossibleAdducts =[M+H]+,[M]+,[M+K]+,[M+Na]+,[M+H-H2O]+,[M+Na2-H]+,[M+2K-H]+,[M+NH4]+,[M+H3O]+,[M+MeOH+H]+,[M+ACN+H]+,[M+2ACN+H]+,[M+IPA+H]+,[M+ACN+Na]+,[M+DMSO+H]+,[M-H]-,[M]-,[M+K-2H]-,[M+Cl]-,[M-H2O-H]-,[M+Na-2H]-,M+FA-H]-,[M+Br]-,[M+HAc-H]-,[M+TFA-H]-,[M+ACN-H]-

# if this annotation is set, recalibration is omited
# Must be either 'ALLOWED' or FORBIDDEN'
ForbidRecalibration =ALLOWED

# This class holds the information how to autodetect elements based on the given
# FormulaConstraints
# Note: during Validation this is compared to the molecular formula an may be changed
FormulaSettings.autoDetectionElements =S,Br,Cl,B,Se
FormulaSettings.allowIsotopeElementFiltering =true

# Determines how strong the isotope pattern score influences the final scoring
# Must be either 'DEFAULT' or DISABLED'
IsotopeScoring =DEFAULT

# If this annotation is set, Tree Builder will stop after reaching the given number of seconds
Timeout.secondsPerInstance =0
Timeout.secondsPerTree =0
IonGuessingMode = ADD_IONS
PossibleIons =
IsotopeHandling = BOTH
IntensityDeviation = 0.02