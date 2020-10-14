// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.storage.queue;

import com.azure.core.annotation.ServiceClient;
import com.azure.core.http.HttpPipeline;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.common.implementation.StorageImplUtils;
import com.azure.storage.queue.models.PeekedMessageItem;
import com.azure.storage.queue.models.QueueMessageItem;
import com.azure.storage.queue.models.QueueProperties;
import com.azure.storage.queue.models.QueueSignedIdentifier;
import com.azure.storage.queue.models.QueueStorageException;
import com.azure.storage.queue.models.SendMessageResult;
import com.azure.storage.queue.models.UpdateMessageResult;
import com.azure.storage.queue.sas.QueueServiceSasSignatureValues;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * This class provides a client that contains all the operations for interacting with a queue in Azure Storage Queue.
 * Operations allowed by the client are creating and deleting the queue, retrieving and updating metadata and access
 * policies of the queue, and enqueuing, dequeuing, peeking, updating, and deleting messages.
 *
 * <p><strong>Instantiating an Synchronous Queue Client</strong></p>
 *
 * <pre>
 * QueueClient client = new QueueClientBuilder&#40;&#41;
 *     .connectionString&#40;&quot;connectionstring&quot;&#41;
 *     .endpoint&#40;&quot;endpoint&quot;&#41;
 *     .buildClient&#40;&#41;;
 * </pre>
 *
 * <p>View {@link QueueClientBuilder this} for additional ways to construct the client.</p>
 *
 * @see QueueClientBuilder
 * @see QueueAsyncClient
 * @see StorageSharedKeyCredential
 */
@ServiceClient(builder = QueueClientBuilder.class)
public final class QueueClient {
    private final QueueAsyncClient client;

    /**
     * Creates a QueueClient that wraps a QueueAsyncClient and blocks requests.
     *
     * @param client QueueAsyncClient that is used to send requests
     */
    QueueClient(QueueAsyncClient client) {
        this.client = client;
    }

    /**
     * @return the URL of the storage queue.
     */
    public String getQueueUrl() {
        return client.getQueueUrl();
    }

