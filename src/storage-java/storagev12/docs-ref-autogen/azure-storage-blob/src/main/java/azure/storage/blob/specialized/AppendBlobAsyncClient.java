// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.storage.blob.specialized;

import com.azure.core.annotation.ServiceClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.RequestConditions;
import com.azure.core.http.rest.Response;
import com.azure.core.http.rest.SimpleResponse;
import com.azure.core.util.Context;
import com.azure.core.util.FluxUtil;
import com.azure.core.util.logging.ClientLogger;
import com.azure.storage.blob.BlobAsyncClient;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobServiceAsyncClient;
import com.azure.storage.blob.BlobServiceVersion;
import com.azure.storage.blob.implementation.models.AppendBlobAppendBlockFromUrlHeaders;
import com.azure.storage.blob.implementation.models.AppendBlobAppendBlockHeaders;
import com.azure.storage.blob.implementation.models.AppendBlobCreateHeaders;
import com.azure.storage.blob.implementation.models.EncryptionScope;
import com.azure.storage.blob.options.AppendBlobCreateOptions;
import com.azure.storage.blob.models.AppendBlobRequestConditions;
import com.azure.storage.blob.models.AppendBlobItem;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobRange;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.CpkInfo;
import com.azure.storage.blob.options.AppendBlobSealOptions;
import com.azure.storage.common.implementation.Constants;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;

import static com.azure.core.util.FluxUtil.monoError;
import static com.azure.core.util.FluxUtil.withContext;
import static com.azure.core.util.tracing.Tracer.AZ_TRACING_NAMESPACE_KEY;
import static com.azure.storage.common.Utility.STORAGE_TRACING_NAMESPACE_VALUE;

/**
 * Client to an append blob. It may only be instantiated through a
 * {@link SpecializedBlobClientBuilder#buildAppendBlobAsyncClient()} or via the method
 * {@link BlobAsyncClient#getAppendBlobAsyncClient()}. This class does not hold any state about a
 * particular blob, but is instead a convenient way of sending appropriate requests to the resource on the service.
 *
 * <p>
 * This client contains operations on a blob. Operations on a container are available on {@link
 * BlobContainerAsyncClient}, and operations on the service are available on {@link BlobServiceAsyncClient}.
 *
 * <p>
 * Please refer to the <a href=https://docs.microsoft.com/en-us/rest/api/storageservices/understanding-block-blobs--append-blobs--and-page-blobs>Azure
 * Docs</a> for more information.
 *
 * <p>
 * Note this client is an async client that returns reactive responses from Spring Reactor Core project
 * (https://projectreactor.io/). Calling the methods in this client will <strong>NOT</strong> start the actual network
 * operation, until {@code .subscribe()} is called on the reactive response. You can simply convert one of these
 * responses to a {@link java.util.concurrent.CompletableFuture} object through {@link Mono#toFuture()}.
 */
@ServiceClient(builder = SpecializedBlobClientBuilder.class, isAsync = true)
public final class AppendBlobAsyncClient extends BlobAsyncClientBase {
    private final ClientLogger logger = new ClientLogger(AppendBlobAsyncClient.class);

    /**
     * Indicates the maximum number of bytes that can be sent in a call to appendBlock.
     */
    public static final int MAX_APPEND_BLOCK_BYTES = 4 * Constants.MB;

    /**
     * Indicates the maximum number of blocks allowed in an append blob.
     */
    public static final int MAX_BLOCKS = 50000;

