package com.example.gameapp;

/**
 * Classe qui gère les collisions entre une balle et un labyrinthe représenté par une grille
 */
public class MazeCollisionHandler {
    private int[][] mazeGrid;      // La grille du labyrinthe (1=mur, 0=passage)
    private float cellSize;        // Taille d'une cellule en pixels
    private int rows, cols;        // Dimensions de la grille
    
    /**
     * Constructeur du gestionnaire de collisions
     * @param mazeGrid La grille du labyrinthe (1=mur, 0=passage)
     * @param cellSize Taille d'une cellule en pixels
     */
    public MazeCollisionHandler(int[][] mazeGrid, float cellSize) {
        this.mazeGrid = mazeGrid;
        this.cellSize = cellSize;
        this.rows = mazeGrid.length;
        this.cols = mazeGrid[0].length;
    }
    
    /**
     * Met à jour la grille du labyrinthe (à utiliser si celle-ci change)
     * @param mazeGrid La nouvelle grille du labyrinthe
     */
    public void updateMazeGrid(int[][] mazeGrid) {
        this.mazeGrid = mazeGrid;
        this.rows = mazeGrid.length;
        this.cols = mazeGrid[0].length;
    }
    
    /**
     * Vérifie si la balle est en collision avec un mur du labyrinthe
     * et calcule les détails de la collision
     * @param ballX Position X du centre de la balle
     * @param ballY Position Y du centre de la balle
     * @param ballRadius Rayon de la balle
     * @return Les informations de collision
     */
    public CollisionInfo checkCollision(float ballX, float ballY, float ballRadius) {
        // Convertir les coordonnées de la balle en indices de la grille
        int gridX = (int)(ballX / cellSize);
        int gridY = (int)(ballY / cellSize);
        
        // Variables pour stocker les informations de collision
        boolean hasCollided = false;
        float normalX = 0, normalY = 0;
        float minPenetration = Float.MAX_VALUE;
        
        // Vérifier les cellules voisines
        for (int y = Math.max(0, gridY - 1); y <= Math.min(rows - 1, gridY + 1); y++) {
            for (int x = Math.max(0, gridX - 1); x <= Math.min(cols - 1, gridX + 1); x++) {
                // Si c'est un mur
                if (y >= 0 && y < rows && x >= 0 && x < cols && mazeGrid[y][x] == 1) {
                    // Coordonnées du coin supérieur gauche du mur
                    float wallX = x * cellSize;
                    float wallY = y * cellSize;
                    
                    // Trouver le point du mur le plus proche du centre de la balle
                    float closestX = Math.max(wallX, Math.min(ballX, wallX + cellSize));
                    float closestY = Math.max(wallY, Math.min(ballY, wallY + cellSize));
                    
                    // Calculer la distance entre ce point et le centre de la balle
                    float distanceX = ballX - closestX;
                    float distanceY = ballY - closestY;
                    float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                    
                    // S'il y a collision
                    if (distanceSquared < ballRadius * ballRadius) {
                        hasCollided = true;
                        
                        // Calculer la profondeur de pénétration
                        float distance = (float) Math.sqrt(distanceSquared);
                        float penetration = ballRadius - distance;
                        
                        // Calculer la normale de collision (éviter division par zéro)
                        float nx = (distance > 0.0001f) ? distanceX / distance : 0;
                        float ny = (distance > 0.0001f) ? distanceY / distance : 0;
                        
                        // Si cette collision est plus significative (pénétration plus profonde)
                        if (penetration < minPenetration) {
                            minPenetration = penetration;
                            normalX = nx;
                            normalY = ny;
                        }
                    }
                }
            }
        }
        
        return new CollisionInfo(hasCollided, normalX, normalY, minPenetration);
    }
    
    /**
     * Calcule la nouvelle vitesse après une collision
     * @param collision Les informations de collision
     * @param velocity Tableau contenant les composantes de vitesse [vx, vy]
     * @param dampingFactor Facteur d'amortissement pour le rebond
     */
    public void resolveCollision(CollisionInfo collision, float[] velocity, float dampingFactor) {
        if (collision.hasCollided) {
            // Calculer la composante de la vitesse dans la direction de la normale
            float dotProduct = velocity[0] * collision.normalX + velocity[1] * collision.normalY;
            
            // Calculer la nouvelle vitesse (réflexion + amortissement)
            velocity[0] = velocity[0] - 2 * dotProduct * collision.normalX * dampingFactor;
            velocity[1] = velocity[1] - 2 * dotProduct * collision.normalY * dampingFactor;
        }
    }
    
    /**
     * Classe pour stocker les informations de collision
     */
    public static class CollisionInfo {
        public boolean hasCollided;   // Indique s'il y a eu collision
        public float normalX, normalY; // Vecteur normal à la surface de collision
        public float penetration;     // Profondeur de pénétration
        
        public CollisionInfo(boolean hasCollided, float normalX, float normalY, float penetration) {
            this.hasCollided = hasCollided;
            this.normalX = normalX;
            this.normalY = normalY;
            this.penetration = penetration;
        }
    }
} 