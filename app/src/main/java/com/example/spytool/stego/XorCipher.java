package com.example.spytool.stego;

public class XorCipher {

    public static byte[] encrypt(byte[] data, String key) {
        byte[] keyBytes = key.getBytes();
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ keyBytes[i % keyBytes.length]);
        }
        return result;
    }

    public static byte[] decrypt(byte[] data, String key) {
        return encrypt(data, key);
    }
}
