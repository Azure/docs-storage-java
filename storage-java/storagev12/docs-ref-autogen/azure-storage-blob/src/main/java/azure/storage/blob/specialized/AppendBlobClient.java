// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob.specialized;

import com.azure.core.annotation.ServiceClient;
import com.azure.core.exception.UnexpectedLengthException;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.options.AppendBlobCreateOptions;
import com.azure.storage.blob.models.AppendBlobItem;
import com.azure.storage.blob.models.AppendBlobRequestConditions;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.options.AppendBlobSealOptions;
import com.azure.storage.common.Utility;
import com.azure.storage.common.implementation.Constants;
import com.azure.storage.common.implementation.StorageImplUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

import static com.azure.storage.common.implementation.StorageImplUtils.blockWithOptionalTimeout;

/**
 * Client to an append blob. It may only be instantiated through a {@link SpecializedBlobClientBuilder} or via the
 * method {@link BlobClient#getAppendBlobClient()}. This class does not hold any state about a particular blob, but is
 * instead a convenient way of sending appropriate requests to the resource on the service.
 *
 * <p>
 * This client contains operations on a blob. Operations on a container are available on {@link BlobContainerClient},
 * and operations on the service are available on {@link BlobServiceClient}.
 *
 * <p>
 * Please refer to the <a href=https://docs.microsoft.com/en-us/rest/api/storageservices/understanding-block-blobs--append-blobs--and-page-blobs>Azure
 * Docs</a> for more information.
 */
@ServiceClient(builder = SpecializedBlobClientBuilder.class)
public final class AppendBlobClient extends BlobClientBase {
    private final AppendBlobAsyncClient appendBlobAsyncClient;

    /**
     * Indicates the maximum number of bytes that can be sent in a call to appendBlock.
     */
    public static final int MAX_APPEND_BLOCK_BYTES = AppendBlobAsyncClient.MAX_APPEND_BLOCK_BYTES;

    /**
     * Indicates the maximum number of blocks allowed in an append blob.
     */
    public static final int MAX_BLOCKS = AppendBlobAsyncClient.MAX_BLOCKS;

    /**
     * Package-private constructor for use by {@link BlobClientBuilder}.
     *
     * @param appendBlobAsyncClient the async append blob client
     */
    AppendBlobClient(AppendBlobAsyncClient appendBlobAsyncClient) {
        super(appendBlobAsyncClient);
        this.appendBlobAsyncClient = appendBlobAsyncClient;
    }

    /**
     * Creates and opens an output stream to write data to the append blob. If the blob already exists on the service,
     * it will be overwritten.
     *
     * @return A {@link BlobOutputStream} object used to write data to the blob.
     * @throws BlobStorageException If a storage service error occurred.
     */
    public BlobOutputStream getBlobOutputStream() {
        return getBlobOutputStream(null);
    }

