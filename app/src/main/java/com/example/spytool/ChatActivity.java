package com.example.spytool;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.spytool.adapter.StegoMessagesAdapter;
import com.example.spytool.model.StegoMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private TextView textViewChatTitle;
    private RecyclerView recyclerViewMessages;
    private LinearLayout layoutEmptyState;
    private Button buttonSendStego;

    private StegoMessagesAdapter messagesAdapter;
    private final List<StegoMessage> messagesList = new ArrayList<>();

    private FirebaseFirestore db;
    private String currentUserId;
    private String otherUserId;
    private String otherUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra("user_id");
        otherUserName = getIntent().getStringExtra("user_name");

        if (otherUserId == null || otherUserName == null) {
            Toast.makeText(this, "Ошибка: не указан собеседник", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        loadStegoMessages();
    }

    private void initViews() {
        textViewChatTitle = findViewById(R.id.textViewChatTitle);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        buttonSendStego = findViewById(R.id.buttonSendStego);

        textViewChatTitle.setText(otherUserName);

        findViewById(R.id.buttonBack).setOnClickListener(v -> finish());
        findViewById(R.id.buttonInfo).setOnClickListener(v -> showChatInfo());

        buttonSendStego.setOnClickListener(v -> openImageSendActivity());
    }

    private void setupRecyclerView() {
        messagesAdapter = new StegoMessagesAdapter(
                messagesList,
                this::openDecode   // 👈 ТОЛЬКО message
        );

        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messagesAdapter);
    }

    private void loadStegoMessages() {
        CollectionReference ref = db.collection("stego_messages");

        ref.whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", otherUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(this::handleSnapshot);

        ref.whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", otherUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(this::handleSnapshot);
    }

    private void handleSnapshot(QuerySnapshot snapshot, FirebaseFirestoreException error) {
        if (error != null) {
            Toast.makeText(this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
            return;
        }

        if (snapshot == null) return;

        messagesList.clear();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            StegoMessage msg = doc.toObject(StegoMessage.class);
            if (msg != null) {
                msg.setMessageId(doc.getId());
                messagesList.add(msg);
            }
        }

        messagesAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = messagesList.isEmpty();
        recyclerViewMessages.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void openImageSendActivity() {
        Intent intent = new Intent(this, ImageSendActivity.class);
        intent.putExtra("receiver_id", otherUserId);
        intent.putExtra("receiver_name", otherUserName);
        startActivity(intent);
    }

    private void openDecode(StegoMessage message) {
        Intent intent = new Intent(this, DecodeActivity.class);
        intent.putExtra("message_id", message.getMessageId());
        startActivity(intent);
    }

    private void showChatInfo() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Информация о чате")
                .setMessage("Чат с: " + otherUserName + "\nФайлы хранятся в Dropbox.")
                .setPositiveButton("OK", null)
                .show();
    }
}
