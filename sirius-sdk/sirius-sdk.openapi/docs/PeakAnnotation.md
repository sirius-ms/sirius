

# PeakAnnotation


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**fragmentId** | **Integer** | Identifier of the peak/fragment. Can be used to map fragments and peaks  among fragmentation trees and spectra. |  |
|**molecularFormula** | **String** | Molecular formula that has been annotated to this peak |  [optional] |
|**adduct** | **String** | Adduct that has been annotated to this peak |  [optional] |
|**exactMass** | **Double** | Exact mass of the annotated molecular formula and adduct |  [optional] |
|**massDeviationMz** | **Double** | Absolute mass deviation of the exact mass to the measured peak mass in mDa |  [optional] |
|**massDeviationPpm** | **Double** | Relative mass deviation of the exact mass to the measured peak mass in ppm |  [optional] |
|**recalibratedMassDeviationMz** | **Double** | Absolute mass deviation of the exact mass to the recalibrated peak mass in mDa |  [optional] |
|**recalibratedMassDeviationPpm** | **Double** | Relative mass deviation of the exact mass to the recalibrated peak mass in ppm |  [optional] |
|**parentPeak** | [**ParentPeak**](ParentPeak.md) |  |  [optional] |
|**substructureAtoms** | **List&lt;Integer&gt;** | Array/List of indices of the atoms of the structure candidate that are part of this fragments substructure  (highlighted atoms) |  [optional] |
|**substructureBonds** | **List&lt;Integer&gt;** | Array/List of indices of the bonds of the structure candidate that are part of this fragments substructure  (highlighted bonds)   Null if substructure annotation not available or not requested. |  [optional] |
|**substructureBondsCut** | **List&lt;Integer&gt;** | Array/List of indices of the bonds of the structure candidate that need to be cut to produce this fragments  substructure (highlighted cutted bonds).   Null if substructure annotation not available or not requested. |  [optional] |
|**substructureScore** | **Float** | This score roughly reflects the probability of this fragment forming.   This is the score of the path from root to this node which has the maximal score or \&quot;profit\&quot;.  The score of a path is equal to the sum of scores of its contained fragments and edges.  Note: Refers to &#39;totalScore&#39; in CombinatorialNode   Null if substructure annotation not available or not requested. |  [optional] |
|**hydrogenRearrangements** | **Integer** | Number of hydrogens rearrangements needed to match the substructure to the fragment formula.   Null if substructure annotation not available or not requested. |  [optional] |



