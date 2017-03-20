package com.example;

public class PushMessageRequest {

    private String queueName;
    private String messageBody;

    public PushMessageRequest(String queueName, String messageBody) {
        this.queueName = queueName;
        this.messageBody = messageBody;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
