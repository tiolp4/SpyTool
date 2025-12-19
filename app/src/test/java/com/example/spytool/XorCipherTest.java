package com.example.spytool;

import com.example.spytool.stego.XorCipher;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class XorCipherTest {

    @Test
    public void testEncryptDecrypt() {
        String key = "secret";
        byte[] original = "Hello World!".getBytes();

        byte[] encrypted = XorCipher.encrypt(original.clone(), key);
        byte[] decrypted = XorCipher.decrypt(encrypted, key);

        assertArrayEquals(original, decrypted);
    }
}
