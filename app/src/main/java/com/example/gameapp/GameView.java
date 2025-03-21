package com.example.gameapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread thread;
    private int y;
    private int x=0;
    // Variables pour le cercle
    private float circleX; // Position X du centre du cercle
    private float circleY; // Position Y du centre du cercle
    private float circleRadius = 25; // Rayon du cercle en pixels
    private Paint circlePaint; // Pinceau pour dessiner le cercle
    public GameView(Context context, int valeur_y) {
        super(context);
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        this.y = valeur_y;

        // Initialisation du cercle
        circleX = 200; // Position X initiale
        circleY = 200; // Position Y initiale

        // Création et configuration du pinceau pour le cercle
        circlePaint = new Paint();
        circlePaint.setColor(Color.BLUE); // Couleur différente du carré
        circlePaint.setAntiAlias(true); // Pour un rendu plus lisse
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retry = false;
        }
    }
    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            canvas.drawColor(Color.WHITE);
            Paint paint = new Paint();
            paint.setColor(Color.rgb(250, 0, 0));
            // Utilisation de valeur_y pour placer le rectangle
            canvas.drawRect(x, y, x + 100, y + 100, paint);

            // Dessin cercle
            canvas.drawCircle(circleX, circleY, circleRadius, circlePaint);
        }
    }
    public void update() {
        x = (x + 1) % 300;
    }

}
