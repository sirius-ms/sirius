/*
 *  This file is part of the SIRIUS libraries for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2024 Bright Giant GmbH
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 *  https://openapi-generator.tech
 *  Do not edit the class manually.
 */


package io.sirius.ms.sdk.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Subscription
 */
@JsonPropertyOrder({
  Subscription.JSON_PROPERTY_SID,
  Subscription.JSON_PROPERTY_SUBSCRIBER_ID,
  Subscription.JSON_PROPERTY_SUBSCRIBER_NAME,
  Subscription.JSON_PROPERTY_EXPIRATION_DATE,
  Subscription.JSON_PROPERTY_START_DATE,
  Subscription.JSON_PROPERTY_COUNT_QUERIES,
  Subscription.JSON_PROPERTY_INSTANCE_LIMIT,
  Subscription.JSON_PROPERTY_INSTANCE_HASH_RECORDING_TIME,
  Subscription.JSON_PROPERTY_MAX_QUERIES_PER_INSTANCE,
  Subscription.JSON_PROPERTY_MAX_USER_ACCOUNTS,
  Subscription.JSON_PROPERTY_SERVICE_URL,
  Subscription.JSON_PROPERTY_DESCRIPTION,
  Subscription.JSON_PROPERTY_NAME,
  Subscription.JSON_PROPERTY_TOS,
  Subscription.JSON_PROPERTY_PP
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class Subscription {
  public static final String JSON_PROPERTY_SID = "sid";
  private String sid;

  public static final String JSON_PROPERTY_SUBSCRIBER_ID = "subscriberId";
  private String subscriberId;

  public static final String JSON_PROPERTY_SUBSCRIBER_NAME = "subscriberName";
  private String subscriberName;

  public static final String JSON_PROPERTY_EXPIRATION_DATE = "expirationDate";
  private Date expirationDate;

  public static final String JSON_PROPERTY_START_DATE = "startDate";
  private Date startDate;

  public static final String JSON_PROPERTY_COUNT_QUERIES = "countQueries";
  private Boolean countQueries;

  public static final String JSON_PROPERTY_INSTANCE_LIMIT = "instanceLimit";
  private Integer instanceLimit;

  public static final String JSON_PROPERTY_INSTANCE_HASH_RECORDING_TIME = "instanceHashRecordingTime";
  private Integer instanceHashRecordingTime;

  public static final String JSON_PROPERTY_MAX_QUERIES_PER_INSTANCE = "maxQueriesPerInstance";
  private Integer maxQueriesPerInstance;

  public static final String JSON_PROPERTY_MAX_USER_ACCOUNTS = "maxUserAccounts";
  private Integer maxUserAccounts;

  public static final String JSON_PROPERTY_SERVICE_URL = "serviceUrl";
  private String serviceUrl;

  public static final String JSON_PROPERTY_DESCRIPTION = "description";
  private String description;

  public static final String JSON_PROPERTY_NAME = "name";
  private String name;

  public static final String JSON_PROPERTY_TOS = "tos";
  private String tos;

  public static final String JSON_PROPERTY_PP = "pp";
  private String pp;

  public Subscription() {
  }

  public Subscription sid(String sid) {
    
    this.sid = sid;
    return this;
  }

   /**
   * Unique identifier of this subscription
   * @return sid
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getSid() {
    return sid;
  }


  @JsonProperty(JSON_PROPERTY_SID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSid(String sid) {
    this.sid = sid;
  }

  public Subscription subscriberId(String subscriberId) {
    
    this.subscriberId = subscriberId;
    return this;
  }

   /**
   * ID of the owner of the subscription.  This can be the ID of any SubscriptionOwner (e.g.  Group or  User)  depending on the level on which a subscription should be is valid.
   * @return subscriberId
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SUBSCRIBER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getSubscriberId() {
    return subscriberId;
  }


  @JsonProperty(JSON_PROPERTY_SUBSCRIBER_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSubscriberId(String subscriberId) {
    this.subscriberId = subscriberId;
  }

  public Subscription subscriberName(String subscriberName) {
    
    this.subscriberName = subscriberName;
    return this;
  }

   /**
   * Optional name of the owner of this subscription
   * @return subscriberName
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SUBSCRIBER_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getSubscriberName() {
    return subscriberName;
  }


  @JsonProperty(JSON_PROPERTY_SUBSCRIBER_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSubscriberName(String subscriberName) {
    this.subscriberName = subscriberName;
  }

  public Subscription expirationDate(Date expirationDate) {
    
    this.expirationDate = expirationDate;
    return this;
  }

   /**
   * Get expirationDate
   * @return expirationDate
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EXPIRATION_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Date getExpirationDate() {
    return expirationDate;
  }


  @JsonProperty(JSON_PROPERTY_EXPIRATION_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setExpirationDate(Date expirationDate) {
    this.expirationDate = expirationDate;
  }

  public Subscription startDate(Date startDate) {
    
    this.startDate = startDate;
    return this;
  }

   /**
   * Get startDate
   * @return startDate
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_START_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Date getStartDate() {
    return startDate;
  }


  @JsonProperty(JSON_PROPERTY_START_DATE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Subscription countQueries(Boolean countQueries) {
    
    this.countQueries = countQueries;
    return this;
  }

   /**
   * Get countQueries
   * @return countQueries
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_COUNT_QUERIES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isCountQueries() {
    return countQueries;
  }


  @JsonProperty(JSON_PROPERTY_COUNT_QUERIES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setCountQueries(Boolean countQueries) {
    this.countQueries = countQueries;
  }

  public Subscription instanceLimit(Integer instanceLimit) {
    
    this.instanceLimit = instanceLimit;
    return this;
  }

   /**
   * Limit of instances (features) that can be computed with this subscription
   * @return instanceLimit
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INSTANCE_LIMIT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getInstanceLimit() {
    return instanceLimit;
  }


  @JsonProperty(JSON_PROPERTY_INSTANCE_LIMIT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setInstanceLimit(Integer instanceLimit) {
    this.instanceLimit = instanceLimit;
  }

  public Subscription instanceHashRecordingTime(Integer instanceHashRecordingTime) {
    
    this.instanceHashRecordingTime = instanceHashRecordingTime;
    return this;
  }

   /**
   * Hash is used to allow recomputing identical data without increasing counted instances (features).  The recording time is the amount of time an instance is memorized is
   * @return instanceHashRecordingTime
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_INSTANCE_HASH_RECORDING_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getInstanceHashRecordingTime() {
    return instanceHashRecordingTime;
  }


  @JsonProperty(JSON_PROPERTY_INSTANCE_HASH_RECORDING_TIME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setInstanceHashRecordingTime(Integer instanceHashRecordingTime) {
    this.instanceHashRecordingTime = instanceHashRecordingTime;
  }

  public Subscription maxQueriesPerInstance(Integer maxQueriesPerInstance) {
    
    this.maxQueriesPerInstance = maxQueriesPerInstance;
    return this;
  }

   /**
   * Maximum number of queries (e.g. prediction) that can be performed  for one instance before it is counted another time.
   * @return maxQueriesPerInstance
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MAX_QUERIES_PER_INSTANCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getMaxQueriesPerInstance() {
    return maxQueriesPerInstance;
  }


  @JsonProperty(JSON_PROPERTY_MAX_QUERIES_PER_INSTANCE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMaxQueriesPerInstance(Integer maxQueriesPerInstance) {
    this.maxQueriesPerInstance = maxQueriesPerInstance;
  }

  public Subscription maxUserAccounts(Integer maxUserAccounts) {
    
    this.maxUserAccounts = maxUserAccounts;
    return this;
  }

   /**
   * Get maxUserAccounts
   * @return maxUserAccounts
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_MAX_USER_ACCOUNTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getMaxUserAccounts() {
    return maxUserAccounts;
  }


  @JsonProperty(JSON_PROPERTY_MAX_USER_ACCOUNTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMaxUserAccounts(Integer maxUserAccounts) {
    this.maxUserAccounts = maxUserAccounts;
  }

  public Subscription serviceUrl(String serviceUrl) {
    
    this.serviceUrl = serviceUrl;
    return this;
  }

   /**
   * Get serviceUrl
   * @return serviceUrl
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SERVICE_URL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getServiceUrl() {
    return serviceUrl;
  }


  @JsonProperty(JSON_PROPERTY_SERVICE_URL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
  }

  public Subscription description(String description) {
    
    this.description = description;
    return this;
  }

   /**
   * Get description
   * @return description
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getDescription() {
    return description;
  }


  @JsonProperty(JSON_PROPERTY_DESCRIPTION)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setDescription(String description) {
    this.description = description;
  }

  public Subscription name(String name) {
    
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getName() {
    return name;
  }


  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setName(String name) {
    this.name = name;
  }

  public Subscription tos(String tos) {
    
    this.tos = tos;
    return this;
  }

   /**
   * Get tos
   * @return tos
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TOS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getTos() {
    return tos;
  }


  @JsonProperty(JSON_PROPERTY_TOS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTos(String tos) {
    this.tos = tos;
  }

  public Subscription pp(String pp) {
    
    this.pp = pp;
    return this;
  }

   /**
   * Get pp
   * @return pp
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public String getPp() {
    return pp;
  }


  @JsonProperty(JSON_PROPERTY_PP)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPp(String pp) {
    this.pp = pp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Subscription subscription = (Subscription) o;
    return Objects.equals(this.sid, subscription.sid) &&
        Objects.equals(this.subscriberId, subscription.subscriberId) &&
        Objects.equals(this.subscriberName, subscription.subscriberName) &&
        Objects.equals(this.expirationDate, subscription.expirationDate) &&
        Objects.equals(this.startDate, subscription.startDate) &&
        Objects.equals(this.countQueries, subscription.countQueries) &&
        Objects.equals(this.instanceLimit, subscription.instanceLimit) &&
        Objects.equals(this.instanceHashRecordingTime, subscription.instanceHashRecordingTime) &&
        Objects.equals(this.maxQueriesPerInstance, subscription.maxQueriesPerInstance) &&
        Objects.equals(this.maxUserAccounts, subscription.maxUserAccounts) &&
        Objects.equals(this.serviceUrl, subscription.serviceUrl) &&
        Objects.equals(this.description, subscription.description) &&
        Objects.equals(this.name, subscription.name) &&
        Objects.equals(this.tos, subscription.tos) &&
        Objects.equals(this.pp, subscription.pp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sid, subscriberId, subscriberName, expirationDate, startDate, countQueries, instanceLimit, instanceHashRecordingTime, maxQueriesPerInstance, maxUserAccounts, serviceUrl, description, name, tos, pp);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Subscription {\n");
    sb.append("    sid: ").append(toIndentedString(sid)).append("\n");
    sb.append("    subscriberId: ").append(toIndentedString(subscriberId)).append("\n");
    sb.append("    subscriberName: ").append(toIndentedString(subscriberName)).append("\n");
    sb.append("    expirationDate: ").append(toIndentedString(expirationDate)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    countQueries: ").append(toIndentedString(countQueries)).append("\n");
    sb.append("    instanceLimit: ").append(toIndentedString(instanceLimit)).append("\n");
    sb.append("    instanceHashRecordingTime: ").append(toIndentedString(instanceHashRecordingTime)).append("\n");
    sb.append("    maxQueriesPerInstance: ").append(toIndentedString(maxQueriesPerInstance)).append("\n");
    sb.append("    maxUserAccounts: ").append(toIndentedString(maxUserAccounts)).append("\n");
    sb.append("    serviceUrl: ").append(toIndentedString(serviceUrl)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    tos: ").append(toIndentedString(tos)).append("\n");
    sb.append("    pp: ").append(toIndentedString(pp)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

