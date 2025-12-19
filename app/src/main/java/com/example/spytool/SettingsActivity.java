package com.example.spytool;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "crypto_settings";
    public static final String KEY_ENCRYPTION = "encryption_key";

    private EditText editEncryptionKey;
    private Button btnSaveKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        editEncryptionKey = findViewById(R.id.editEncryptionKey);
        btnSaveKey = findViewById(R.id.btnSaveKey);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        String savedKey = prefs.getString(KEY_ENCRYPTION, "spy_secret_2024");
        editEncryptionKey.setText(savedKey);

        btnSaveKey.setOnClickListener(v -> {
            String newKey = editEncryptionKey.getText().toString().trim();

            if (newKey.length() < 4) {
                Toast.makeText(this, "Ключ должен быть не короче 4 символов", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putString(KEY_ENCRYPTION, newKey)
                    .apply();

            Toast.makeText(this, "Ключ шифрования сохранён", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
