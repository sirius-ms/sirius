

# Subscription


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**sid** | **String** | Unique identifier of this subscription |  [optional] |
|**subscriberId** | **String** | ID of the owner of the subscription.  This can be the ID of any SubscriptionOwner (e.g.  Group or  User)  depending on the level on which a subscription should be is valid. |  [optional] |
|**subscriberName** | **String** | Optional name of the owner of this subscription |  [optional] |
|**expirationDate** | **Date** |  |  [optional] |
|**startDate** | **Date** |  |  [optional] |
|**countQueries** | **Boolean** |  |  [optional] |
|**instanceLimit** | **Integer** | Limit of instances (features) that can be computed with this subscription |  [optional] |
|**instanceHashRecordingTime** | **Integer** | Hash is used to allow recomputing identical data without increasing counted instances (features).  The recording time is the amount of time an instance is memorized is |  [optional] |
|**maxQueriesPerInstance** | **Integer** | Maximum number of queries (e.g. prediction) that can be performed  for one instance before it is counted another time. |  [optional] |
|**maxUserAccounts** | **Integer** |  |  [optional] |
|**serviceUrl** | **String** |  |  [optional] |
|**description** | **String** |  |  [optional] |
|**name** | **String** |  |  [optional] |
|**tos** | **String** |  |  [optional] |
|**pp** | **String** |  |  [optional] |
|**allowedFeatures** | [**AllowedFeatures**](AllowedFeatures.md) |  |  [optional] |



