// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.blob.implementation.models;

import com.azure.core.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * The result of a Filter Blobs API call.
 */
@JacksonXmlRootElement(localName = "EnumerationResults")
@Fluent
public final class FilterBlobSegment {
    /*
     * The serviceEndpoint property.
     */
    @JacksonXmlProperty(localName = "ServiceEndpoint", isAttribute = true)
    private String serviceEndpoint;

    /*
     * The where property.
     */
    @JsonProperty(value = "Where", required = true)
    private String where;

    private static final class BlobsWrapper {
        @JacksonXmlProperty(localName = "Blob")
        private final List<FilterBlobItem> items;

        @JsonCreator
        private BlobsWrapper(@JacksonXmlProperty(localName = "Blob") List<FilterBlobItem> items) {
            this.items = items;
        }
    }

    /*
     * The blobs property.
     */
    @JsonProperty(value = "Blobs", required = true)
    private BlobsWrapper blobs;

    /*
     * The nextMarker property.
     */
    @JsonProperty(value = "NextMarker")
    private String nextMarker;

    /**
     * Get the serviceEndpoint property: The serviceEndpoint property.
     *
     * @return the serviceEndpoint value.
     */
    public String getServiceEndpoint() {
        return this.serviceEndpoint;
    }

    /**
     * Set the serviceEndpoint property: The serviceEndpoint property.
     *
     * @param serviceEndpoint the serviceEndpoint value to set.
     * @return the FilterBlobSegment object itself.
     */
    public FilterBlobSegment setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
        return this;
    }

    /**
     * Get the where property: The where property.
     *
     * @return the where value.
     */
    public String getWhere() {
        return this.where;
    }

    /**
     * Set the where property: The where property.
     *
     * @param where the where value to set.
     * @return the FilterBlobSegment object itself.
     */
    public FilterBlobSegment setWhere(String where) {
        this.where = where;
        return this;
    }

    /**
     * Get the blobs property: The blobs property.
     *
     * @return the blobs value.
     */
    public List<FilterBlobItem> getBlobs() {
        if (this.blobs == null) {
            this.blobs = new BlobsWrapper(new ArrayList<FilterBlobItem>());
        }
        return this.blobs.items;
    }

    /**
     * Set the blobs property: The blobs property.
     *
     * @param blobs the blobs value to set.
     * @return the FilterBlobSegment object itself.
     */
    public FilterBlobSegment setBlobs(List<FilterBlobItem> blobs) {
        this.blobs = new BlobsWrapper(blobs);
        return this;
    }

    /**
     * Get the nextMarker property: The nextMarker property.
     *
     * @return the nextMarker value.
     */
    public String getNextMarker() {
        return this.nextMarker;
    }

    /**
     * Set the nextMarker property: The nextMarker property.
     *
     * @param nextMarker the nextMarker value to set.
     * @return the FilterBlobSegment object itself.
     */
    public FilterBlobSegment setNextMarker(String nextMarker) {
        this.nextMarker = nextMarker;
        return this;
    }
}
