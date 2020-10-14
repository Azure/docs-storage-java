// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob;

import com.azure.core.annotation.ServiceClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobContainerAccessPolicies;
import com.azure.storage.blob.models.BlobContainerProperties;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobSignedIdentifier;
import com.azure.storage.blob.models.CpkInfo;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.blob.models.PublicAccessType;
import com.azure.storage.blob.models.StorageAccountInfo;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.azure.storage.common.implementation.StorageImplUtils.blockWithOptionalTimeout;

/**
 * Client to a container. It may only be instantiated through a {@link BlobContainerClientBuilder} or via the method
 * {@link BlobServiceClient#getBlobContainerClient(String)}. This class does not hold any state about a particular
 * container but is instead a convenient way of sending off appropriate requests to the resource on the service. It may
 * also be used to construct URLs to blobs.
 *
 * <p>
 * This client contains operations on a container. Operations on a blob are available on {@link BlobClient} through
 * {@link #getBlobClient(String)}, and operations on the service are available on {@link BlobServiceClient}.
 *
 * <p>
 * Please refer to the <a href=https://docs.microsoft.com/en-us/azure/storage/blobs/storage-blobs-introduction>Azure
 * Docs</a> for more information on containers.
 */
@ServiceClient(builder = BlobContainerClientBuilder.class)
public final class BlobContainerClient {
    private final BlobContainerAsyncClient client;

    public static final String ROOT_CONTAINER_NAME = BlobContainerAsyncClient.ROOT_CONTAINER_NAME;

    public static final String STATIC_WEBSITE_CONTAINER_NAME = BlobContainerAsyncClient.STATIC_WEBSITE_CONTAINER_NAME;

    public static final String LOG_CONTAINER_NAME = BlobContainerAsyncClient.LOG_CONTAINER_NAME;

    /**
     * Package-private constructor for use by {@link BlobContainerClientBuilder}.
     *
     * @param client the async container client
     */
    BlobContainerClient(BlobContainerAsyncClient client) {
        this.client = client;
    }


    /**
     * Initializes a new BlobClient object by concatenating blobName to the end of ContainerAsyncClient's URL. The new
     * BlobClient uses the same request policy pipeline as the ContainerAsyncClient.
     *
     * @param blobName A {@code String} representing the name of the blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobClient blobClient = client.getBlobClient&#40;blobName&#41;;
     * </pre>
     * @return A new {@link BlobClient} object which references the blob with the specified name in this container.
     */
    public BlobClient getBlobClient(String blobName) {
        return new BlobClient(client.getBlobAsyncClient(blobName));
    }

    /**
     * Initializes a new BlobClient object by concatenating blobName to the end of ContainerAsyncClient's URL. The new
     * BlobClient uses the same request policy pipeline as the ContainerAsyncClient.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobClient blobClient = client.getBlobClient&#40;blobName, snapshot&#41;;
     * </pre>
     *
     * @param blobName A {@code String} representing the name of the blob.
     * @param snapshot the snapshot identifier for the blob.
     * @return A new {@link BlobClient} object which references the blob with the specified name in this container.
     */
    public BlobClient getBlobClient(String blobName, String snapshot) {
        return new BlobClient(client.getBlobAsyncClient(blobName, snapshot));
    }

    /**
     * Initializes a new BlobClient object by concatenating blobName to the end of ContainerAsyncClient's URL. The new
     * BlobClient uses the same request policy pipeline as the ContainerAsyncClient.
     *
     * @param blobName A {@code String} representing the name of the blob.
     * @param versionId the version identifier for the blob, pass {@code null} to interact with the latest blob version.
     * @return A new {@link BlobClient} object which references the blob with the specified name in this container.
     */
    public BlobClient getBlobVersionClient(String blobName, String versionId) {
        return new BlobClient(client.getBlobVersionAsyncClient(blobName, versionId));
    }

    /**
     * Get the container name.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * String containerName = client.getBlobContainerName&#40;&#41;;
     * System.out.println&#40;&quot;The name of the blob is &quot; + containerName&#41;;
     * </pre>
     *
     * @return The name of container.
     */
    public String getBlobContainerName() {
        return this.client.getBlobContainerName();
    }

