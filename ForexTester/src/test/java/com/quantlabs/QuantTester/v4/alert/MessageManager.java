package com.quantlabs.QuantTester.v4.alert;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MessageManager {
    public enum MessageStatus {
        NEW, READ, DISMISSED, SENT, UNSENT
    }

    public static class Message {
        private final String id;
        private final String header;
        private String body;
        private MessageStatus status;
        private final LocalDateTime timestamp;

        public Message(String header, String body, MessageStatus status) {
            this.id = UUID.randomUUID().toString();
            this.header = header;
            this.body = body;
            this.status = status;
            this.timestamp = LocalDateTime.now();
        }

        public String getId() { return id; }
        public String getHeader() { return header; }
        public String getBody() { return body; }
        public MessageStatus getStatus() { return status; }
        public void setStatus(MessageStatus status) { this.status = status; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }

    private final List<Message> messages = new ArrayList<>();
    private List<Message> filteredMessages = new ArrayList<>();
    private int rowsPerPage = 20;
    private int currentPage = 1;
    private Comparator<Message> currentSortComparator = Comparator.comparing(Message::getTimestamp).reversed();
    private String currentFilter = "";

    public void addMessage(String header, String body, MessageStatus status) {
        Message newMessage = new Message(header, body, status);
        messages.add(newMessage);
        sortMessages(currentSortComparator);
        applyFilter(currentFilter, null);
    }

    public void sortMessages(Comparator<Message> comparator) {
        this.currentSortComparator = comparator;
        messages.sort(comparator);
        applyFilter(currentFilter, null);
        currentPage = 1;
    }

    public Comparator<Message> getCurrentSortComparator() {
        return currentSortComparator;
    }

    public Message getMessageById(String id) {
        return messages.stream()
                .filter(msg -> msg.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public List<Message> getPaginatedMessages() {
        List<Message> activeMessages = filteredMessages.isEmpty() && currentFilter.isEmpty() ? messages : filteredMessages;
        int start = (currentPage - 1) * rowsPerPage;
        int end = Math.min(start + rowsPerPage, activeMessages.size());
        return start >= activeMessages.size() ? new ArrayList<>() : activeMessages.subList(start, end);
    }

    public void deleteMessages(List<String> ids) {
        messages.removeIf(msg -> ids.contains(msg.getId()));
        applyFilter(currentFilter, null);
        if (currentPage > getTotalPages()) {
            currentPage = Math.max(1, getTotalPages());
        }
    }

    public void applyFilter(String searchText, DateTimeFormatter formatter) {
        currentFilter = searchText.trim().toLowerCase();
        if (currentFilter.isEmpty()) {
            filteredMessages = new ArrayList<>(messages);
        } else {
            filteredMessages = messages.stream()
                    .filter(msg -> msg.getHeader().toLowerCase().contains(currentFilter) ||
                                   msg.getBody().toLowerCase().contains(currentFilter) ||
                                   msg.getStatus().name().toLowerCase().contains(currentFilter) ||
                                   (formatter != null && msg.getTimestamp().format(formatter).toLowerCase().contains(currentFilter)))
                    .collect(Collectors.toList());
        }
        filteredMessages.sort(currentSortComparator);
        currentPage = 1;
    }

    public int getRowsPerPage() { return rowsPerPage; }
    public void setRowsPerPage(int rows) {
        this.rowsPerPage = rows;
        currentPage = 1;
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int page) { this.currentPage = page; }

    public int getTotalPages() {
        List<Message> activeMessages = filteredMessages.isEmpty() && currentFilter.isEmpty() ? messages : filteredMessages;
        return (int) Math.ceil((double) activeMessages.size() / rowsPerPage);
    }

    public void prevPage() {
        if (currentPage > 1) currentPage--;
    }

    public void nextPage() {
        if (currentPage < getTotalPages()) currentPage++;
    }

    public List<Message> getAllMessages() { return new ArrayList<>(messages); }

    // New method to get messages with NEW status
    public List<Message> getNewMessages() {
        return messages.stream()
                .filter(msg -> msg.getStatus() == MessageStatus.NEW)
                .sorted(currentSortComparator)
                .collect(Collectors.toList());
    }
}