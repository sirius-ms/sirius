

# FormulaCandidate

Molecular formula candidate that holds a unique identifier (molecular formula + adduct).  It can be extended with optional scoring metrics and the raw results  such as fragmentation trees and simulated isotope pattern.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**formulaId** | **String** | Unique identifier of this formula candidate |  [optional] |
|**molecularFormula** | **String** | molecular formula of this formula candidate |  [optional] |
|**adduct** | **String** | Adduct of this formula candidate |  [optional] |
|**rank** | **Integer** |  |  [optional] |
|**siriusScoreNormalized** | **Double** | Normalized Sirius Score of the formula candidate.  If NULL result is not available |  [optional] |
|**siriusScore** | **Double** | Sirius Score (isotope + tree score) of the formula candidate.  If NULL result is not available |  [optional] |
|**isotopeScore** | **Double** |  |  [optional] |
|**treeScore** | **Double** |  |  [optional] |
|**zodiacScore** | **Double** | Zodiac Score of the formula candidate.  If NULL result is not available |  [optional] |
|**numOfExplainedPeaks** | **Integer** |  |  [optional] |
|**numOfExplainablePeaks** | **Integer** |  |  [optional] |
|**totalExplainedIntensity** | **Double** |  |  [optional] |
|**medianMassDeviation** | [**Deviation**](Deviation.md) |  |  [optional] |
|**fragmentationTree** | [**FragmentationTree**](FragmentationTree.md) |  |  [optional] |
|**annotatedSpectrum** | [**AnnotatedSpectrum**](AnnotatedSpectrum.md) |  |  [optional] |
|**isotopePatternAnnotation** | [**IsotopePatternAnnotation**](IsotopePatternAnnotation.md) |  |  [optional] |
|**lipidAnnotation** | [**LipidAnnotation**](LipidAnnotation.md) |  |  [optional] |
|**predictedFingerprint** | **List&lt;Double&gt;** | Probabilistic molecular fingerprint predicted by CSI:FingerID |  [optional] |
|**compoundClasses** | [**CompoundClasses**](CompoundClasses.md) |  |  [optional] |
|**canopusPrediction** | [**CanopusPrediction**](CanopusPrediction.md) |  |  [optional] |



