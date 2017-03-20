package com.example;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileQueueTest {

    FileQueueService queue = new FileQueueService();
    String queueName = "testQueueName";
    String messageBody = "testMessageBody";
    PushMessageRequest pushMessageRequest = new PushMessageRequest(queueName, messageBody);
    PullMessageRequest pullMessageRequest = new PullMessageRequest(queueName);

    @Before
    public void before() {
        File file = new File(FileQueueService.FILE_ROOT_PATH +
                File.separator + queueName + File.separator +
                FileQueueService.MESSAGES_FILE_NAME);
        file.delete();
        file = new File(FileQueueService.FILE_ROOT_PATH +
                File.separator + queueName + File.separator +
                FileQueueService.IDS_FILE_NAME);
        file.delete();
    }

    @After
    public void after() {
        File file = new File(FileQueueService.FILE_ROOT_PATH +
                File.separator + queueName + File.separator +
                FileQueueService.MESSAGES_FILE_NAME);
        file.delete();
        file = new File(FileQueueService.FILE_ROOT_PATH +
                File.separator + queueName + File.separator +
                FileQueueService.IDS_FILE_NAME);
        file.delete();
    }

    @Test
    public void testMessageIsPushed() {
        PushMessageResult pushMessageResult = queue.push(pushMessageRequest);
        assertTrue(pushMessageResult != null);
    }

    @Test
    public void testMessageIsPulled() {
        queue.push(pushMessageRequest);
        Message message = queue.pull(pullMessageRequest);
        assertTrue(message != null);
    }

    @Test
    public void testMessageIsDeleted() {
        PushMessageResult pushMessageResult = queue.push(pushMessageRequest);
        DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(pushMessageResult.getMessageId(), queueName);
        queue.delete(deleteMessageRequest);
        Message result = queue.pull(pullMessageRequest);
        assertTrue(result == null);
    }

    @Test
    public void testMessageBecomesInvisibleAfterFirstPull() {
        queue.push(pushMessageRequest);
        queue.push(pushMessageRequest);
        Message message1 = queue.pull(pullMessageRequest);
        Message message2 = queue.pull(pullMessageRequest);
        assertTrue(message1.getId().equals(message2.getId()) == false);
    }

    @Test
    public void testMessageBecomesVisibleAfterTimeoutExpires() {
        queue.push(pushMessageRequest);
        queue.push(pushMessageRequest);
        PullMessageRequest pullMessageRequest = new PullMessageRequest(queueName, 1);

        Message message1 = queue.pull(pullMessageRequest);

        long currentMilliseconds = System.currentTimeMillis();
        Clock mockedClock = mock(Clock.class);
        when(mockedClock.currentTimeMillis()).thenReturn(currentMilliseconds + 50000);
        queue.setClock(mockedClock);

        Message message2 = queue.pull(pullMessageRequest);

        assertTrue(message1.getId().equals(message2.getId()) == true);
    }

    @Test
    public void testMessageIdsAreUnique() {
        PushMessageResult pushMessageResult1 = queue.push(pushMessageRequest);
        PushMessageResult pushMessageResult2 = queue.push(pushMessageRequest);
        assertFalse(pushMessageResult1.getMessageId().equals(pushMessageResult2.getMessageId()));
    }

    @Test
    public void testMessageIdsAreIncreasing() {
        PushMessageResult pushMessageResult1 = queue.push(pushMessageRequest);
        PushMessageResult pushMessageResult2 = queue.push(pushMessageRequest);
        assertTrue(pushMessageResult1.getMessageId().compareTo(pushMessageResult2.getMessageId()) < 0);
    }
}
