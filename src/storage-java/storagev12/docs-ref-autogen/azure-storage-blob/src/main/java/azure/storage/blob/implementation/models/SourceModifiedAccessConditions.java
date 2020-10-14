// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.blob.implementation.models;

import com.azure.core.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Additional parameters for startCopyFromURL operation.
 */
@JacksonXmlRootElement(localName = "source-modified-access-conditions")
@Fluent
public final class SourceModifiedAccessConditions {
    /*
     * Specify a SQL where clause on blob tags to operate only on blobs with a
     * matching value.
     */
    @JsonProperty(value = "sourceIfTags")
    private String sourceIfTags;

    /**
     * Get the sourceIfTags property: Specify a SQL where clause on blob tags
     * to operate only on blobs with a matching value.
     *
     * @return the sourceIfTags value.
     */
    public String getSourceIfTags() {
        return this.sourceIfTags;
    }

    /**
     * Set the sourceIfTags property: Specify a SQL where clause on blob tags
     * to operate only on blobs with a matching value.
     *
     * @param sourceIfTags the sourceIfTags value to set.
     * @return the SourceModifiedAccessConditions object itself.
     */
    public SourceModifiedAccessConditions setSourceIfTags(String sourceIfTags) {
        this.sourceIfTags = sourceIfTags;
        return this;
    }
}
