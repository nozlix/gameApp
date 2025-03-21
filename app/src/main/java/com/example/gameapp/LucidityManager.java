package com.example.gameapp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Matrix;

/**
 * Gère la lucidité et tous les effets associés au mode LSD
 */
public class LucidityManager {
    // Valeur de lucidité (1.0 = totalement lucide, 0.0 = pas du tout lucide)
    private float lucidity = 1.0f;
    
    // Taux de décroissance de la lucidité par frame
    private static final float LUCIDITY_DECAY = 0.0004f;
    
    // Paramètres pour les effets
    private boolean invertXAxis = false;
    private boolean invertYAxis = false;
    private float controlRotation = 0.0f; // En degrés
    private static final float MAX_ROTATION = 180.0f;
    
    // Paramètres pour l'ondulation
    private float waveAmplitude = 0.0f;
    private float waveFrequency = 0.1f;
    private float wavePhase = 0.0f;
    
    // Peintres pour les effets visuels
    private Paint lucidityGaugePaint;
    private Paint lucidityGaugeBackgroundPaint;
    private Paint lucidityTextPaint;
    
    /**
     * Constructeur
     */
    public LucidityManager() {
        this(1.0f); // Appel au constructeur avec lucidité par défaut (100%)
    }
    
    /**
     * Constructeur avec lucidité initiale
     * @param initialLucidity Valeur initiale de la lucidité (entre 0.0f et 1.0f)
     */
    public LucidityManager(float initialLucidity) {
        // Initialiser la lucidité avec la valeur fournie
        lucidity = Math.max(0.0f, Math.min(1.0f, initialLucidity));
        
        // Initialiser les peintres
        lucidityGaugePaint = new Paint();
        lucidityGaugePaint.setColor(Color.GREEN);
        
        lucidityGaugeBackgroundPaint = new Paint();
        lucidityGaugeBackgroundPaint.setColor(Color.DKGRAY);
        
        lucidityTextPaint = new Paint();
        lucidityTextPaint.setColor(Color.BLACK);
        lucidityTextPaint.setTextSize(30);
        
        // Mettre à jour immédiatement les effets en fonction de la lucidité
        updateEffectsIntensity();
    }
    
    /**
     * Met à jour la lucidité et les effets associés
     */
    public void update() {
        // Diminuer la lucidité progressivement
        lucidity = Math.max(0.0f, lucidity - LUCIDITY_DECAY);
        
        // Mise à jour des effets en fonction de la lucidité
        updateEffectsIntensity();
        
        // Mise à jour de la phase de l'ondulation
        wavePhase += 0.05f;
    }
    
    /**
     * Met à jour l'intensité des effets en fonction du niveau de lucidité
     */
    private void updateEffectsIntensity() {
        // Établir une stratégie d'effets qui évite les annulations
        if (lucidity < 0.3f) {
            // À très faible lucidité: rotation pure sans inversion
            invertXAxis = false;
            invertYAxis = false;
            controlRotation = 180.0f; // Rotation complète
        } else if (lucidity < 0.6f) {
            // À lucidité moyenne-basse: inversion de Y et rotation partielle
            invertXAxis = false;
            invertYAxis = true;
            controlRotation = (0.6f - lucidity) * 2.0f * 90.0f; // Rotation de 0° à 90°
        } else if (lucidity < 0.8f) {
            // À lucidité moyenne-haute: inversion de X seulement
            invertXAxis = true;
            invertYAxis = false;
            controlRotation = 0.0f; // Pas de rotation
        } else {
            // À haute lucidité: aucun effet
            invertXAxis = false;
            invertYAxis = false;
            controlRotation = 0.0f;
        }
        
        // Amplitude de l'ondulation (augmente avec la baisse de lucidité)
        waveAmplitude = (1.0f - lucidity) * 15.0f;
        
        // Mettre à jour la couleur de la jauge en fonction de la lucidité
        if (lucidity > 0.7f) {
            lucidityGaugePaint.setColor(Color.GREEN);
        } else if (lucidity > 0.4f) {
            lucidityGaugePaint.setColor(Color.YELLOW);
        } else {
            lucidityGaugePaint.setColor(Color.RED);
        }
    }
    
