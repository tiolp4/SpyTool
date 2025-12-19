package com.example.spytool.repository;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.spytool.model.Chat;
import com.example.spytool.model.Message;
import com.example.spytool.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void getAllUsersExceptCurrent(UsersListener listener) {
        String currentUserId = getCurrentUserId();

        if (currentUserId == null) {
            listener.onError("Пользователь не авторизован");
            return;
        }

        db.collection("users")
                .whereNotEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            users.add(user);
                            Log.d(TAG, "Найден пользователь: " + user.getEmail());
                        }
                        listener.onUsersLoaded(users);
                        Log.d(TAG, "Всего найдено пользователей: " + users.size());
                    } else {
                        Log.e(TAG, "Ошибка получения пользователей: ", task.getException());
                        listener.onError("Ошибка: " + task.getException().getMessage());
                    }
                });
    }

    public void createChatWithUser(User otherUser, ChatListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            listener.onError("Пользователь не авторизован");
            return;
        }

        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User currentUser = documentSnapshot.toObject(User.class);

                    if (currentUser != null) {
                        Chat chat = new Chat();
                        chat.setUser1Id(currentUserId);
                        chat.setUser2Id(otherUser.getUserId());
                        chat.setUser1Name(currentUser.getDisplayName() != null ?
                                currentUser.getDisplayName() : currentUser.getEmail());
                        chat.setUser2Name(otherUser.getDisplayName() != null ?
                                otherUser.getDisplayName() : otherUser.getEmail());
                        chat.setLastActivity(System.currentTimeMillis());

                        // Сохраняем чат
                        db.collection("chats").add(chat)
                                .addOnSuccessListener(documentReference -> {
                                    chat.setChatId(documentReference.getId());
                                    listener.onChatCreated(chat);
                                })
                                .addOnFailureListener(e -> {
                                    listener.onError("Ошибка создания чата: " + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onError("Ошибка получения текущего пользователя");
                });
    }

    public void getUserChats(ChatsListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        db.collection("chats")
                .whereEqualTo("user1Id", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    handleChatsSnapshot(querySnapshot, error, listener, "user1Id");
                });

        db.collection("chats")
                .whereEqualTo("user2Id", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    handleChatsSnapshot(querySnapshot, error, listener, "user2Id");
                });
    }

    private void handleChatsSnapshot(QuerySnapshot querySnapshot, FirebaseFirestoreException error,
                                     ChatsListener listener, String field) {
        if (error != null) {
            Log.e(TAG, "Ошибка прослушивания чатов: ", error);
            return;
        }

        if (querySnapshot != null) {
            List<Chat> chats = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Chat chat = doc.toObject(Chat.class);
                if (chat != null) {
                    chat.setChatId(doc.getId());
                    chats.add(chat);
                }
            }
            listener.onChatsLoaded(chats);
        }
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }

    public interface UsersListener {
        void onUsersLoaded(List<User> users);
        void onError(String error);
    }

    public interface ChatListener {
        void onChatCreated(Chat chat);
        void onError(String error);
    }

    public interface ChatsListener {
        void onChatsLoaded(List<Chat> chats);
    }
}