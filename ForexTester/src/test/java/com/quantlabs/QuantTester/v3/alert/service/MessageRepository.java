package com.quantlabs.QuantTester.v3.alert.service;

import com.quantlabs.QuantTester.v3.alert.Message;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MessageRepository {
    private final List<Message> messages = new ArrayList<>();

    // CREATE
    public void addMessage(Message message) {
        messages.add(message);
    }

    // READ (all messages)
    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    // READ (by status)
    public List<Message> getMessagesByStatus(Message.MessageStatus status) {
        return messages.stream()
                .filter(msg -> msg.getStatus() == status)
                .collect(Collectors.toList());
    }

    // READ (single message)
    public Message getMessageById(String id) {
        return messages.stream()
                .filter(msg -> msg.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // UPDATE (status)
    public void updateMessageStatus(String id, Message.MessageStatus status) {
        messages.stream()
                .filter(msg -> msg.getId().equals(id))
                .findFirst()
                .ifPresent(msg -> msg.setStatus(status));
    }

    // UPDATE (multiple statuses)
    public void updateMessagesStatus(List<String> ids, Message.MessageStatus status) {
        messages.stream()
                .filter(msg -> ids.contains(msg.getId()))
                .forEach(msg -> msg.setStatus(status));
    }

    // DELETE (single)
    public boolean deleteMessage(String id) {
        return messages.removeIf(msg -> msg.getId().equals(id));
    }

    // DELETE (multiple)
    public void deleteMessages(List<String> ids) {
        messages.removeIf(msg -> ids.contains(msg.getId()));
    }

    // Count unread messages
    public long countUnreadMessages() {
        return messages.stream()
                .filter(msg -> msg.getStatus() == Message.MessageStatus.UNREAD)
                .count();
    }
}