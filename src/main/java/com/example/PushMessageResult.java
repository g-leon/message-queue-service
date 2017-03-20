package com.example;

/**
 * A class that returns the ID of the newly created message.
 * I've chosen a class instead of returning a string because
 * this way would be much easier to expand the implementation
 * to accommodate other attributes that should be returned.
 */
public class PushMessageResult {

    private String messageId;

    public PushMessageResult(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
