

# FingerprintPrediction

User/developer friendly parameter subset for the CSI:FingerID Fingerprint tool  Needs results from the Formula identification tool (Sirius).

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | Indicates whether the tool is enabled. |  [optional] |
|**useScoreThreshold** | **Boolean** | If true, an adaptive soft threshold will be applied to only compute Fingerprints for promising formula candidates  Enabling is highly recommended. |  [optional] |
|**alwaysPredictHighRefMatches** | **Boolean** | If true, fingerprints, compound classes and structures will be predicted for formula candidates whose  reference spectrum similarity is above Sirius.minReferenceMatchScoreToInject, no matter which  score threshold rules would otherwise apply.  If NULL default value will be used. |  [optional] |



