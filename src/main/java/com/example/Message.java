package com.example;

import java.util.UUID;

public class Message {

    private String id;
    private String body;

    public Message(String id, String body) {
        this.id = id;
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getId() {
        return id;
    }
}
