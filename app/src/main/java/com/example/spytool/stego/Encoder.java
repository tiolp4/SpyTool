package com.example.spytool.stego;

import android.graphics.Bitmap;
import android.util.Log;

public class Encoder {

    private static final String TAG = "Encoder";

    public void encode(int[] pixels, int width, int height, byte[] message) {
        int[] lengthBits = BitUtils.intToBits(message.length);
        int[] messageBits = BitUtils.bytesToBits(message);

        int[] allBits = new int[lengthBits.length + messageBits.length];
        System.arraycopy(lengthBits, 0, allBits, 0, lengthBits.length);
        System.arraycopy(messageBits, 0, allBits, lengthBits.length, messageBits.length);

        Log.d(TAG, "Encoding message of length: " + message.length + " bytes, total bits: " + allBits.length);

        int bitIndex = 0;

        outer:
        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int r = (pixel >> 16) & 0xFF;
            int g = (pixel >> 8) & 0xFF;
            int b = pixel & 0xFF;

            if (bitIndex < allBits.length) r = (r & 0xFE) | allBits[bitIndex++];
            if (bitIndex < allBits.length) g = (g & 0xFE) | allBits[bitIndex++];
            if (bitIndex < allBits.length) b = (b & 0xFE) | allBits[bitIndex++];

            pixels[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;

            if (bitIndex >= allBits.length) break outer;
        }
        Log.d(TAG, "Encoding finished. Bits written: " + bitIndex);
    }

    public static boolean canEncode(int pixelCount, int messageBytes) {
        int availableBits = pixelCount * 3;
        int requiredBits = 32 + (messageBytes * 8);
        return availableBits >= requiredBits;
    }

}
