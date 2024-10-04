

# CompoundClasses

Container class that holds the most likely compound class for different levels of each ontology for a  certain Compound/Feature/FormulaCandidate/PredictedFingerprint.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**npcPathway** | [**CompoundClass**](CompoundClass.md) |  |  [optional] |
|**npcSuperclass** | [**CompoundClass**](CompoundClass.md) |  |  [optional] |
|**npcClass** | [**CompoundClass**](CompoundClass.md) |  |  [optional] |
|**classyFireLineage** | [**List&lt;CompoundClass&gt;**](CompoundClass.md) | Most likely ClassyFire lineage from ordered from least specific to most specific class  classyFireLineage.get(classyFireLineage.size() - 1) gives the most specific ClassyFire compound class annotation |  [optional] |
|**classyFireAlternatives** | [**List&lt;CompoundClass&gt;**](CompoundClass.md) | Alternative ClassyFire classes with high probability that do not fit into the linage |  [optional] |