    /**
     * Applique les effets aux contrôles
     * @param accelerometerX Valeur X de l'accéléromètre
     * @param accelerometerY Valeur Y de l'accéléromètre
     * @return Un tableau avec les valeurs modifiées [X, Y]
     */
    public float[] applyControlEffects(float accelerometerX, float accelerometerY) {
        // Inverser les axes si nécessaire
        if (invertXAxis) accelerometerX = -accelerometerX;
        if (invertYAxis) accelerometerY = -accelerometerY;
        
        // Appliquer la rotation des contrôles
        if (controlRotation != 0) {
            float radians = (float) Math.toRadians(controlRotation);
            float cos = (float) Math.cos(radians);
            float sin = (float) Math.sin(radians);
            
            float newX = accelerometerX * cos - accelerometerY * sin;
            float newY = accelerometerX * sin + accelerometerY * cos;
            
            accelerometerX = newX;
            accelerometerY = newY;
        }
        
        return new float[] {accelerometerX, accelerometerY};
    }
    
    /**
     * Applique une transformation au canvas pour l'ondulation du labyrinthe
     * @param canvas Canvas à transformer
     * @param matrix Matrice pour appliquer l'ondulation
     * @param x Coordonnée X
     * @param y Coordonnée Y
     * @param width Largeur de la cellule
     * @param height Hauteur de la cellule
     */
    public void applyWaveEffect(Canvas canvas, Matrix matrix, float x, float y, float width, float height) {
        if (waveAmplitude > 0) {
            // Réinitialiser la matrice
            matrix.reset();
            
            // Calculer l'ondulation
            float offsetX = (float) Math.sin((y + wavePhase) * waveFrequency) * waveAmplitude;
            
            // Appliquer la translation
            matrix.postTranslate(offsetX, 0);
            
            // Appliquer la matrice au canvas
            canvas.setMatrix(matrix);
        }
    }
    
    /**
     * Dessine la jauge de lucidité
     * @param canvas Canvas sur lequel dessiner
     * @param screenWidth Largeur de l'écran
     * @param screenHeight Hauteur de l'écran
     */
    public void drawLucidityGauge(Canvas canvas, int screenWidth, int screenHeight) {
        boolean isLandscape = screenWidth > screenHeight;
        
        if (isLandscape) {
            // Mode paysage : jauge verticale sur le côté gauche
            float gaugeWidth = 40;  // Un peu plus large pour meilleure visibilité
            float gaugeHeight = screenHeight * 0.6f;  // Légèrement plus courte
            float gaugeX = 20; // Marge à gauche
            float gaugeY = screenHeight * 0.2f;  // Centrer davantage
            
            // Dessiner le fond de la jauge
            canvas.drawRect(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + gaugeHeight, lucidityGaugeBackgroundPaint);
            
            // Dessiner la jauge de lucidité (de bas en haut)
            canvas.drawRect(
                gaugeX, 
                gaugeY + gaugeHeight * (1 - lucidity), 
                gaugeX + gaugeWidth, 
                gaugeY + gaugeHeight, 
                lucidityGaugePaint
            );
            
            // Augmenter la taille du texte pour le mode paysage
            float originalTextSize = lucidityTextPaint.getTextSize();
            lucidityTextPaint.setTextSize(36);
            
            // Dessiner le pourcentage au-dessus de la jauge
            String percentText = (int)(lucidity * 100) + "%";
            canvas.drawText(
                percentText,
                gaugeX + gaugeWidth / 2 - lucidityTextPaint.measureText(percentText) / 2,
                gaugeY - 15,
                lucidityTextPaint
            );
            
            // Restaurer la taille du texte
            lucidityTextPaint.setTextSize(originalTextSize);
        } else {
            // Mode portrait : jauge horizontale en bas
            float gaugeHeight = 30;
            float gaugeWidth = screenWidth * 0.8f;
            float gaugeX = screenWidth * 0.1f;
            float gaugeY = screenHeight - gaugeHeight - 20;
            
            // Dessiner le fond de la jauge
            canvas.drawRect(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + gaugeHeight, lucidityGaugeBackgroundPaint);
            
            // Dessiner la jauge de lucidité
            canvas.drawRect(gaugeX, gaugeY, gaugeX + gaugeWidth * lucidity, gaugeY + gaugeHeight, lucidityGaugePaint);
            
            // Dessiner le texte "Lucidité"
            canvas.drawText("Lucidité: " + (int)(lucidity * 100) + "%", gaugeX + 10, gaugeY + gaugeHeight - 5, lucidityTextPaint);
        }
    }
    
    /**
     * Renvoie le niveau actuel de lucidité
     * @return Valeur entre 0.0 et 1.0
     */
    public float getLucidity() {
        return lucidity;
    }
    
    /**
     * Augmente la lucidité (pour les bonus)
     * @param amount Montant à ajouter (entre 0.0 et 1.0)
     */
    public void increaseLucidity(float amount) {
        lucidity = Math.min(1.0f, lucidity + amount);
        updateEffectsIntensity();
    }
} 