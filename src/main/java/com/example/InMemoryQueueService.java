package com.example;

import sun.lwawt.macosx.CocoaConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryQueueService extends BaseQueueService implements QueueService {

    /**
     * I am using a "TreeMap" for each queue such that I will be able to preserve
     * the order when the visibility timeout expires at the cost of log N
     * time complexity for insertions.
     * The cost of pull will be amortized by the fact that I will always retrieve
     * the first element of the collection.
     */
    private ConcurrentHashMap<String, ConcurrentSkipListMap<String, Message>> queues;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, Long>> visibilityTimestamps;
    private HashMap<String, AtomicLong> ids;

    public InMemoryQueueService() {
        this.queues = new ConcurrentHashMap<>();
        this.visibilityTimestamps = new ConcurrentHashMap<>();
        this.ids = new HashMap<>();
    }

    @Override
    public PushMessageResult push(PushMessageRequest pushMessageRequest) {
        ConcurrentSkipListMap<String, Message> queue = queues.get(pushMessageRequest.getQueueName());
        if (queue == null) {
            queue = new ConcurrentSkipListMap<>();
            this.queues.put(pushMessageRequest.getQueueName(), queue);
        }

        String id = generateId(pushMessageRequest.getQueueName());
        Message message = new Message(id, pushMessageRequest.getMessageBody());
        queue.put(id, message);

        PushMessageResult result = new PushMessageResult(id);
        return result;
    }

    @Override
    public Message pull(PullMessageRequest pullMessageRequest) {
        ConcurrentSkipListMap<String, Message> queue = this.queues.get(pullMessageRequest.getQueueName());

        for (Map.Entry<String, Message> message : queue.entrySet()) {
            if (visible(pullMessageRequest.getQueueName(), message.getKey()))  {
                // If message request contains a visibility timeout
                // then it is used, else the default value is used
                int timeout = pullMessageRequest.getVisibilityTimeout() != null ?
                        pullMessageRequest.getVisibilityTimeout() : getVisibilityTimeout();
                makeInvisible(pullMessageRequest.getQueueName(), message.getKey(), timeout);
                return message.getValue();
            }
        }
        return null;
    }

    @Override
    public void delete(DeleteMessageRequest deleteMessageRequest) {
        ConcurrentSkipListMap<String, Message> queue = this.queues.get(deleteMessageRequest.getQueueName());
        if (queue != null) {
            queue.remove(deleteMessageRequest.getMessageId());
        }
    }

    private boolean visible(String queueName, String id) {
        ConcurrentHashMap<String, Long> timestamps = this.visibilityTimestamps.get(queueName);
        if (timestamps == null) {
            timestamps = new ConcurrentHashMap<>();
            this.visibilityTimestamps.put(queueName, timestamps);
        }

        Long timestamp = timestamps.get(id);
        if (timestamp == null) {
            return true;
        }

        // If visibility timeout has expired
        // then the key is removed, otherwise
        // the visibilityTimestamps would grow
        // infinitely
        if (timestamp < getClock().currentTimeMillis()) {
            this.visibilityTimestamps.remove(id);
            return true;
        }
        return false;
    }

    private void makeInvisible(String queueName, String id, int timeout) {
        this.visibilityTimestamps.get(queueName)
                .put(id, getClock().currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeout));
    }

    /**
     * I've chosen to use a map of synchronized ids generator
     * to have the guarantee of an unique id for each queue
     * at the cost of the locking performance impact
     */
    protected String generateId(String queueName) {
        AtomicLong id = ids.get(queueName);
        if (id != null) {
            return Long.toString(id.getAndIncrement());
        } else {
            id = new AtomicLong();
            ids.put(queueName, id);
            return Long.toString(id.getAndIncrement());
        }
    }


}
