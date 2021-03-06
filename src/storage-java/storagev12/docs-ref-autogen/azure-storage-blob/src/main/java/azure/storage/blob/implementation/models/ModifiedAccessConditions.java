// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.blob.implementation.models;

import com.azure.core.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Additional parameters for a set of operations.
 */
@JacksonXmlRootElement(localName = "modified-access-conditions")
@Fluent
public final class ModifiedAccessConditions {
    /*
     * Specify a SQL where clause on blob tags to operate only on blobs with a
     * matching value.
     */
    @JsonProperty(value = "ifTags")
    private String ifTags;

    /**
     * Get the ifTags property: Specify a SQL where clause on blob tags to
     * operate only on blobs with a matching value.
     *
     * @return the ifTags value.
     */
    public String getIfTags() {
        return this.ifTags;
    }

    /**
     * Set the ifTags property: Specify a SQL where clause on blob tags to
     * operate only on blobs with a matching value.
     *
     * @param ifTags the ifTags value to set.
     * @return the ModifiedAccessConditions object itself.
     */
    public ModifiedAccessConditions setIfTags(String ifTags) {
        this.ifTags = ifTags;
        return this;
    }
}
