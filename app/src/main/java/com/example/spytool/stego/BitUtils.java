package com.example.spytool.stego;

public class BitUtils {

    public static int[] intToBits(int value) {
        int[] bits = new int[32];
        for (int i = 31; i >= 0; i--) {
            bits[31 - i] = (value >> i) & 1;
        }
        return bits;
    }

    public static int[] bytesToBits(byte[] data) {
        int[] bits = new int[data.length * 8];
        int index = 0;
        for (byte b : data) {
            for (int i = 7; i >= 0; i--) {
                bits[index++] = (b >> i) & 1;
            }
        }
        return bits;
    }

    public static byte[] bitsToBytes(int[] bits) {
        byte[] bytes = new byte[bits.length / 8];
        for (int i = 0; i < bytes.length; i++) {
            byte value = 0;
            for (int j = 0; j < 8; j++) {
                value = (byte) ((value << 1) | bits[i * 8 + j]);
            }
            bytes[i] = value;
        }
        return bytes;
    }
    public static byte[] bitsToBytes(byte[] bits) {
        byte[] bytes = new byte[bits.length / 8];
        for (int i = 0; i < bytes.length; i++) {
            byte value = 0;
            for (int j = 0; j < 8; j++) {
                value = (byte)((value << 1) | (bits[i * 8 + j] & 1));
            }
            bytes[i] = value;
        }
        return bytes;
    }

}