    /**
     * Creates and opens an output stream to write data to the append blob. If the blob already exists on the service,
     * it will be overwritten.
     *
     * @param requestConditions A {@link BlobRequestConditions} object that represents the access conditions for the
     * blob.
     * @return A {@link BlobOutputStream} object used to write data to the blob.
     * @throws BlobStorageException If a storage service error occurred.
     */
    public BlobOutputStream getBlobOutputStream(AppendBlobRequestConditions requestConditions) {
        return BlobOutputStream.appendBlobOutputStream(appendBlobAsyncClient, requestConditions);
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob. By default this method will
     * not overwrite an existing blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;, client.create&#40;&#41;.getLastModified&#40;&#41;&#41;;
     * </pre>
     *
     * @return The information of the created appended blob.
     */
    public AppendBlobItem create() {
        return create(false);
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * boolean overwrite = false; &#47;&#47; Default value
     * System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;, client.create&#40;overwrite&#41;.getLastModified&#40;&#41;&#41;;
     * </pre>
     *
     * @param overwrite Whether or not to overwrite, should data exist on the blob.
     *
     * @return The information of the created appended blob.
     */
    public AppendBlobItem create(boolean overwrite) {
        BlobRequestConditions blobRequestConditions = new BlobRequestConditions();
        if (!overwrite) {
            blobRequestConditions.setIfNoneMatch(Constants.HeaderConstants.ETAG_WILDCARD);
        }
        return createWithResponse(null, null, blobRequestConditions, null, Context.NONE).getValue();
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob.
     * <p>
     * To avoid overwriting, pass "*" to {@link BlobRequestConditions#setIfNoneMatch(String)}.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobHttpHeaders headers = new BlobHttpHeaders&#40;&#41;
     *     .setContentType&#40;&quot;binary&quot;&#41;
     *     .setContentLanguage&#40;&quot;en-US&quot;&#41;;
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;
     *     .setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * Context context = new Context&#40;&quot;key&quot;, &quot;value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;,
     *     client.createWithResponse&#40;headers, metadata, requestConditions, timeout, context&#41;.getValue&#40;&#41;
     *         .getLastModified&#40;&#41;&#41;;
     * </pre>
     *
     * @param headers {@link BlobHttpHeaders}
     * @param metadata Metadata to associate with the blob.
     * @param requestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A {@link Response} whose {@link Response#getValue() value} contains the created appended blob.
     */
    public Response<AppendBlobItem> createWithResponse(BlobHttpHeaders headers, Map<String, String> metadata,
        BlobRequestConditions requestConditions, Duration timeout, Context context) {
        return this.createWithResponse(new AppendBlobCreateOptions().setHeaders(headers).setMetadata(metadata)
            .setRequestConditions(requestConditions), timeout, context);
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob.
     * <p>
     * To avoid overwriting, pass "*" to {@link BlobRequestConditions#setIfNoneMatch(String)}.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * BlobHttpHeaders headers = new BlobHttpHeaders&#40;&#41;
     *     .setContentType&#40;&quot;binary&quot;&#41;
     *     .setContentLanguage&#40;&quot;en-US&quot;&#41;;
     * Map&lt;String, String&gt; metadata = Collections.singletonMap&#40;&quot;metadata&quot;, &quot;value&quot;&#41;;
     * Map&lt;String, String&gt; tags = Collections.singletonMap&#40;&quot;tags&quot;, &quot;value&quot;&#41;;
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;
     *     .setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * Context context = new Context&#40;&quot;key&quot;, &quot;value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;,
     *     client.createWithResponse&#40;new AppendBlobCreateOptions&#40;&#41;.setHeaders&#40;headers&#41;.setMetadata&#40;metadata&#41;
     *         .setTags&#40;tags&#41;.setRequestConditions&#40;requestConditions&#41;, timeout, context&#41;.getValue&#40;&#41;
     *         .getLastModified&#40;&#41;&#41;;
     * </pre>
     *
     * @param options {@link AppendBlobCreateOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A {@link Response} whose {@link Response#getValue() value} contains the created appended blob.
     */
    public Response<AppendBlobItem> createWithResponse(AppendBlobCreateOptions options, Duration timeout,
        Context context) {
        return StorageImplUtils.blockWithOptionalTimeout(appendBlobAsyncClient.
            createWithResponse(options, context), timeout);
    }

    /**
     * Commits a new block of data to the end of the existing append blob.
     * <p>
     * Note that the data passed must be replayable if retries are enabled (the default). In other words, the
     * {@code Flux} must produce the same data each time it is subscribed to.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;,
     *     client.appendBlock&#40;data, length&#41;.getBlobCommittedBlockCount&#40;&#41;&#41;;
     * </pre>
     *
     * @param data The data to write to the blob. The data must be markable. This is in order to support retries. If
     * the data is not markable, consider using {@link #getBlobOutputStream()} and writing to the returned OutputStream.
     * Alternatively, consider wrapping your data source in a {@link java.io.BufferedInputStream} to add mark support.
     * @param length The exact length of the data. It is important that this value match precisely the length of the
     * data emitted by the {@code Flux}.
     * @return The information of the append blob operation.
     */
    public AppendBlobItem appendBlock(InputStream data, long length) {
        return appendBlockWithResponse(data, length, null, null, null, Context.NONE).getValue();
    }

    /**
     * Commits a new block of data to the end of the existing append blob.
     * <p>
     * Note that the data passed must be replayable if retries are enabled (the default). In other words, the
     * {@code Flux} must produce the same data each time it is subscribed to.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * byte[] md5 = MessageDigest.getInstance&#40;&quot;MD5&quot;&#41;.digest&#40;&quot;data&quot;.getBytes&#40;StandardCharsets.UTF_8&#41;&#41;;
     * AppendBlobRequestConditions requestConditions = new AppendBlobRequestConditions&#40;&#41;
     *     .setAppendPosition&#40;POSITION&#41;
     *     .setMaxSize&#40;maxSize&#41;;
     * Context context = new Context&#40;&quot;key&quot;, &quot;value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;,
     *     client.appendBlockWithResponse&#40;data, length, md5, requestConditions, timeout, context&#41;
     *         .getValue&#40;&#41;.getBlobCommittedBlockCount&#40;&#41;&#41;;
     * </pre>
     *
     * @param data The data to write to the blob. The data must be markable. This is in order to support retries. If
     * the data is not markable, consider using {@link #getBlobOutputStream()} and writing to the returned OutputStream.
     * Alternatively, consider wrapping your data source in a {@link java.io.BufferedInputStream} to add mark support.
     * @param length The exact length of the data. It is important that this value match precisely the length of the
     * data emitted by the {@code Flux}.
     * @param contentMd5 An MD5 hash of the block content. This hash is used to verify the integrity of the block during
     * transport. When this header is specified, the storage service compares the hash of the content that has arrived
     * with this header value. Note that this MD5 hash is not stored with the blob. If the two hashes do not match, the
     * operation will fail.
     * @param appendBlobRequestConditions {@link AppendBlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A {@link Response} whose {@link Response#getValue() value} contains the append blob operation.
     * @throws UnexpectedLengthException when the length of data does not match the input {@code length}.
     * @throws NullPointerException if the input data is null.
     */
    public Response<AppendBlobItem> appendBlockWithResponse(InputStream data, long length, byte[] contentMd5,
        AppendBlobRequestConditions appendBlobRequestConditions, Duration timeout, Context context) {
        Objects.requireNonNull(data, "'data' cannot be null.");
        Flux<ByteBuffer> fbb = Utility.convertStreamToByteBuffer(data, length, MAX_APPEND_BLOCK_BYTES);
        Mono<Response<AppendBlobItem>> response = appendBlobAsyncClient.appendBlockWithResponse(
            fbb.subscribeOn(Schedulers.elastic()), length, contentMd5, appendBlobRequestConditions, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Commits a new block of data from another blob to the end of this append blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;,
     *     client.appendBlockFromUrl&#40;sourceUrl, new BlobRange&#40;offset, count&#41;&#41;.getBlobCommittedBlockCount&#40;&#41;&#41;;
     * </pre>
     *
     * @param sourceUrl The url to the blob that will be the source of the copy.  A source blob in the same storage
     * account can be authenticated via Shared Key. However, if the source is a blob in another account, the source blob
     * must either be public or must be authenticated via a shared access signature. If the source blob is public, no
     * authentication is required to perform the operation.
     * @param sourceRange The source {@link BlobRange} to copy.
     * @return The information of the append blob operation.
     */
    public AppendBlobItem appendBlockFromUrl(String sourceUrl, BlobRange sourceRange) {
        return appendBlockFromUrlWithResponse(sourceUrl, sourceRange, null, null, null, null, Context.NONE).getValue();
    }

    /**
     * Commits a new block of data from another blob to the end of this append blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * AppendBlobRequestConditions appendBlobRequestConditions = new AppendBlobRequestConditions&#40;&#41;
     *     .setAppendPosition&#40;POSITION&#41;
     *     .setMaxSize&#40;maxSize&#41;;
     * 
     * BlobRequestConditions modifiedRequestConditions = new BlobRequestConditions&#40;&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * 
     * Context context = new Context&#40;&quot;key&quot;, &quot;value&quot;&#41;;
     * 
     * System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;,
     *     client.appendBlockFromUrlWithResponse&#40;sourceUrl, new BlobRange&#40;offset, count&#41;, null,
     *         appendBlobRequestConditions, modifiedRequestConditions, timeout,
     *         context&#41;.getValue&#40;&#41;.getBlobCommittedBlockCount&#40;&#41;&#41;;
     * </pre>
     *
     * @param sourceUrl The url to the blob that will be the source of the copy.  A source blob in the same storage
     * account can be authenticated via Shared Key. However, if the source is a blob in another account, the source blob
     * must either be public or must be authenticated via a shared access signature. If the source blob is public, no
     * authentication is required to perform the operation.
     * @param sourceRange {@link BlobRange}
     * @param sourceContentMd5 An MD5 hash of the block content from the source blob. If specified, the service will
     * calculate the MD5 of the received data and fail the request if it does not match the provided MD5.
     * @param destRequestConditions {@link AppendBlobRequestConditions}
     * @param sourceRequestConditions {@link BlobRequestConditions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return The information of the append blob operation.
     */
    public Response<AppendBlobItem> appendBlockFromUrlWithResponse(String sourceUrl, BlobRange sourceRange,
        byte[] sourceContentMd5, AppendBlobRequestConditions destRequestConditions,
        BlobRequestConditions sourceRequestConditions, Duration timeout, Context context) {
        Mono<Response<AppendBlobItem>> response = appendBlobAsyncClient.appendBlockFromUrlWithResponse(sourceUrl,
            sourceRange, sourceContentMd5, destRequestConditions, sourceRequestConditions, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Seals an append blob, making it read only. Any subsequent appends will fail.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.seal&#40;&#41;;
     * System.out.println&#40;&quot;Sealed AppendBlob&quot;&#41;;
     * </pre>
     */
    public void seal() {
        sealWithResponse(new AppendBlobSealOptions(), null, Context.NONE);
    }

    /**
     * Seals an append blob, making it read only. Any subsequent appends will fail.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * AppendBlobRequestConditions requestConditions = new AppendBlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * Context context = new Context&#40;&quot;key&quot;, &quot;value&quot;&#41;;
     * 
     * client.sealWithResponse&#40;new AppendBlobSealOptions&#40;&#41;.setRequestConditions&#40;requestConditions&#41;, timeout, context&#41;;
     * System.out.println&#40;&quot;Sealed AppendBlob&quot;&#41;;
     * </pre>
     *
     * @param options {@link AppendBlobSealOptions}
     * @param timeout An optional timeout value beyond which a {@link RuntimeException} will be raised.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A reactive response signalling completion.
     */
    public Response<Void> sealWithResponse(AppendBlobSealOptions options, Duration timeout, Context context) {
        Mono<Response<Void>> response = appendBlobAsyncClient.sealWithResponse(options, context);

        return blockWithOptionalTimeout(response, timeout);
    }
}