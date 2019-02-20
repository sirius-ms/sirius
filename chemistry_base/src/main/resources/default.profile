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
MS1MassDeviation.allowedMassDeviation =10.0 ppm
MS1MassDeviation.standardMassDeviation =10.0 ppm
MS1MassDeviation.massDifferenceDeviation =5.0 ppm

# MS/MS mass deviation in ppm
MS2MassDeviation.allowedMassDeviation =10.0 ppm
MS2MassDeviation.standardMassDeviation =10.0 ppm

MedianNoiseIntensity =0.015

# number of suboptimal results to keep
NumberOfCandidates = 10

# additional to NumberOfCandidates, keep for each ion mode the top k suboptimal results
NumberOfCandidatesPerIon = 1

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
FormulaSettings.detectable =S,Br,Cl,B,Se
FormulaSettings.enforced =C,H,N,O,P
FormulaSettings.fallback=S

# Determines how strong the isotope pattern score influences the final scoring
IsotopeSettings.multiplier = 1
IsotopeSettings.filter = True

# If this annotation is set, Tree Builder will stop after reaching the given number of seconds
Timeout.secondsPerInstance =0
Timeout.secondsPerTree =0

AdductSettings.enforced = ,
AdductSettings.fallback = [M+H]+,[M+Na]+,[M+K]+,[M-H]-,[M+Cl]-
AdductSettings.detectable = [M+H]+,[M-H2O+H]+, [M+NH3+H]+,[M+Na]+,[M+K]+,[M-H]-,[M+Cl]-

NoiseThresholdSettings.intensityThreshold = 0.002
NoiseThresholdSettings.maximalNumberOfPeaks = 60
# this determines which base peak should be used for determining relative intensities. We recommend using NO_PRECURSOR,
# which will ignore the precursor peak. Other possible values are LARGEST or SECOND_LARGEST. Note, that with LARGEST
# you might lost all peaks in the spectrum whenever the precursor peak is very large.
NoiseThresholdSettings.basePeak = NOT_PRECURSOR
# use this property to implement a absolute intensity threshold
NoiseThresholdSettings.absoluteThreshold = 0

IntensityDeviation = 0.02