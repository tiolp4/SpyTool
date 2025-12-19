package com.example.spytool;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.spytool.repository.DropboxRepository;
import com.example.spytool.stego.Encoder;
import com.example.spytool.stego.XorCipher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;

public class ImageSendActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 101;

    private TextView textViewReceiver;
    private ImageView imageViewSelected;
    private Button btnSelectImage, btnEnterMessage, btnEncodeAndSend;

    private Bitmap selectedImage;
    private String secretMessage = "";

    private String receiverId;
    private String receiverName;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private DropboxRepository dropboxRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_send_activty);

        receiverId = getIntent().getStringExtra("receiver_id");
        receiverName = getIntent().getStringExtra("receiver_name");

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dropboxRepository = new DropboxRepository();

        initViews();

        if (receiverName != null) {
            textViewReceiver.setText("Получатель: " + receiverName);
        }

        btnSelectImage.setOnClickListener(v -> selectImage());
        btnEnterMessage.setOnClickListener(v -> enterSecretMessage());
        btnEncodeAndSend.setOnClickListener(v -> encodeAndSendToDropbox());
    }

    private void initViews() {
        textViewReceiver = findViewById(R.id.textViewReceiver);
        imageViewSelected = findViewById(R.id.imageViewSelected);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnEnterMessage = findViewById(R.id.btnEnterMessage);
        btnEncodeAndSend = findViewById(R.id.btnEncodeAndSend);
    }
    private String getEncryptionKey() {
        return getSharedPreferences(
                SettingsActivity.PREFS_NAME,
                MODE_PRIVATE
        ).getString(SettingsActivity.KEY_ENCRYPTION, "spy_secret_2024");
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageViewSelected.setImageBitmap(selectedImage);
                Toast.makeText(this, "Изображение выбрано", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enterSecretMessage() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Секретное сообщение");

        final EditText input = new EditText(this);
        input.setHint("Введите сообщение для скрытия в изображении");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            secretMessage = input.getText().toString();
            if (!secretMessage.isEmpty()) {
                Toast.makeText(this, "Сообщение сохранено (" + secretMessage.length() + " символов)", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void encodeAndSendToDropbox() {
        if (selectedImage == null || secretMessage.isEmpty() || receiverId == null) {
            Toast.makeText(this, "Не хватает данных", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] messageBytes = secretMessage.getBytes("UTF-8");
            String key = getEncryptionKey();
            if (key == null || key.isEmpty()) {
                Toast.makeText(this, "Ключ шифрования не задан", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] encryptedBytes = XorCipher.encrypt(messageBytes, getEncryptionKey());

            int width = selectedImage.getWidth();
            int height = selectedImage.getHeight();
            int[] pixels = new int[width * height];
            if (pixels.length < 11) {
                Toast.makeText(this, "Изображение слишком маленькое", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedImage.getPixels(pixels, 0, width, 0, 0, width, height);
            int maxPixels = pixels.length;

            if (!Encoder.canEncode(maxPixels, encryptedBytes.length)) {
                Toast.makeText(
                        this,
                        "Сообщение слишком большое для выбранного изображения",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }
            Encoder encoder = new Encoder();
            encoder.encode(pixels, width, height, encryptedBytes);

            Bitmap encodedImage = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);

            String fileName = "stego_" + System.currentTimeMillis() + ".png";

            dropboxRepository.uploadImage(encodedImage, fileName, new DropboxRepository.UploadCallback() {
                @Override
                public void onSuccess(String dropboxPath) {
                    saveMessageInfo(dropboxPath);
                    Toast.makeText(ImageSendActivity.this, "Стего-изображение загружено!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(ImageSendActivity.this, "Ошибка Dropbox: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка кодирования", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveMessageInfo(String dropboxPath) {
        String senderId = auth.getCurrentUser().getUid();

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("senderId", senderId);
        data.put("receiverId", receiverId);
        data.put("dropboxPath", dropboxPath);
        data.put("timestamp", System.currentTimeMillis());
        data.put("isStego", true);

        db.collection("stego_messages").add(data)
                .addOnSuccessListener(documentReference -> sendPushNotification(dropboxPath));
    }

    private void sendPushNotification(String dropboxPath) {
        db.collection("users").document(receiverId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String token = documentSnapshot.getString("fcmToken");
                        if (token != null) {
                            new Thread(() -> {
                                try {
                                    String senderId = auth.getCurrentUser().getUid();
                                    String senderName = auth.getCurrentUser().getDisplayName();
                                    String email = auth.getCurrentUser().getEmail();

                                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                                    okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/json; charset=utf-8");

                                    String json = "{"
                                            + "\"to\":\"" + token + "\","
                                            + "\"data\":{"
                                            + "\"title\":\"Новое стего-сообщение\","
                                            + "\"body\":\"У вас новое сообщение от " + (senderName != null ? senderName : email) + "\","
                                            + "\"senderId\":\"" + senderId + "\","
                                            + "\"senderName\":\"" + (senderName != null ? senderName : email) + "\","
                                            + "\"dropboxPath\":\"" + dropboxPath + "\""
                                            + "}"
                                            + "}";

                                    okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, json);
                                    okhttp3.Request request = new okhttp3.Request.Builder()
                                            .url("https://fcm.googleapis.com/fcm/send")
                                            .post(body)
                                            .addHeader("Authorization", "key=BK0juNXgaa0v6Re4YCv45qdCCasM4yXEZHWK2KvDE6vA1Exxl2Q9hlF-It1nGoVlLmbqbgBCRhiWbg9yw5jqvoU")
                                            .addHeader("Content-Type", "application/json")
                                            .build();

                                    okhttp3.Response response = client.newCall(request).execute();
                                    response.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }).start();
                        }
                    }
                });
    }

}
