package com.example.gameapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Gestionnaire de bonus qui gère la génération et la collecte des bonus
 */
public class BonusManager {
    // Liste des bonus actifs
    private List<Bonus> bonusList = new ArrayList<>();
    
    // Générateur de nombres aléatoires
    private Random random = new Random();
    
    // Probabilité de génération d'un bonus à chaque frame (1/1000)
    private static final float BONUS_SPAWN_PROBABILITY = 0.501f;
    
    // Délai minimum entre deux générations de bonus (en frames)
    private static final int MIN_SPAWN_DELAY = 120; // Environ 2 secondes à 60 FPS
    
    // Nombre maximum de bonus actifs simultanément
    private static final int MAX_ACTIVE_BONUSES = 4;
    
    // Compteur de frames depuis la dernière génération
    private int framesSinceLastSpawn = 0;
    
    // Dimensions de l'écran
    private int screenWidth;
    private int screenHeight;
    
    // Position de la balle
    private float ballX;
    private float ballY;
    
    // Grille du labyrinthe
    private int[][] mazeGrid;
    private float cellSize;
    private float mazeOffsetX = 0;
    private float mazeOffsetY = 0;
    
    // Image du cachet pour les bonus
    private Bitmap cachetImage;
    private Context context;
    
    /**
     * Constructeur
     * @param screenWidth Largeur de l'écran
     * @param screenHeight Hauteur de l'écran
     */
    public BonusManager(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
    
    /**
     * Définit le contexte pour charger les ressources
     * @param context Le contexte de l'application
     */
    public void setContext(Context context) {
        this.context = context;
        // Charger l'image du cachet
        if (cachetImage == null && context != null) {
            cachetImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.pill);
            // Redimensionner l'image si nécessaire
            if (cachetImage != null && cellSize > 0) {
                int bonusSize = (int)(cellSize * 0.8f);
                cachetImage = Bitmap.createScaledBitmap(cachetImage, bonusSize, bonusSize, true);
            }
        }
    }
    
    /**
     * Met à jour la position de la balle
     * @param x Position X de la balle
     * @param y Position Y de la balle
     */
    public void updateBallPosition(float x, float y) {
        this.ballX = x;
        this.ballY = y;
    }
    
    /**
     * Met à jour la grille du labyrinthe
     * @param mazeGrid Grille du labyrinthe
     * @param cellSize Taille d'une cellule en pixels
     */
    public void updateMazeGrid(int[][] mazeGrid, float cellSize) {
        this.mazeGrid = mazeGrid;
        this.cellSize = cellSize;
        
        // Redimensionner l'image du cachet si nécessaire
        if (cachetImage != null && context != null) {
            int bonusSize = (int)(cellSize * 0.8f);
            cachetImage = Bitmap.createScaledBitmap(cachetImage, bonusSize, bonusSize, true);
        }
    }
    
    /**
     * Définit les offsets du labyrinthe pour le centrage
     * @param offsetX Décalage horizontal
     * @param offsetY Décalage vertical
     */
    public void setMazeOffset(float offsetX, float offsetY) {
        this.mazeOffsetX = offsetX;
        this.mazeOffsetY = offsetY;
    }
    
    /**
     * Met à jour les dimensions de l'écran
     * @param width Largeur de l'écran
     * @param height Hauteur de l'écran
     */
    public void updateScreenDimensions(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }
    
    /**
     * Met à jour les bonus et en génère éventuellement de nouveaux
     * @param ballRadius Rayon de la balle pour la détection de collision
     * @return Valeur du bonus collecté, ou 0 si aucun bonus n'a été collecté
     */
    public float update(float ballRadius) {
        float collectedValue = 0;
        
        // Incrémenter le compteur de frames
        framesSinceLastSpawn++;
        
        // Vérifier les collisions et supprimer les bonus collectés
        Iterator<Bonus> iterator = bonusList.iterator();
        while (iterator.hasNext()) {
            Bonus bonus = iterator.next();
            if (bonus.checkCollision(ballX, ballY, ballRadius)) {
                collectedValue += bonus.collect();
                iterator.remove();
            }
        }
        
        // Éventuellement générer un nouveau bonus si on n'a pas atteint la limite
        if (bonusList.size() < MAX_ACTIVE_BONUSES && 
            framesSinceLastSpawn > MIN_SPAWN_DELAY && 
            random.nextFloat() < BONUS_SPAWN_PROBABILITY) {
            spawnBonus();
            framesSinceLastSpawn = 0;
        }
        
        return collectedValue;
    }
    
