package cn.edu.sustech.cs209.chatting.common;

import java.util.HashSet;

public class Message {

    private Long timestamp;

    private String sentBy;

    private HashSet<String> sendTo;
    private String data;

    public Message(Long timestamp, String sentBy,  String data) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.data = data;
    }
    public Long getTimestamp() {
        return timestamp;
    }
    public String getSentBy() {
        return sentBy;
    }

    public HashSet<String> getSendTo() {
        return sendTo;
    }
    public String getData() {
        return data;
    }
    @Override
    public String toString() {
        return data;
    }
}
