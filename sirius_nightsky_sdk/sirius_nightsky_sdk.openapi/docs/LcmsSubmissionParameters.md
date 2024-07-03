

# LcmsSubmissionParameters


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**alignLCMSRuns** | **Boolean** | Specifies whether LC/MS runs should be aligned |  [optional] |
|**noise** | **Double** | Features must be larger than &lt;value&gt; * detected noise level. |  [optional] |
|**persistence** | **Double** | Features must have larger persistence (intensity above valley) than &lt;value&gt; * max trace intensity. |  [optional] |
|**merge** | **Double** | Merge neighboring features with valley less than &lt;value&gt; * intensity. |  [optional] |
|**filter** | **DataSmoothing** |  |  [optional] |
|**gaussianSigma** | **Double** | Sigma (kernel width) for gaussian filter algorithm. |  [optional] |
|**waveletScale** | **Integer** | Number of coefficients for wavelet filter algorithm. |  [optional] |
|**waveletWindow** | **Double** | Wavelet window size (%) for wavelet filter algorithm. |  [optional] |