    /**
     * Génère un nouveau bonus à une position aléatoire valide
     */
    private void spawnBonus() {
        // Ne pas générer si on a déjà atteint le maximum
        if (bonusList.size() >= MAX_ACTIVE_BONUSES || mazeGrid == null || cellSize <= 0) return;
        
        // Valeur aléatoire du bonus entre 0.1 et 0.6
        float value = 0.1f + random.nextFloat() * 0.5f;
        
        // Position du bonus
        float bonusX, bonusY;
        int gridX, gridY;
        
        // Nombre maximal de tentatives pour trouver une position valide
        int maxAttempts = 50;
        int attempts = 0;
        
        do {
            // Générer des coordonnées aléatoires dans le labyrinthe
            gridX = random.nextInt(mazeGrid[0].length);
            gridY = random.nextInt(mazeGrid.length);
            
            // Convertir en coordonnées de pixel
            bonusX = mazeOffsetX + (gridX + 0.5f) * cellSize;
            bonusY = mazeOffsetY + (gridY + 0.5f) * cellSize;
            
            // Calculer la distance au carré avec la balle
            float dx = bonusX - ballX;
            float dy = bonusY - ballY;
            float distanceSquared = dx * dx + dy * dy;
            
            // Vérifier si la position est valide (passage, pas trop proche, pas trop loin)
            boolean isFreeCell = (gridY >= 0 && gridY < mazeGrid.length && 
                                 gridX >= 0 && gridX < mazeGrid[0].length && 
                                 mazeGrid[gridY][gridX] == 0);
            
            float minDistanceSquared = (cellSize * 3) * (cellSize * 3); // Au moins 3 cellules de distance
            float maxDistanceSquared = (cellSize * 10) * (cellSize * 10); // Au plus 10 cellules de distance
            
            if (isFreeCell && distanceSquared > minDistanceSquared && distanceSquared < maxDistanceSquared) {
                // Position valide, créer le bonus
                Bonus bonus = new Bonus(bonusX, bonusY, value);
                // Si l'image du cachet est disponible, définir la taille du bonus en fonction
                if (cachetImage != null) {
                    bonus.setImage(cachetImage);
                }
                bonusList.add(bonus);
                return;
            }
            
            attempts++;
        } while (attempts < maxAttempts);
    }
    
    /**
     * Dessine tous les bonus actifs
     * @param canvas Canvas sur lequel dessiner
     */
    public void draw(Canvas canvas) {
        for (Bonus bonus : bonusList) {
            bonus.draw(canvas);
        }
    }
    
