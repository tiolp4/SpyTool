package com.example.spytool;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.spytool.stego.Decoder;
import com.example.spytool.stego.Encoder;
import com.example.spytool.stego.XorCipher;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private FirebaseAuth mAuth;

    private Button btnEncodeImage;
    private Button btnDecodeImage;
    private TextView textViewStatus;

    private Bitmap selectedImage;
    private String secretMessage = "";
    private String encryptionKey = "spytool_secret_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupNavigationDrawer();
        updateUserProfile();
        onMessagingPush();
        btnEncodeImage.setOnClickListener(v -> openImageSelector());
        btnDecodeImage.setOnClickListener(v -> decodeImageFromGallery());
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        btnEncodeImage = findViewById(R.id.btnEncodeImage);
        btnDecodeImage = findViewById(R.id.btnDecodeImage);
        textViewStatus = findViewById(R.id.textViewStatus);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("SpyTool - Стеганограф");
        }
    }

    private void setupNavigationDrawer() {
        androidx.appcompat.app.ActionBarDrawerToggle toggle =
                new androidx.appcompat.app.ActionBarDrawerToggle(
                        this, drawerLayout, toolbar,
                        R.string.navigation_drawer_open,
                        R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        updateNavHeader();
    }

    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView textViewUserName = headerView.findViewById(R.id.textViewUserName);
        TextView textViewUserEmail = headerView.findViewById(R.id.textViewUserEmail);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            textViewUserName.setText(currentUser.getDisplayName() != null ?
                    currentUser.getDisplayName() : "Агент");
            textViewUserEmail.setText(currentUser.getEmail() != null ?
                    currentUser.getEmail() : "");
        }
    }

    private void openImageSelector() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    private void decodeImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(
                Intent.createChooser(intent, "Выберите изображение со скрытым сообщением"),
                PICK_IMAGE_REQUEST + 100
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();

            try {
                selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                if (requestCode == PICK_IMAGE_REQUEST) {
                    openMessageInputDialog();
                } else if (requestCode == PICK_IMAGE_REQUEST + 100) {
                    decodeSecretMessage();
                }

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openMessageInputDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Секретное сообщение");

        final EditText input = new EditText(this);
        input.setHint("Введите сообщение для скрытия");
        builder.setView(input);

        builder.setPositiveButton("Зашифровать", (dialog, which) -> {
            secretMessage = input.getText().toString();
            if (!secretMessage.isEmpty()) {
                encodeMessageIntoImage();
            } else {
                Toast.makeText(MainActivity.this, "Введите сообщение", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private void encodeMessageIntoImage() {
        if (selectedImage == null) {
            Toast.makeText(this, "Сначала выберите изображение", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] messageBytes = secretMessage.getBytes("UTF-8");
            byte[] encryptedBytes = XorCipher.encrypt(messageBytes, encryptionKey);

            int width = selectedImage.getWidth();
            int height = selectedImage.getHeight();
            int[] pixels = new int[width * height];
            selectedImage.getPixels(pixels, 0, width, 0, 0, width, height);

            Encoder encoder = new Encoder();
            encoder.encode(pixels, width, height, encryptedBytes);

            Bitmap encodedImage = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);

            saveEncodedImage(encodedImage);

            textViewStatus.setText("Сообщение успешно скрыто в изображении!");

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка кодирования: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void decodeSecretMessage() {
        if (selectedImage == null) {
            Toast.makeText(this, "Сначала выберите изображение", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int width = selectedImage.getWidth();
            int height = selectedImage.getHeight();
            int[] pixels = new int[width * height];
            selectedImage.getPixels(pixels, 0, width, 0, 0, width, height);

            Decoder decoder = new Decoder();
            byte[] decodedBytes = decoder.decode(pixels, width, height);

            byte[] decryptedBytes = XorCipher.decrypt(decodedBytes, encryptionKey);
            String decodedMessage = new String(decryptedBytes, "UTF-8");

            showDecodedMessage(decodedMessage);

        } catch (IllegalStateException e) {
            Toast.makeText(this,
                    "В этом изображении нет скрытого сообщения",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка декодирования", Toast.LENGTH_SHORT).show();
        }
    }


    private void showDecodedMessage(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Секретное сообщение");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();

        textViewStatus.setText("Сообщение извлечено!");
    }

    private void saveEncodedImage(Bitmap bitmap) {
        try {
            String fileName = "stego_" + System.currentTimeMillis() + ".png";

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SpyTool");

            Uri uri = getContentResolver().insert(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
            }

            Toast.makeText(this, "Изображение сохранено в галерее", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show();
        }
    }


    private void openChatsForSending() {
        Intent intent = new Intent(this, ChatListActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_encode) {
            openImageSelector();
        } else if (id == R.id.nav_decode) {
            decodeImageFromGallery();
        } else if (id == R.id.nav_chats) {
            openChatsForSending();
        } else if (id == R.id.nav_gallery) {
            openStegoGallery();
        } else if (id == R.id.nav_settings) {
            openSettings();
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(navigationView);
        return true;
    }

    private void onMessagingPush(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                String token = task.getResult();
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore.getInstance().collection("users")
                        .document(userId)
                        .update("fcmToken",token);
            }
        });
    }
    private void openStegoGallery() {
        Intent intent = new Intent(this, ReceivedStegoActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkCurrentUser();
    }

    private void checkCurrentUser() {
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        } else {
            updateNavHeader();
        }
    }
    private void updateUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        String email = currentUser.getEmail();
                        String displayName = currentUser.getDisplayName();

                        if (email == null) email = "";
                        if (displayName == null || displayName.isEmpty()) {
                            displayName = email.contains("@") ?
                                    email.substring(0, email.indexOf("@")) : "Пользователь";
                        }

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userId", currentUser.getUid());
                        userData.put("email", email);
                        userData.put("displayName", displayName);
                        userData.put("photoUrl", "");
                        userData.put("timestamp", FieldValue.serverTimestamp());

                        db.collection("users").document(currentUser.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid ->
                                        Log.d("MainActivity", "Пользователь создан в Firestore"))
                                .addOnFailureListener(e ->
                                        Log.e("MainActivity", "Ошибка создания пользователя", e));
                    }
                });
    }
}
