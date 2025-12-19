package com.example.spytool.stego;


import android.graphics.Bitmap;

public interface IMessageHider {

    void encode(
            Bitmap image,
            String message,
            String key
    );

    String decode(
            Bitmap image,
            String key
    );
}
