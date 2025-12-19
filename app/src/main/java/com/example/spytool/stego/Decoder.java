package com.example.spytool.stego;

import android.util.Log;

public class Decoder {

    private static final String TAG = "Decoder";

    public byte[] decode(int[] pixels, int width, int height) {

        int totalBits = pixels.length * 3;
        byte[] allBits = new byte[(totalBits + 7) / 8];

        int bitIndex = 0;

        for (int pixel : pixels) {
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            setBit(allBits, bitIndex++, r & 1);
            setBit(allBits, bitIndex++, g & 1);
            setBit(allBits, bitIndex++, b & 1);
        }

        int length = 0;
        for (int i = 0; i < 32; i++) {
            length = (length << 1) | getBit(allBits, i);
        }

        Log.d(TAG, "Decoded length: " + length + " bytes");

        if (length <= 0) {
            throw new IllegalStateException("Invalid message length");
        }

        int requiredBits = length * 8;
        int availableBits = totalBits - 32;

        if (requiredBits > availableBits) {
            throw new IllegalStateException("Image does not contain full message");
        }

        byte[] messageBits = new byte[requiredBits];
        for (int i = 0; i < requiredBits; i++) {
            messageBits[i] = (byte) getBit(allBits, 32 + i);
        }

        return BitUtils.bitsToBytes(messageBits);
    }

    private void setBit(byte[] array, int index, int value) {
        int byteIndex = index / 8;
        int bitPos = 7 - (index % 8);
        if (value == 1) array[byteIndex] |= (1 << bitPos);
        else array[byteIndex] &= ~(1 << bitPos);
    }

    private int getBit(byte[] array, int index) {
        int byteIndex = index / 8;
        int bitPos = 7 - (index % 8);
        return (array[byteIndex] >> bitPos) & 1;
    }
}
