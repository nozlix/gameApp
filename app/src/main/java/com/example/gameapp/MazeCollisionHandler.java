package com.example.gameapp;

/**
 * Classe qui gère les collisions entre une balle et un labyrinthe représenté par une grille
 */
public class MazeCollisionHandler {
    private int[][] mazeGrid;      // La grille du labyrinthe (1=mur, 0=passage)
    private float cellSize;        // Taille d'une cellule en pixels
    private float mazeOffsetX = 0;
    private float mazeOffsetY = 0;
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
     * Définit les offsets du labyrinthe pour le centrage
     * @param offsetX Décalage horizontal
     * @param offsetY Décalage vertical
     */
    public void setMazeOffset(float offsetX, float offsetY) {
        this.mazeOffsetX = offsetX;
        this.mazeOffsetY = offsetY;
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
        CollisionInfo info = new CollisionInfo();
        
        // Convertir les coordonnées de la balle en indices de la grille
        int gridX = (int)((ballX - mazeOffsetX) / cellSize);
        int gridY = (int)((ballY - mazeOffsetY) / cellSize);
        
        // Vérifier les 9 cellules autour de la position actuelle de la balle
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int checkX = gridX + dx;
                int checkY = gridY + dy;
                
                // Vérifier que les indices sont valides
                if (checkX >= 0 && checkX < cols && 
                    checkY >= 0 && checkY < rows) {
                    
                    // Si la cellule est un mur, vérifier la collision
                    if (mazeGrid[checkY][checkX] == 1) {
                        // Coordonnées du coin supérieur gauche du mur
                        float wallLeft = mazeOffsetX + checkX * cellSize;
                        float wallTop = mazeOffsetY + checkY * cellSize;
                        
                        // Coordonnées du coin inférieur droit du mur
                        float wallRight = wallLeft + cellSize;
                        float wallBottom = wallTop + cellSize;
                        
                        // Trouver le point du mur le plus proche du centre de la balle
                        float closestX = Math.max(wallLeft, Math.min(ballX, wallRight));
                        float closestY = Math.max(wallTop, Math.min(ballY, wallBottom));
                        
                        // Calculer la distance entre ce point et le centre de la balle
                        float distanceX = ballX - closestX;
                        float distanceY = ballY - closestY;
                        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
                        
                        // S'il y a collision
                        if (distanceSquared < ballRadius * ballRadius) {
                            // Calcul de la normale et de la pénétration
                            float distance = (float) Math.sqrt(distanceSquared);
                            info.hasCollided = true;
                            
                            // Éviter la division par zéro
                            info.normalX = (distance > 0.0001f) ? distanceX / distance : 0;
                            info.normalY = (distance > 0.0001f) ? distanceY / distance : 0;
                            
                            info.penetration = ballRadius - distance;
                            info.wallX = closestX;
                            info.wallY = closestY;
                            
                            return info; // Retourner dès la première collision
                        }
                    }
                }
            }
        }
        
        return info; // Pas de collision
    }
    
    /**
     * Calcule la nouvelle vitesse après une collision
     * @param collision Les informations de collision
     * @param velocity Tableau contenant les composantes de vitesse [vx, vy]
     * @param dampingFactor Facteur d'amortissement pour le rebond
     */
    public void resolveCollision(CollisionInfo collision, float[] velocity, float dampingFactor) {
        if (!collision.hasCollided) return;
        
        // Calculer le produit scalaire entre la vélocité et la normale
        float dotProduct = velocity[0] * collision.normalX + velocity[1] * collision.normalY;
        
        // Si la balle s'éloigne déjà du mur, ne pas appliquer de rebond
        if (dotProduct > 0) return;
        
        // Calculer la vélocité de rebond (réflexion)
        velocity[0] = velocity[0] - 2 * dotProduct * collision.normalX * dampingFactor;
        velocity[1] = velocity[1] - 2 * dotProduct * collision.normalY * dampingFactor;
    }
    
    /**
     * Classe pour stocker les informations de collision
     */
    public static class CollisionInfo {
        public boolean hasCollided = false;
        public float normalX = 0;
        public float normalY = 0;
        public float penetration = 0;
        public float wallX = 0;
        public float wallY = 0;
    }
} 