    /**
     * Gets the service version the client is using.
     *
     * @return the service version the client is using.
     */
    public QueueServiceVersion getServiceVersion() {
        return client.getServiceVersion();
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
     * Creates a new queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Create a queue</p>
     *
     * <pre>
     * client.create&#40;&#41;;
     * System.out.println&#40;&quot;Complete creating queue.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/create-queue4">Azure Docs</a>.</p>
     *
     * @throws QueueStorageException If a queue with the same name already exists in the queue service.
     */
    public void create() {
        createWithResponse(null, null, Context.NONE);
    }

    /**
     * Creates a new queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Create a queue with metadata "queue:metadataMap"</p>
     *
     * <pre>
     * Response&lt;Void&gt; response = client.createWithResponse&#40;Collections.singletonMap&#40;&quot;queue&quot;, &quot;metadataMap&quot;&#41;,
     *     Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;;
     * System.out.println&#40;&quot;Complete creating queue with status code: &quot; + response.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/create-queue4">Azure Docs</a>.</p>
     *
     * @param metadata Metadata to associate with the queue
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response that only contains headers and response status code
     * @throws QueueStorageException If a queue with the same name and different metadata already exists in the queue
     * service.
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<Void> createWithResponse(Map<String, String> metadata, Duration timeout, Context context) {
        Mono<Response<Void>> response = client.createWithResponse(metadata, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Permanently deletes the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete a queue</p>
     *
     * <pre>
     * client.delete&#40;&#41;;
     * System.out.println&#40;&quot;Complete deleting the queue.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-queue3">Azure Docs</a>.</p>
     *
     * @throws QueueStorageException If the queue doesn't exist
     */
    public void delete() {
        deleteWithResponse(null, Context.NONE);
    }

    /**
     * Permanently deletes the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete a queue</p>
     *
     * <pre>
     * Response&lt;Void&gt; response = client.deleteWithResponse&#40;Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;;
     * System.out.println&#40;&quot;Complete deleting the queue with status code: &quot; + response.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-queue3">Azure Docs</a>.</p>
     *
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response that only contains headers and response status code
     * @throws QueueStorageException If the queue doesn't exist
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<Void> deleteWithResponse(Duration timeout, Context context) {
        Mono<Response<Void>> response = client.deleteWithResponse(context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Retrieves metadata and approximate message count of the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Get the properties of the queue</p>
     *
     * <pre>
     * QueueProperties properties = client.getProperties&#40;&#41;;
     * System.out.printf&#40;&quot;Metadata: %s, Approximate message count: %d&quot;, properties.getMetadata&#40;&#41;,
     *     properties.getApproximateMessagesCount&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-queue-metadata">Azure Docs</a>.</p>
     *
     * @return A response containing a {@link QueueProperties} value which contains the metadata and approximate
     * messages count of the queue.
     * @throws QueueStorageException If the queue doesn't exist
     */
    public QueueProperties getProperties() {
        return getPropertiesWithResponse(null, Context.NONE).getValue();
    }

    /**
     * Retrieves metadata and approximate message count of the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Get the properties of the queue</p>
     *
     * <pre>
     * QueueProperties properties = client.getPropertiesWithResponse&#40;Duration.ofSeconds&#40;1&#41;,
     *     new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Metadata: %s, Approximate message count: %d&quot;, properties.getMetadata&#40;&#41;,
     *     properties.getApproximateMessagesCount&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-queue-metadata">Azure Docs</a>.</p>
     *
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing a {@link QueueProperties} value which contains the metadata and approximate
     * messages count of the queue.
     * @throws QueueStorageException If the queue doesn't exist
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<QueueProperties> getPropertiesWithResponse(Duration timeout, Context context) {
        Mono<Response<QueueProperties>> response = client.getPropertiesWithResponse(context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Sets the metadata of the queue.
     *
     * Passing in a {@code null} value for metadata will clear the metadata associated with the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set the queue's metadata to "queue:metadataMap"</p>
     *
     * <pre>
     * client.setMetadata&#40;Collections.singletonMap&#40;&quot;queue&quot;, &quot;metadataMap&quot;&#41;&#41;;
     * System.out.println&#40;&quot;Setting metadata completed.&quot;&#41;;
     * </pre>
     *
     * <p>Clear the queue's metadata</p>
     *
     * <pre>
     * client.setMetadata&#40;null&#41;;
     * System.out.println&#40;&quot;Clearing metadata completed.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-queue-metadata">Azure Docs</a>.</p>
     *
     * @param metadata Metadata to set on the queue
     * @throws QueueStorageException If the queue doesn't exist
     */
    public void setMetadata(Map<String, String> metadata) {
        setMetadataWithResponse(metadata, null, Context.NONE);
    }

    /**
     * Sets the metadata of the queue.
     *
     * Passing in a {@code null} value for metadata will clear the metadata associated with the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set the queue's metadata to "queue:metadataMap"</p>
     *
     * <pre>
     * client.setMetadataWithResponse&#40;Collections.singletonMap&#40;&quot;queue&quot;, &quot;metadataMap&quot;&#41;,
     *     Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;;
     * System.out.println&#40;&quot;Setting metadata completed.&quot;&#41;;
     * </pre>
     *
     * <p>Clear the queue's metadata</p>
     *
     * <pre>
     * Response&lt;Void&gt; response = client.setMetadataWithResponse&#40;null, Duration.ofSeconds&#40;1&#41;,
     *     new Context&#40;key1, value1&#41;&#41;;
     * System.out.printf&#40;&quot;Clearing metadata completed with status code %d&quot;, response.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-queue-metadata">Azure Docs</a>.</p>
     *
     * @param metadata Metadata to set on the queue
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response that only contains headers and response status code
     * @throws QueueStorageException If the queue doesn't exist
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<Void> setMetadataWithResponse(Map<String, String> metadata, Duration timeout, Context context) {
        Mono<Response<Void>> response = client.setMetadataWithResponse(metadata, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Retrieves stored access policies specified on the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>List the stored access policies</p>
     *
     * <pre>
     * for &#40;QueueSignedIdentifier permission : client.getAccessPolicy&#40;&#41;&#41; &#123;
     *     System.out.printf&#40;&quot;Access policy %s allows these permissions: %s&quot;, permission.getId&#40;&#41;,
     *         permission.getAccessPolicy&#40;&#41;.getPermissions&#40;&#41;&#41;;
     * &#125;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-queue-acl">Azure Docs</a>.</p>
     *
     * @return The stored access policies specified on the queue.
     * @throws QueueStorageException If the queue doesn't exist
     */
    public PagedIterable<QueueSignedIdentifier> getAccessPolicy() {
        return new PagedIterable<>(client.getAccessPolicy());
    }

    /**
     * Sets stored access policies on the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set a read only stored access policy</p>
     *
     * <pre>
     * QueueAccessPolicy accessPolicy = new QueueAccessPolicy&#40;&#41;.setPermissions&#40;&quot;r&quot;&#41;
     *     .setStartsOn&#40;OffsetDateTime.now&#40;ZoneOffset.UTC&#41;&#41;
     *     .setExpiresOn&#40;OffsetDateTime.now&#40;ZoneOffset.UTC&#41;.plusDays&#40;10&#41;&#41;;
     * QueueSignedIdentifier permission = new QueueSignedIdentifier&#40;&#41;.setId&#40;&quot;mypolicy&quot;&#41;.setAccessPolicy&#40;accessPolicy&#41;;
     * client.setAccessPolicy&#40;Collections.singletonList&#40;permission&#41;&#41;;
     * System.out.println&#40;&quot;Setting access policies completed.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-queue-acl">Azure Docs</a>.</p>
     *
     * @param permissions Access policies to set on the queue
     * @throws QueueStorageException If the queue doesn't exist, a stored access policy doesn't have all fields filled
     * out, or the queue will have more than five policies.
     */
    public void setAccessPolicy(List<QueueSignedIdentifier> permissions) {
        setAccessPolicyWithResponse(permissions, null, Context.NONE);
    }

    /**
     * Sets stored access policies on the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Set a read only stored access policy</p>
     *
     * <pre>
     * QueueAccessPolicy accessPolicy = new QueueAccessPolicy&#40;&#41;.setPermissions&#40;&quot;r&quot;&#41;
     *     .setStartsOn&#40;OffsetDateTime.now&#40;ZoneOffset.UTC&#41;&#41;
     *     .setExpiresOn&#40;OffsetDateTime.now&#40;ZoneOffset.UTC&#41;.plusDays&#40;10&#41;&#41;;
     * QueueSignedIdentifier permission = new QueueSignedIdentifier&#40;&#41;.setId&#40;&quot;mypolicy&quot;&#41;.setAccessPolicy&#40;accessPolicy&#41;;
     * Response&lt;Void&gt; response = client.setAccessPolicyWithResponse&#40;Collections.singletonList&#40;permission&#41;,
     *     Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;;
     * System.out.printf&#40;&quot;Setting access policies completed with status code %d&quot;, response.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/set-queue-acl">Azure Docs</a>.</p>
     *
     * @param permissions Access policies to set on the queue
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response that only contains headers and response status code
     * @throws QueueStorageException If the queue doesn't exist, a stored access policy doesn't have all fields filled
     * out, or the queue will have more than five policies.
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<Void> setAccessPolicyWithResponse(List<QueueSignedIdentifier> permissions, Duration timeout,
        Context context) {
        Mono<Response<Void>> response = client.setAccessPolicyWithResponse(permissions, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Deletes all messages in the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Clear the messages</p>
     *
     * <pre>
     * client.clearMessages&#40;&#41;;
     * System.out.println&#40;&quot;Clearing messages completed.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/clear-messages">Azure Docs</a>.</p>
     *
     * @throws QueueStorageException If the queue doesn't exist
     */
    public void clearMessages() {
        clearMessagesWithResponse(null, Context.NONE);
    }

    /**
     * Deletes all messages in the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Clear the messages</p>
     *
     * <pre>
     * Response&lt;Void&gt; response = client.clearMessagesWithResponse&#40;Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;;
     * System.out.printf&#40;&quot;Clearing messages completed with status code %d&quot;, response.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/clear-messages">Azure Docs</a>.</p>
     *
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response that only contains headers and response status code
     * @throws QueueStorageException If the queue doesn't exist
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<Void> clearMessagesWithResponse(Duration timeout, Context context) {
        Mono<Response<Void>> response = client.clearMessagesWithResponse(context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Sends a message that has a time-to-live of 7 days and is instantly visible.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Sends a message of "Hello, Azure"</p>
     *
     * <pre>
     * SendMessageResult response = client.sendMessage&#40;&quot;hello msg&quot;&#41;;
     * System.out.println&#40;&quot;Complete enqueuing the message with message Id&quot; + response.getMessageId&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-message">Azure Docs</a>.</p>
     *
     * @param messageText Message text
     * @return A {@link SendMessageResult} value that contains the {@link SendMessageResult#getMessageId() messageId}
     * and {@link SendMessageResult#getPopReceipt() popReceipt} that are used to interact with the message
     * and other metadata about the enqueued message.
     * @throws QueueStorageException If the queue doesn't exist
     */
    public SendMessageResult sendMessage(String messageText) {
        return sendMessageWithResponse(messageText, null, null, null, Context.NONE).getValue();
    }

    /**
     * Sends a message with a given time-to-live and a timeout period where the message is invisible in the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Add a message of "Hello, Azure" that has a timeout of 5 seconds</p>
     *
     * <pre>
     * SendMessageResult sentMessageItem = client.sendMessageWithResponse&#40;&quot;Hello, Azure&quot;,
     *     Duration.ofSeconds&#40;5&#41;, null, Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Message %s expires at %s&quot;, sentMessageItem.getMessageId&#40;&#41;,
     *     sentMessageItem.getExpirationTime&#40;&#41;&#41;;
     * </pre>
     *
     * <p>Add a message of "Goodbye, Azure" that has a time to live of 5 seconds</p>
     *
     * <pre>
     * SendMessageResult enqueuedMessage = client.sendMessageWithResponse&#40;&quot;Goodbye, Azure&quot;,
     *     null, Duration.ofSeconds&#40;5&#41;, Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;.getValue&#40;&#41;;
     * System.out.printf&#40;&quot;Message %s expires at %s&quot;, enqueuedMessage.getMessageId&#40;&#41;,
     *     enqueuedMessage.getExpirationTime&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/put-message">Azure Docs</a>.</p>
     *
     * @param messageText Message text
     * @param visibilityTimeout Optional. The timeout period for how long the message is invisible in the queue. If
     * unset the value will default to 0 and the message will be instantly visible. The timeout must be between 0
     * seconds and 7 days.
     * @param timeToLive Optional. How long the message will stay alive in the queue. If unset the value will default to
     * 7 days, if -1 is passed the message will not expire. The time to live must be -1 or any positive number.
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the {@link SendMessageResult} value that contains the
     * {@link SendMessageResult#getMessageId() messageId} and
     * {@link SendMessageResult#getPopReceipt() popReceipt} that are used to
     * interact with the message and other metadata about the enqueued message.
     * @throws QueueStorageException If the queue doesn't exist or the {@code visibilityTimeout} or {@code timeToLive}
     * are outside of the allowed limits.
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<SendMessageResult> sendMessageWithResponse(String messageText, Duration visibilityTimeout,
        Duration timeToLive, Duration timeout, Context context) {
        Mono<Response<SendMessageResult>> response = client.sendMessageWithResponse(messageText,
            visibilityTimeout, timeToLive, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Retrieves the first message in the queue and hides it from other operations for 30 seconds.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Receive a message</p>
     *
     * <pre>
     * QueueMessageItem queueMessageItem = client.receiveMessage&#40;&#41;;
     * System.out.println&#40;&quot;Complete receiving the message: &quot; + queueMessageItem.getMessageId&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-messages">Azure Docs</a>.</p>
     *
     * @return The first {@link QueueMessageItem MessageItem} in the queue, it contains
     * {@link QueueMessageItem#getMessageId() messageId} and
     * {@link QueueMessageItem#getPopReceipt() popReceipt} used to interact with the message,
     * additionally it contains other metadata about the message.
     * @throws QueueStorageException If the queue doesn't exist
     */
    public QueueMessageItem receiveMessage() {
        return client.receiveMessage().block();
    }

    /**
     * Retrieves up to the maximum number of messages from the queue and hides them from other operations for 30
     * seconds.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Receive up to 5 messages</p>
     *
     * <pre>
     * for &#40;QueueMessageItem message : client.receiveMessages&#40;5&#41;&#41; &#123;
     *     System.out.printf&#40;&quot;Received %s and it becomes visible at %s&quot;,
     *         message.getMessageId&#40;&#41;, message.getTimeNextVisible&#40;&#41;&#41;;
     * &#125;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-messages">Azure Docs</a>.</p>
     *
     * @param maxMessages Optional. Maximum number of messages to get, if there are less messages exist in the queue
     * than requested all the messages will be returned. If left empty only 1 message will be retrieved, the allowed
     * range is 1 to 32 messages.
     * @return Up to {@code maxMessages} {@link QueueMessageItem ReceiveMessageItem} from the queue.
     * Each ReceiveMessageItem contains {@link QueueMessageItem#getMessageId() messageId} and
     * {@link QueueMessageItem#getPopReceipt() popReceipt}
     * used to interact with the message and other metadata about the message.
     * @throws QueueStorageException If the queue doesn't exist or {@code maxMessages} is outside of the allowed bounds
     */
    public PagedIterable<QueueMessageItem> receiveMessages(Integer maxMessages) {
        return receiveMessages(maxMessages, Duration.ofSeconds(30), null, Context.NONE);
    }

    /**
     * Retrieves up to the maximum number of messages from the queue and hides them from other operations for the
     * timeout period.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Receive up to 5 messages and give them a 60 second timeout period</p>
     *
     * <pre>
     * for &#40;QueueMessageItem message : client.receiveMessages&#40;5, Duration.ofSeconds&#40;60&#41;,
     *     Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;&#41; &#123;
     *     System.out.printf&#40;&quot;Received %s and it becomes visible at %s&quot;,
     *         message.getMessageId&#40;&#41;, message.getTimeNextVisible&#40;&#41;&#41;;
     * &#125;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/get-messages">Azure Docs</a>.</p>
     *
     * @param maxMessages Optional. Maximum number of messages to get, if there are less messages exist in the queue
     * than requested all the messages will be returned. If left empty only 1 message will be retrieved, the allowed
     * range is 1 to 32 messages.
     * @param visibilityTimeout Optional. The timeout period for how long the message is invisible in the queue. If left
     * empty the received messages will be invisible for 30 seconds. The timeout must be between 1 second and 7 days.
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return Up to {@code maxMessages} {@link QueueMessageItem DequeuedMessages} from the queue. Each DeqeuedMessage
     * contains {@link QueueMessageItem#getMessageId() messageId} and
     * {@link QueueMessageItem#getPopReceipt() popReceipt}
     * used to interact with the message and other metadata about the message.
     * @throws QueueStorageException If the queue doesn't exist or {@code maxMessages} or {@code visibilityTimeout} is
     * outside of the allowed bounds
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public PagedIterable<QueueMessageItem> receiveMessages(Integer maxMessages, Duration visibilityTimeout,
        Duration timeout, Context context) {
        return new PagedIterable<>(
            client.receiveMessagesWithOptionalTimeout(maxMessages, visibilityTimeout, timeout, context));
    }

    /**
     * Peeks the first message in the queue.
     *
     * Peeked messages don't contain the necessary information needed to interact with the message nor will it hide
     * messages from other operations on the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Peek the first message</p>
     *
     * <pre>
     * PeekedMessageItem peekedMessageItem = client.peekMessage&#40;&#41;;
     * System.out.println&#40;&quot;Complete peeking the message: &quot; + peekedMessageItem.getMessageText&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/peek-messages">Azure Docs</a>.</p>
     *
     * @return A {@link PeekedMessageItem} that contains metadata about the message.
     */
    public PeekedMessageItem peekMessage() {
        return client.peekMessage().block();
    }

    /**
     * Peek messages from the front of the queue up to the maximum number of messages.
     *
     * Peeked messages don't contain the necessary information needed to interact with the message nor will it hide
     * messages from other operations on the queue.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Peek up to the first five messages</p>
     *
     * <pre>
     * client.peekMessages&#40;5, Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;.forEach&#40;
     *     peekMessage -&gt; System.out.printf&#40;&quot;Peeked message %s has been received %d times&quot;,
     *         peekMessage.getMessageId&#40;&#41;, peekMessage.getDequeueCount&#40;&#41;&#41;
     * &#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/peek-messages">Azure Docs</a>.</p>
     *
     * @param maxMessages Optional. Maximum number of messages to peek, if there are less messages exist in the queue
     * than requested all the messages will be peeked. If left empty only 1 message will be peeked, the allowed range is
     * 1 to 32 messages.
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return Up to {@code maxMessages} {@link PeekedMessageItem PeekedMessages} from the queue. Each PeekedMessage
     * contains metadata about the message.
     * @throws QueueStorageException If the queue doesn't exist or {@code maxMessages} is outside of the allowed bounds
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public PagedIterable<PeekedMessageItem> peekMessages(Integer maxMessages, Duration timeout, Context context) {
        return new PagedIterable<>(client.peekMessagesWithOptionalTimeout(maxMessages, timeout, context));
    }

    /**
     * Updates the specific message in the queue with a new message and resets the visibility timeout.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Dequeue the first message and update it to "Hello again, Azure" and hide it for 5 seconds</p>
     *
     * <pre>
     * QueueMessageItem queueMessageItem = client.receiveMessage&#40;&#41;;
     * UpdateMessageResult result = client.updateMessage&#40;queueMessageItem.getMessageId&#40;&#41;,
     *     queueMessageItem.getPopReceipt&#40;&#41;, &quot;newText&quot;, null&#41;;
     * System.out.println&#40;&quot;Complete updating the message with the receipt &quot; + result.getPopReceipt&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/update-message">Azure Docs</a>.</p>
     *
     * @param messageId Id of the message to update
     * @param popReceipt Unique identifier that must match for the message to be updated
     * @param messageText Updated value for the message
     * @param visibilityTimeout The timeout period for how long the message is invisible in the queue in seconds. The
     * timeout period must be between 1 second and 7 days.
     * @return A {@link UpdateMessageResult} that contains the new
     * {@link UpdateMessageResult#getPopReceipt() popReceipt} to interact with the message,
     * additionally contains the updated metadata about the message.
     * @throws QueueStorageException If the queue or messageId don't exist, the popReceipt doesn't match on the message,
     * or the {@code visibilityTimeout} is outside the allowed bounds.
     */
    public UpdateMessageResult updateMessage(String messageId, String popReceipt, String messageText,
                                             Duration visibilityTimeout) {
        return updateMessageWithResponse(messageId, popReceipt,  messageText, visibilityTimeout, null, Context.NONE)
            .getValue();
    }

    /**
     * Updates the specific message in the queue with a new message and resets the visibility timeout.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Dequeue the first message and update it to "Hello again, Azure" and hide it for 5 seconds</p>
     *
     * <pre>
     * QueueMessageItem queueMessageItem = client.receiveMessage&#40;&#41;;
     * Response&lt;UpdateMessageResult&gt; response = client.updateMessageWithResponse&#40;queueMessageItem.getMessageId&#40;&#41;,
     *     queueMessageItem.getPopReceipt&#40;&#41;, &quot;newText&quot;, null, Duration.ofSeconds&#40;1&#41;,
     *     new Context&#40;key1, value1&#41;&#41;;
     * System.out.println&#40;&quot;Complete updating the message with status code &quot; + response.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/update-message">Azure Docs</a>.</p>
     *
     * @param messageId Id of the message to update
     * @param popReceipt Unique identifier that must match for the message to be updated
     * @param messageText Updated value for the message
     * @param visibilityTimeout The timeout period for how long the message is invisible in the queue in seconds. The
     * timeout period must be between 1 second and 7 days.
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @return A response containing the {@link UpdateMessageResult} that contains the new {@link
     * UpdateMessageResult#getPopReceipt() popReceipt} to interact with the message, additionally contains the updated
     * metadata about the message.
     * @throws QueueStorageException If the queue or messageId don't exist, the popReceipt doesn't match on the message,
     * or the {@code visibilityTimeout} is outside the allowed bounds.
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<UpdateMessageResult> updateMessageWithResponse(String messageId, String popReceipt,
        String messageText, Duration visibilityTimeout, Duration timeout, Context context) {
        Mono<Response<UpdateMessageResult>> response = client.updateMessageWithResponse(messageId,
            popReceipt, messageText, visibilityTimeout, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Deletes the specified message in the queue
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete the first message</p>
     *
     * <pre>
     * QueueMessageItem queueMessageItem = client.receiveMessage&#40;&#41;;
     * client.deleteMessage&#40;queueMessageItem.getMessageId&#40;&#41;, queueMessageItem.getPopReceipt&#40;&#41;&#41;;
     * System.out.println&#40;&quot;Complete deleting the message.&quot;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-message2">Azure Docs</a>.</p>
     *
     * @param messageId Id of the message to deleted
     * @param popReceipt Unique identifier that must match for the message to be deleted
     * @throws QueueStorageException If the queue or messageId don't exist or the popReceipt doesn't match on the
     * message.
     */
    public void deleteMessage(String messageId, String popReceipt) {
        deleteMessageWithResponse(messageId, popReceipt, null, Context.NONE);
    }

    /**
     * Deletes the specified message in the queue
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <p>Delete the first message</p>
     *
     * <pre>
     * QueueMessageItem queueMessageItem = client.receiveMessage&#40;&#41;;
     * Response&lt;Void&gt; response = client.deleteMessageWithResponse&#40;queueMessageItem.getMessageId&#40;&#41;,
     *     queueMessageItem.getPopReceipt&#40;&#41;, Duration.ofSeconds&#40;1&#41;, new Context&#40;key1, value1&#41;&#41;;
     * System.out.println&#40;&quot;Complete deleting the message with status code &quot; + response.getStatusCode&#40;&#41;&#41;;
     * </pre>
     *
     * <p>For more information, see the
     * <a href="https://docs.microsoft.com/en-us/rest/api/storageservices/delete-message2">Azure Docs</a>.</p>
     *
     * @param messageId Id of the message to deleted
     * @param popReceipt Unique identifier that must match for the message to be deleted
     * @param context Additional context that is passed through the Http pipeline during the service call.
     * @param timeout An optional timeout applied to the operation. If a response is not returned before the timeout
     * concludes a {@link RuntimeException} will be thrown.
     * @return A response that only contains headers and response status code
     * @throws QueueStorageException If the queue or messageId don't exist or the popReceipt doesn't match on the
     * message.
     * @throws RuntimeException if the operation doesn't complete before the timeout concludes.
     */
    public Response<Void> deleteMessageWithResponse(String messageId, String popReceipt, Duration timeout,
        Context context) {
        Mono<Response<Void>> response = client.deleteMessageWithResponse(messageId, popReceipt, context);
        return StorageImplUtils.blockWithOptionalTimeout(response, timeout);
    }

    /**
     * Get the queue name of the client.
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * String queueName = client.getQueueName&#40;&#41;;
     * System.out.println&#40;&quot;The name of the queue is &quot; + queueName&#41;;
     * </pre>
     *
     * @return The name of the queue.
     */
    public String getQueueName() {
        return this.client.getQueueName();
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
     * Generates a service sas for the queue using the specified {@link QueueServiceSasSignatureValues}
     * Note : The client must be authenticated via {@link StorageSharedKeyCredential}
     * <p>See {@link QueueServiceSasSignatureValues} for more information on how to construct a service SAS.</p>
     *
     * <p><strong>Code Samples</strong></p>
     *
     * <pre>
     * OffsetDateTime expiryTime = OffsetDateTime.now&#40;&#41;.plusDays&#40;1&#41;;
     * QueueSasPermission permission = new QueueSasPermission&#40;&#41;.setReadPermission&#40;true&#41;;
     * 
     * QueueServiceSasSignatureValues values = new QueueServiceSasSignatureValues&#40;expiryTime, permission&#41;
     *     .setStartTime&#40;OffsetDateTime.now&#40;&#41;&#41;;
     * 
     * client.generateSas&#40;values&#41;; &#47;&#47; Client must be authenticated via StorageSharedKeyCredential
     * </pre>
     *
     * @param queueServiceSasSignatureValues {@link QueueServiceSasSignatureValues}
     *
     * @return A {@code String} representing all SAS query parameters.
     */
    public String generateSas(QueueServiceSasSignatureValues queueServiceSasSignatureValues) {
        return this.client.generateSas(queueServiceSasSignatureValues);
    }
}