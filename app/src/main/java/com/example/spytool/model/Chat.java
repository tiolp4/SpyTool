package com.example.spytool.model;

import java.util.Date;

public class Chat {
    private String chatId;
    private String user1Id;
    private String user2Id;
    private String user1Name;
    private String user2Name;
    private long lastActivity;

    public Chat() {
        this.lastActivity = System.currentTimeMillis();
    }

    // Геттеры и сеттеры
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public String getUser1Id() { return user1Id; }
    public void setUser1Id(String user1Id) { this.user1Id = user1Id; }

    public String getUser2Id() { return user2Id; }
    public void setUser2Id(String user2Id) { this.user2Id = user2Id; }

    public String getUser1Name() { return user1Name; }
    public void setUser1Name(String user1Name) { this.user1Name = user1Name; }

    public String getUser2Name() { return user2Name; }
    public void setUser2Name(String user2Name) { this.user2Name = user2Name; }

    public long getLastActivity() { return lastActivity; }
    public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }

    // Вспомогательные методы
    public String getOtherUserId(String currentUserId) {
        if (user1Id.equals(currentUserId)) {
            return user2Id;
        } else {
            return user1Id;
        }
    }

    public String getOtherUserName(String currentUserId) {
        if (user1Id.equals(currentUserId)) {
            return user2Name;
        } else {
            return user1Name;
        }
    }
}