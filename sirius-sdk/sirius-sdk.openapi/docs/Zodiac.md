

# Zodiac

User/developer friendly parameter subset for the ZODIAC tool (Network base molecular formula re-ranking).  Needs results from Formula/SIRIUS Tool

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | tags whether the tool is enabled |  [optional] |
|**consideredCandidatesAt300Mz** | **Integer** | Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds below 300 m/z. |  [optional] |
|**consideredCandidatesAt800Mz** | **Integer** | Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds above 800 m/z. |  [optional] |
|**runInTwoSteps** | **Boolean** | As default ZODIAC runs a 2-step approach. First running &#39;good quality compounds&#39; only, and afterwards including the remaining. |  [optional] |
|**edgeFilterThresholds** | [**ZodiacEdgeFilterThresholds**](ZodiacEdgeFilterThresholds.md) |  |  [optional] |
|**gibbsSamplerParameters** | [**ZodiacEpochs**](ZodiacEpochs.md) |  |  [optional] |



