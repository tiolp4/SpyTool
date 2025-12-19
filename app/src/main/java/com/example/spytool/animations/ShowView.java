package com.example.spytool.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ShowView extends View {

    public ShowView(Context context,AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(Color.WHITE);
    }

    private static class Snowflake {
        float x;
        float y;
        float radius;
        float speed;
    }

    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();

    private static final int MAX_SNOWFLAKES = 120;


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        snowflakes.clear();

        for (int i = 0; i < MAX_SNOWFLAKES; i++) {
            Snowflake flake = new Snowflake();
            flake.x = random.nextInt(w);
            flake.y = random.nextInt(h);
            flake.radius = random.nextFloat() * 3f + 1f;
            flake.speed = random.nextFloat() * 2f + 1f;
            snowflakes.add(flake);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Snowflake flake : snowflakes) {
            canvas.drawCircle(flake.x, flake.y, flake.radius, paint);

            flake.y += flake.speed;
            flake.x += random.nextFloat() * 0.5f - 0.25f;

            if (flake.y > getHeight()) {
                flake.y = 0;
                flake.x = random.nextInt(getWidth());
            }
        }

        postInvalidateOnAnimation();
    }
}
