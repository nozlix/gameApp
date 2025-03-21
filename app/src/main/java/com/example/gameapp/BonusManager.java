package com.example.gameapp;

import android.graphics.Canvas;

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
        
        // Éventuellement générer un nouveau bonus
        if (framesSinceLastSpawn > MIN_SPAWN_DELAY && random.nextFloat() < BONUS_SPAWN_PROBABILITY) {
            spawnBonus();
            framesSinceLastSpawn = 0;
        }
        
        return collectedValue;
    }
    
    /**
     * Génère un nouveau bonus à une position aléatoire valide
     */
    private void spawnBonus() {
        if (mazeGrid == null || cellSize <= 0) return;
        
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
            bonusX = (gridX + 0.5f) * cellSize;
            bonusY = (gridY + 0.5f) * cellSize;
            
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
                bonusList.add(new Bonus(bonusX, bonusY, value));
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
} 