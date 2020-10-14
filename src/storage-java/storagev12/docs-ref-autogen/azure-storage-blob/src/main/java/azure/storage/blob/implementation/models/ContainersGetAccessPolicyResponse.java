// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.blob.implementation.models;

import com.azure.core.http.HttpHeaders;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.rest.ResponseBase;
import com.azure.storage.blob.models.BlobSignedIdentifier;
import java.util.List;

/**
 * Contains all response data for the getAccessPolicy operation.
 */
public final class ContainersGetAccessPolicyResponse extends ResponseBase<ContainerGetAccessPolicyHeaders, List<BlobSignedIdentifier>> {
    /**
     * Creates an instance of ContainersGetAccessPolicyResponse.
     *
     * @param request the request which resulted in this ContainersGetAccessPolicyResponse.
     * @param statusCode the status code of the HTTP response.
     * @param rawHeaders the raw headers of the HTTP response.
     * @param value the deserialized value of the HTTP response.
     * @param headers the deserialized headers of the HTTP response.
     */
    public ContainersGetAccessPolicyResponse(HttpRequest request, int statusCode, HttpHeaders rawHeaders, List<BlobSignedIdentifier> value, ContainerGetAccessPolicyHeaders headers) {
        super(request, statusCode, rawHeaders, value, headers);
    }

    /**
     * @return the deserialized response body.
     */
    @Override
    public List<BlobSignedIdentifier> getValue() {
        return super.getValue();
    }
}
