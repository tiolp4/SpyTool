package com.example.spytool;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonRegister;
    private TextView textViewError;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewError = findViewById(R.id.textViewError);
        progressBar = findViewById(R.id.progressBar);

        buttonLogin.setOnClickListener(v -> loginUser());
        buttonRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Заполните все поля");
            return;
        }

        showProgress(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showProgress(false);
                    if (task.isSuccessful()) {
                        // Обновляем/сохраняем пользователя в Firestore
                        updateUserInFirestore(email);

                        Toast.makeText(LoginActivity.this, "Вход выполнен", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        showError("Ошибка входа: " + task.getException().getMessage());
                    }
                });
    }

    private void updateUserInFirestore(String email) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) return;

        // Создаем displayName из email
        String displayName = email.contains("@") ?
                email.substring(0, email.indexOf("@")) : "Пользователь";

        if (!displayName.isEmpty()) {
            displayName = displayName.substring(0, 1).toUpperCase() +
                    displayName.substring(1);
        }

        // Данные для обновления
        Map<String, Object> updates = new HashMap<>();
        updates.put("userId", firebaseUser.getUid());
        updates.put("email", email);
        updates.put("displayName", displayName);
        updates.put("timestamp", FieldValue.serverTimestamp());

        // Обновляем или создаем документ пользователя
        db.collection("users").document(firebaseUser.getUid())
                .set(updates, SetOptions.merge()) // merge обновит только указанные поля
                .addOnSuccessListener(aVoid -> {
                    Log.d("LoginActivity", "Данные пользователя обновлены в Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Ошибка обновления пользователя", e);
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!show);
        buttonRegister.setEnabled(!show);
    }

    private void showError(String error) {
        textViewError.setText(error);
        textViewError.setVisibility(View.VISIBLE);
    }
}