    /**
     * Package-private constructor for use by {@link SpecializedBlobClientBuilder}.
     *
     * @param pipeline The pipeline used to send and receive service requests.
     * @param url The endpoint where to send service requests.
     * @param serviceVersion The version of the service to receive requests.
     * @param accountName The storage account name.
     * @param containerName The container name.
     * @param blobName The blob name.
     * @param snapshot The snapshot identifier for the blob, pass {@code null} to interact with the blob directly.
     * @param customerProvidedKey Customer provided key used during encryption of the blob's data on the server, pass
     * {@code null} to allow the service to use its own encryption.
     * @param encryptionScope Encryption scope used during encryption of the blob's data on the server, pass
     * {@code null} to allow the service to use its own encryption.
     * @param versionId The version identifier for the blob, pass {@code null} to interact with the latest blob version.
     */
    AppendBlobAsyncClient(HttpPipeline pipeline, String url, BlobServiceVersion serviceVersion,
        String accountName, String containerName, String blobName, String snapshot, CpkInfo customerProvidedKey,
        EncryptionScope encryptionScope, String versionId) {
        super(pipeline, url, serviceVersion, accountName, containerName, blobName, snapshot, customerProvidedKey,
            encryptionScope, versionId);
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob. By default this method will
     * not overwrite an existing blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.create&#40;&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;, response.getLastModified&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @return A {@link Mono} containing the information of the created appended blob.
     */
    public Mono<AppendBlobItem> create() {
        try {
            return create(false);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Creates a 0-length append blob. Call appendBlock to append data to an append blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * boolean overwrite = false; &#47;&#47; Default behavior
     * client.create&#40;overwrite&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;, response.getLastModified&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param overwrite Whether or not to overwrite, should data exist on the blob.
     *
     * @return A {@link Mono} containing the information of the created appended blob.
     */
    public Mono<AppendBlobItem> create(boolean overwrite) {
        try {
            BlobRequestConditions blobRequestConditions = new BlobRequestConditions();
            if (!overwrite) {
                blobRequestConditions.setIfNoneMatch(Constants.HeaderConstants.ETAG_WILDCARD);
            }
            return createWithResponse(null, null, blobRequestConditions).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
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
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * 
     * client.createWithResponse&#40;headers, metadata, requestConditions&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;, response.getValue&#40;&#41;.getLastModified&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param headers {@link BlobHttpHeaders}
     * @param metadata Metadata to associate with the blob.
     * @param requestConditions {@link BlobRequestConditions}
     * @return A {@link Mono} containing {@link Response} whose {@link Response#getValue() value} contains the created
     * appended blob.
     */
    public Mono<Response<AppendBlobItem>> createWithResponse(BlobHttpHeaders headers, Map<String, String> metadata,
        BlobRequestConditions requestConditions) {
        return this.createWithResponse(new AppendBlobCreateOptions().setHeaders(headers).setMetadata(metadata)
            .setRequestConditions(requestConditions));
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
     * Map&lt;String, String&gt; tags = Collections.singletonMap&#40;&quot;tag&quot;, &quot;value&quot;&#41;;
     * BlobRequestConditions requestConditions = new BlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * 
     * client.createWithResponse&#40;new AppendBlobCreateOptions&#40;&#41;.setHeaders&#40;headers&#41;.setMetadata&#40;metadata&#41;
     *     .setTags&#40;tags&#41;.setRequestConditions&#40;requestConditions&#41;&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;Created AppendBlob at %s%n&quot;, response.getValue&#40;&#41;.getLastModified&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param options {@link AppendBlobCreateOptions}
     * @return A {@link Mono} containing {@link Response} whose {@link Response#getValue() value} contains the created
     * appended blob.
     */
    public Mono<Response<AppendBlobItem>> createWithResponse(AppendBlobCreateOptions options) {
        try {
            return withContext(context -> createWithResponse(options, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<AppendBlobItem>> createWithResponse(AppendBlobCreateOptions options, Context context) {
        options = (options == null) ? new AppendBlobCreateOptions() : options;

        BlobRequestConditions requestConditions = options.getRequestConditions();
        requestConditions = (requestConditions == null) ? new BlobRequestConditions() : requestConditions;
        context = context == null ? Context.NONE : context;

        return this.azureBlobStorage.appendBlobs().createWithRestResponseAsync(null, null, 0, null,
            options.getMetadata(), requestConditions.getLeaseId(), requestConditions.getIfModifiedSince(),
            requestConditions.getIfUnmodifiedSince(), requestConditions.getIfMatch(),
            requestConditions.getIfNoneMatch(), requestConditions.getTagsConditions(), null,
            tagsToString(options.getTags()), options.getHeaders(), getCustomerProvidedKey(), encryptionScope,
            context.addData(AZ_TRACING_NAMESPACE_KEY, STORAGE_TRACING_NAMESPACE_VALUE))
            .map(rb -> {
                AppendBlobCreateHeaders hd = rb.getDeserializedHeaders();
                AppendBlobItem item = new AppendBlobItem(hd.getETag(), hd.getLastModified(), hd.getContentMD5(),
                    hd.isServerEncrypted(), hd.getEncryptionKeySha256(), hd.getEncryptionScope(), null, null,
                    hd.getVersionId());
                return new SimpleResponse<>(rb, item);
            });
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
     * client.appendBlock&#40;data, length&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;, response.getBlobCommittedBlockCount&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param data The data to write to the blob. Note that this {@code Flux} must be replayable if retries are enabled
     * (the default). In other words, the Flux must produce the same data each time it is subscribed to.
     * @param length The exact length of the data. It is important that this value match precisely the length of the
     * data emitted by the {@code Flux}.
     * @return {@link Mono} containing the information of the append blob operation.
     */
    public Mono<AppendBlobItem> appendBlock(Flux<ByteBuffer> data, long length) {
        try {
            return appendBlockWithResponse(data, length, null, null).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
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
     * 
     * client.appendBlockWithResponse&#40;data, length, md5, requestConditions&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;, response.getValue&#40;&#41;.getBlobCommittedBlockCount&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param data The data to write to the blob. Note that this {@code Flux} must be replayable if retries are enabled
     * (the default). In other words, the Flux must produce the same data each time it is subscribed to.
     * @param length The exact length of the data. It is important that this value match precisely the length of the
     * data emitted by the {@code Flux}.
     * @param contentMd5 An MD5 hash of the block content. This hash is used to verify the integrity of the block during
     * transport. When this header is specified, the storage service compares the hash of the content that has arrived
     * with this header value. Note that this MD5 hash is not stored with the blob. If the two hashes do not match, the
     * operation will fail.
     * @param appendBlobRequestConditions {@link AppendBlobRequestConditions}
     * @return A {@link Mono} containing {@link Response} whose {@link Response#getValue() value} contains the append
     * blob operation.
     */
    public Mono<Response<AppendBlobItem>> appendBlockWithResponse(Flux<ByteBuffer> data, long length, byte[] contentMd5,
        AppendBlobRequestConditions appendBlobRequestConditions) {
        try {
            return withContext(context ->
                appendBlockWithResponse(data, length, contentMd5, appendBlobRequestConditions, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<AppendBlobItem>> appendBlockWithResponse(Flux<ByteBuffer> data, long length, byte[] contentMd5,
        AppendBlobRequestConditions appendBlobRequestConditions, Context context) {
        appendBlobRequestConditions = appendBlobRequestConditions == null ? new AppendBlobRequestConditions()
            : appendBlobRequestConditions;
        context = context == null ? Context.NONE : context;

        return this.azureBlobStorage.appendBlobs().appendBlockWithRestResponseAsync(
            null, null, data, length, null, contentMd5, null, appendBlobRequestConditions.getLeaseId(),
            appendBlobRequestConditions.getMaxSize(), appendBlobRequestConditions.getAppendPosition(),
            appendBlobRequestConditions.getIfModifiedSince(), appendBlobRequestConditions.getIfUnmodifiedSince(),
            appendBlobRequestConditions.getIfMatch(), appendBlobRequestConditions.getIfNoneMatch(),
            appendBlobRequestConditions.getTagsConditions(), null, getCustomerProvidedKey(), encryptionScope,
            context.addData(AZ_TRACING_NAMESPACE_KEY, STORAGE_TRACING_NAMESPACE_VALUE))
            .map(rb -> {
                AppendBlobAppendBlockHeaders hd = rb.getDeserializedHeaders();
                AppendBlobItem item = new AppendBlobItem(hd.getETag(), hd.getLastModified(), hd.getContentMD5(),
                    hd.isServerEncrypted(), hd.getEncryptionKeySha256(), hd.getEncryptionScope(),
                    hd.getBlobAppendOffset(), hd.getBlobCommittedBlockCount());
                return new SimpleResponse<>(rb, item);
            });
    }

    /**
     * Commits a new block of data from another blob to the end of this append blob.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.appendBlockFromUrl&#40;sourceUrl, new BlobRange&#40;offset, count&#41;&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;, response.getBlobCommittedBlockCount&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param sourceUrl The url to the blob that will be the source of the copy.  A source blob in the same storage
     * account can be authenticated via Shared Key. However, if the source is a blob in another account, the source blob
     * must either be public or must be authenticated via a shared access signature. If the source blob is public, no
     * authentication is required to perform the operation.
     * @param sourceRange The source {@link BlobRange} to copy.
     * @return {@link Mono} containing the information of the append blob operation.
     */
    public Mono<AppendBlobItem> appendBlockFromUrl(String sourceUrl, BlobRange sourceRange) {
        try {
            return appendBlockFromUrlWithResponse(sourceUrl, sourceRange, null, null, null).flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
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
     * client.appendBlockFromUrlWithResponse&#40;sourceUrl, new BlobRange&#40;offset, count&#41;, null,
     *     appendBlobRequestConditions, modifiedRequestConditions&#41;.subscribe&#40;response -&gt;
     *     System.out.printf&#40;&quot;AppendBlob has %d committed blocks%n&quot;, response.getValue&#40;&#41;.getBlobCommittedBlockCount&#40;&#41;&#41;&#41;;
     * </pre>
     *
     * @param sourceUrl The url to the blob that will be the source of the copy.  A source blob in the same storage
     * account can be authenticated via Shared Key. However, if the source is a blob in another account, the source blob
     * must either be public or must be authenticated via a shared access signature. If the source blob is public, no
     * authentication is required to perform the operation.
     * @param sourceRange {@link BlobRange}
     * @param sourceContentMD5 An MD5 hash of the block content from the source blob. If specified, the service will
     * calculate the MD5 of the received data and fail the request if it does not match the provided MD5.
     * @param destRequestConditions {@link AppendBlobRequestConditions}
     * @param sourceRequestConditions {@link BlobRequestConditions}
     * @return A {@link Mono} containing {@link Response} whose {@link Response#getValue() value} contains the append
     * blob operation.
     */
    public Mono<Response<AppendBlobItem>> appendBlockFromUrlWithResponse(String sourceUrl, BlobRange sourceRange,
        byte[] sourceContentMD5, AppendBlobRequestConditions destRequestConditions,
        BlobRequestConditions sourceRequestConditions) {
        try {
            return withContext(context ->
                appendBlockFromUrlWithResponse(sourceUrl, sourceRange, sourceContentMD5,
                    destRequestConditions, sourceRequestConditions, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<AppendBlobItem>> appendBlockFromUrlWithResponse(String sourceUrl, BlobRange sourceRange,
        byte[] sourceContentMD5, AppendBlobRequestConditions destRequestConditions,
        RequestConditions sourceRequestConditions, Context context) {
        sourceRange = (sourceRange == null) ? new BlobRange(0) : sourceRange;
        destRequestConditions = (destRequestConditions == null)
            ? new AppendBlobRequestConditions() : destRequestConditions;
        sourceRequestConditions = (sourceRequestConditions == null)
            ? new RequestConditions() : sourceRequestConditions;

        URL url;
        try {
            url = new URL(sourceUrl);
        } catch (MalformedURLException ex) {
            throw logger.logExceptionAsError(new IllegalArgumentException("'sourceUrl' is not a valid url."));
        }
        context = context == null ? Context.NONE : context;

        return this.azureBlobStorage.appendBlobs().appendBlockFromUrlWithRestResponseAsync(null, null, url, 0,
            sourceRange.toString(), sourceContentMD5, null, null, null, destRequestConditions.getLeaseId(),
            destRequestConditions.getMaxSize(), destRequestConditions.getAppendPosition(),
            destRequestConditions.getIfModifiedSince(), destRequestConditions.getIfUnmodifiedSince(),
            destRequestConditions.getIfMatch(), destRequestConditions.getIfNoneMatch(),
            destRequestConditions.getTagsConditions(), sourceRequestConditions.getIfModifiedSince(),
            sourceRequestConditions.getIfUnmodifiedSince(), sourceRequestConditions.getIfMatch(),
            sourceRequestConditions.getIfNoneMatch(), null, getCustomerProvidedKey(), encryptionScope,
            context.addData(AZ_TRACING_NAMESPACE_KEY, STORAGE_TRACING_NAMESPACE_VALUE))
            .map(rb -> {
                AppendBlobAppendBlockFromUrlHeaders hd = rb.getDeserializedHeaders();
                AppendBlobItem item = new AppendBlobItem(hd.getETag(), hd.getLastModified(), hd.getContentMD5(),
                    hd.isServerEncrypted(), hd.getEncryptionKeySha256(), hd.getEncryptionScope(),
                    hd.getBlobAppendOffset(), hd.getBlobCommittedBlockCount());
                return new SimpleResponse<>(rb, item);
            });
    }

    /**
     * Seals an append blob, making it read only. Any subsequent appends will fail.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * client.seal&#40;&#41;.subscribe&#40;response -&gt; System.out.println&#40;&quot;Sealed AppendBlob&quot;&#41;&#41;;
     * </pre>
     *
     * @return A reactive response signalling completion.
     */
    public Mono<Void> seal() {
        try {
            return sealWithResponse(new AppendBlobSealOptions())
                .flatMap(FluxUtil::toMono);
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    /**
     * Seals an append blob, making it read only. Any subsequent appends will fail.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * AppendBlobRequestConditions requestConditions = new AppendBlobRequestConditions&#40;&#41;.setLeaseId&#40;leaseId&#41;
     *     .setIfUnmodifiedSince&#40;OffsetDateTime.now&#40;&#41;.minusDays&#40;3&#41;&#41;;
     * 
     * client.sealWithResponse&#40;new AppendBlobSealOptions&#40;&#41;.setRequestConditions&#40;requestConditions&#41;&#41;
     *     .subscribe&#40;response -&gt; System.out.println&#40;&quot;Sealed AppendBlob&quot;&#41;&#41;;
     * </pre>
     *
     * @param options {@link AppendBlobSealOptions}
     * @return A reactive response signalling completion.
     */
    public Mono<Response<Void>> sealWithResponse(AppendBlobSealOptions options) {
        try {
            return withContext(context -> sealWithResponse(options, context));
        } catch (RuntimeException ex) {
            return monoError(logger, ex);
        }
    }

    Mono<Response<Void>> sealWithResponse(AppendBlobSealOptions options, Context context) {
        options = (options == null) ? new AppendBlobSealOptions() : options;

        AppendBlobRequestConditions requestConditions = options.getRequestConditions();
        requestConditions = (requestConditions == null) ? new AppendBlobRequestConditions() : requestConditions;
        context = context == null ? Context.NONE : context;

        return this.azureBlobStorage.appendBlobs().sealWithRestResponseAsync(null, null, null, null,
            requestConditions.getLeaseId(), requestConditions.getIfModifiedSince(),
            requestConditions.getIfUnmodifiedSince(), requestConditions.getIfMatch(),
            requestConditions.getIfNoneMatch(), requestConditions.getAppendPosition(),
            context.addData(AZ_TRACING_NAMESPACE_KEY, STORAGE_TRACING_NAMESPACE_VALUE))
            .map(response -> new SimpleResponse<>(response, null));
    }
}