    /**
     * Gets the URL of the container represented by this client.
     *
     * @return the URL.
     */
    public String getBlobContainerUrl() {
        return client.getBlobContainerUrl();
    }

    /**
     * Get associated account name.
     *
     * @return account name associated with this storage resource.
     */
    public String getAccountName() {
        return this.client.getAccountName();
    }

    /**
     * Gets the service version the client is using.
     *
     * @return the service version the client is using.
     */
    public BlobServiceVersion getServiceVersion() {
        return this.client.getServiceVersion();
    }

    /**
     * Gets the {@link HttpPipeline} powering this client.
     *
     * @return The pipeline.
     */
    public HttpPipeline getHttpPipeline() {
        return client.getHttpPipeline();
    }

    /**
     * Gets the {@link CpkInfo} associated with this client that will be passed to {@link BlobClient BlobClients} when
     * {@link #getBlobClient(String) getBlobClient} is called.
     *
     * @return the customer provided key used for encryption.
     */
    public CpkInfo getCustomerProvidedKey() {
        return client.getCustomerProvidedKey();
    }

    /**
     * Gets the {@code encryption scope} used to encrypt this blob's content on the server.
     *
     * @return the encryption scope used for encryption.
     */
    public String getEncryptionScope() {
        return client.getEncryptionScope();
    }

    /**
     * Gets if the container this client represents exists in the cloud.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Exists? %b%n&quot;, client.exists&#40;&#41;&#41;;
     * </pre>
     *
     * @return true if the container exists, false if it doesn't
     */
    public boolean exists() {
        return existsWithResponse(null, Context.NONE).getValue();
    }

