package com.example.spytool.model;

import android.graphics.Bitmap;

public class StegoMessage {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String fileName;
    private String dropboxPath;
    private long timestamp;
    private boolean decoded;
    private Bitmap bitmap; // локальный preview
    private Bitmap previewBitmap;

    public StegoMessage() {}
    public Bitmap getPreviewBitmap() {
        return previewBitmap;
    }

    public void setPreviewBitmap(Bitmap bitmap) {
        this.previewBitmap = bitmap;
    }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getDropboxPath() { return dropboxPath; }
    public void setDropboxPath(String dropboxPath) { this.dropboxPath = dropboxPath; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isDecoded() { return decoded; }
    public void setDecoded(boolean decoded) { this.decoded = decoded; }

    public Bitmap getBitmap() { return bitmap; }
    public void setBitmap(Bitmap bitmap) { this.bitmap = bitmap; }
}
