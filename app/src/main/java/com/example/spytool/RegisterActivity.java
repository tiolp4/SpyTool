package com.example.spytool;

import static com.example.spytool.R.drawable.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonRegister;
    private TextView textViewError;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewError = findViewById(R.id.textViewError);
        progressBar = findViewById(R.id.progressBar);

        buttonRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError();
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError();
            return;
        }

        if (password.length() < 6) {
            showError();
            return;
        }

        showProgress(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestore(email);

                        Toast.makeText(RegisterActivity.this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        showError("Ошибка регистрации: " + task.getException().getMessage());
                    }
                    showProgress(false);
                });
    }

    private void saveUserToFirestore(String email) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) return;

        String displayName = email.contains("@") ?
                email.substring(0, email.indexOf("@")) : "Пользователь";

        if (!displayName.isEmpty()) {
            displayName = displayName.substring(0, 1).toUpperCase() +
                    displayName.substring(1);
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", firebaseUser.getUid());
        userData.put("email", email);
        userData.put("displayName", displayName);
        userData.put("photoUrl", "");
        userData.put("timestamp", FieldValue.serverTimestamp());

        // Сохраняем в Firestore с ID = UID пользователя
        db.collection("users").document(firebaseUser.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("RegisterActivity", "Пользователь сохранен в Firestore: " + email);
                })
                .addOnFailureListener(e -> {
                    Log.e("RegisterActivity", "Ошибка сохранения пользователя", e);
                });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonRegister.setEnabled(!show);
    }

    private void showError(String error) {
        textViewError.setText(error);
        textViewError.setVisibility(View.VISIBLE);
    }
    private void showError() {
        textViewError.setVisibility(View.VISIBLE);
    }
}