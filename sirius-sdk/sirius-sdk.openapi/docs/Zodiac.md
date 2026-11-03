

# Zodiac

User/developer friendly parameter subset for the ZODIAC tool (network-based molecular formula re-ranking).  Needs results from the Formula/SIRIUS tool.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**enabled** | **Boolean** | Indicates whether the tool is enabled. |  [optional] |
|**consideredCandidatesAt300Mz** | **Integer** | Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds below 300 m/z. |  [optional] |
|**consideredCandidatesAt800Mz** | **Integer** | Maximum number of candidate molecular formulas (fragmentation trees computed by SIRIUS) per compound which are considered by ZODIAC for compounds above 800 m/z. |  [optional] |
|**runInTwoSteps** | **Boolean** | By default, ZODIAC runs a 2-step approach: first running only &#39;good quality compounds&#39;, and afterwards including the remaining ones. |  [optional] |
|**edgeFilterThresholds** | [**ZodiacEdgeFilterThresholds**](ZodiacEdgeFilterThresholds.md) |  |  [optional] |
|**gibbsSamplerParameters** | [**ZodiacEpochs**](ZodiacEpochs.md) |  |  [optional] |
|**librarySearchAnchors** | [**ZodiacLibraryScoring**](ZodiacLibraryScoring.md) |  |  [optional] |
|**analogueSearchAnchors** | [**ZodiacAnalogueNodes**](ZodiacAnalogueNodes.md) |  |  [optional] |



