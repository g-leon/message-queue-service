package com.example;

import java.util.List;

public interface QueueService {

    public PushMessageResult push(PushMessageRequest pushMessageRequest);

    public Message pull(PullMessageRequest pullMessageRequest);

    public void delete(DeleteMessageRequest deleteMessageRequest);
}
