package com.example.gameapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.example.gameapp.utils.MazeGenerator;
import com.example.gameapp.utils.MazePainter;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread thread;
    private int y;
    private int x=0;

    private MazePainter mazePainter;


    private Context context;

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
    
    // Gestionnaire de lucidité pour les effets LSD
    private LucidityManager lucidityManager;
    private Matrix waveMatrix;
    
    // Variables pour la dernière position de l'accéléromètre
    private float lastAccelerometerX = 0;
    private float lastAccelerometerY = 0;
    
    // Variables pour détecter la balle bloquée
    private float lastCircleX = 0;
    private float lastCircleY = 0;
    private int stuckCounter = 0;
    private static final int MAX_STUCK_FRAMES = 15; // Nombre de frames avant de considérer la balle comme bloquée

    public GameView(Context context, int valeur_y, float initialLucidity) {
        super(context);
        this.context = context;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        this.y = valeur_y;

        this.mazePainter = new MazePainter(context, 10, 10);

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
        
        // Initialiser le gestionnaire de lucidité avec la valeur sauvegardée
        lucidityManager = new LucidityManager(initialLucidity);
        waveMatrix = new Matrix();
    }
    
    /**
     * Crée un labyrinthe de test simple
     */
    private void createTestMaze() {
        // Un petit labyrinthe de test 5x5
        // 1=mur, 0=passage
        mazeGrid = this.mazePainter.getLabyrinth();
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
            
            // Sauvegarder l'état actuel du canvas
            canvas.save();
            
            // Dessiner le labyrinthe si disponible
            if (mazeGrid != null && cellSize > 0) {
                for (int y = 0; y < mazeGrid.length; y++) {
                    for (int x = 0; x < mazeGrid[0].length; x++) {
                        if (mazeGrid[y][x] == 1) {
                            // Calculer la position du mur
                            float wallX = x * cellSize;
                            float wallY = y * cellSize;
                            
                            // Appliquer l'effet d'ondulation si nécessaire
                            canvas.save();
                            lucidityManager.applyWaveEffect(canvas, waveMatrix, wallX, wallY, cellSize, cellSize);
                            
                            // Dessiner un mur
                            canvas.drawRect(
                                wallX,
                                wallY,
                                wallX + cellSize,
                                wallY + cellSize,
                                wallPaint
                            );
                            
                            // Restaurer l'état du canvas
                            canvas.restore();
                        }
                    }
                }
            }
            
            // Restaurer l'état du canvas
            canvas.restore();
            
            // Dessiner le carré rouge existant
            Paint squarePaint = new Paint();
            squarePaint.setColor(Color.rgb(250, 0, 0));
            canvas.drawRect(x, y, x + 100, y + 100, squarePaint);

            // Dessiner le cercle
            canvas.drawCircle(circleX, circleY, circleRadius, circlePaint);
            
            // Dessiner la jauge de lucidité
            lucidityManager.drawLucidityGauge(canvas, screenWidth, screenHeight);
        }

        mazePainter.drawLabyrinth(canvas);
    }
    
    public void update() {
        // Mise à jour du gestionnaire de lucidité
        lucidityManager.update();
        
        // Mise à jour du carré existant
        x = (x + 1) % 300;
        
        // Vérifier si la balle est bloquée
        if (Math.abs(circleX - lastCircleX) < 0.1f && Math.abs(circleY - lastCircleY) < 0.1f) {
            stuckCounter++;
            if (stuckCounter > MAX_STUCK_FRAMES) {
                // La balle est bloquée, appliquer une petite force aléatoire pour la débloquer
                velocityX += (Math.random() - 0.5f) * 1.5f;
                velocityY += (Math.random() - 0.5f) * 1.5f;
                stuckCounter = 0; // Réinitialiser le compteur
            }
        } else {
            stuckCounter = 0; // Réinitialiser le compteur si la balle bouge
        }
        
        // Sauvegarder la position actuelle
        lastCircleX = circleX;
        lastCircleY = circleY;
        
        // Sauvegarder la position actuelle pour revenir en arrière en cas de collision
        float prevX = circleX;
        float prevY = circleY;
        
        // Mise à jour de la position de la balle en fonction de sa vitesse
        circleX += velocityX;
        circleY += velocityY;
        
        // Empêcher des valeurs trop petites qui pourraient causer un gel
        if (Math.abs(velocityX) < 0.01f && Math.abs(velocityY) < 0.01f) {
            // Si la balle est presque immobile mais l'accéléromètre indique un mouvement,
            // donner une petite impulsion pour éviter le gel
            if (Math.abs(lastAccelerometerX) > 0.1f || Math.abs(lastAccelerometerY) > 0.1f) {
                velocityX = lastAccelerometerX * 0.2f;  
                velocityY = lastAccelerometerY * 0.2f;
            }
        }
        
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
        // Sauvegarder les valeurs de l'accéléromètre pour référence
        lastAccelerometerX = accelerometerX;
        lastAccelerometerY = accelerometerY;
        
        // Appliquer les effets LSD aux contrôles
        float[] modifiedControls = lucidityManager.applyControlEffects(accelerometerX, accelerometerY);
        
        // Ajout de l'accélération aux vitesses
        velocityX += modifiedControls[0] * gravity;
        velocityY += modifiedControls[1] * gravity;
        
        // Limiter les vitesses pour éviter les comportements extrêmes
        float maxSpeed = 20.0f;
        if (velocityX > maxSpeed) velocityX = maxSpeed;
        if (velocityX < -maxSpeed) velocityX = -maxSpeed;
        if (velocityY > maxSpeed) velocityY = maxSpeed;
        if (velocityY < -maxSpeed) velocityY = -maxSpeed;
    }
    
    /**
     * Augmente la lucidité (pour les bonus)
     * @param amount Montant à ajouter (entre 0.0 et 1.0)
     */
    public void increaseLucidity(float amount) {
        if (lucidityManager != null) {
            lucidityManager.increaseLucidity(amount);
        }
    }
    
    /**
     * Récupère la valeur actuelle de lucidité
     * @return Valeur entre 0.0 et 1.0
     */
    public float getLucidityValue() {
        if (lucidityManager != null) {
            return lucidityManager.getLucidity();
        }
        return 1.0f;
    }
}
