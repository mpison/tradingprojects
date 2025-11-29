package com.quantlabs.QuantTester.v3.alert;

import java.time.LocalDateTime;
import java.util.UUID;

public class Message {
    private String id;
    private String header;
    private String body;
    private MessageStatus status;
    private LocalDateTime timestamp;

    public enum MessageStatus {
        OK, UNREAD, MESSAGE_SENT, ERROR
    }

    public Message(String header, String body, MessageStatus status, LocalDateTime timestamp) {
        this.id = UUID.randomUUID().toString();
        this.header = header;
        this.body = body;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getHeader() { return header; }
    public String getBody() { return body; }
    public MessageStatus getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    
    public void setStatus(MessageStatus status) { this.status = status; }
}