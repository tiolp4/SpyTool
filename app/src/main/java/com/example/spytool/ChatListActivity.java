package com.example.spytool;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.example.spytool.adapter.UsersAdapter;
import com.example.spytool.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private static final String TAG = "ChatListActivity";
    private RecyclerView recyclerViewUsers;
    private UsersAdapter usersAdapter;
    private List<User> usersList = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView textViewTitle;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Проверка аутентификации
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        textViewTitle = findViewById(R.id.textViewTitle);
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);

        textViewTitle.setText("Загрузка пользователей...");

        setupRecyclerView();
        loadUsers();

        // Временная отладка структуры данных
        debugFirestoreStructure();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "=== ТЕКУЩИЙ ПОЛЬЗОВАТЕЛЬ ===");
            Log.d(TAG, "ID: " + currentUser.getUid());
            Log.d(TAG, "Email: " + currentUser.getEmail());
            Log.d(TAG, "DisplayName: " + currentUser.getDisplayName());

            // Сравните этот ID с тем, что в логах
            // Если ID совпадает с LGpxGdCzFvOp7AydxQoskfMz0uH2,
            // значит вы всё еще под kirill@gmail.com
        } else {
            Log.d(TAG, "Нет авторизованного пользователя!");
        }
    }

    private void setupRecyclerView() {
        usersAdapter = new UsersAdapter(usersList, user -> {
            Intent intent = new Intent(this, ImageSendActivity.class);
            intent.putExtra("receiver_id", user.getUserId());
            intent.putExtra("receiver_name", user.getDisplayName());
            startActivity(intent);
        });

        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(usersAdapter);
    }

    private void loadUsers() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Log.d(TAG, "Загрузка пользователей с сервера...");

        // Используйте Source.SERVER для принудительной загрузки с сервера
        db.collection("users")
                .get(Source.SERVER)  // <-- Добавьте это
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        usersList.clear();
                        int count = 0;

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            // ОТЛАДКА: выводим все данные документа
                            Log.d(TAG, "=== ДОКУМЕНТ ===");
                            Log.d(TAG, "ID документа: " + doc.getId());
                            Log.d(TAG, "Данные: " + doc.getData());

                            User user = doc.toObject(User.class);
                            if (user == null) {
                                Log.w(TAG, "Не удалось преобразовать в User");
                                continue;
                            }

                            Log.d(TAG, "User ID: " + user.getUserId());
                            Log.d(TAG, "Email: " + user.getEmail());

                            if (!currentUserId.equals(user.getUserId())) {
                                usersList.add(user);
                                count++;
                            }
                        }

                        usersAdapter.notifyDataSetChanged();
                        Log.d(TAG, "Загружено пользователей: " + count);

                        if (usersList.isEmpty()) {
                            textViewTitle.setText("Нет других пользователей");
                        } else {
                            textViewTitle.setText("Выберите получателя (" + usersList.size() + ")");
                        }
                    } else {
                        Log.e(TAG, "Ошибка загрузки: ", task.getException());
                        textViewTitle.setText("Ошибка загрузки");
                    }
                });
    }

    // Метод для отладки структуры данных в Firestore
    private void debugFirestoreStructure() {
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "=== ОТЛАДКА СТРУКТУРЫ ДАННЫХ ===");
                    Log.d(TAG, "Всего документов в коллекции users: " + querySnapshot.size());

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d(TAG, "--- Документ: " + doc.getId() + " ---");
                        Log.d(TAG, "Все поля документа:");

                        for (String field : doc.getData().keySet()) {
                            Object value = doc.get(field);
                            Log.d(TAG, field + ": " + value + " (тип: " +
                                    (value != null ? value.getClass().getSimpleName() : "null") + ")");
                        }

                        // Проверяем конкретные поля
                        Log.d(TAG, "displayName: " + doc.getString("displayName"));
                        Log.d(TAG, "email: " + doc.getString("email"));
                        Log.d(TAG, "userId: " + doc.getString("userId"));
                        Log.d(TAG, "photoUrl: " + doc.getString("photoUrl"));

                        if (doc.get("timestamp") != null) {
                            Log.d(TAG, "timestamp: " + doc.get("timestamp"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Ошибка при отладке структуры: ", e);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем список при возвращении на экран
        if (mAuth.getCurrentUser() != null) {
            loadUsers();
        }
    }
}