package com.example.gameapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Représente un bonus qui augmente la lucidité du joueur lorsqu'il est collecté
 */
public class Bonus {
    // Position du bonus
    private float x;
    private float y;
    
    // Taille du bonus
    private float radius = 15.0f;
    
    // Couleur du bonus
    private Paint paint;
    
    // Valeur du bonus (augmentation de lucidité)
    private float value;
    
    // Indique si le bonus est actif
    private boolean active = true;
    
    // Effet de pulsation
    private float pulsePhase = 0.0f;
    
    /**
     * Constructeur
     * @param x Position X initiale
     * @param y Position Y initiale
     * @param value Valeur du bonus (augmentation de lucidité)
     */
    public Bonus(float x, float y, float value) {
        this.x = x;
        this.y = y;
        this.value = value;
        
        // Initialiser la couleur du bonus
        paint = new Paint();
        paint.setAntiAlias(true);
        
        // Couleur en fonction de la valeur
        if (value > 0.5f) {
            paint.setColor(Color.GREEN); // Bonus majeur
        } else if (value > 0.3f) {
            paint.setColor(Color.YELLOW); // Bonus moyen
        } else {
            paint.setColor(Color.CYAN); // Bonus mineur
        }
    }
    
    /**
     * Dessine le bonus sur le canvas
     * @param canvas Canvas sur lequel dessiner
     */
    public void draw(Canvas canvas) {
        if (!active) return;
        
        // Mettre à jour l'effet de pulsation
        pulsePhase += 0.05f;
        float pulseFactor = 1.0f + 0.2f * (float) Math.sin(pulsePhase);
        
        // Dessiner le cercle du bonus
        canvas.drawCircle(x, y, radius * pulseFactor, paint);
        
        // Dessiner un contour blanc
        Paint strokePaint = new Paint(paint);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(Color.WHITE);
        strokePaint.setStrokeWidth(2.0f);
        canvas.drawCircle(x, y, radius * pulseFactor, strokePaint);
    }
    
    /**
     * Vérifie si la balle a collecté le bonus
     * @param ballX Position X de la balle
     * @param ballY Position Y de la balle
     * @param ballRadius Rayon de la balle
     * @return true si le bonus a été collecté, false sinon
     */
    public boolean checkCollision(float ballX, float ballY, float ballRadius) {
        if (!active) return false;
        
        // Calculer la distance entre le centre du bonus et le centre de la balle
        float distanceX = x - ballX;
        float distanceY = y - ballY;
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        
        // Vérifier si la distance est inférieure à la somme des rayons
        float minDistance = radius + ballRadius;
        return distanceSquared < minDistance * minDistance;
    }
    
    /**
     * Collecte le bonus
     * @return La valeur du bonus
     */
    public float collect() {
        active = false;
        return value;
    }
    
    /**
     * Vérifie si le bonus est actif
     * @return true si le bonus est actif, false sinon
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Obtient la position X du bonus
     * @return Position X
     */
    public float getX() {
        return x;
    }
    
    /**
     * Obtient la position Y du bonus
     * @return Position Y
     */
    public float getY() {
        return y;
    }
    
    /**
     * Définit la position du bonus
     * @param x Nouvelle position X
     * @param y Nouvelle position Y
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Désactive le bonus sans le collecter
     */
    public void deactivate() {
        active = false;
    }
} 