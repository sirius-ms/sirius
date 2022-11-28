package de.unijena.bioinf.ms.rest.model.license;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Date;

public class Subscription {

    private String sid;

    /**
     * ID of the owner of the subscription.
     * This can be the ID of any SubscriptionOwner (e.g.  Group or  User)
     * depending on the level on which a subscription should be is valid.
     */
    private String subscriberId;

    private String subscriberName;

    private Date expirationDate;
    private Date startDate;

    private boolean countQueries;

    private Integer compoundLimit;

    private Integer compoundHashRecordingTime;

    private Integer maxQueriesPerCompound;

    private Integer maxUserAccounts;

    private String serviceUrl;

    private String description;

    private String name;

    private String tos;

    private String pp;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Boolean getCountQueries() {
        return countQueries;
    }

    public void setCountQueries(Boolean countQueries) {
        this.countQueries = countQueries;
    }

    public Integer getCompoundLimit() {
        return compoundLimit;
    }

    public void setCompoundLimit(Integer compoundLimit) {
        this.compoundLimit = compoundLimit;
    }

    public Integer getCompoundHashRecordingTime() {
        return compoundHashRecordingTime;
    }

    public void setCompoundHashRecordingTime(Integer compoundHashRecordingTime) {
        this.compoundHashRecordingTime = compoundHashRecordingTime;
    }

    public Integer getMaxQueriesPerCompound() {
        return maxQueriesPerCompound;
    }

    public void setMaxQueriesPerCompound(Integer maxQueriesPerCompound) {
        this.maxQueriesPerCompound = maxQueriesPerCompound;
    }

    public Integer getMaxUserAccounts() {
        return maxUserAccounts;
    }

    public void setMaxUserAccounts(Integer maxUserAccounts) {
        this.maxUserAccounts = maxUserAccounts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTos() {
        return tos;
    }

    public void setTos(String tos) {
        this.tos = tos;
    }

    public String getPp() {
        return pp;
    }

    public void setPp(String pp) {
        this.pp = pp;
    }


    // non json methods
    @JsonIgnore
    public boolean hasCompoundLimit() {
        Integer l = getCompoundLimit();
        return  l != null && l > 0;
    }

    @JsonIgnore
    public boolean hasExpirationTime(){
        return getExpirationDate() != null;
    }

    @JsonIgnore
    public boolean isExpired() {
        if (!hasExpirationTime())
            return false;
        return getExpirationDate().getTime() < System.currentTimeMillis();
    }

    @JsonIgnore
    public long getExpirationTime() {
        Date d = getExpirationDate();
        if (d == null)
            return -1;
        return d.getTime();
    }
}
