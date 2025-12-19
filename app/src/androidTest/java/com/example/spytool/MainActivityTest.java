package com.example.spytool;

import static org.junit.Assert.assertEquals;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.spytool.stego.Decoder;
import com.example.spytool.stego.Encoder;
import com.example.spytool.stego.XorCipher;


import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Test
    public void testEncodeDecodeLogic() {
        String message = "Hello";
        String key = "123";

        byte[] encrypted = XorCipher.encrypt(message.getBytes(), key);

        int width = 8;
        int height = 8;
        int[] pixels = new int[width * height];

        Encoder encoder = new Encoder();
        encoder.encode(pixels, width, height, encrypted);

        Decoder decoder = new Decoder();
        byte[] decodedBytes = decoder.decode(pixels, width, height);

        byte[] decrypted = XorCipher.decrypt(decodedBytes, key);
        String result = new String(decrypted);

        assertEquals("Hello", result);
        Log.d("Test", "Original message: " + message);
        Log.d("Test", "Encrypted bytes: " + Arrays.toString(encrypted));
        Log.d("Test", "Bitmap encoded");
        Log.d("Test", "Decoded bytes: " + Arrays.toString(decodedBytes));
        Log.d("Test", "Decrypted message: " + result);
    }






}
