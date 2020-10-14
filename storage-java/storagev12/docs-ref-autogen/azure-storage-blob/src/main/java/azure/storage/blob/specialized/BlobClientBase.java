// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob.specialized;

import com.azure.core.http.HttpPipeline;
import com.azure.core.http.RequestConditions;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.ResponseBase;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.util.Context;
import com.azure.core.util.FluxUtil;
import com.azure.core.util.logging.ClientLogger;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.implementation.util.ModelHelper;
import com.azure.storage.blob.options.BlobBeginCopyOptions;
import com.azure.storage.blob.options.BlobCopyFromUrlOptions;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.BlobServiceVersion;
import com.azure.storage.blob.models.AccessTier;
import com.azure.storage.blob.models.BlobCopyInfo;
import com.azure.storage.blob.models.BlobDownloadResponse;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobQueryAsyncResponse;
import com.azure.storage.blob.options.BlobGetTagsOptions;
import com.azure.storage.blob.options.BlobQueryOptions;
import com.azure.storage.blob.models.BlobQueryResponse;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.CpkInfo;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import com.azure.storage.blob.models.DownloadRetryOptions;
import com.azure.storage.blob.models.ParallelTransferOptions;
import com.azure.storage.blob.models.RehydratePriority;
import com.azure.storage.blob.models.StorageAccountInfo;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.options.BlobSetAccessTierOptions;
import com.azure.storage.blob.options.BlobSetTagsOptions;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.implementation.FluxInputStream;
import com.azure.storage.common.implementation.StorageImplUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.azure.storage.common.implementation.StorageImplUtils.blockWithOptionalTimeout;

/**
 * This class provides a client that contains all operations that apply to any blob type.
 *
 * <p>
 * This client offers the ability to download blobs. Note that uploading data is specific to each type of blob. Please
 * refer to the {@link BlockBlobClient}, {@link PageBlobClient}, or {@link AppendBlobClient} for upload options.
 */
public class BlobClientBase {
    private final ClientLogger logger = new ClientLogger(BlobClientBase.class);

    private final BlobAsyncClientBase client;

    /**
     * Constructor used by {@link SpecializedBlobClientBuilder}.
     *
     * @param client the async blob client
     */
    protected BlobClientBase(BlobAsyncClientBase client) {
        this.client = client;
    }

    /**
     * Creates a new {@link BlobClientBase} linked to the {@code snapshot} of this blob resource.
     *
     * @param snapshot the identifier for a specific snapshot of this blob
     * @return a {@link BlobClientBase} used to interact with the specific snapshot.
     */
    public BlobClientBase getSnapshotClient(String snapshot) {
        return new BlobClientBase(client.getSnapshotClient(snapshot));
    }

    /**
     * Creates a new {@link BlobClientBase} linked to the {@code version} of this blob resource.
     *
     * @param versionId the identifier for a specific version of this blob,
     * pass {@code null} to interact with the latest blob version.
     * @return a {@link BlobClientBase} used to interact with the specific version.
     */
    public BlobClientBase getVersionClient(String versionId) {
        return new BlobClientBase(client.getVersionClient(versionId));
    }

    /**
     * Gets the URL of the blob represented by this client.
     *
     * @return the URL.
     */
    public String getBlobUrl() {
        return client.getBlobUrl();
    }

    /**
     * Get associated account name.
     *
     * @return account name associated with this storage resource.
     */
    public String getAccountName() {
        return client.getAccountName();
    }

    /**
     * Get the container name.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * String containerName = client.getContainerName&#40;&#41;;
     * System.out.println&#40;&quot;The name of the blob is &quot; + containerName&#41;;
     * </pre>
     *
     * @return The name of the container.
     */
    public final String getContainerName() {
        return client.getContainerName();
    }

