

# ConsensusAnnotationsCSI


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**molecularFormula** | **String** | Molecular formula of the consensus annotation  Might be null if no consensus formula is available. |  [optional] |
|**compoundClasses** | [**CompoundClasses**](CompoundClasses.md) |  |  [optional] |
|**supportingFeatureIds** | **List&lt;String&gt;** | FeatureIds where the topAnnotation supports this annotation. |  [optional] |
|**selectionCriterion** | **ConsensusCriterionCSI** |  |  [optional] |
|**csiFingerIdStructure** | [**StructureCandidate**](StructureCandidate.md) |  |  [optional] |
|**confidenceExactMatch** | **Double** | Confidence value that represents the certainty that reported consensus structure is exactly the measured one  If multiple features support this consensus structure the maximum confidence is reported |  [optional] |
|**confidenceApproxMatch** | **Double** | Confidence value that represents the certainty that the exact consensus structure or a very similar  structure (e.g. measured by Maximum Common Edge Subgraph Distance) is the measured one.  If multiple features support this consensus structure the maximum confidence is reported |  [optional] |



