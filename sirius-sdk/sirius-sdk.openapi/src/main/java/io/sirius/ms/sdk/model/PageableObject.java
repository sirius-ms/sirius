/*
 * SIRIUS Nightsky API
 * REST API that provides the full functionality of SIRIUS and its web services as background service. It is intended as entry-point for scripting languages and software integration SDKs.This API is exposed by SIRIUS 6
 *
 * The version of the OpenAPI document: 2.1
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package io.sirius.ms.sdk.model;

import java.util.Objects;
import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import io.sirius.ms.sdk.model.SortObject;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * PageableObject
 */
@JsonPropertyOrder({
  PageableObject.JSON_PROPERTY_OFFSET,
  PageableObject.JSON_PROPERTY_SORT,
  PageableObject.JSON_PROPERTY_PAGED,
  PageableObject.JSON_PROPERTY_PAGE_NUMBER,
  PageableObject.JSON_PROPERTY_PAGE_SIZE,
  PageableObject.JSON_PROPERTY_UNPAGED
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class PageableObject {
  public static final String JSON_PROPERTY_OFFSET = "offset";
  private Long offset;

  public static final String JSON_PROPERTY_SORT = "sort";
  private SortObject sort;

  public static final String JSON_PROPERTY_PAGED = "paged";
  private Boolean paged;

  public static final String JSON_PROPERTY_PAGE_NUMBER = "pageNumber";
  private Integer pageNumber;

  public static final String JSON_PROPERTY_PAGE_SIZE = "pageSize";
  private Integer pageSize;

  public static final String JSON_PROPERTY_UNPAGED = "unpaged";
  private Boolean unpaged;

  public PageableObject() {
  }

  public PageableObject offset(Long offset) {
    
    this.offset = offset;
    return this;
  }

   /**
   * Get offset
   * @return offset
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_OFFSET)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getOffset() {
    return offset;
  }


  @JsonProperty(JSON_PROPERTY_OFFSET)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setOffset(Long offset) {
    this.offset = offset;
  }

  public PageableObject sort(SortObject sort) {
    
    this.sort = sort;
    return this;
  }

   /**
   * Get sort
   * @return sort
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SORT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public SortObject getSort() {
    return sort;
  }


  @JsonProperty(JSON_PROPERTY_SORT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSort(SortObject sort) {
    this.sort = sort;
  }

  public PageableObject paged(Boolean paged) {
    
    this.paged = paged;
    return this;
  }

   /**
   * Get paged
   * @return paged
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PAGED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isPaged() {
    return paged;
  }


  @JsonProperty(JSON_PROPERTY_PAGED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPaged(Boolean paged) {
    this.paged = paged;
  }

  public PageableObject pageNumber(Integer pageNumber) {
    
    this.pageNumber = pageNumber;
    return this;
  }

   /**
   * Get pageNumber
   * @return pageNumber
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PAGE_NUMBER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getPageNumber() {
    return pageNumber;
  }


  @JsonProperty(JSON_PROPERTY_PAGE_NUMBER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPageNumber(Integer pageNumber) {
    this.pageNumber = pageNumber;
  }

  public PageableObject pageSize(Integer pageSize) {
    
    this.pageSize = pageSize;
    return this;
  }

   /**
   * Get pageSize
   * @return pageSize
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PAGE_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getPageSize() {
    return pageSize;
  }


  @JsonProperty(JSON_PROPERTY_PAGE_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

  public PageableObject unpaged(Boolean unpaged) {
    
    this.unpaged = unpaged;
    return this;
  }

   /**
   * Get unpaged
   * @return unpaged
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_UNPAGED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isUnpaged() {
    return unpaged;
  }


  @JsonProperty(JSON_PROPERTY_UNPAGED)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setUnpaged(Boolean unpaged) {
    this.unpaged = unpaged;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageableObject pageableObject = (PageableObject) o;
    return Objects.equals(this.offset, pageableObject.offset) &&
        Objects.equals(this.sort, pageableObject.sort) &&
        Objects.equals(this.paged, pageableObject.paged) &&
        Objects.equals(this.pageNumber, pageableObject.pageNumber) &&
        Objects.equals(this.pageSize, pageableObject.pageSize) &&
        Objects.equals(this.unpaged, pageableObject.unpaged);
  }

  @Override
  public int hashCode() {
    return Objects.hash(offset, sort, paged, pageNumber, pageSize, unpaged);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PageableObject {\n");
    sb.append("    offset: ").append(toIndentedString(offset)).append("\n");
    sb.append("    sort: ").append(toIndentedString(sort)).append("\n");
    sb.append("    paged: ").append(toIndentedString(paged)).append("\n");
    sb.append("    pageNumber: ").append(toIndentedString(pageNumber)).append("\n");
    sb.append("    pageSize: ").append(toIndentedString(pageSize)).append("\n");
    sb.append("    unpaged: ").append(toIndentedString(unpaged)).append("\n");
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
