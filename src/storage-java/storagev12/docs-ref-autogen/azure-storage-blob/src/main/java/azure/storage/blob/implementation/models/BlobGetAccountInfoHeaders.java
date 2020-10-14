// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.blob.implementation.models;

import com.azure.core.annotation.Fluent;
import com.azure.core.util.DateTimeRfc1123;
import com.azure.storage.blob.models.AccountKind;
import com.azure.storage.blob.models.SkuName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.time.OffsetDateTime;

/**
 * Defines headers for GetAccountInfo operation.
 */
@JacksonXmlRootElement(localName = "Blob-GetAccountInfo-Headers")
@Fluent
public final class BlobGetAccountInfoHeaders {
    /*
     * If a client request id header is sent in the request, this header will
     * be present in the response with the same value.
     */
    @JsonProperty(value = "x-ms-client-request-id")
    private String clientRequestId;

    /*
     * This header uniquely identifies the request that was made and can be
     * used for troubleshooting the request.
     */
    @JsonProperty(value = "x-ms-request-id")
    private String requestId;

    /*
     * Indicates the version of the Blob service used to execute the request.
     * This header is returned for requests made against version 2009-09-19 and
     * above.
     */
    @JsonProperty(value = "x-ms-version")
    private String version;

    /*
     * UTC date/time value generated by the service that indicates the time at
     * which the response was initiated
     */
    @JsonProperty(value = "Date")
    private DateTimeRfc1123 dateProperty;

    /*
     * Identifies the sku name of the account. Possible values include:
     * 'Standard_LRS', 'Standard_GRS', 'Standard_RAGRS', 'Standard_ZRS',
     * 'Premium_LRS'
     */
    @JsonProperty(value = "x-ms-sku-name")
    private SkuName skuName;

    /*
     * Identifies the account kind. Possible values include: 'Storage',
     * 'BlobStorage', 'StorageV2', 'FileStorage', 'BlockBlobStorage'
     */
    @JsonProperty(value = "x-ms-account-kind")
    private AccountKind accountKind;

    /*
     * The errorCode property.
     */
    @JsonProperty(value = "x-ms-error-code")
    private String errorCode;

    /**
     * Get the clientRequestId property: If a client request id header is sent
     * in the request, this header will be present in the response with the
     * same value.
     *
     * @return the clientRequestId value.
     */
    public String getClientRequestId() {
        return this.clientRequestId;
    }

    /**
     * Set the clientRequestId property: If a client request id header is sent
     * in the request, this header will be present in the response with the
     * same value.
     *
     * @param clientRequestId the clientRequestId value to set.
     * @return the BlobGetAccountInfoHeaders object itself.
     */
    public BlobGetAccountInfoHeaders setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
        return this;
    }

    /**
     * Get the requestId property: This header uniquely identifies the request
     * that was made and can be used for troubleshooting the request.
     *
     * @return the requestId value.
     */
    public String getRequestId() {
        return this.requestId;
    }

    /**
     * Set the requestId property: This header uniquely identifies the request
     * that was made and can be used for troubleshooting the request.
     *
     * @param requestId the requestId value to set.
     * @return the BlobGetAccountInfoHeaders object itself.
     */
    public BlobGetAccountInfoHeaders setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    /**
     * Get the version property: Indicates the version of the Blob service used
     * to execute the request. This header is returned for requests made
     * against version 2009-09-19 and above.
     *
     * @return the version value.
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Set the version property: Indicates the version of the Blob service used
     * to execute the request. This header is returned for requests made
     * against version 2009-09-19 and above.
     *
     * @param version the version value to set.
     * @return the BlobGetAccountInfoHeaders object itself.
     */
    public BlobGetAccountInfoHeaders setVersion(String version) {
        this.version = version;
        return this;
    }

    /**
     * Get the dateProperty property: UTC date/time value generated by the
     * service that indicates the time at which the response was initiated.
     *
     * @return the dateProperty value.
     */
    public OffsetDateTime getDateProperty() {
        if (this.dateProperty == null) {
            return null;
        }
        return this.dateProperty.getDateTime();
    }

    /**
     * Set the dateProperty property: UTC date/time value generated by the
     * service that indicates the time at which the response was initiated.
     *
     * @param dateProperty the dateProperty value to set.
     * @return the BlobGetAccountInfoHeaders object itself.
     */
    public BlobGetAccountInfoHeaders setDateProperty(OffsetDateTime dateProperty) {
        if (dateProperty == null) {
            this.dateProperty = null;
        } else {
            this.dateProperty = new DateTimeRfc1123(dateProperty);
        }
        return this;
    }

    /**
     * Get the skuName property: Identifies the sku name of the account.
     * Possible values include: 'Standard_LRS', 'Standard_GRS',
     * 'Standard_RAGRS', 'Standard_ZRS', 'Premium_LRS'.
     *
     * @return the skuName value.
     */
    public SkuName getSkuName() {
        return this.skuName;
    }

    /**
     * Set the skuName property: Identifies the sku name of the account.
     * Possible values include: 'Standard_LRS', 'Standard_GRS',
     * 'Standard_RAGRS', 'Standard_ZRS', 'Premium_LRS'.
     *
     * @param skuName the skuName value to set.
     * @return the BlobGetAccountInfoHeaders object itself.
     */
    public BlobGetAccountInfoHeaders setSkuName(SkuName skuName) {
        this.skuName = skuName;
        return this;
    }

    /**
     * Get the accountKind property: Identifies the account kind. Possible
     * values include: 'Storage', 'BlobStorage', 'StorageV2', 'FileStorage',
     * 'BlockBlobStorage'.
     *
     * @return the accountKind value.
     */
    public AccountKind getAccountKind() {
        return this.accountKind;
    }

    /**
     * Set the accountKind property: Identifies the account kind. Possible
     * values include: 'Storage', 'BlobStorage', 'StorageV2', 'FileStorage',
     * 'BlockBlobStorage'.
     *
     * @param accountKind the accountKind value to set.
     * @return the BlobGetAccountInfoHeaders object itself.
     */
    public BlobGetAccountInfoHeaders setAccountKind(AccountKind accountKind) {
        this.accountKind = accountKind;
        return this;
    }

    /**
     * Get the errorCode property: The errorCode property.
     *
     * @return the errorCode value.
     */
    public String getErrorCode() {
        return this.errorCode;
    }

    /**
     * Set the errorCode property: The errorCode property.
     *
     * @param errorCode the errorCode value to set.
     * @return the BlobGetAccountInfoHeaders object itself.
     */
    public BlobGetAccountInfoHeaders setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }
}
