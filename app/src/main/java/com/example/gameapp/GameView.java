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
    
    // Variables pour le labyrinthe
    private int[][] mazeGrid;  // La grille du labyrinthe (1=mur, 0=passage)
    private float cellSize;    // Taille d'une cellule en pixels
    private MazeCollisionHandler collisionHandler;
    private Paint wallPaint;   // Pinceau pour dessiner les murs
    
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
        
        // Initialisation du pinceau pour les murs du labyrinthe
        wallPaint = new Paint();
        wallPaint.setColor(Color.BLACK);
        
        // Exemple de labyrinthe simple (pour les tests)
        // Sera remplacé par le labyrinthe fourni par votre collègue
        createTestMaze();
    }
    
    /**
     * Crée un labyrinthe de test simple
     */
    private void createTestMaze() {
        // Un petit labyrinthe de test 5x5
        // 1=mur, 0=passage
        mazeGrid = new int[][] {
            {1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1},
            {1, 0, 1, 0, 1},
            {1, 0, 0, 0, 1},
            {1, 1, 1, 1, 1}
        };
    }
    
    /**
     * Méthode pour définir ou mettre à jour le labyrinthe
     * @param grid La grille du labyrinthe (1=mur, 0=passage)
     * @param cellSize Taille d'une cellule en pixels
     */
    public void setMaze(int[][] grid, float cellSize) {
        this.mazeGrid = grid;
        this.cellSize = cellSize;
        
        if (collisionHandler == null) {
            collisionHandler = new MazeCollisionHandler(grid, cellSize);
        } else {
            collisionHandler.updateMazeGrid(grid);
        }
        
        // Placer la balle à une position valide dans le labyrinthe
        placeBallInMaze();
    }
    
    /**
     * Place la balle à une position valide dans le labyrinthe
     */
    private void placeBallInMaze() {
        // Recherche simple d'une position valide (première cellule vide trouvée)
        if (mazeGrid != null) {
            for (int y = 0; y < mazeGrid.length; y++) {
                for (int x = 0; x < mazeGrid[0].length; x++) {
                    if (mazeGrid[y][x] == 0) {
                        // Position trouvée, placer la balle au centre de cette cellule
                        circleX = (x + 0.5f) * cellSize;
                        circleY = (y + 0.5f) * cellSize;
                        return;
                    }
                }
            }
        }
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
        
        // Si le labyrinthe n'est pas encore initialisé, le faire maintenant
        if (mazeGrid != null && collisionHandler == null) {
            // Calculer une taille de cellule appropriée
            float mazeCellSize = Math.min(
                screenWidth / mazeGrid[0].length,
                screenHeight / mazeGrid.length
            );
            setMaze(mazeGrid, mazeCellSize);
        }
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
            
            // Dessiner le labyrinthe si disponible
            if (mazeGrid != null && cellSize > 0) {
                for (int y = 0; y < mazeGrid.length; y++) {
                    for (int x = 0; x < mazeGrid[0].length; x++) {
                        if (mazeGrid[y][x] == 1) {
                            // Dessiner un mur
                            canvas.drawRect(
                                x * cellSize,
                                y * cellSize,
                                (x + 1) * cellSize,
                                (y + 1) * cellSize,
                                wallPaint
                            );
                        }
                    }
                }
            }
            
            // Dessiner le carré rouge existant
            Paint squarePaint = new Paint();
            squarePaint.setColor(Color.rgb(250, 0, 0));
            canvas.drawRect(x, y, x + 100, y + 100, squarePaint);

            // Dessiner le cercle
            canvas.drawCircle(circleX, circleY, circleRadius, circlePaint);
        }
    }
    
    public void update() {
        // Mise à jour du carré existant
        x = (x + 1) % 300;
        
        // Sauvegarder la position actuelle pour revenir en arrière en cas de collision
        float prevX = circleX;
        float prevY = circleY;
        
        // Mise à jour de la position de la balle en fonction de sa vitesse
        circleX += velocityX;
        circleY += velocityY;
        
        // Vérifier les collisions avec le labyrinthe
        if (collisionHandler != null) {
            // Vérifier les collisions
            MazeCollisionHandler.CollisionInfo collision = 
                collisionHandler.checkCollision(circleX, circleY, circleRadius);
            
            if (collision.hasCollided) {
                // Repositionner la balle hors du mur
                circleX = prevX + collision.normalX * collision.penetration;
                circleY = prevY + collision.normalY * collision.penetration;
                
                // Calculer le rebond
                float[] velocity = {velocityX, velocityY};
                collisionHandler.resolveCollision(collision, velocity, damping);
                velocityX = velocity[0];
                velocityY = velocity[1];
            }
        }
        
        // Vérifier la collision avec le carré rouge existant
        checkSquareCollision();
        
        // Collision avec les bords de l'écran
        if (circleX < circleRadius) {
            circleX = circleRadius;
            velocityX = -velocityX * damping;
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
    
    /**
     * Vérifie et gère la collision avec le carré rouge existant
     */
    private void checkSquareCollision() {
        // Trouver le point du carré le plus proche du centre du cercle
        float closestX = Math.max(x, Math.min(circleX, x + 100));
        float closestY = Math.max(y, Math.min(circleY, y + 100));
        
        // Calculer la distance entre ce point et le centre du cercle
        float distanceX = circleX - closestX;
        float distanceY = circleY - closestY;
        float distanceSquared = distanceX * distanceX + distanceY * distanceY;
        
        // S'il y a collision
        if (distanceSquared < circleRadius * circleRadius) {
            // Calculer la distance et la normale
            float distance = (float) Math.sqrt(distanceSquared);
            float normalX = (distance > 0.0001f) ? distanceX / distance : 0;
            float normalY = (distance > 0.0001f) ? distanceY / distance : 0;
            
            // Repositionner la balle hors du carré
            float penetration = circleRadius - distance;
            circleX += normalX * penetration;
            circleY += normalY * penetration;
            
            // Calculer le rebond
            float dotProduct = velocityX * normalX + velocityY * normalY;
            velocityX = velocityX - 2 * dotProduct * normalX * damping;
            velocityY = velocityY - 2 * dotProduct * normalY * damping;
        }
    }
    
    // Méthode appelée par MainActivity pour mettre à jour la position de la balle
    public void updateBallPosition(float accelerometerX, float accelerometerY) {
        // Ajout de l'accélération aux vitesses
        velocityX += accelerometerX * gravity;
        velocityY += accelerometerY * gravity;
    }
}
