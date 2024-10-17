

# FingerprintPrediction

User/developer friendly parameter subset for the CSI:FingerID Fingerprint tool  Needs results from Formula/SIRIUS Tool

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**useScoreThreshold** | **Boolean** | If true, an adaptive soft threshold will be applied to only compute Fingerprints for promising formula candidates  Enabling is highly recommended. |  [optional] |
|**alwaysPredictHighRefMatches** | **Boolean** | If true Fingerprint/Classes/Structures will be predicted for formulas candidates with  reference spectrum similarity &gt; Sirius.minReferenceMatchScoreToInject will be predicted no matter which  score threshold rules apply.  If NULL default value will be used. |  [optional] |



