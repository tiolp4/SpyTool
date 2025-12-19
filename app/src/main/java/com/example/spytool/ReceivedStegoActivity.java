package com.example.spytool;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.example.spytool.adapter.StegoMessagesAdapter;
import com.example.spytool.model.StegoMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class ReceivedStegoActivity extends AppCompatActivity
        implements StegoMessagesAdapter.OnMessageClickListener {

    private static final String TAG = "ReceivedStegoActivity";
    private RecyclerView recyclerViewMessages;
    private StegoMessagesAdapter messagesAdapter;
    private List<StegoMessage> messagesList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView textViewTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_received_stego);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        loadReceivedMessages();
    }

    private void initViews() {
        textViewTitle = findViewById(R.id.textViewTitle);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);

        messagesAdapter = new StegoMessagesAdapter(messagesList, this);
        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messagesAdapter);

        textViewTitle.setText("Стеганографические сообщения");
    }

    private void loadReceivedMessages() {
        String currentUserId = auth.getCurrentUser().getUid();

        if (currentUserId == null) {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Загружаю сообщения для пользователя: " + currentUserId);

        db.collection("stego_messages")
                .whereEqualTo("receiverId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Ошибка загрузки сообщений: ", error);
                        Toast.makeText(this, "Ошибка загрузки сообщений", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        messagesList.clear();

                        Log.d(TAG, "Найдено сообщений: " + querySnapshot.size());

                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            StegoMessage message = doc.toObject(StegoMessage.class);
                            if (message != null) {
                                message.setMessageId(doc.getId());
                                messagesList.add(message);
                                Log.d(TAG, "Добавлено сообщение от: " + message.getSenderId());
                            }
                        }

                        messagesAdapter.notifyDataSetChanged();
                        textViewTitle.setText("Сообщения (" + messagesList.size() + ")");

                    } else {
                        Log.d(TAG, "Нет сообщений для этого пользователя");
                        textViewTitle.setText("Нет стего-сообщений");
                        Toast.makeText(this, "У вас пока нет стеганографических сообщений",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDecodeClick(StegoMessage message) {
        Log.d(TAG, "Декодирование сообщения: " + message.getMessageId());

        // Переходим в активность декодирования
        Intent intent = new Intent(this, DecodeStegoActivity.class);
        intent.putExtra("message_id", message.getMessageId());
        intent.putExtra("dropbox_path", message.getDropboxPath());
        intent.putExtra("file_name", message.getFileName());
        startActivity(intent);
    }
}