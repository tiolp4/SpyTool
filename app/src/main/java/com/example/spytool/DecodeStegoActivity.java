package com.example.spytool;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.spytool.repository.DropboxRepository;
import com.example.spytool.stego.Decoder;
import com.example.spytool.stego.XorCipher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.nio.charset.StandardCharsets;

public class DecodeStegoActivity extends AppCompatActivity {

    private static final String TAG = "DecodeStegoActivity";

    private TextView textViewStatus, textViewFileName;
    private ImageView imageViewStego;
    private ProgressBar progressBar;
    private Button btnDecodeMessage;

    private String messageId;
    private String dropboxPath;
    private String fileName;
    private Bitmap stegoImage;

    private DropboxRepository dropboxRepository;
    private FirebaseFirestore db;
    private FirebaseAuth auth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode_stego);

        messageId = getIntent().getStringExtra("message_id");
        dropboxPath = getIntent().getStringExtra("dropbox_path");
        fileName = getIntent().getStringExtra("file_name");

        if (dropboxPath == null || messageId == null) {
            Toast.makeText(this, "Ошибка: нет данных сообщения", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        dropboxRepository = new DropboxRepository();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        loadStegoImage();
    }

    private void initViews() {
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewFileName = findViewById(R.id.textViewFileName);
        imageViewStego = findViewById(R.id.imageViewStego);
        progressBar = findViewById(R.id.progressBar);
        btnDecodeMessage = findViewById(R.id.btnDecodeMessage);

        if (fileName != null) {
            textViewFileName.setText("Файл: " + fileName);
        }

        textViewStatus.setText("Загрузка изображения из Dropbox...");
        progressBar.setVisibility(android.view.View.VISIBLE);
        btnDecodeMessage.setEnabled(false);

        btnDecodeMessage.setOnClickListener(v -> decodeSecretMessage());
    }

    private void loadStegoImage() {
        Log.d(TAG, "Загрузка изображения: " + dropboxPath);

        dropboxRepository.downloadImage(dropboxPath, new DropboxRepository.DownloadCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                runOnUiThread(() -> {
                    stegoImage = bitmap;
                    imageViewStego.setImageBitmap(bitmap);
                    progressBar.setVisibility(android.view.View.GONE);
                    textViewStatus.setText("Изображение загружено. Нажмите 'Декодировать'");
                    btnDecodeMessage.setEnabled(true);
                    Log.d(TAG, "Изображение успешно загружено");
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    textViewStatus.setText("Ошибка загрузки: " + e.getMessage());
                    Toast.makeText(DecodeStegoActivity.this,
                            "Не удалось загрузить изображение: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Ошибка загрузки изображения", e);
                });
            }
        });
    }
    private String getEncryptionKey() {
        return getSharedPreferences(
                SettingsActivity.PREFS_NAME,
                MODE_PRIVATE
        ).getString(SettingsActivity.KEY_ENCRYPTION, "spy_secret_2024");
    }
    private void decodeSecretMessage() {
        if (stegoImage == null) {
            Toast.makeText(this, "Изображение не загружено", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(android.view.View.VISIBLE);
        textViewStatus.setText("Декодирование скрытого сообщения...");
        btnDecodeMessage.setEnabled(false);

        new Thread(() -> {
            try {
                int width = stegoImage.getWidth();
                int height = stegoImage.getHeight();
                int[] pixels = new int[width * height];
                stegoImage.getPixels(pixels, 0, width, 0, 0, width, height);

                Log.d(TAG, "Начинаю декодирование: " + width + "x" + height);

                Decoder decoder = new Decoder();
                byte[] decodedBytes = decoder.decode(pixels, width, height);

                Log.d(TAG, "Декодировано байт: " + decodedBytes.length);

                byte[] decryptedBytes = XorCipher.decrypt(decodedBytes,getEncryptionKey());
                String secretMessage = new String(decryptedBytes, StandardCharsets.UTF_8);

                Log.d(TAG, "Сообщение получено: " + secretMessage);

                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    showDecodedMessage(secretMessage);
                    updateMessageAsDecoded(secretMessage);
                });

            } catch (IllegalStateException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    textViewStatus.setText("В этом изображении нет скрытого сообщения");
                    Toast.makeText(DecodeStegoActivity.this,
                            "Это обычное изображение без скрытого сообщения",
                            Toast.LENGTH_LONG).show();
                    btnDecodeMessage.setEnabled(true);
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    textViewStatus.setText("Ошибка декодирования");
                    Toast.makeText(DecodeStegoActivity.this,
                            "Ошибка декодирования: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    btnDecodeMessage.setEnabled(true);
                    Log.e(TAG, "Ошибка декодирования", e);
                });
            }
        }).start();
    }

    private void showDecodedMessage(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("🔍 Секретное сообщение");
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            textViewStatus.setText("✅ Сообщение успешно декодировано");
        });
        builder.setNegativeButton("Удалить", (dialog, which) -> {
            deleteMessageFromDatabase();
        });
        builder.show();
    }

    private void updateMessageAsDecoded(String secretMessage) {
        if (messageId == null) return;

        DocumentReference messageRef = db.collection("stego_messages").document(messageId);

        messageRef.update(
                "isDecoded", true,
                "secretMessage", secretMessage,
                "isRead", true
        ).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Сообщение помечено как декодированное");
            Toast.makeText(this, "Сообщение сохранено", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Ошибка обновления сообщения", e);
        });
    }

    private void deleteMessageFromDatabase() {
        if (messageId == null) return;

        db.collection("stego_messages").document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Сообщение удалено", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка удаления: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}