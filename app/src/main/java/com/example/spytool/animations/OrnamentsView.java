package com.example.spytool.animations;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class OrnamentsView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float phase = 0f;

    public OrnamentsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        linePaint.setColor(Color.parseColor("#9CA3AF"));
        linePaint.setStrokeWidth(3f);

        ballPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();

        // координаты шаров
        drawBall(canvas, width * 0.2f, 60, 16, "#EF4444");
        drawBall(canvas, width * 0.45f, 80, 20, "#3B82F6");
        drawBall(canvas, width * 0.7f, 65, 20, "#22C55E");

        phase += 0.05f;
        postInvalidateOnAnimation();
    }

    private void drawBall(Canvas canvas, float x, float length, float radius, String color) {
        float sway = (float) Math.sin(phase + x) * 4f;

        canvas.drawLine(x, 0, x + sway, length, linePaint);

        ballPaint.setColor(Color.parseColor(color));
        canvas.drawCircle(x + sway, length + radius, radius, ballPaint);
    }
}