    /**
     * Fait pivoter tous les bonus en utilisant les coordonnées normalisées
     * @param rotations Nombre de rotations de 90° (sens horaire)
     * @param mazeWidth Largeur totale du labyrinthe en pixels
     * @param mazeHeight Hauteur totale du labyrinthe en pixels
     */
    public void rotateBonus(int rotations, float mazeWidth, float mazeHeight) {
        // Normaliser le nombre de rotations (0-3)
        rotations = rotations % 4;
        
        // Pas de rotation nécessaire
        if (rotations == 0) {
            return;
        }
        
        // Pour chaque bonus, calculer sa nouvelle position
        for (Bonus bonus : bonusList) {
            float oldX = bonus.getX();
            float oldY = bonus.getY();
            
            // Position normalisée (0-1) par rapport à l'ensemble du labyrinthe
            float normalizedX = oldX / mazeWidth;
            float normalizedY = oldY / mazeHeight;
            
            // Calculer la nouvelle position normalisée après rotation
            float newNormalizedX = normalizedX;
            float newNormalizedY = normalizedY;
            
            for (int i = 0; i < rotations; i++) {
                // Rotation de 90° dans le sens horaire pour des coordonnées normalisées
                float tempX = newNormalizedX;
                newNormalizedX = newNormalizedY;
                newNormalizedY = 1.0f - tempX;
            }
            
            // Convertir les coordonnées normalisées en coordonnées réelles
            float newX = newNormalizedX * mazeWidth;
            float newY = newNormalizedY * mazeHeight;
            
            // Mettre à jour la position du bonus
            bonus.setPosition(newX, newY);
            
            // Vérifier si le bonus est maintenant dans un mur
            int gridX = (int)(newX / cellSize);
            int gridY = (int)(newY / cellSize);
            
            if (gridX < 0 || gridX >= mazeGrid[0].length || 
                gridY < 0 || gridY >= mazeGrid.length || 
                mazeGrid[gridY][gridX] == 1) {
                // Le bonus se retrouve dans un mur, le désactiver
                bonus.deactivate();
            }
        }
        
        // Nettoyer les bonus désactivés
        Iterator<Bonus> iterator = bonusList.iterator();
        while (iterator.hasNext()) {
            Bonus bonus = iterator.next();
            if (!bonus.isActive()) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Fait pivoter tous les bonus actifs autour du centre du labyrinthe
     * @param centerX Coordonnée X du centre du labyrinthe
     * @param centerY Coordonnée Y du centre du labyrinthe
     * @param antiClockwise Si vrai, la rotation est dans le sens anti-horaire, sinon dans le sens horaire
     */
    public void rotateAroundCenter(float centerX, float centerY, boolean antiClockwise) {
        // Pour chaque bonus, calculer sa nouvelle position
        for (Bonus bonus : bonusList) {
            float oldX = bonus.getX();
            float oldY = bonus.getY();
            
            // Calculer la différence par rapport au centre
            float dx = oldX - centerX;
            float dy = oldY - centerY;
            
            // Rotation autour du centre
            float newX, newY;
            if (antiClockwise) {
                // Rotation de 90° dans le sens anti-horaire
                newX = centerX + dy;
                newY = centerY - dx;
            } else {
                // Rotation de 90° dans le sens horaire
                newX = centerX - dy;
                newY = centerY + dx;
            }
            
            // Mettre à jour la position du bonus
            bonus.setPosition(newX, newY);
            
            // Vérifier si le bonus est maintenant dans un mur
            int gridX = (int)((newX - mazeOffsetX) / cellSize);
            int gridY = (int)((newY - mazeOffsetY) / cellSize);
            
            if (gridX < 0 || gridX >= mazeGrid[0].length || 
                gridY < 0 || gridY >= mazeGrid.length || 
                mazeGrid[gridY][gridX] == 1) {
                // Le bonus se retrouve dans un mur, le désactiver
                bonus.deactivate();
            }
        }
        
        // Nettoyer les bonus désactivés
        Iterator<Bonus> iterator = bonusList.iterator();
        while (iterator.hasNext()) {
            Bonus bonus = iterator.next();
            if (!bonus.isActive()) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Fait pivoter tous les bonus actifs autour du centre du labyrinthe dans le sens horaire
     * Cette surcharge est fournie pour la compatibilité avec le code existant
     * @param centerX Coordonnée X du centre du labyrinthe
     * @param centerY Coordonnée Y du centre du labyrinthe
     */
    public void rotateAroundCenter(float centerX, float centerY) {
        rotateAroundCenter(centerX, centerY, false); // Rotation dans le sens horaire par défaut
    }
    
    /**
     * Fait pivoter tous les bonus actifs autour du centre du labyrinthe, plusieurs fois
     * @param centerX Coordonnée X du centre du labyrinthe
     * @param centerY Coordonnée Y du centre du labyrinthe
     * @param antiClockwise Si vrai, la rotation est dans le sens anti-horaire, sinon dans le sens horaire
     * @param rotations Nombre de rotations à effectuer
     */
    public void rotateAroundCenterMultiple(float centerX, float centerY, boolean antiClockwise, int rotations) {
        for (int i = 0; i < rotations; i++) {
            rotateAroundCenter(centerX, centerY, antiClockwise);
        }
    }
    
    /**
     * Régénère tous les bonus actuels à de nouvelles positions valides
     * Utile après un changement drastique du labyrinthe (configuration "folle")
     */
    public void regenerateBonuses() {
        // Sauvegarder le nombre actuel de bonus
        int bonusCount = Math.min(bonusList.size(), MAX_ACTIVE_BONUSES);
        
        // Supprimer tous les bonus existants
        bonusList.clear();
        
        // Générer de nouveaux bonus, mais limité au maximum autorisé
        for (int i = 0; i < bonusCount; i++) {
            spawnBonus();
        }
        
        // Si aucun bonus n'a été généré (peut arriver si les conditions ne sont pas réunies),
        // essayer d'en générer un quand même
        if (bonusList.isEmpty() && bonusCount > 0) {
            framesSinceLastSpawn = MIN_SPAWN_DELAY + 1;
        }
    }
}

/**
 * Classe représentant un bonus qui peut être collecté par le joueur
 */
class Bonus {
    private float x, y;         // Position du bonus
    private float size = 20;    // Taille du bonus (diamètre)
    private float value;        // Valeur du bonus (entre 0.0 et 1.0)
    private boolean active = true; // Si le bonus est actif
    private Paint paint;        // Pinceau pour dessiner le bonus
    private Bitmap image;       // Image pour représenter le bonus (cachet)
    
    /**
     * Constructeur
     * @param x Position X du bonus
     * @param y Position Y du bonus
     * @param value Valeur du bonus (entre 0.0 et 1.0)
     */
    public Bonus(float x, float y, float value) {
        this.x = x;
        this.y = y;
        this.value = value;
        
        // Initialiser le pinceau
        paint = new Paint();
        paint.setARGB(255, 255, 255, 0); // Jaune opaque
        paint.setAntiAlias(true);
    }
    
    /**
     * Définit l'image à utiliser pour ce bonus
     * @param image Bitmap de l'image
     */
    public void setImage(Bitmap image) {
        this.image = image;
        if (image != null) {
            // Ajuster la taille en fonction de l'image
            this.size = Math.max(image.getWidth(), image.getHeight());
        }
    }
    
    /**
     * Dessine le bonus sur le canvas
     * @param canvas Canvas sur lequel dessiner
     */
    public void draw(Canvas canvas) {
        if (!active) return;
        
        if (image != null) {
            // Dessiner l'image au lieu du cercle
            canvas.drawBitmap(image, x - image.getWidth()/2, y - image.getHeight()/2, null);
        } else {
            // Fallback : dessiner un cercle jaune
            canvas.drawCircle(x, y, size/2, paint);
        }
    }
    
    /**
     * Vérifie si la balle est en collision avec ce bonus
     * @param ballX Position X de la balle
     * @param ballY Position Y de la balle
     * @param ballRadius Rayon de la balle
     * @return Vrai si la balle entre en collision avec le bonus
     */
    public boolean checkCollision(float ballX, float ballY, float ballRadius) {
        if (!active) return false;
        
        // Calculer la distance au carré entre les centres
        float dx = ballX - x;
        float dy = ballY - y;
        float distanceSquared = dx * dx + dy * dy;
        
        // Calculer le rayon de collision (somme des rayons)
        float collisionRadius = ballRadius + size/2;
        
        // S'il y a collision
        return distanceSquared < collisionRadius * collisionRadius;
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
     * Désactive le bonus (par exemple s'il se retrouve dans un mur)
     */
    public void deactivate() {
        active = false;
    }
    
    /**
     * Vérifie si le bonus est actif
     * @return Vrai si le bonus est actif
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Récupère la position X du bonus
     * @return Position X
     */
    public float getX() {
        return x;
    }
    
    /**
     * Récupère la position Y du bonus
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
} 