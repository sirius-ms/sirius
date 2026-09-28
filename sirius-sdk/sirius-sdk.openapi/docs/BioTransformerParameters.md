

# BioTransformerParameters


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**cyp450Mode** | **Cyp450Mode** |  |  |
|**p2Mode** | **P2Mode** |  |  |
|**useDB** | **Boolean** | Additionally look up known biotransformations in the online HMDB database (a third-party MySQL server  operated by the BioTransformer authors), instead of relying on rule-based prediction alone.  &lt;p&gt;  WARNING: when enabled, the first block of the InChIKey (the 2D skeleton) of every input structure and  of every predicted intermediate metabolite is sent to that external server over the internet. Do not  enable this for confidential structures. Requires internet access; the lookup fails without it.  Disabled by default. |  [optional] |
|**bioTransformerSequenceSteps** | [**List&lt;BioTransformerSequenceStep&gt;**](BioTransformerSequenceStep.md) | Specify BioTransformerSequenceSteps to be applied to input structures. MultiStep MetabolicTransformations can  only be used as singletons (list size of one). |  |