    /**
     * Decodes and gets the blob name.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * String blobName = client.getBlobName&#40;&#41;;
     * System.out.println&#40;&quot;The name of the blob is &quot; + blobName&#41;;
     * </pre>
     *
     * @return The decoded name of the blob.
     */
    public final String getBlobName() {
        return client.getBlobName();
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
     * Gets the {@link CpkInfo} used to encrypt this blob's content on the server.
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
    String getEncryptionScope() {
        return client.getEncryptionScope();
    }

    /**
     * Gets the service version the client is using.
     *
     * @return the service version the client is using.
     */
    public BlobServiceVersion getServiceVersion() {
        return client.getServiceVersion();
    }

    /**
     * Gets the snapshotId for a blob resource
     *
     * @return A string that represents the snapshotId of the snapshot blob
     */
    public String getSnapshotId() {
        return client.getSnapshotId();
    }

    /**
     * Gets the versionId for a blob resource
     *
     * @return A string that represents the versionId of the snapshot blob
     */
    public String getVersionId() {
        return client.getVersionId();
    }

    /**
     * Determines if a blob is a snapshot
     *
     * @return A boolean that indicates if a blob is a snapshot
     */
    public boolean isSnapshot() {
        return client.isSnapshot();
    }

    /**
     * Opens a blob input stream to download the blob.
     * <p>
     *
     * @return An <code>InputStream</code> object that represents the stream to use for reading from the blob.
     * @throws BlobStorageException If a storage service error occurred.
     */
    public final BlobInputStream openInputStream() {
        return openInputStream(new BlobRange(0), null);
    }

    /**
     * Opens a blob input stream to download the specified range of the blob.
     * <p>
     *
     * @param range {@link BlobRange}
     * @param requestConditions An {@link BlobRequestConditions} object that represents the access conditions for the
     * blob.
     * @return An <code>InputStream</code> object that represents the stream to use for reading from the blob.
     * @throws BlobStorageException If a storage service error occurred.
     */
    public final BlobInputStream openInputStream(BlobRange range, BlobRequestConditions requestConditions) {
        return new BlobInputStream(client, range.getOffset(), range.getCount(), requestConditions);
    }

    /**
     * Gets if the blob this client represents exists in the cloud.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Exists? %b%n&quot;, client.exists&#40;&#41;&#41;;
     * </pre>
     *
     * @return true if the blob exists, false if it doesn't
     */
    public Boolean exists() {
        return existsWithResponse(null, Context.NONE).getValue();
    }

    /**
     * Gets if the blob this client represents exists in the cloud.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Exists? %b%n&quot;, client.existsWithResponse&#40;timeout, new Context&#40;key2, value2&#41;&#41;.getValue&#40;&#41;&#41;;
     * </pre>
     *
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return true if the blob exists, false if it doesn't
     */
    public Response<Boolean> existsWithResponse(Duration timeout, Context context) {
        Mono<Response<Boolean>> response = client.existsWithResponse(context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Copies the data at the source URL to a blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * final SyncPoller&lt;BlobCopyInfo, Void&gt; poller = client.beginCopy&#40;url, Duration.ofSeconds&#40;2&#41;&#41;;
     * PollResponse&lt;BlobCopyInfo&gt; pollResponse = poller.poll&#40;&#41;;
     * System.out.printf&#40;&quot;Copy identifier: %s%n&quot;, pollResponse.getValue&#40;&#41;.getCopyId&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/copy-blob">Azure Docs</a></p>
     *
     * @param sourceUrl The source URL to copy from. URLs outside of Azure may only be copied to block blobs.
     * @param pollInterval Duration between each poll for the copy status. If none is specified, a default of one second
     * is used.
     * @return A {@link SyncPoller} to poll the progress of blob copy operation.
     */
    public SyncPoller<BlobCopyInfo, Void> beginCopy(String sourceUrl, Duration pollInterval) {
        return beginCopy(sourceUrl,
                null,
                null,
                null,
                null,
                null, pollInterval);
    }

    /**
     * Copies the data at the source URL to a blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * RequestConditions modifiedRequestConditions = new RequestConditions&#40;&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;7&#41;&#41;;
     * BlobRequestConditions blobRequestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * SyncPoller&lt;BlobCopyInfo, Void&gt; poller = client.beginCopy&#40;url, metadata, AccessTier.HOT,
     *     RehydratePriority.STANDARD, modifiedRequestConditions, blobRequestConditions, Duration.ofSeconds&#40;2&#41;&#41;;
     * 
     * PollResponse&lt;BlobCopyInfo&gt; response = poller.waitUntil&#40;LongRunningOperationStatus.SUCCESSFULLY_COMPLETED&#41;;
     * System.out.printf&#40;&quot;Copy identifier: %s%n&quot;, response.getValue&#40;&#41;.getCopyId&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/copy-blob">Azure Docs</a></p>
     *
     * @param sourceUrl The source URL to copy from. URLs outside of Azure may only be copied to block blobs.
     * @param metadata Metadata to associate with the destination blob.
     * @param tier {@link AccessTier} for the destination blob.
     * @param priority {@link RehydratePriority} for rehydrating the blob.
     * @param sourceModifiedRequestConditions {@link RequestConditions} against the source. Standard HTTP Access
     * conditions related to the modification of data. ETag and LastModifiedTime are used to construct conditions
     * related to when the blob was changed relative to the given request. The request will fail if the specified
     * condition is not satisfied.
     * @param destRequestConditions {@link BlobRequestConditions} against the destination.
     * @param pollInterval Duration between each poll for the copy status. If none is specified, a default of one second
     * is used.
     * @return A {@link SyncPoller} to poll the progress of blob copy operation.
     */
    public SyncPoller<BlobCopyInfo, Void> beginCopy(String sourceUrl, Map<String, String> metadata, AccessTier tier,
            RehydratePriority priority, RequestConditions sourceModifiedRequestConditions,
            BlobRequestConditions destRequestConditions, Duration pollInterval) {
        return this.beginCopy(new BlobBeginCopyOptions(sourceUrl).setMetadata(metadata).setTier(tier)
            .setRehydratePriority(priority).setSourceRequestConditions(
                ModelHelper.populateBlobSourceRequestConditions(sourceModifiedRequestConditions))
            .setDestinationRequestConditions(destRequestConditions).setPollInterval(pollInterval));
    }

    /**
     * Copies the data at the source URL to a blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * Map&lt;String, String&gt; tags = Collections.singletonMap&#40;&quot;tag&quot;, &quot;value&quot;&#41;;
     * BlobBeginCopySourceRequestConditions modifiedRequestConditions = new BlobBeginCopySourceRequestConditions&#40;&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;7&#41;&#41;;
     * BlobRequestConditions blobRequestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * SyncPoller&lt;BlobCopyInfo, Void&gt; poller = client.beginCopy&#40;new BlobBeginCopyOptions&#40;url&#41;.setMetadata&#40;metadata&#41;
     *     .setTags&#40;tags&#41;.setTier&#40;AccessTier.HOT&#41;.setRehydratePriority&#40;RehydratePriority.STANDARD&#41;
     *     .setSourceRequestConditions&#40;modifiedRequestConditions&#41;
     *     .setDestinationRequestConditions&#40;blobRequestConditions&#41;.setPollInterval&#40;Duration.ofSeconds&#40;2&#41;&#41;&#41;;
     * 
     * PollResponse&lt;BlobCopyInfo&gt; response = poller.waitUntil&#40;LongRunningOperationStatus.SUCCESSFULLY_COMPLETED&#41;;
     * System.out.printf&#40;&quot;Copy identifier: %s%n&quot;, response.getValue&#40;&#41;.getCopyId&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/rest/api/storageservices/copy-blob">Azure Docs</a></p>
     *
     * @param options {@link BlobBeginCopyOptions}
     * @return A {@link SyncPoller} to poll the progress of blob copy operation.
     */
    public SyncPoller<BlobCopyInfo, Void> beginCopy(BlobBeginCopyOptions options) {
        return client.beginCopy(options).getSyncPoller();
    }



    /**
     * Stops a pending copy that was previously started and leaves a destination blob with 0 length and metadata.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.abortCopyFromUrl&#40;copyId&#41;;
     * System.out.println&#40;&quot;Aborted copy completed.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/abort-copy-blob">Azure Docs</a></p>
     *
     * @param copyId The id of the copy operation to abort.
     */
    public void abortCopyFromUrl(String copyId) {
        abortCopyFromUrlWithResponse(copyId, null, null, Context.NONE);
    }

    /**
     * Stops a pending copy that was previously started and leaves a destination blob with 0 length and metadata.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Aborted copy completed with status %d%n&quot;,
     *     client.abortCopyFromUrlWithResponse&#40;copyId, leaseId, timeout,
     *         new Context&#40;key2, value2&#41;&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/abort-copy-blob">Azure Docs</a></p>
     *
     * @param copyId The id of the copy operation to abort.
     * @param leaseId The lease ID the active lease on the blob must match.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> abortCopyFromUrlWithResponse(String copyId, String leaseId, Duration timeout,
            Context context) {
        return blockWithOptionalTimeout(client.abortCopyFromUrlWithResponse(copyId, leaseId, context), timeout);
    }

    /**
     * Copies the data at the source URL to a blob and waits for the copy to complete before returning a response.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Copy identifier: %s%n&quot;, client.copyFromUrl&#40;url&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/copy-blob-from-url">Azure Docs</a></p>
     *
     * @param copySource The source URL to copy from.
     * @return The copy ID for the long running operation.
     * @throws IllegalArgumentException If {@code copySource} is a malformed {@link URL}.
     */
    public String copyFromUrl(String copySource) {
        return copyFromUrlWithResponse(copySource, null, null, null, null, null, Context.NONE).getValue();
    }

    /**
     * Copies the data at the source URL to a blob and waits for the copy to complete before returning a response.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * RequestConditions modifiedRequestConditions = new RequestConditions&#40;&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;7&#41;&#41;;
     * BlobRequestConditions blobRequestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * 
     * System.out.printf&#40;&quot;Copy identifier: %s%n&quot;,
     *     client.copyFromUrlWithResponse&#40;url, metadata, AccessTier.HOT, modifiedRequestConditions,
     *         blobRequestConditions, timeout,
     *         new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/copy-blob-from-url">Azure Docs</a></p>
     *
     * @param copySource The source URL to copy from. URLs outside of Azure may only be copied to block blobs.
     * @param metadata Metadata to associate with the destination blob.
     * @param tier {@link AccessTier} for the destination blob.
     * @param sourceModifiedRequestConditions {@link RequestConditions} against the source. Standard HTTP Access
     * conditions related to the modification of data. ETag and LastModifiedTime are used to construct conditions
     * related to when the blob was changed relative to the given request. The request will fail if the specified
     * condition is not satisfied.
     * @param destRequestConditions {@link BlobRequestConditions} against the destination.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The copy ID for the long running operation.
     * @throws IllegalArgumentException If {@code copySource} is a malformed {@link URL}.
     */
    public Response<String> copyFromUrlWithResponse(String copySource, Map<String, String> metadata, AccessTier tier,
            RequestConditions sourceModifiedRequestConditions, BlobRequestConditions destRequestConditions,
            Duration timeout, Context context) {
        return this.copyFromUrlWithResponse(new BlobCopyFromUrlOptions(copySource).setMetadata(metadata)
            .setTier(tier).setSourceRequestConditions(sourceModifiedRequestConditions)
            .setDestinationRequestConditions(destRequestConditions), timeout, context);
    }

    /**
     * Copies the data at the source URL to a blob and waits for the copy to complete before returning a response.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * Map&lt;String, String&gt; tags = Collections.singletonMap&#40;&quot;tag&quot;, &quot;value&quot;&#41;;
     * RequestConditions modifiedRequestConditions = new RequestConditions&#40;&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;7&#41;&#41;;
     * BlobRequestConditions blobRequestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * 
     * System.out.printf&#40;&quot;Copy identifier: %s%n&quot;,
     *     client.copyFromUrlWithResponse&#40;new BlobCopyFromUrlOptions&#40;url&#41;.setMetadata&#40;metadata&#41;.setTags&#40;tags&#41;
     *         .setTier&#40;AccessTier.HOT&#41;.setSourceRequestConditions&#40;modifiedRequestConditions&#41;
     *         .setDestinationRequestConditions&#40;blobRequestConditions&#41;, timeout,
     *         new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/copy-blob-from-url">Azure Docs</a></p>
     *
     * @param options {@link BlobCopyFromUrlOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The copy ID for the long running operation.
     * @throws IllegalArgumentException If {@code copySource} is a malformed {@link URL}.
     */
    public Response<String> copyFromUrlWithResponse(BlobCopyFromUrlOptions options, Duration timeout,
        Context context) {
        Mono<Response<String>> response = client
            .copyFromUrlWithResponse(options, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Downloads the entire blob into an output stream. Uploading data must be done from the {@link BlockBlobClient},
     * {@link PageBlobClient}, or {@link AppendBlobClient}.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.download&#40;new ByteArrayOutputStream&#40;&#41;&#41;;
     * System.out.println&#40;&quot;Download completed.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob">Azure Docs</a></p>
     *
     * @param stream A non-null {@link OutputStream} instance where the downloaded data will be written.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws NullPointerException if {@code stream} is null
     */
    public void download(OutputStream stream) {
        downloadWithResponse(stream, null, null, null, false, null, Context.NONE);
    }

    /**
     * Downloads a range of bytes from a blob into an output stream. Uploading data must be done from the {@link
     * BlockBlobClient}, {@link PageBlobClient}, or {@link AppendBlobClient}.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobRange range = new BlobRange&#40;1024, 2048L&#41;;
     * DownloadRetryOptions options = new DownloadRetryOptions&#40;&#41;.setMaxRetryRequests&#40;5&#41;;
     * 
     * System.out.printf&#40;&quot;Download completed with status %d%n&quot;,
     *     client.downloadWithResponse&#40;new ByteArrayOutputStream&#40;&#41;, range, options, null, false,
     *         timeout, new Context&#40;key2, value2&#41;&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob">Azure Docs</a></p>
     *
     * @param stream A non-null {@link OutputStream} instance where the downloaded data will be written.
     * @param range {@link BlobRange}
     * @param options {@link DownloadRetryOptions}
     * @param requestConditions {@link BlobRequestConditions}
     * @param getRangeContentMd5 Whether the contentMD5 for the specified blob range should be returned.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws NullPointerException if {@code stream} is null
     */
    public BlobDownloadResponse downloadWithResponse(OutputStream stream, BlobRange range,
        DownloadRetryOptions options, BlobRequestConditions requestConditions, boolean getRangeContentMd5,
        Duration timeout, Context context) {
        StorageImplUtils.assertNotNull("stream", stream);
        Mono<BlobDownloadResponse> download = client
            .downloadWithResponse(range, options, requestConditions, getRangeContentMd5, context)
            .flatMap(response -> response.getValue().reduce(stream, (outputStream, buffer) -> {
                try {
                    outputStream.write(FluxUtil.byteBufferToArray(buffer));
                    return outputStream;
                } catch (IOException ex) {
                    throw logger.logExceptionAsError(Exceptions.propagate(new UncheckedIOException(ex)));
                }
            }).thenReturn(new BlobDownloadResponse(response)));

        return blockWithOptionalTimeout(download, timeout);
    }

    /**
     * Downloads the entire blob into a file specified by the path.
     *
     * <p>The file will be created and must not exist, if the file already exists a {@link FileAlreadyExistsException}
     * will be thrown.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.downloadToFile&#40;file&#41;;
     * System.out.println&#40;&quot;Completed download to file&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob">Azure Docs</a></p>
     *
     * @param filePath A {@link String} representing the filePath where the downloaded data will be written.
     * @return The blob properties and metadata.
     * @throws UncheckedIOException If an I/O error occurs
     */
    public BlobProperties downloadToFile(String filePath) {
        return downloadToFile(filePath, false);
    }

    /**
     * Downloads the entire blob into a file specified by the path.
     *
     * <p>If overwrite is set to false, the file will be created and must not exist, if the file already exists a
     * {@link FileAlreadyExistsException} will be thrown.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * boolean overwrite = false; &#47;&#47; Default value
     * client.downloadToFile&#40;file, overwrite&#41;;
     * System.out.println&#40;&quot;Completed download to file&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob">Azure Docs</a></p>
     *
     * @param filePath A {@link String} representing the filePath where the downloaded data will be written.
     * @param overwrite Whether or not to overwrite the file, should the file exist.
     * @return The blob properties and metadata.
     * @throws UncheckedIOException If an I/O error occurs
     */
    public BlobProperties downloadToFile(String filePath, boolean overwrite) {
        Set<OpenOption> openOptions = null;
        if (overwrite) {
            openOptions = new HashSet<>();
            openOptions.add(StandardOpenOption.CREATE);
            openOptions.add(StandardOpenOption.TRUNCATE_EXISTING); // If the file already exists and it is opened
            // for WRITE access, then its length is truncated to 0.
            openOptions.add(StandardOpenOption.READ);
            openOptions.add(StandardOpenOption.WRITE);
        }
        return downloadToFileWithResponse(filePath, null, null, null, null, false, openOptions, null, Context.NONE)
            .getValue();
    }

    /**
     * Downloads the entire blob into a file specified by the path.
     *
     * <p>The file will be created and must not exist, if the file already exists a {@link FileAlreadyExistsException}
     * will be thrown.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobRange range = new BlobRange&#40;1024, 2048L&#41;;
     * DownloadRetryOptions options = new DownloadRetryOptions&#40;&#41;.setMaxRetryRequests&#40;5&#41;;
     * 
     * client.downloadToFileWithResponse&#40;file, range, new ParallelTransferOptions&#40;&#41;.setBlockSizeLong&#40;4L * Constants.MB&#41;,
     *     options, null, false, timeout, new Context&#40;key2, value2&#41;&#41;;
     * System.out.println&#40;&quot;Completed download to file&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob">Azure Docs</a></p>
     *
     * @param filePath A {@link String} representing the filePath where the downloaded data will be written.
     * @param range {@link BlobRange}
     * @param parallelTransferOptions {@link ParallelTransferOptions} to use to download to file. Number of parallel
     *        transfers parameter is ignored.
     * @param downloadRetryOptions {@link DownloadRetryOptions}
     * @param requestConditions {@link BlobRequestConditions}
     * @param rangeGetContentMd5 Whether the contentMD5 for the specified blob range should be returned.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the blob properties and metadata.
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public Response<BlobProperties> downloadToFileWithResponse(String filePath, BlobRange range,
        ParallelTransferOptions parallelTransferOptions, DownloadRetryOptions downloadRetryOptions,
        BlobRequestConditions requestConditions, boolean rangeGetContentMd5, Duration timeout, Context context) {
        return downloadToFileWithResponse(filePath, range, parallelTransferOptions, downloadRetryOptions,
            requestConditions, rangeGetContentMd5, null, timeout, context);
    }

    /**
     * Downloads the entire blob into a file specified by the path.
     *
     * <p>By default the file will be created and must not exist, if the file already exists a
     * {@link FileAlreadyExistsException} will be thrown. To override this behavior, provide appropriate
     * {@link OpenOption OpenOptions} </p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobRange blobRange = new BlobRange&#40;1024, 2048L&#41;;
     * DownloadRetryOptions downloadRetryOptions = new DownloadRetryOptions&#40;&#41;.setMaxRetryRequests&#40;5&#41;;
     * Set&lt;OpenOption&gt; openOptions = new HashSet&lt;&gt;&#40;Arrays.asList&#40;StandardOpenOption.CREATE_NEW,
     *     StandardOpenOption.WRITE, StandardOpenOption.READ&#41;&#41;; &#47;&#47; Default options
     * 
     * client.downloadToFileWithResponse&#40;file, blobRange, new ParallelTransferOptions&#40;&#41;.setBlockSizeLong&#40;4L * Constants.MB&#41;,
     *     downloadRetryOptions, null, false, openOptions, timeout, new Context&#40;key2, value2&#41;&#41;;
     * System.out.println&#40;&quot;Completed download to file&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob">Azure Docs</a></p>
     *
     * @param filePath A {@link String} representing the filePath where the downloaded data will be written.
     * @param range {@link BlobRange}
     * @param parallelTransferOptions {@link ParallelTransferOptions} to use to download to file. Number of parallel
     *        transfers parameter is ignored.
     * @param downloadRetryOptions {@link DownloadRetryOptions}
     * @param requestConditions {@link BlobRequestConditions}
     * @param rangeGetContentMd5 Whether the contentMD5 for the specified blob range should be returned.
     * @param openOptions {@link OpenOption OpenOptions} to use to configure how to open or create the file.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the blob properties and metadata.
     * @throws UncheckedIOException If an I/O error occurs.
     */
    public Response<BlobProperties> downloadToFileWithResponse(String filePath, BlobRange range,
        ParallelTransferOptions parallelTransferOptions, DownloadRetryOptions downloadRetryOptions,
        BlobRequestConditions requestConditions, boolean rangeGetContentMd5, Set<OpenOption> openOptions,
        Duration timeout, Context context) {
        Mono<Response<BlobProperties>> download = client.downloadToFileWithResponse(filePath, range,
            parallelTransferOptions, downloadRetryOptions, requestConditions, rangeGetContentMd5, openOptions, context);
        return blockWithOptionalTimeout(download, timeout);
    }

    /**
     * Deletes the specified blob or snapshot. Note that deleting a blob also deletes all its snapshots.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.delete&#40;&#41;;
     * System.out.println&#40;&quot;Delete completed.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-blob">Azure Docs</a></p>
     */
    public void delete() {
        deleteWithResponse(null, null, null, Context.NONE);
    }

    /**
     * Deletes the specified blob or snapshot. Note that deleting a blob also deletes all its snapshots.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Delete completed with status %d%n&quot;,
     *     client.deleteWithResponse&#40;DeleteSnapshotsOptionType.INCLUDE, null, timeout,
     *         new Context&#40;key1, value1&#41;&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-blob">Azure Docs</a></p>
     *
     * @param deleteBlobSnapshotOptions Specifies the behavior for deleting the snapshots on this blob. {@code Include}
     * will delete the base blob and all snapshots. {@code Only} will delete only the snapshots. If a snapshot is being
     * deleted, you must pass null.
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> deleteWithResponse(DeleteSnapshotsOptionType deleteBlobSnapshotOptions,
        BlobRequestConditions requestConditions, Duration timeout, Context context) {
        Mono<Response<Void>> response = client
            .deleteWithResponse(deleteBlobSnapshotOptions, requestConditions, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Returns the blob's metadata and properties.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobProperties properties = client.getProperties&#40;&#41;;
     * System.out.printf&#40;&quot;Type: %s, Size: %d%n&quot;, properties.getBlobType&#40;&#41;, properties.getBlobSize&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-properties">Azure Docs</a></p>
     *
     * @return The blob properties and metadata.
     */
    public BlobProperties getProperties() {
        return getPropertiesWithResponse(null, null, Context.NONE).getValue();
    }

    /**
     * Returns the blob's metadata and properties.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * 
     * BlobProperties properties = client.getPropertiesWithResponse&#40;requestConditions, timeout,
     *     new Context&#40;key2, value2&#41;&#41;.getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Type: %s, Size: %d%n&quot;, properties.getBlobType&#40;&#41;, properties.getBlobSize&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-properties">Azure Docs</a></p>
     *
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The blob properties and metadata.
     */
    public Response<BlobProperties> getPropertiesWithResponse(BlobRequestConditions requestConditions, Duration timeout,
        Context context) {
        Mono<Response<BlobProperties>> response = client.getPropertiesWithResponse(requestConditions, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Changes a blob's HTTP header properties. if only one HTTP header is updated, the others will all be erased. In
     * order to preserve existing values, they must be passed alongside the header being changed.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.setHttpHeaders&#40;new BlobHttpHeaders&#40;&#41;
     *     .setContentLanguage&#40;&quot;en-US&quot;&#41;
     *     .setContentType&#40;&quot;binary&quot;&#41;&#41;;
     * System.out.println&#40;&quot;Set HTTP headers completed&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-properties">Azure Docs</a></p>
     *
     * @param headers {@link BlobHttpHeaders}
     */
    public void setHttpHeaders(BlobHttpHeaders headers) {
        setHttpHeadersWithResponse(headers, null, null, Context.NONE);
    }

    /**
     * Changes a blob's HTTP header properties. if only one HTTP header is updated, the others will all be erased. In
     * order to preserve existing values, they must be passed alongside the header being changed.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * 
     * System.out.printf&#40;&quot;Set HTTP headers completed with status %d%n&quot;,
     *     client.setHttpHeadersWithResponse&#40;new BlobHttpHeaders&#40;&#41;
     *         .setContentLanguage&#40;&quot;en-US&quot;&#41;
     *         .setContentType&#40;&quot;binary&quot;&#41;, requestConditions, timeout, new Context&#40;key1, value1&#41;&#41;
     *         .getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-properties">Azure Docs</a></p>
     *
     * @param headers {@link BlobHttpHeaders}
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> setHttpHeadersWithResponse(BlobHttpHeaders headers, BlobRequestConditions requestConditions,
        Duration timeout, Context context) {
        Mono<Response<Void>> response = client
            .setHttpHeadersWithResponse(headers, requestConditions, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Changes a blob's metadata. The specified metadata in this method will replace existing metadata. If old values
     * must be preserved, they must be downloaded and included in the call to this method.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.setMetadata&#40;Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;&#41;;
     * System.out.println&#40;&quot;Set metadata completed&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-metadata">Azure Docs</a></p>
     *
     * @param metadata Metadata to associate with the blob.
     */
    public void setMetadata(Map<String, String> metadata) {
        setMetadataWithResponse(metadata, null, null, Context.NONE);
    }

    /**
     * Changes a blob's metadata. The specified metadata in this method will replace existing metadata. If old values
     * must be preserved, they must be downloaded and included in the call to this method.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * 
     * System.out.printf&#40;&quot;Set metadata completed with status %d%n&quot;,
     *     client.setMetadataWithResponse&#40;Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;, requestConditions, timeout,
     *         new Context&#40;key1, value1&#41;&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-metadata">Azure Docs</a></p>
     *
     * @param metadata Metadata to associate with the blob.
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> setMetadataWithResponse(Map<String, String> metadata, BlobRequestConditions requestConditions,
        Duration timeout, Context context) {
        Mono<Response<Void>> response = client.setMetadataWithResponse(metadata, requestConditions, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Returns the blob's tags.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; tags = client.getTags&#40;&#41;;
     * System.out.printf&#40;&quot;Number of tags: %d%n&quot;, tags.size&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-tags">Azure Docs</a></p>
     *
     * @return The blob's tags.
     */
    public Map<String, String> getTags() {
        return this.getTagsWithResponse(new BlobGetTagsOptions(), null, Context.NONE).getValue();
    }

    /**
     * Returns the blob's tags.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; tags = client.getTagsWithResponse&#40;new BlobGetTagsOptions&#40;&#41;, timeout,
     *     new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Number of tags: %d%n&quot;, tags.size&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-blob-tags">Azure Docs</a></p>
     *
     * @param options {@link BlobGetTagsOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The blob's tags.
     */
    public Response<Map<String, String>> getTagsWithResponse(BlobGetTagsOptions options, Duration timeout,
        Context context) {
        Mono<Response<Map<String, String>>> response = client.getTagsWithResponse(options, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Sets user defined tags. The specified tags in this method will replace existing tags. If old values
     * must be preserved, they must be downloaded and included in the call to this method.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.setTags&#40;Collections.singletonMap&#40;&quot;tag&quot;, &quot;value&quot;&#41;&#41;;
     * System.out.println&#40;&quot;Set tag completed&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-tags">Azure Docs</a></p>
     *
     * @param tags Tags to associate with the blob.
     */
    public void setTags(Map<String, String> tags) {
        this.setTagsWithResponse(new BlobSetTagsOptions(tags), null, Context.NONE);
    }

    /**
     * Sets user defined tags. The specified tags in this method will replace existing tags. If old values
     * must be preserved, they must be downloaded and included in the call to this method.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Set metadata completed with status %d%n&quot;,
     *     client.setTagsWithResponse&#40;new BlobSetTagsOptions&#40;Collections.singletonMap&#40;&quot;tag&quot;, &quot;value&quot;&#41;&#41;, timeout,
     *         new Context&#40;key1, value1&#41;&#41;
     *         .getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-tags">Azure Docs</a></p>
     *
     * @param options {@link BlobSetTagsOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> setTagsWithResponse(BlobSetTagsOptions options, Duration timeout, Context context) {
        Mono<Response<Void>> response = client.setTagsWithResponse(options, context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Creates a read-only snapshot of the blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Identifier for the snapshot is %s%n&quot;, client.createSnapshot&#40;&#41;.getSnapshotId&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/snapshot-blob">Azure Docs</a></p>
     *
     * @return A response containing a {@link BlobClientBase} which is used to interact with the created snapshot, use
     * {@link BlobClientBase#getSnapshotId()} to get the identifier for the snapshot.
     */
    public BlobClientBase createSnapshot() {
        return createSnapshotWithResponse(null, null, null, Context.NONE).getValue();
    }


    /**
     * Creates a read-only snapshot of the blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * Map&lt;String, String&gt; snapshotMetadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * 
     * System.out.printf&#40;&quot;Identifier for the snapshot is %s%n&quot;,
     *     client.createSnapshotWithResponse&#40;snapshotMetadata, requestConditions, timeout,
     *         new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/snapshot-blob">Azure Docs</a></p>
     *
     * @param metadata Metadata to associate with the blob snapshot.
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing a {@link BlobClientBase} which is used to interact with the created snapshot, use
     * {@link BlobClientBase#getSnapshotId()} to get the identifier for the snapshot.
     */
    public Response<BlobClientBase> createSnapshotWithResponse(Map<String, String> metadata,
        BlobRequestConditions requestConditions, Duration timeout, Context context) {
        Mono<Response<BlobClientBase>> response = client
            .createSnapshotWithResponse(metadata, requestConditions, context)
            .map(rb -> new SimpleResponse<>(rb, new BlobClientBase(rb.getValue())));

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Sets the tier on a blob. The operation is allowed on a page blob in a premium storage account or a block blob in
     * a blob storage or GPV2 account. A premium page blob's tier determines the allowed size, IOPS, and bandwidth of
     * the blob. A block blob's tier determines the Hot/Cool/Archive storage type. This does not update the blob's
     * etag.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.setAccessTier&#40;AccessTier.HOT&#41;;
     * System.out.println&#40;&quot;Set tier completed.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-tier">Azure Docs</a></p>
     *
     * @param tier The new tier for the blob.
     */
    public void setAccessTier(AccessTier tier) {
        setAccessTierWithResponse(tier, null, null, null, Context.NONE);
    }

    /**
     * Sets the tier on a blob. The operation is allowed on a page blob in a premium storage account or a block blob in
     * a blob storage or GPV2 account. A premium page blob's tier determines the allowed size, IOPS, and bandwidth of
     * the blob. A block blob's tier determines the Hot/Cool/Archive storage type. This does not update the blob's
     * etag.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Set tier completed with status code %d%n&quot;,
     *     client.setAccessTierWithResponse&#40;AccessTier.HOT, RehydratePriority.STANDARD, leaseId, timeout,
     *         new Context&#40;key2, value2&#41;&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-tier">Azure Docs</a></p>
     *
     * @param tier The new tier for the blob.
     * @param priority Optional priority to set for re-hydrating blobs.
     * @param leaseId The lease ID the active lease on the blob must match.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> setAccessTierWithResponse(AccessTier tier, RehydratePriority priority, String leaseId,
        Duration timeout, Context context) {
        return setAccessTierWithResponse(new BlobSetAccessTierOptions(tier).setPriority(priority).setLeaseId(leaseId),
            timeout, context);
    }

    /**
     * Sets the tier on a blob. The operation is allowed on a page blob in a premium storage account or a block blob in
     * a blob storage or GPV2 account. A premium page blob's tier determines the allowed size, IOPS, and bandwidth of
     * the blob. A block blob's tier determines the Hot/Cool/Archive storage type. This does not update the blob's
     * etag.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Set tier completed with status code %d%n&quot;,
     *     client.setAccessTierWithResponse&#40;new BlobSetAccessTierOptions&#40;AccessTier.HOT&#41;
     *         .setPriority&#40;RehydratePriority.STANDARD&#41;
     *         .setLeaseId&#40;leaseId&#41;
     *         .setTagsConditions&#40;tags&#41;,
     *         timeout, new Context&#40;key2, value2&#41;&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-blob-tier">Azure Docs</a></p>
     *
     * @param options {@link BlobSetAccessTierOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> setAccessTierWithResponse(BlobSetAccessTierOptions options,
        Duration timeout, Context context) {
        return blockWithOptionalTimeout(client.setTierWithResponse(options, context), timeout);
    }

    /**
     * Undelete restores the content and metadata of a soft-deleted blob and/or any associated soft-deleted snapshots.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.undelete&#40;&#41;;
     * System.out.println&#40;&quot;Undelete completed&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/undelete-blob">Azure Docs</a></p>
     */
    public void undelete() {
        undeleteWithResponse(null, Context.NONE);
    }

    /**
     * Undelete restores the content and metadata of a soft-deleted blob and/or any associated soft-deleted snapshots.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Undelete completed with status %d%n&quot;, client.undeleteWithResponse&#40;timeout,
     *     new Context&#40;key1, value1&#41;&#41;.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/undelete-blob">Azure Docs</a></p>
     *
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     */
    public Response<Void> undeleteWithResponse(Duration timeout, Context context) {
        Mono<Response<Void>> response = client.undeleteWithResponse(context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Returns the sku name and account kind for the account.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * StorageAccountInfo accountInfo = client.getAccountInfo&#40;&#41;;
     * System.out.printf&#40;&quot;Account Kind: %s, SKU: %s%n&quot;, accountInfo.getAccountKind&#40;&#41;, accountInfo.getSkuName&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-account-information">Azure Docs</a></p>
     *
     * @return The sku name and account kind.
     */
    public StorageAccountInfo getAccountInfo() {
        return getAccountInfoWithResponse(null, Context.NONE).getValue();
    }

    /**
     * Returns the sku name and account kind for the account.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * StorageAccountInfo accountInfo = client.getAccountInfoWithResponse&#40;timeout, new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Account Kind: %s, SKU: %s%n&quot;, accountInfo.getAccountKind&#40;&#41;, accountInfo.getSkuName&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-account-information">Azure Docs</a></p>
     *
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The sku name and account kind.
     */
    public Response<StorageAccountInfo> getAccountInfoWithResponse(Duration timeout, Context context) {
        Mono<Response<StorageAccountInfo>> response = client.getAccountInfoWithResponse(context);

        return blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Generates a user delegation SAS for the blob using the specified {@link BlobServiceSasSignatureValues}.
     * <p>See {@link BlobServiceSasSignatureValues} for more information on how to construct a user delegation SAS.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * OffsetDateTime myExpiryTime = OffsetDateTime.now&#40;&#41;.plusDays&#40;1&#41;;
     * BlobSasPermission myPermission = new BlobSasPermission&#40;&#41;.setReadPermission&#40;true&#41;;
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
     * @return A {@code String} representing all SAS query parameters.
     */
    public String generateUserDelegationSas(BlobServiceSasSignatureValues blobServiceSasSignatureValues,
        UserDelegationKey userDelegationKey) {
        return this.client.generateUserDelegationSas(blobServiceSasSignatureValues, userDelegationKey);
    }

    /**
     * Generates a service SAS for the blob using the specified {@link BlobServiceSasSignatureValues}
     * Note : The client must be authenticated via {@link StorageSharedKeyCredential}
     * <p>See {@link BlobServiceSasSignatureValues} for more information on how to construct a service SAS.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * OffsetDateTime expiryTime = OffsetDateTime.now&#40;&#41;.plusDays&#40;1&#41;;
     * BlobSasPermission permission = new BlobSasPermission&#40;&#41;.setReadPermission&#40;true&#41;;
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

    /**
     * Opens a blob input stream to query the blob.
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/query-blob-contents">Azure Docs</a></p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * String expression = &quot;SELECT * from BlobStorage&quot;;
     * InputStream inputStream = client.openQueryInputStream&#40;expression&#41;;
     * &#47;&#47; Now you can read from the input stream like you would normally.
     * </pre>
     *
     * @param expression The query expression.
     * @return An <code>InputStream</code> object that represents the stream to use for reading the query response.
     */
    public InputStream openQueryInputStream(String expression) {
        return openQueryInputStreamWithResponse(new BlobQueryOptions(expression)).getValue();
    }

    /**
     * Opens a blob input stream to query the blob.
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/query-blob-contents">Azure Docs</a></p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * String expression = &quot;SELECT * from BlobStorage&quot;;
     * BlobQuerySerialization input = new BlobQueryDelimitedSerialization&#40;&#41;
     *     .setColumnSeparator&#40;','&#41;
     *     .setEscapeChar&#40;'&#92;n'&#41;
     *     .setRecordSeparator&#40;'&#92;n'&#41;
     *     .setHeadersPresent&#40;true&#41;
     *     .setFieldQuote&#40;'&quot;'&#41;;
     * BlobQuerySerialization output = new BlobQueryJsonSerialization&#40;&#41;
     *     .setRecordSeparator&#40;'&#92;n'&#41;;
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;
     *     .setLeaseId&#40;&quot;leaseId&quot;&#41;;
     * Consumer&lt;BlobQueryError&gt; errorConsumer = System.out::println;
     * Consumer&lt;BlobQueryProgress&gt; progressConsumer = progress -&gt; System.out.println&#40;&quot;total blob bytes read: &quot;
     *     + progress.getBytesScanned&#40;&#41;&#41;;
     * BlobQueryOptions queryOptions = new BlobQueryOptions&#40;expression&#41;
     *     .setInputSerialization&#40;input&#41;
     *     .setOutputSerialization&#40;output&#41;
     *     .setRequestConditions&#40;requestConditions&#41;
     *     .setErrorConsumer&#40;errorConsumer&#41;
     *     .setProgressConsumer&#40;progressConsumer&#41;;
     * 
     * InputStream inputStream = client.openQueryInputStreamWithResponse&#40;queryOptions&#41;.getValue&#40;&#41;;
     * &#47;&#47; Now you can read from the input stream like you would normally.
     * </pre>
     *
     * @param queryOptions {@link BlobQueryOptions The query options}.
     * @return A response containing status code and HTTP headers including an <code>InputStream</code> object
     * that represents the stream to use for reading the query response.
     */
    public Response<InputStream> openQueryInputStreamWithResponse(BlobQueryOptions queryOptions) {

        // Data to subscribe to and read from.
        BlobQueryAsyncResponse response = client.queryWithResponse(queryOptions).block();

        // Create input stream from the data.
        if (response == null) {
            throw logger.logExceptionAsError(new IllegalStateException("Query response cannot be null"));
        }
        return new ResponseBase<>(response.getRequest(), response.getStatusCode(), response.getHeaders(),
            new FluxInputStream(response.getValue()), response.getDeserializedHeaders());
    }

    /**
     * Queries an entire blob into an output stream.
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/query-blob-contents">Azure Docs</a></p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * ByteArrayOutputStream queryData = new ByteArrayOutputStream&#40;&#41;;
     * String expression = &quot;SELECT * from BlobStorage&quot;;
     * client.query&#40;queryData, expression&#41;;
     * System.out.println&#40;&quot;Query completed.&quot;&#41;;
     * </pre>
     *
     * @param stream A non-null {@link OutputStream} instance where the downloaded data will be written.
     * @param expression The query expression.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws NullPointerException if {@code stream} is null.
     */
    public void query(OutputStream stream, String expression) {
        queryWithResponse(new BlobQueryOptions(expression, stream), null, Context.NONE);
    }

    /**
     * Queries an entire blob into an output stream.
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/query-blob-contents">Azure Docs</a></p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * ByteArrayOutputStream queryData = new ByteArrayOutputStream&#40;&#41;;
     * String expression = &quot;SELECT * from BlobStorage&quot;;
     * BlobQueryJsonSerialization input = new BlobQueryJsonSerialization&#40;&#41;
     *     .setRecordSeparator&#40;'&#92;n'&#41;;
     * BlobQueryDelimitedSerialization output = new BlobQueryDelimitedSerialization&#40;&#41;
     *     .setEscapeChar&#40;'&#92;0'&#41;
     *     .setColumnSeparator&#40;','&#41;
     *     .setRecordSeparator&#40;'&#92;n'&#41;
     *     .setFieldQuote&#40;'&#92;''&#41;
     *     .setHeadersPresent&#40;true&#41;;
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;;
     * Consumer&lt;BlobQueryError&gt; errorConsumer = System.out::println;
     * Consumer&lt;BlobQueryProgress&gt; progressConsumer = progress -&gt; System.out.println&#40;&quot;total blob bytes read: &quot;
     *     + progress.getBytesScanned&#40;&#41;&#41;;
     * BlobQueryOptions queryOptions = new BlobQueryOptions&#40;expression, queryData&#41;
     *     .setInputSerialization&#40;input&#41;
     *     .setOutputSerialization&#40;output&#41;
     *     .setRequestConditions&#40;requestConditions&#41;
     *     .setErrorConsumer&#40;errorConsumer&#41;
     *     .setProgressConsumer&#40;progressConsumer&#41;;
     * System.out.printf&#40;&quot;Query completed with status %d%n&quot;,
     *     client.queryWithResponse&#40;queryOptions, timeout, new Context&#40;key1, value1&#41;&#41;
     *         .getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * @param queryOptions {@link BlobQueryOptions The query options}.
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing status code and HTTP headers.
     * @throws UncheckedIOException If an I/O error occurs.
     * @throws NullPointerException if {@code stream} is null.
     */
    public BlobQueryResponse queryWithResponse(BlobQueryOptions queryOptions, Duration timeout, Context context) {
        StorageImplUtils.assertNotNull("options", queryOptions);
        StorageImplUtils.assertNotNull("outputStream", queryOptions.getOutputStream());
        Mono<BlobQueryResponse> download = client
            .queryWithResponse(queryOptions, context)
            .flatMap(response -> response.getValue().reduce(queryOptions.getOutputStream(), (outputStream, buffer) -> {
                try {
                    outputStream.write(FluxUtil.byteBufferToArray(buffer));
                    return outputStream;
                } catch (IOException ex) {
                    throw logger.logExceptionAsError(Exceptions.propagate(new UncheckedIOException(ex)));
                }
            }).thenReturn(new BlobQueryResponse(response)));

        return blockWithOptionalTimeout(download, timeout);
    }
}
