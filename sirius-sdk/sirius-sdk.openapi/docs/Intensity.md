

# Intensity

The MS1 intensity of a feature (aligned over runs), as peak height and as area under the chromatographic  peak, each in raw instrument counts and in percent of the strongest feature it is compared with.  <p>  The values in counts are the highest over the runs the feature was detected in, as in the quantification  table. Data imported without an LC-MS trace (peak lists, features imported through the API) has the height  of the precursor peak in its MS1 spectrum and no area.  <p>  The percentages compare the feature with the strongest feature its run has held - or, for a feature without  a run, with the strongest feature the project has held - and take the best run. The reference only ever  rises: after the strongest feature of a run is deleted, the remaining ones keep the percentage they had, so  none of them reaches 100.  <p>  Each value is null when it is unknown.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**apex** | **Double** | Peak height: the highest apex intensity over the runs the feature was detected in, or the precursor peak  height for data without an LC-MS trace. In raw instrument counts. |  [optional] |
|**auc** | **Double** | Area under the chromatographic peak: the highest over the runs the feature was detected in. In raw  instrument counts. Null for data without an LC-MS trace. |  [optional] |
|**apexPercent** | **Double** | Peak height in percent of the highest peak height its run has held, the best over its runs:  &lt;code&gt;intensity.apexPercent:[10 TO *]&lt;/code&gt; keeps the features that reach 10 % of that in at least one run.  A feature without a run is compared with the highest peak height the project has held. |  [optional] |
|**aucPercent** | **Double** | Area under the peak in percent of the highest area its run has held, the best over its runs.  Null for data without an LC-MS trace. |  [optional] |



