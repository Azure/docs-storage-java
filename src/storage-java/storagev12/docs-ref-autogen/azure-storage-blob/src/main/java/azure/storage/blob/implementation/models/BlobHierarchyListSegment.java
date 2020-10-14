// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.blob.implementation.models;

import com.azure.core.annotation.Fluent;
import com.azure.storage.blob.models.BlobPrefix;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * The BlobHierarchyListSegment model.
 */
@JacksonXmlRootElement(localName = "Blobs")
@Fluent
@JsonDeserialize(using = CustomHierarchicalListingDeserializer.class)
public final class BlobHierarchyListSegment {
    /*
     * The blobPrefixes property.
     */
    @JsonProperty("BlobPrefix")
    private List<BlobPrefix> blobPrefixes = new ArrayList<>();

    /*
     * The blobItems property.
     */
    @JsonProperty("Blob")
    private List<BlobItemInternal> blobItems = new ArrayList<>();

    /**
     * Get the blobPrefixes property: The blobPrefixes property.
     *
     * @return the blobPrefixes value.
     */
    public List<BlobPrefix> getBlobPrefixes() {
        return this.blobPrefixes;
    }

    /**
     * Set the blobPrefixes property: The blobPrefixes property.
     *
     * @param blobPrefixes the blobPrefixes value to set.
     * @return the BlobHierarchyListSegment object itself.
     */
    public BlobHierarchyListSegment setBlobPrefixes(List<BlobPrefix> blobPrefixes) {
        this.blobPrefixes = blobPrefixes;
        return this;
    }

    /**
     * Get the blobItems property: The blobItems property.
     *
     * @return the blobItems value.
     */
    public List<BlobItemInternal> getBlobItems() {
        return this.blobItems;
    }

    /**
     * Set the blobItems property: The blobItems property.
     *
     * @param blobItems the blobItems value to set.
     * @return the BlobHierarchyListSegment object itself.
     */
    public BlobHierarchyListSegment setBlobItems(List<BlobItemInternal> blobItems) {
        this.blobItems = blobItems;
        return this;
    }
}
