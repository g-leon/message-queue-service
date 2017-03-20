package com.example;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class BaseQueueService {
    private int visibilityTimeout = 30;
    private Clock clock;

    public BaseQueueService() {
        clock = new CurrentClock();
    }

    protected int getVisibilityTimeout() {
        return visibilityTimeout;
    }

    protected void setVisibilityTimeout(int visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }

    public Clock getClock() {
        return clock;
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }

}
