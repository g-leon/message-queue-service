package com.example;

public class PullMessageRequest {

    private String queueName;
    private Integer visibilityTimeout;

    public PullMessageRequest(String queueName) {
        this.queueName = queueName;
    }

    // TODO check timeout limit
    public PullMessageRequest(String queueName, Integer visibilityTimeout) {
        this.queueName = queueName;
        this.visibilityTimeout = visibilityTimeout;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

}
