package com.example;

public class DeleteMessageRequest {

    private String messageId;
    private String queueName;

    public DeleteMessageRequest(String messageId, String queueName) {
        this.messageId = messageId;
        this.queueName = queueName;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}