    /**
     * Gets if the container this client represents exists in the cloud.
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * System.out.printf&#40;&quot;Exists? %b%n&quot;, client.existsWithResponse&#40;timeout, context&#41;.getValue&#40;&#41;&#41;;
     * </pre>
     *
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return true if the container exists, false if it doesn't
     */
    public Response<Boolean> existsWithResponse(Duration timeout, Context context) {
        Mono<Response<Boolean>> response = client.existsWithResponse(context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Creates a new container within a storage account. If a container with the same name already exists, the operation
     * fails. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/create-container">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * try &#123;
     *     client.create&#40;&#41;;
     *     System.out.printf&#40;&quot;Create completed%n&quot;&#41;;
     * &#125; catch &#40;BlobStorageException error&#41; &#123;
     *     if &#40;error.getErrorCode&#40;&#41;.equals&#40;BlobErrorCode.CONTAINER_ALREADY_EXISTS&#41;&#41; &#123;
     *         System.out.printf&#40;&quot;Can't create container. It already exists %n&quot;&#41;;
     *     &#125;
     * &#125;
     * </pre>
     */
    public void create() {
        createWithResponse(null, null, null, Context.NONE);
    }

    /**
     * Creates a new container within a storage account. If a container with the same name already exists, the operation
     * fails. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/create-container">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;Create completed with status %d%n&quot;,
     *     client.createWithResponse&#40;metadata, PublicAccessType.CONTAINER, timeout, context&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * @param metadata Metadata to associate with the container.
     * @param accessType Specifies how the data in this container is available to the public. See the
     * x-ms-blob-public-access header in the Azure Docs for more information. Pass null for no public access.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers
     */
    public Response<Void> createWithResponse(Map<String, String> metadata, PublicAccessType accessType,
        Duration timeout, Context context) {
        Mono<Response<Void>> response = client.createWithResponse(metadata, accessType, context);
        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Marks the specified container for deletion. The container and any blobs contained within it are later deleted
     * during garbage collection. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/delete-container">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * try &#123;
     *     client.delete&#40;&#41;;
     *     System.out.printf&#40;&quot;Delete completed%n&quot;&#41;;
     * &#125; catch &#40;BlobStorageException error&#41; &#123;
     *     if &#40;error.getErrorCode&#40;&#41;.equals&#40;BlobErrorCode.CONTAINER_NOT_FOUND&#41;&#41; &#123;
     *         System.out.printf&#40;&quot;Delete failed. Container was not found %n&quot;&#41;;
     *     &#125;
     * &#125;
     * </pre>
     */
    public void delete() {
        deleteWithResponse(null, null, Context.NONE);
    }

    /**
     * Marks the specified container for deletion. The container and any blobs contained within it are later deleted
     * during garbage collection. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/delete-container">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;
     *     .setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;Delete completed with status %d%n&quot;, client.deleteWithResponse&#40;
     *     requestConditions, timeout, context&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers
     */
    public Response<Void> deleteWithResponse(BlobRequestConditions requestConditions, Duration timeout,
        Context context) {
        Mono<Response<Void>> response = client.deleteWithResponse(requestConditions, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Returns the container's metadata and system properties. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/get-container-metadata">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobContainerProperties properties = client.getProperties&#40;&#41;;
     * System.out.printf&#40;&quot;Public Access Type: %s, Legal Hold? %b, Immutable? %b%n&quot;,
     *     properties.getBlobPublicAccess&#40;&#41;,
     *     properties.hasLegalHold&#40;&#41;,
     *     properties.hasImmutabilityPolicy&#40;&#41;&#41;;
     * </pre>
     *
     * @return The container properties.
     */
    public BlobContainerProperties getProperties() {
        return getPropertiesWithResponse(null, null, Context.NONE).getValue();
    }

    /**
     * Returns the container's metadata and system properties. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/get-container-metadata">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * 
     * BlobContainerProperties properties = client.getPropertiesWithResponse&#40;leaseId, timeout, context&#41;
     *     .getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Public Access Type: %s, Legal Hold? %b, Immutable? %b%n&quot;,
     *     properties.getBlobPublicAccess&#40;&#41;,
     *     properties.hasLegalHold&#40;&#41;,
     *     properties.hasImmutabilityPolicy&#40;&#41;&#41;;
     * </pre>
     *
     * @param leaseId The lease ID the active lease on the container must match.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The container properties.
     */
    public Response<BlobContainerProperties> getPropertiesWithResponse(String leaseId, Duration timeout,
        Context context) {
        return blockWithOptionalTimeout(client.getPropertiesWithResponse(leaseId, context), timeout);
    }

    /**
     * Sets the container's metadata. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/set-container-metadata">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * try &#123;
     *     client.setMetadata&#40;metadata&#41;;
     *     System.out.printf&#40;&quot;Set metadata completed with status %n&quot;&#41;;
     * &#125; catch &#40;UnsupportedOperationException error&#41; &#123;
     *     System.out.printf&#40;&quot;Fail while setting metadata %n&quot;&#41;;
     * &#125;
     * </pre>
     *
     * @param metadata Metadata to associate with the container.
     */
    public void setMetadata(Map<String, String> metadata) {
        setMetadataWithResponse(metadata, null, null, Context.NONE);
    }

    /**
     * Sets the container's metadata. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/set-container-metadata">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;
     *     .setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;Set metadata completed with status %d%n&quot;,
     *     client.setMetadataWithResponse&#40;metadata, requestConditions, timeout, context&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     * @param metadata Metadata to associate with the container.
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers
     */
    public Response<Void> setMetadataWithResponse(Map<String, String> metadata,
        BlobRequestConditions requestConditions, Duration timeout, Context context) {
        Mono<Response<Void>> response = client.setMetadataWithResponse(metadata, requestConditions,
            context);
        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Returns the container's permissions. The permissions indicate whether container's blobs may be accessed publicly.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/get-container-acl">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobContainerAccessPolicies accessPolicies = client.getAccessPolicy&#40;&#41;;
     * System.out.printf&#40;&quot;Blob Access Type: %s%n&quot;, accessPolicies.getBlobAccessType&#40;&#41;&#41;;
     * 
     * for &#40;BlobSignedIdentifier identifier : accessPolicies.getIdentifiers&#40;&#41;&#41; &#123;
     *     System.out.printf&#40;&quot;Identifier Name: %s, Permissions %s%n&quot;,
     *         identifier.getId&#40;&#41;,
     *         identifier.getAccessPolicy&#40;&#41;.getPermissions&#40;&#41;&#41;;
     * &#125;
     * </pre>
     *
     * @return The container access policy.
     */
    public BlobContainerAccessPolicies getAccessPolicy() {
        return getAccessPolicyWithResponse(null, null, Context.NONE).getValue();
    }

    /**
     * Returns the container's permissions. The permissions indicate whether container's blobs may be accessed publicly.
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/get-container-acl">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * BlobContainerAccessPolicies accessPolicies = client.getAccessPolicyWithResponse&#40;leaseId, timeout, context&#41;
     *     .getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Blob Access Type: %s%n&quot;, accessPolicies.getBlobAccessType&#40;&#41;&#41;;
     * 
     * for &#40;BlobSignedIdentifier identifier : accessPolicies.getIdentifiers&#40;&#41;&#41; &#123;
     *     System.out.printf&#40;&quot;Identifier Name: %s, Permissions %s%n&quot;,
     *         identifier.getId&#40;&#41;,
     *         identifier.getAccessPolicy&#40;&#41;.getPermissions&#40;&#41;&#41;;
     * &#125;
     * </pre>
     *
     * @param leaseId The lease ID the active lease on the container must match.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The container access policy.
     */
    public Response<BlobContainerAccessPolicies> getAccessPolicyWithResponse(String leaseId, Duration timeout,
        Context context) {
        return blockWithOptionalTimeout(client.getAccessPolicyWithResponse(leaseId, context), timeout);
    }

    /**
     * Sets the container's permissions. The permissions indicate whether blobs in a container may be accessed publicly.
     * Note that, for each signed identifier, we will truncate the start and expiry times to the nearest second to
     * ensure the time formatting is compatible with the service. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/set-container-acl">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobSignedIdentifier identifier = new BlobSignedIdentifier&#40;&#41;
     *     .setId&#40;&quot;name&quot;&#41;
     *     .setAccessPolicy&#40;new BlobAccessPolicy&#40;&#41;
     *         .setStartsOn&#40;OffsetDateTime.now&#40;&#41;&#41;
     *         .setExpiresOn&#40;OffsetDateTime.now&#40;&#41;.plusDays&#40;7&#41;&#41;
     *         .setPermissions&#40;&quot;permissionString&quot;&#41;&#41;;
     * 
     * try &#123;
     *     client.setAccessPolicy&#40;PublicAccessType.CONTAINER, Collections.singletonList&#40;identifier&#41;&#41;;
     *     System.out.printf&#40;&quot;Set Access Policy completed %n&quot;&#41;;
     * &#125; catch &#40;UnsupportedOperationException error&#41; &#123;
     *     System.out.printf&#40;&quot;Set Access Policy completed %s%n&quot;, error&#41;;
     * &#125;
     * </pre>
     *
     * @param accessType Specifies how the data in this container is available to the public. See the
     * x-ms-blob-public-access header in the Azure Docs for more information. Pass null for no public access.
     * @param identifiers A list of {@link BlobSignedIdentifier} objects that specify the permissions for the container.
     * Please see
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/establishing-a-stored-access-policy">here</a>
     * for more information. Passing null will clear all access policies.
     */
    public void setAccessPolicy(PublicAccessType accessType,
        List<BlobSignedIdentifier> identifiers) {
        setAccessPolicyWithResponse(accessType, identifiers, null, null, Context.NONE);
    }

    /**
     * Sets the container's permissions. The permissions indicate whether blobs in a container may be accessed publicly.
     * Note that, for each signed identifier, we will truncate the start and expiry times to the nearest second to
     * ensure the time formatting is compatible with the service. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/set-container-acl">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobSignedIdentifier identifier = new BlobSignedIdentifier&#40;&#41;
     *     .setId&#40;&quot;name&quot;&#41;
     *     .setAccessPolicy&#40;new BlobAccessPolicy&#40;&#41;
     *         .setStartsOn&#40;OffsetDateTime.now&#40;&#41;&#41;
     *         .setExpiresOn&#40;OffsetDateTime.now&#40;&#41;.plusDays&#40;7&#41;&#41;
     *         .setPermissions&#40;&quot;permissionString&quot;&#41;&#41;;
     * 
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;
     *     .setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * 
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;Set access policy completed with status %d%n&quot;,
     *     client.setAccessPolicyWithResponse&#40;PublicAccessType.CONTAINER,
     *         Collections.singletonList&#40;identifier&#41;,
     *         requestConditions,
     *         timeout,
     *         context&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * @param accessType Specifies how the data in this container is available to the public. See the
     * x-ms-blob-public-access header in the Azure Docs for more information. Pass null for no public access.
     * @param identifiers A list of {@link BlobSignedIdentifier} objects that specify the permissions for the container.
     * Please see
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/establishing-a-stored-access-policy">here</a>
     * for more information. Passing null will clear all access policies.
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers
     */
    public Response<Void> setAccessPolicyWithResponse(PublicAccessType accessType,
        List<BlobSignedIdentifier> identifiers, BlobRequestConditions requestConditions,
        Duration timeout, Context context) {
        Mono<Response<Void>> response = client
            .setAccessPolicyWithResponse(accessType, identifiers, requestConditions, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Returns a lazy loaded list of blobs in this container, with folder structures flattened. The returned {@link
     * PagedIterable} can be consumed through while new items are automatically retrieved as needed.
     *
     * <p>
     * Blob names are returned in lexicographic order.
     *
     * <p>
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/list-blobs">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.listBlobs&#40;&#41;.forEach&#40;blob -&gt;
     *     System.out.printf&#40;&quot;Name: %s, Directory? %b%n&quot;, blob.getName&#40;&#41;, blob.isPrefix&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @return The listed blobs, flattened.
     */
    public PagedIterable<BlobItem> listBlobs() {
        return this.listBlobs(new ListBlobsOptions(), null);
    }

    /**
     * Returns a lazy loaded list of blobs in this container, with folder structures flattened. The returned {@link
     * PagedIterable} can be consumed through while new items are automatically retrieved as needed.
     *
     * <p>
     * Blob names are returned in lexicographic order.
     *
     * <p>
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/list-blobs">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * ListBlobsOptions options = new ListBlobsOptions&#40;&#41;
     *     .setPrefix&#40;&quot;prefixToMatch&quot;&#41;
     *     .setDetails&#40;new BlobListDetails&#40;&#41;
     *         .setRetrieveDeletedBlobs&#40;true&#41;
     *         .setRetrieveSnapshots&#40;true&#41;&#41;;
     * 
     * client.listBlobs&#40;options, timeout&#41;.forEach&#40;blob -&gt;
     *     System.out.printf&#40;&quot;Name: %s, Directory? %b, Deleted? %b, Snapshot ID: %s%n&quot;,
     *         blob.getName&#40;&#41;,
     *         blob.isPrefix&#40;&#41;,
     *         blob.isDeleted&#40;&#41;,
     *         blob.getSnapshot&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param options {@link ListBlobsOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @return The listed blobs, flattened.
     */
    public PagedIterable<BlobItem> listBlobs(ListBlobsOptions options, Duration timeout) {
        return this.listBlobs(options, null, timeout);
    }

    /**
     * Returns a lazy loaded list of blobs in this container, with folder structures flattened. The returned {@link
     * PagedIterable} can be consumed through while new items are automatically retrieved as needed.
     *
     * <p>
     * Blob names are returned in lexicographic order.
     *
     * <p>
     * For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/list-blobs">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * ListBlobsOptions options = new ListBlobsOptions&#40;&#41;
     *     .setPrefix&#40;&quot;prefixToMatch&quot;&#41;
     *     .setDetails&#40;new BlobListDetails&#40;&#41;
     *         .setRetrieveDeletedBlobs&#40;true&#41;
     *         .setRetrieveSnapshots&#40;true&#41;&#41;;
     * 
     * String continuationToken = &quot;continuationToken&quot;;
     * 
     * client.listBlobs&#40;options, continuationToken, timeout&#41;.forEach&#40;blob -&gt;
     *     System.out.printf&#40;&quot;Name: %s, Directory? %b, Deleted? %b, Snapshot ID: %s%n&quot;,
     *         blob.getName&#40;&#41;,
     *         blob.isPrefix&#40;&#41;,
     *         blob.isDeleted&#40;&#41;,
     *         blob.getSnapshot&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param options {@link ListBlobsOptions}
     * @param continuationToken Identifies the portion of the list to be returned with the next list operation.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @return The listed blobs, flattened.
     */
    public PagedIterable<BlobItem> listBlobs(ListBlobsOptions options, String continuationToken, Duration timeout) {
        return new PagedIterable<>(client.listBlobsFlatWithOptionalTimeout(options, continuationToken, timeout));
    }

    /**
     * Returns a reactive Publisher emitting all the blobs and directories (prefixes) under the given directory
     * (prefix). Directories will have {@link BlobItem#isPrefix()} set to true.
     *
     * <p>
     * Blob names are returned in lexicographic order. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/list-blobs">Azure Docs</a>.
     *
     * <p>
     * E.g. listing a container containing a 'foo' folder, which contains blobs 'foo1' and 'foo2', and a blob on the
     * root level 'bar', will return the following results when prefix=null:
     *
     * <ul>
     * <li>foo/ (isPrefix = true)
     * <li>bar (isPrefix = false)
     * </ul>
     * <p>
     * will return the following results when prefix="foo/":
     *
     * <ul>
     * <li>foo/foo1 (isPrefix = false)
     * <li>foo/foo2 (isPrefix = false)
     * </ul>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.listBlobsByHierarchy&#40;&quot;directoryName&quot;&#41;.forEach&#40;blob -&gt;
     *     System.out.printf&#40;&quot;Name: %s, Directory? %b%n&quot;, blob.getName&#40;&#41;, blob.isPrefix&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param directory The directory to list blobs underneath
     * @return A reactive response emitting the prefixes and blobs.
     */
    public PagedIterable<BlobItem> listBlobsByHierarchy(String directory) {
        return this.listBlobsByHierarchy("/", new ListBlobsOptions().setPrefix(directory), null);
    }

    /**
     * Returns a reactive Publisher emitting all the blobs and prefixes (directories) under the given prefix
     * (directory). Directories will have {@link BlobItem#isPrefix()} set to true.
     *
     * <p>
     * Blob names are returned in lexicographic order. For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/list-blobs">Azure Docs</a>.
     *
     * <p>
     * E.g. listing a container containing a 'foo' folder, which contains blobs 'foo1' and 'foo2', and a blob on the
     * root level 'bar', will return the following results when prefix=null:
     *
     * <ul>
     * <li>foo/ (isPrefix = true)
     * <li>bar (isPrefix = false)
     * </ul>
     * <p>
     * will return the following results when prefix="foo/":
     *
     * <ul>
     * <li>foo/foo1 (isPrefix = false)
     * <li>foo/foo2 (isPrefix = false)
     * </ul>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * ListBlobsOptions options = new ListBlobsOptions&#40;&#41;
     *     .setPrefix&#40;&quot;directoryName&quot;&#41;
     *     .setDetails&#40;new BlobListDetails&#40;&#41;
     *         .setRetrieveDeletedBlobs&#40;true&#41;
     *         .setRetrieveSnapshots&#40;true&#41;&#41;;
     * 
     * client.listBlobsByHierarchy&#40;&quot;&#47;&quot;, options, timeout&#41;.forEach&#40;blob -&gt;
     *     System.out.printf&#40;&quot;Name: %s, Directory? %b, Deleted? %b, Snapshot ID: %s%n&quot;,
     *         blob.getName&#40;&#41;,
     *         blob.isPrefix&#40;&#41;,
     *         blob.isDeleted&#40;&#41;,
     *         blob.getSnapshot&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param delimiter The delimiter for blob hierarchy, "/" for hierarchy based on directories
     * @param options {@link ListBlobsOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @return A reactive response emitting the prefixes and blobs.
     */
    public PagedIterable<BlobItem> listBlobsByHierarchy(String delimiter, ListBlobsOptions options, Duration timeout) {
        return new PagedIterable<>(client
            .listBlobsHierarchyWithOptionalTimeout(delimiter, options, timeout));
    }

    /**
     * Returns the sku name and account kind for the account. For more information, please see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-account-information">Azure Docs</a>.
     *
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * StorageAccountInfo accountInfo = client.getAccountInfo&#40;timeout&#41;;
     * System.out.printf&#40;&quot;Account Kind: %s, SKU: %s%n&quot;, accountInfo.getAccountKind&#40;&#41;, accountInfo.getSkuName&#40;&#41;&#41;;
     * </pre>
     * @return The account info.
     */
    public StorageAccountInfo getAccountInfo(Duration timeout) {
        return getAccountInfoWithResponse(timeout, Context.NONE).getValue();
    }

    /**
     * Returns the sku name and account kind for the account. For more information, please see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-account-information">Azure Docs</a>.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Context context = new Context&#40;&quot;Key&quot;, &quot;Value&quot;&#41;;
     * StorageAccountInfo accountInfo = client.getAccountInfoWithResponse&#40;timeout, context&#41;.getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Account Kind: %s, SKU: %s%n&quot;, accountInfo.getAccountKind&#40;&#41;, accountInfo.getSkuName&#40;&#41;&#41;;
     * </pre>
     *
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The account info.
     */
    public Response<StorageAccountInfo> getAccountInfoWithResponse(Duration timeout, Context context) {
        Mono<Response<StorageAccountInfo>> response = client.getAccountInfoWithResponse(context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Generates a user delegation SAS for the container using the specified {@link BlobServiceSasSignatureValues}.
     * <p>See {@link BlobServiceSasSignatureValues} for more information on how to construct a user delegation SAS.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * OffsetDateTime myExpiryTime = OffsetDateTime.now&#40;&#41;.plusDays&#40;1&#41;;
     * BlobContainerSasPermission myPermission = new BlobContainerSasPermission&#40;&#41;.setReadPermission&#40;true&#41;;
     * 
     * BlobServiceSasSignatureValues myValues = new BlobServiceSasSignatureValues&#40;expiryTime, permission&#41;
     *     .setStartTime&#40;OffsetDateTime.now&#40;&#41;&#41;;
     * 
     * client.generateUserDelegationSas&#40;values, userDelegationKey&#41;;
     * </pre>
     *
     * @param blobServiceSasSignatureValues {@link BlobServiceSasSignatureValues}
     * @param userDelegationKey A {@link UserDelegationKey} object used to sign the SAS values.
     * @see BlobServiceClient#getUserDelegationKey(OffsetDateTime, OffsetDateTime) for more information on how to get a
     * user delegation key.
     *
     * @return A {@code String} representing all SAS query parameters.
     */
    public String generateUserDelegationSas(BlobServiceSasSignatureValues blobServiceSasSignatureValues,
        UserDelegationKey userDelegationKey) {
        return this.client.generateUserDelegationSas(blobServiceSasSignatureValues, userDelegationKey);
    }

    /**
     * Generates a service SAS for the container using the specified {@link BlobServiceSasSignatureValues}
     * Note : The client must be authenticated via {@link StorageSharedKeyCredential}
     * <p>See {@link BlobServiceSasSignatureValues} for more information on how to construct a service SAS.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * OffsetDateTime expiryTime = OffsetDateTime.now&#40;&#41;.plusDays&#40;1&#41;;
     * BlobContainerSasPermission permission = new BlobContainerSasPermission&#40;&#41;.setReadPermission&#40;true&#41;;
     * 
     * BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues&#40;expiryTime, permission&#41;
     *     .setStartTime&#40;OffsetDateTime.now&#40;&#41;&#41;;
     * 
     * client.generateSas&#40;values&#41;; &#47;&#47; Client must be authenticated via StorageSharedKeyCredential
     * </pre>
     *
     * @param blobServiceSasSignatureValues {@link BlobServiceSasSignatureValues}
     *
     * @return A {@code String} representing all SAS query parameters.
     */
    public String generateSas(BlobServiceSasSignatureValues blobServiceSasSignatureValues) {
        return this.client.generateSas(blobServiceSasSignatureValues);
    }
}
