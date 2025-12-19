package com.example.spytool;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spytool.repository.DropboxRepository;
import com.example.spytool.stego.Decoder;
import com.example.spytool.stego.XorCipher;

public class DecodeActivity extends AppCompatActivity {

    private TextView textViewResult;
    private DropboxRepository dropboxRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode);

        textViewResult = findViewById(R.id.textViewDecodedMessage);
        dropboxRepository = new DropboxRepository();

        String dropboxPath = getIntent().getStringExtra("dropbox_path");

        if (dropboxPath == null || dropboxPath.isEmpty()) {
            Toast.makeText(this, "Нет пути к файлу", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        decodeFromDropbox(dropboxPath);
    }
    private String getEncryptionKey() {
        return getSharedPreferences(
                SettingsActivity.PREFS_NAME,
                MODE_PRIVATE
        ).getString(SettingsActivity.KEY_ENCRYPTION, "spy_secret_2024");
    }
    private void decodeFromDropbox(String dropboxPath) {
        textViewResult.setText("🔐 Декодирование...");

        dropboxRepository.downloadImage(dropboxPath,
                new DropboxRepository.DownloadCallback() {

                    @Override
                    public void onSuccess(Bitmap bitmap) {
                        try {
                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();

                            int[] pixels = new int[width * height];
                            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

                            Decoder decoder = new Decoder();
                            byte[] encodedData = decoder.decode(pixels, width, height);

                            byte[] decrypted = XorCipher.decrypt(encodedData, getEncryptionKey());
                            String secretMessage = new String(decrypted, "UTF-8");

                            textViewResult.setText("💬 " + secretMessage);

                        } catch (Exception e) {
                            Toast.makeText(DecodeActivity.this,
                                    "Ошибка декодирования", Toast.LENGTH_SHORT).show();
                            textViewResult.setText("❌ Не удалось декодировать");
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(DecodeActivity.this,
                                "Ошибка загрузки из Dropbox", Toast.LENGTH_SHORT).show();
                        textViewResult.setText("❌ Ошибка загрузки");
                    }
                });
    }
}
