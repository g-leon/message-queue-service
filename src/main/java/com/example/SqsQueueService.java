package com.example;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import java.util.List;

public class SqsQueueService implements QueueService {

  AmazonSQSClient client;

  public SqsQueueService(AmazonSQSClient sqsClient) {
    this.client = sqsClient;
  }

  @Override
  public PushMessageResult push(PushMessageRequest pushMessageRequest) {
	SendMessageResult sendMessageResult = client.sendMessage(pushRequestToSendRequest(pushMessageRequest));
    return sendResulttToPushResult(sendMessageResult);
  }

  @Override
  public Message pull(PullMessageRequest pullMessageRequest) {
    ReceiveMessageResult receiveMessageResult = client.receiveMessage(pullRequestToReceiveRequest(pullMessageRequest));
    Message message = clientMessageToMessage(receiveMessageResult);
    return message;
  }

  @Override
  public void delete(DeleteMessageRequest deleteMessageRequest) {
    com.amazonaws.services.sqs.model.DeleteMessageRequest clientDeleteRequest =
            deleteRequestToClientDeleteRequest(deleteMessageRequest);
    client.deleteMessage(clientDeleteRequest);
  }

  /**
   *  These methods are helper methods intended to
   *  transfer data to and from Amazon API and
   *  local API
   */
  private SendMessageRequest pushRequestToSendRequest(PushMessageRequest pushMessageRequest) {
    SendMessageRequest sendMessageRequest =
            new SendMessageRequest(pushMessageRequest.getQueueName(), pushMessageRequest.getMessageBody());
    return sendMessageRequest;
  }

  private PushMessageResult sendResulttToPushResult(SendMessageResult sendMessageResult) {
    PushMessageResult pushMessageResult = new PushMessageResult(sendMessageResult.getMessageId());
    return pushMessageResult;
  }

  private ReceiveMessageRequest pullRequestToReceiveRequest(PullMessageRequest pullMessageRequest) {
    ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(pullMessageRequest.getQueueName());
    receiveMessageRequest.setMaxNumberOfMessages(1);
    receiveMessageRequest.setVisibilityTimeout(pullMessageRequest.getVisibilityTimeout());
    return receiveMessageRequest;
  }

  private Message clientMessageToMessage(ReceiveMessageResult receiveMessageResult) {
    for (com.amazonaws.services.sqs.model.Message message : receiveMessageResult.getMessages()) {
      Message result = new Message(message.getReceiptHandle(), message.getBody());
      return result;
    }
    return null;
  }

  private com.amazonaws.services.sqs.model.DeleteMessageRequest deleteRequestToClientDeleteRequest
          (DeleteMessageRequest deleteMessageRequest) {
    com.amazonaws.services.sqs.model.DeleteMessageRequest clientDeleteRequest = new com.amazonaws.services
            .sqs.model.DeleteMessageRequest(deleteMessageRequest.getQueueName(), deleteMessageRequest.getMessageId());
    return clientDeleteRequest;
  }
}
