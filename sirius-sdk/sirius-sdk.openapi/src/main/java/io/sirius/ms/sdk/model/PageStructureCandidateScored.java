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
import io.sirius.ms.sdk.model.PageableObject;
import io.sirius.ms.sdk.model.SortObject;
import io.sirius.ms.sdk.model.StructureCandidateScored;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * PageStructureCandidateScored
 */
@JsonPropertyOrder({
  PageStructureCandidateScored.JSON_PROPERTY_TOTAL_PAGES,
  PageStructureCandidateScored.JSON_PROPERTY_TOTAL_ELEMENTS,
  PageStructureCandidateScored.JSON_PROPERTY_FIRST,
  PageStructureCandidateScored.JSON_PROPERTY_LAST,
  PageStructureCandidateScored.JSON_PROPERTY_SIZE,
  PageStructureCandidateScored.JSON_PROPERTY_CONTENT,
  PageStructureCandidateScored.JSON_PROPERTY_NUMBER,
  PageStructureCandidateScored.JSON_PROPERTY_SORT,
  PageStructureCandidateScored.JSON_PROPERTY_NUMBER_OF_ELEMENTS,
  PageStructureCandidateScored.JSON_PROPERTY_PAGEABLE,
  PageStructureCandidateScored.JSON_PROPERTY_EMPTY
})
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.6.0")
public class PageStructureCandidateScored {
  public static final String JSON_PROPERTY_TOTAL_PAGES = "totalPages";
  private Integer totalPages;

  public static final String JSON_PROPERTY_TOTAL_ELEMENTS = "totalElements";
  private Long totalElements;

  public static final String JSON_PROPERTY_FIRST = "first";
  private Boolean first;

  public static final String JSON_PROPERTY_LAST = "last";
  private Boolean last;

  public static final String JSON_PROPERTY_SIZE = "size";
  private Integer size;

  public static final String JSON_PROPERTY_CONTENT = "content";
  private List<StructureCandidateScored> content = new ArrayList<>();

  public static final String JSON_PROPERTY_NUMBER = "number";
  private Integer number;

  public static final String JSON_PROPERTY_SORT = "sort";
  private SortObject sort;

  public static final String JSON_PROPERTY_NUMBER_OF_ELEMENTS = "numberOfElements";
  private Integer numberOfElements;

  public static final String JSON_PROPERTY_PAGEABLE = "pageable";
  private PageableObject pageable;

  public static final String JSON_PROPERTY_EMPTY = "empty";
  private Boolean empty;

  public PageStructureCandidateScored() {
  }

  public PageStructureCandidateScored totalPages(Integer totalPages) {
    
    this.totalPages = totalPages;
    return this;
  }

