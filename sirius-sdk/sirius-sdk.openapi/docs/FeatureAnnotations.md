

# FeatureAnnotations

Summary of the results of a feature (aligned over runs). Can be added to a AlignedFeature.  The different annotation fields within this summary object are null if the corresponding  feature does not contain the represented results. If fields are non-null  the corresponding result has been computed but might still be empty.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**formulaAnnotation** | [**FormulaCandidate**](FormulaCandidate.md) |  |  [optional] |
|**structureAnnotation** | [**StructureCandidateScored**](StructureCandidateScored.md) |  |  [optional] |
|**compoundClassAnnotation** | [**CompoundClasses**](CompoundClasses.md) |  |  [optional] |
|**confidenceExactMatch** | **Double** | Confidence Score that represents the confidence whether the top hit is correct. |  [optional] |
|**confidenceApproxMatch** | **Double** | Confidence Score that represents the confidence whether the top hit or a very similar hit (estimated by MCES distance) is correct. |  [optional] |
|**expansiveSearchState** | **ConfidenceMode** |  |  [optional] |
|**specifiedDatabases** | **List&lt;String&gt;** | List of databases that have been specified by for structure db search. Null if no structure db search has been performed. |  [optional] |
|**expandedDatabases** | **List&lt;String&gt;** | List of databases that have been used to expand search space during expansive search. Null if no structure db search has been performed. |  [optional] |



