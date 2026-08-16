

# SearchableField

Describes one field of the search index that can be used in lucene search queries (searchQuery parameter).  Use this information to build valid queries, e.g. which fields support range queries ([300 TO 400])  and which support word based (full text) search.

## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**name** | **String** | Name of the field to be used in search queries, e.g. &lt;code&gt;ionMass:[300 TO 400]&lt;/code&gt;.  Nested fields are addressed with dot notation. Dynamic, map-like fields are reported as concrete,  directly usable field names - one entry per key currently present in the project index - rather  than a wildcard template, e.g. &lt;code&gt;tags.&amp;lt;tagName&amp;gt;&lt;/code&gt;, an element symbol in  &lt;code&gt;...molecularFormula.&amp;lt;element&amp;gt;&lt;/code&gt;, or a database id in  &lt;code&gt;topAnnotations.matchedDatabases.&amp;lt;dbId&amp;gt;&lt;/code&gt;. |  |
|**fieldType** | **SearchableFieldType** |  |  |
|**fullTextSearch** | **Boolean** | If true, the field content is split into words and can be searched word by word (full text search).  If false, TEXT fields only match as exact terms (though wildcards and regex are still possible). |  [optional] |
|**sortable** | **Boolean** | If true, search results can be sorted by this field. |  [optional] |
|**defaultSearchField** | **Boolean** | If true, this field is searched when a query term does not specify a field name. |  [optional] |
|**possibleValues** | **List&lt;String&gt;** | The values this field can take, exactly as they are indexed, or null if the field accepts free text.  &lt;p&gt;  Reported for ENUM and BOOLEAN fields, and for TEXT fields that hold a closed vocabulary. Note that for  those the indexed value is not necessarily the name you would expect: the chromatography of a run, for  example, is indexed as &#39;Liquid Chromatography&#39;, not as &#39;LC&#39;. Use these values as offered rather than  constructing them. |  [optional] |
|**description** | **String** | Optional human-readable description of the field content, as shown in the API documentation.  Plain text suitable for direct display (e.g. tooltips); deliberate paragraph breaks are newlines. |  [optional] |
|**significantSuffixLength** | **Integer** | How many trailing dot-separated segments of the field name carry its meaning, for compact  display. &lt;code&gt;1&lt;/code&gt; for a normal field (show the terminal segment, e.g. &lt;code&gt;lipid&lt;/code&gt;); &lt;code&gt;2&lt;/code&gt;  for a dynamic map key (show field + key, e.g. &lt;code&gt;matchedDatabases.GNPS&lt;/code&gt;,  &lt;code&gt;qualities.PEAK_QUALITY&lt;/code&gt;, &lt;code&gt;tags.pfas&lt;/code&gt;); larger for multi-segment keys. Clients may  show the last &lt;code&gt;significantSuffixLength&lt;/code&gt; name segments as a short label; the full field name  always stays authoritative for queries. |  |