   /**
   * Get totalPages
   * @return totalPages
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TOTAL_PAGES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getTotalPages() {
    return totalPages;
  }


  @JsonProperty(JSON_PROPERTY_TOTAL_PAGES)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  public PageStructureCandidateScored totalElements(Long totalElements) {
    
    this.totalElements = totalElements;
    return this;
  }

   /**
   * Get totalElements
   * @return totalElements
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_TOTAL_ELEMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Long getTotalElements() {
    return totalElements;
  }


  @JsonProperty(JSON_PROPERTY_TOTAL_ELEMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setTotalElements(Long totalElements) {
    this.totalElements = totalElements;
  }

  public PageStructureCandidateScored first(Boolean first) {
    
    this.first = first;
    return this;
  }

   /**
   * Get first
   * @return first
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_FIRST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isFirst() {
    return first;
  }


  @JsonProperty(JSON_PROPERTY_FIRST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setFirst(Boolean first) {
    this.first = first;
  }

  public PageStructureCandidateScored last(Boolean last) {
    
    this.last = last;
    return this;
  }

   /**
   * Get last
   * @return last
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_LAST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isLast() {
    return last;
  }


  @JsonProperty(JSON_PROPERTY_LAST)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLast(Boolean last) {
    this.last = last;
  }

  public PageStructureCandidateScored size(Integer size) {
    
    this.size = size;
    return this;
  }

   /**
   * Get size
   * @return size
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getSize() {
    return size;
  }


  @JsonProperty(JSON_PROPERTY_SIZE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setSize(Integer size) {
    this.size = size;
  }

  public PageStructureCandidateScored content(List<StructureCandidateScored> content) {
    
    this.content = content;
    return this;
  }

  public PageStructureCandidateScored addContentItem(StructureCandidateScored contentItem) {
    if (this.content == null) {
      this.content = new ArrayList<>();
    }
    this.content.add(contentItem);
    return this;
  }

   /**
   * Get content
   * @return content
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_CONTENT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public List<StructureCandidateScored> getContent() {
    return content;
  }


  @JsonProperty(JSON_PROPERTY_CONTENT)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setContent(List<StructureCandidateScored> content) {
    this.content = content;
  }

  public PageStructureCandidateScored number(Integer number) {
    
    this.number = number;
    return this;
  }

   /**
   * Get number
   * @return number
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NUMBER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getNumber() {
    return number;
  }


  @JsonProperty(JSON_PROPERTY_NUMBER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNumber(Integer number) {
    this.number = number;
  }

  public PageStructureCandidateScored sort(SortObject sort) {
    
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

  public PageStructureCandidateScored numberOfElements(Integer numberOfElements) {
    
    this.numberOfElements = numberOfElements;
    return this;
  }

   /**
   * Get numberOfElements
   * @return numberOfElements
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_NUMBER_OF_ELEMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Integer getNumberOfElements() {
    return numberOfElements;
  }


  @JsonProperty(JSON_PROPERTY_NUMBER_OF_ELEMENTS)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setNumberOfElements(Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
  }

  public PageStructureCandidateScored pageable(PageableObject pageable) {
    
    this.pageable = pageable;
    return this;
  }

   /**
   * Get pageable
   * @return pageable
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_PAGEABLE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public PageableObject getPageable() {
    return pageable;
  }


  @JsonProperty(JSON_PROPERTY_PAGEABLE)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setPageable(PageableObject pageable) {
    this.pageable = pageable;
  }

  public PageStructureCandidateScored empty(Boolean empty) {
    
    this.empty = empty;
    return this;
  }

   /**
   * Get empty
   * @return empty
  **/
  @jakarta.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_EMPTY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)

  public Boolean isEmpty() {
    return empty;
  }


  @JsonProperty(JSON_PROPERTY_EMPTY)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setEmpty(Boolean empty) {
    this.empty = empty;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageStructureCandidateScored pageStructureCandidateScored = (PageStructureCandidateScored) o;
    return Objects.equals(this.totalPages, pageStructureCandidateScored.totalPages) &&
        Objects.equals(this.totalElements, pageStructureCandidateScored.totalElements) &&
        Objects.equals(this.first, pageStructureCandidateScored.first) &&
        Objects.equals(this.last, pageStructureCandidateScored.last) &&
        Objects.equals(this.size, pageStructureCandidateScored.size) &&
        Objects.equals(this.content, pageStructureCandidateScored.content) &&
        Objects.equals(this.number, pageStructureCandidateScored.number) &&
        Objects.equals(this.sort, pageStructureCandidateScored.sort) &&
        Objects.equals(this.numberOfElements, pageStructureCandidateScored.numberOfElements) &&
        Objects.equals(this.pageable, pageStructureCandidateScored.pageable) &&
        Objects.equals(this.empty, pageStructureCandidateScored.empty);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalPages, totalElements, first, last, size, content, number, sort, numberOfElements, pageable, empty);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PageStructureCandidateScored {\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
    sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
    sb.append("    first: ").append(toIndentedString(first)).append("\n");
    sb.append("    last: ").append(toIndentedString(last)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    sort: ").append(toIndentedString(sort)).append("\n");
    sb.append("    numberOfElements: ").append(toIndentedString(numberOfElements)).append("\n");
    sb.append("    pageable: ").append(toIndentedString(pageable)).append("\n");
    sb.append("    empty: ").append(toIndentedString(empty)).append("\n");
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

