package com.example;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class FileQueueService extends BaseQueueService implements QueueService {

    public static final String FILE_ROOT_PATH = String.format("%stmp%ssqs", File.separator, File.separator);
    public static final String MESSAGES_FILE_NAME = "messages";
    public static final String LOCK_FILE_NAME = ".lock";
    public static final String IDS_FILE_NAME = "ids";

    /**
     * Trying to avoid creating a new File object
     * every time the messages file is accessed
     */
    private HashMap<String, File> messagesFiles;

    public FileQueueService() {
        this.messagesFiles = new HashMap<>();
    }

    @Override
    public PushMessageResult push(PushMessageRequest pushMessageRequest) {
        String id = generateId(pushMessageRequest.getQueueName());
        Message message = new Message(id, pushMessageRequest.getMessageBody());

        File messagesFile = getMessagesFile(pushMessageRequest.getQueueName());
        File lockFile = getLockFile(getFilePath(pushMessageRequest.getQueueName(), LOCK_FILE_NAME));

        lock(lockFile);
        try {
            PrintWriter messagesWriter = new PrintWriter(new FileWriter(messagesFile, true));
            messagesWriter.println(buildRecord(message, 0));
            messagesWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            unlock(lockFile);
        }

        PushMessageResult result = new PushMessageResult(id);
        return result;
    }

    @Override
    public Message pull(PullMessageRequest pullMessageRequest) {
        File messagesFile = messagesFiles.get(pullMessageRequest.getQueueName());
        if (messagesFile == null) {
            return null;
        }

        File lockFile = getLockFile(getFilePath(pullMessageRequest.getQueueName(), LOCK_FILE_NAME));
        File tmpFile = getTemporaryFile(pullMessageRequest.getQueueName());
        Message message = null;

        lock(lockFile);
        try {
            BufferedReader messagesReader = new BufferedReader(new FileReader(messagesFile));
            PrintWriter tmpWriter = new PrintWriter(new FileWriter(tmpFile, true));

            String line;
            boolean found = false;
            while((line = messagesReader.readLine()) != null) {
                if (isVisible(getRecordTimestamp(line)) && !found) {
                    message = getMessageFromRecord(line);
                    found = true;
                    int timeout = pullMessageRequest.getVisibilityTimeout() != null ?
                            pullMessageRequest.getVisibilityTimeout() : getVisibilityTimeout();
                    String invisibleRecord = buildRecord(message, timeout);
                    tmpWriter.println(invisibleRecord);
                } else {
                    tmpWriter.println(line);
                }
            }

            messagesFile.delete();
            tmpFile.renameTo(messagesFile);
            tmpWriter.close();
        } catch (IOException e) {
            tmpFile.delete();
            e.printStackTrace();
            return null;
        } finally {
            unlock(lockFile);
        }

        return message;
    }

    @Override
    public void delete(DeleteMessageRequest deleteMessageRequest) {
        File messagesFile = messagesFiles.get(deleteMessageRequest.getQueueName());
        if (messagesFile == null) {
            return;
        }

        File lockFile = getLockFile(getFilePath(deleteMessageRequest.getQueueName(), LOCK_FILE_NAME));
        File tmpFile = getTemporaryFile(deleteMessageRequest.getQueueName());
        Message message = null;

        lock(lockFile);
        try {
            BufferedReader messagesReader = new BufferedReader(new FileReader(messagesFile));
            PrintWriter tmpWriter = new PrintWriter(new FileWriter(tmpFile, true));

            String line;
            while ((line = messagesReader.readLine()) != null) {
                message = getMessageFromRecord(line);
                if (message.getId().equals(deleteMessageRequest.getMessageId())) {
                    continue;
                } else {
                    tmpWriter.println(line);
                }
            }

            messagesFile.delete();
            tmpFile.renameTo(messagesFile);
            tmpWriter.close();
        } catch (IOException e) {
            tmpFile.delete();
            e.printStackTrace();
        } finally {
            unlock(lockFile);
        }
    }

    private Message getMessageFromRecord(String record) {
        String id = record.substring(0, record.indexOf(':'));
        String body = record.substring(record.indexOf(':') + 1, record.lastIndexOf(':'));
        Message message = new Message(id, body);
        return message;
    }

    private long getRecordTimestamp(String record) {
        String timestamp = record.substring(record.lastIndexOf(":") + 1);
        return Long.parseLong(timestamp);
    }

    private boolean isVisible(Long timestamp) {
        return timestamp < getClock().currentTimeMillis();
    }

    private String buildRecord(Message message, Integer visibilityTimeout) {
        Long timestamp = new Long(0);
        if (visibilityTimeout != 0) {
            timestamp = getClock().currentTimeMillis() + TimeUnit.SECONDS.toMillis(visibilityTimeout);
        }
        return message.getId() + ":" + message.getBody() + ":" + timestamp.toString();
    }

    private String getRootFolderPath(String queueName) {
        return FILE_ROOT_PATH + File.separator + queueName;
    }

    private File getMessagesFile(String queueName) {
        File file = messagesFiles.get(queueName);
        if (file != null) {
            return file;
        }
        file = new File(getFilePath(queueName, MESSAGES_FILE_NAME));
        File folder = new File(getRootFolderPath(queueName));

        if (!folder.exists()) {
            folder.mkdirs();
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        messagesFiles.put(queueName, file);
        return file;
    }

    private String getFilePath(String queueName, String fileName) {
        return FILE_ROOT_PATH + File.separator + queueName + File.separator + fileName;
    }

    private File getLockFile(String filePath) {
        File file = new File(filePath);
        return file;
    }

    private File getTemporaryFile(String queueName) {
        File folder = new File(getRootFolderPath(queueName));
        folder.mkdirs();
        try {
            File file = File.createTempFile("sqsTemp", ".sqstmp", folder);
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Synchronize id creation at process level through a file
     */
    private String generateId(String queueName) {
        File file = new File(getFilePath(queueName, IDS_FILE_NAME));
        if (!file.exists()) {
            File folder = new File(getRootFolderPath(queueName));
            folder.mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                PrintWriter writer = new PrintWriter(new FileWriter(file));
                writer.print(0);
                writer.close();
                return "0";
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            long id = Long.parseLong(reader.readLine());

            File newFile = getTemporaryFile(queueName);
            PrintWriter writer = new PrintWriter(new FileWriter(newFile));
            writer.print(id + 1);
            writer.close();
            file.delete();
            newFile.renameTo(file);
            return String.valueOf(id + 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void lock(File lockFile) {
        while (!lockFile.mkdir()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void unlock(File lockFile) {
        lockFile.delete();
    }
}
