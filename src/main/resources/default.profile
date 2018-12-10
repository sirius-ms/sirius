AlgorithmProfile = default
# Must be one of 'IGNORE', 'IF_NECESSARY', 'BRUKER_ONLY', 'BRUKER_IF_NECESSARY' or ALWAYS'
IsotopeHandlingMs2 =BRUKER_ONLY

# Must be either 'LOCAL' or GLOBAL'
NormalizationType = GLOBAL

# default alphabet
FormulaConstraints.alphabet = CHNOP[5]S

# minimal allowed RDBE value
FormulaConstraints.valenceFilter =-0.5

# MS1 mass deviation in ppm
MS1MassDeviation.allowedMassDeviation =10.0
MS1MassDeviation.standardMassDeviation =10.0
MS1MassDeviation.massDifferenceDeviation =5.0

# MS/MS mass deviation in ppm
MS2MassDeviation.allowedMassDeviation =10.0
MS2MassDeviation.standardMassDeviation =10.0

MedianNoiseIntensity =0.015

# number of suboptimal results to keep
NumberOfCandidates = 5

# additional to NumberOfCandidates, keep for each ion mode the top k suboptimal results
NumberOfCandidatesPerIon = 0

#
PossibleAdductSwitches =[M+Na]+:{[M+H]+}

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