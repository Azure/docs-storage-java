// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
// Code generated by Microsoft (R) AutoRest Code Generator.

package com.azure.storage.blob.models;

import com.azure.core.annotation.Fluent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Additional parameters for a set of operations.
 */
@JacksonXmlRootElement(localName = "cpk-info")
@Fluent
public final class CpkInfo {
    /*
     * Optional. Specifies the encryption key to use to encrypt the data
     * provided in the request. If not specified, encryption is performed with
     * the root account encryption key.  For more information, see Encryption
     * at Rest for Azure Storage Services.
     */
    @JsonProperty(value = "encryptionKey")
    private String encryptionKey;

    /*
     * The SHA-256 hash of the provided encryption key. Must be provided if the
     * x-ms-encryption-key header is provided.
     */
    @JsonProperty(value = "encryptionKeySha256")
    private String encryptionKeySha256;

    /*
     * The algorithm used to produce the encryption key hash. Currently, the
     * only accepted value is "AES256". Must be provided if the
     * x-ms-encryption-key header is provided. Possible values include:
     * 'AES256'
     */
    @JsonProperty(value = "encryptionAlgorithm")
    private EncryptionAlgorithmType encryptionAlgorithm;

    /**
     * Get the encryptionKey property: Optional. Specifies the encryption key
     * to use to encrypt the data provided in the request. If not specified,
     * encryption is performed with the root account encryption key.  For more
     * information, see Encryption at Rest for Azure Storage Services.
     *
     * @return the encryptionKey value.
     */
    public String getEncryptionKey() {
        return this.encryptionKey;
    }

    /**
     * Set the encryptionKey property: Optional. Specifies the encryption key
     * to use to encrypt the data provided in the request. If not specified,
     * encryption is performed with the root account encryption key.  For more
     * information, see Encryption at Rest for Azure Storage Services.
     *
     * @param encryptionKey the encryptionKey value to set.
     * @return the CpkInfo object itself.
     */
    public CpkInfo setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
        return this;
    }

    /**
     * Get the encryptionKeySha256 property: The SHA-256 hash of the provided
     * encryption key. Must be provided if the x-ms-encryption-key header is
     * provided.
     *
     * @return the encryptionKeySha256 value.
     */
    public String getEncryptionKeySha256() {
        return this.encryptionKeySha256;
    }

    /**
     * Set the encryptionKeySha256 property: The SHA-256 hash of the provided
     * encryption key. Must be provided if the x-ms-encryption-key header is
     * provided.
     *
     * @param encryptionKeySha256 the encryptionKeySha256 value to set.
     * @return the CpkInfo object itself.
     */
    public CpkInfo setEncryptionKeySha256(String encryptionKeySha256) {
        this.encryptionKeySha256 = encryptionKeySha256;
        return this;
    }

    /**
     * Get the encryptionAlgorithm property: The algorithm used to produce the
     * encryption key hash. Currently, the only accepted value is "AES256".
     * Must be provided if the x-ms-encryption-key header is provided. Possible
     * values include: 'AES256'.
     *
     * @return the encryptionAlgorithm value.
     */
    public EncryptionAlgorithmType getEncryptionAlgorithm() {
        return this.encryptionAlgorithm;
    }

    /**
     * Set the encryptionAlgorithm property: The algorithm used to produce the
     * encryption key hash. Currently, the only accepted value is "AES256".
     * Must be provided if the x-ms-encryption-key header is provided. Possible
     * values include: 'AES256'.
     *
     * @param encryptionAlgorithm the encryptionAlgorithm value to set.
     * @return the CpkInfo object itself.
     */
    public CpkInfo setEncryptionAlgorithm(EncryptionAlgorithmType encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
        return this;
    }
}
