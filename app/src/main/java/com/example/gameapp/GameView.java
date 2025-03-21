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
    
    // Variables pour la physique de la balle
    private float velocityX = 0;
    private float velocityY = 0;
    private float gravity = 0.5f; // Force de la gravité simulée
    private float damping = 0.95f; // Facteur de friction/amortissement
    
    // Dimensions de l'écran
    private int screenWidth;
    private int screenHeight;
    
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
        // Sauvegarder les positions relatives actuelles (en pourcentage)
        float relativeX = 0.5f;
        float relativeY = 0.5f;
        
        if (screenWidth > 0 && screenHeight > 0) {
            relativeX = circleX / (float) screenWidth;
            relativeY = circleY / (float) screenHeight;
        }
        
        // Mettre à jour les dimensions de l'écran
        screenWidth = width;
        screenHeight = height;
        
        // Repositionner la balle tout en préservant sa position relative
        circleX = relativeX * screenWidth;
        circleY = relativeY * screenHeight;
        
        // S'assurer que la balle reste dans les limites
        if (circleX < circleRadius) circleX = circleRadius;
        if (circleX > screenWidth - circleRadius) circleX = screenWidth - circleRadius;
        if (circleY < circleRadius) circleY = circleRadius;
        if (circleY > screenHeight - circleRadius) circleY = screenHeight - circleRadius;
        
        // Réinitialiser les vitesses lors du changement d'orientation
        velocityX = 0;
        velocityY = 0;
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
        // Mise à jour du carré existant
        x = (x + 1) % 300;
        
        // Mise à jour de la position de la balle en fonction de sa vitesse
        circleX += velocityX;
        circleY += velocityY;
        
        // Collision avec les bords de l'écran
        if (circleX < circleRadius) {
            circleX = circleRadius;
            velocityX = -velocityX * damping; // Rebond avec perte d'énergie
        } else if (circleX > screenWidth - circleRadius) {
            circleX = screenWidth - circleRadius;
            velocityX = -velocityX * damping;
        }
        
        if (circleY < circleRadius) {
            circleY = circleRadius;
            velocityY = -velocityY * damping;
        } else if (circleY > screenHeight - circleRadius) {
            circleY = screenHeight - circleRadius;
            velocityY = -velocityY * damping;
        }
        
        // Appliquer l'amortissement/la friction
        velocityX *= damping;
        velocityY *= damping;
    }
    
    // Méthode appelée par MainActivity pour mettre à jour la position de la balle
    public void updateBallPosition(float accelerometerX, float accelerometerY) {
        // Ajout de l'accélération aux vitesses
        velocityX += accelerometerX * gravity;
        velocityY += accelerometerY * gravity;
    }
}
