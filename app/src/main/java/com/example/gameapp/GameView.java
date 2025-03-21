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
    private float circleRadius = 12; // Rayon du cercle en pixels
    private Paint circlePaint; // Pinceau pour dessiner le cercle
    
    // Variables pour la physique de la balle
    private float velocityX = 0;
    private float velocityY = 0;
    private float gravity = 0.05f; // Force de la gravité simulée
    private float damping = 0.95f; // Facteur de friction/amortissement
    
    // Dimensions de l'écran
    private int screenWidth;
    private int screenHeight;
    
    // Variables pour le labyrinthe
    private int[][] mazeGrid;  // La grille du labyrinthe (1=mur, 0=passage)
    private float cellSize;    // Taille d'une cellule en pixels
    private float mazeOffsetX; // Décalage X pour centrer le labyrinthe
    private float mazeOffsetY; // Décalage Y pour centrer le labyrinthe
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


    // Gestionnaire de bonus
    private BonusManager bonusManager;

    // Variables pour les différentes configurations de labyrinthe
    private int[][][] mazeConfigurations;
    private int currentMazeIndex = 0;
    private float lastLucidityThreshold = 1.0f;

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

        // Initialiser le gestionnaire de bonus
        bonusManager = new BonusManager(0, 0); // Les dimensions seront mises à jour dans surfaceChanged
    }
    
    /**
     * Crée un labyrinthe de test simple
     */
    private void createTestMaze() {
        // Un petit labyrinthe de test 5x5
        // 1=mur, 0=passage
        mazeGrid = this.mazePainter.getLabyrinth();
        // Initialiser les configurations (4 rotations différentes)
        mazeConfigurations = new int[4][][];

        // Configuration 1 (100-76% de lucidité) - labyrinthe original
        mazeConfigurations[0] = mazeGrid;

        // Configuration 2 (75-51% de lucidité) - rotation 90°
        mazeConfigurations[1] = rotateMaze(mazeGrid, 1);

        // Configuration 3 (50-26% de lucidité) - rotation 180°
        mazeConfigurations[2] = rotateMaze(mazeGrid, 2);

        // Configuration 4 (25-0% de lucidité) - rotation 270°
        mazeConfigurations[3] = rotateMaze(mazeGrid, 3);

        // Initialiser le labyrinthe avec la première configuration
        mazeGrid = mazeConfigurations[0];
    }
    /**
     * Fait pivoter la grille du labyrinthe
     * @param original La grille originale
     * @param rotations Le nombre de rotations de 90° à effectuer (1-3)
     * @return La grille pivotée
     */
    private int[][] rotateMaze(int[][] original, int rotations) {
        int rows = original.length;
        int cols = original[0].length;
        int[][] result = new int[rows][cols];

        // Normaliser le nombre de rotations (0-3)
        rotations = rotations % 4;

        // Pas de rotation nécessaire
        if (rotations == 0) {
            return original;
        }

        // Effectuer la rotation en fonction du nombre spécifié
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                switch (rotations) {
                    case 1: // 90° dans le sens horaire
                        result[j][rows - 1 - i] = original[i][j];
                        break;
                    case 2: // 180°
                        result[rows - 1 - i][cols - 1 - j] = original[i][j];
                        break;
                    case 3: // 270° dans le sens horaire (ou 90° dans le sens anti-horaire)
                        result[cols - 1 - j][i] = original[i][j];
                        break;
                }
            }
        }

        return result;
    }

    /**
     * Méthode pour définir ou mettre à jour le labyrinthe
     * @param grid La grille du labyrinthe (1=mur, 0=passage)
     * @param cellSize Taille d'une cellule en pixels
     */
    public void setMaze(int[][] grid, float cellSize) {
        this.mazeGrid = grid;
        this.cellSize = cellSize;
        
        // Calculer les offsets pour centrer le labyrinthe
        if (grid != null) {
            float mazeWidth = grid[0].length * cellSize;
            float mazeHeight = grid.length * cellSize;
            this.mazeOffsetX = (screenWidth - mazeWidth) / 2;
            this.mazeOffsetY = (screenHeight - mazeHeight) / 2;
        }
        
        if (collisionHandler == null) {
            collisionHandler = new MazeCollisionHandler(grid, cellSize);
            collisionHandler.setMazeOffset(mazeOffsetX, mazeOffsetY);
        } else {
            collisionHandler.updateMazeGrid(grid);
            collisionHandler.setMazeOffset(mazeOffsetX, mazeOffsetY);
        }
        
        // Mettre à jour la grille pour le gestionnaire de bonus
        if (bonusManager != null) {
            bonusManager.updateMazeGrid(grid, cellSize);
            bonusManager.setMazeOffset(mazeOffsetX, mazeOffsetY);
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
                        circleX = mazeOffsetX + (x + 0.5f) * cellSize;
                        circleY = mazeOffsetY + (y + 0.5f) * cellSize;
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
        
        // Mettre à jour les dimensions de l'écran pour le gestionnaire de bonus
        if (bonusManager != null) {
            bonusManager.updateScreenDimensions(width, height);
        }

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
                            float wallX = mazeOffsetX + x * cellSize;
                            float wallY = mazeOffsetY + y * cellSize;
                            
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

            // Dessiner les bonus
            if (bonusManager != null) {
                bonusManager.draw(canvas);
            }

            // Dessiner le cercle
            canvas.drawCircle(circleX, circleY, circleRadius, circlePaint);
            
            // Dessiner la jauge de lucidité
            lucidityManager.drawLucidityGauge(canvas, screenWidth, screenHeight);
        }
    }
    
    public void update() {
        // Mise à jour du gestionnaire de lucidité
        lucidityManager.update();
        if(lucidityManager.getLucidity() == 0.0){
            gameOver();
        }
        
        // Mise à jour de la configuration du labyrinthe en fonction de la lucidité
        updateMazeConfiguration();

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

        // Mise à jour du gestionnaire de bonus et collecte des bonus
        if (bonusManager != null) {
            bonusManager.updateBallPosition(circleX, circleY);
            float bonusValue = bonusManager.update(circleRadius);

            // Si un bonus a été collecté, augmenter la lucidité
            if (bonusValue > 0) {
                increaseLucidity(bonusValue);
            }
        }
    }

    private void gameOver() {
            // Arrêter le jeu
            thread.setRunning(false);

            // Lancer l'activité Victory
            android.content.Intent intent = new android.content.Intent(context, GameOverActivity.class);

            // Passer des données supplémentaires si nécessaire
            // intent.putExtra("score", someScore);

            context.startActivity(intent);

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

    /**
     * Met à jour la configuration du labyrinthe en fonction du niveau de lucidité
     */
    private void updateMazeConfiguration() {
        if (lucidityManager == null || mazeConfigurations == null) return;

        float lucidity = lucidityManager.getLucidity();
        int newMazeIndex;

        // Déterminer l'index de la configuration en fonction du niveau de lucidité
        if (lucidity > 0.75f) {
            newMazeIndex = 0;
        } else if (lucidity > 0.5f) {
            newMazeIndex = 1;
        } else if (lucidity > 0.25f) {
            newMazeIndex = 2;
        } else {
            newMazeIndex = 3;
        }

        // Si la configuration doit changer
        if (newMazeIndex != currentMazeIndex) {
            // Dimensions du labyrinthe et centre
            int rows = mazeGrid.length;
            int cols = mazeGrid[0].length;
            float centerX = mazeOffsetX + cols * cellSize / 2;
            float centerY = mazeOffsetY + rows * cellSize / 2;
            
            // Détecter la direction du changement et calculer le nombre de rotations
            boolean lucidityIncreasing = newMazeIndex < currentMazeIndex;
            int rotationsNeeded = Math.abs(newMazeIndex - currentMazeIndex);
            
            // Mettre à jour l'index et la grille
            currentMazeIndex = newMazeIndex;
            mazeGrid = mazeConfigurations[currentMazeIndex];
            
            // Mettre à jour le gestionnaire de collisions
            if (collisionHandler != null) {
                collisionHandler.updateMazeGrid(mazeGrid);
            }
            
            // Effectuer les rotations nécessaires
            float newX = circleX;
            float newY = circleY;
            
            for (int i = 0; i < rotationsNeeded; i++) {
                // Rotation de la balle autour du centre du labyrinthe
                float dx = newX - centerX;
                float dy = newY - centerY;
                
                if (lucidityIncreasing) {
                    // Rotation anti-horaire (car la lucidité augmente)
                    float tempX = newX;
                    newX = centerX + dy;
                    newY = centerY - dx;
                } else {
                    // Rotation horaire (car la lucidité diminue)
                    float tempX = newX;
                    newX = centerX - dy;
                    newY = centerY + dx;
                }
            }
            
            // Mettre à jour la position de la balle
            circleX = newX;
            circleY = newY;
            
            // Mettre à jour la grille pour le gestionnaire de bonus
            if (bonusManager != null) {
                bonusManager.updateMazeGrid(mazeGrid, cellSize);
                bonusManager.rotateAroundCenterMultiple(centerX, centerY, lucidityIncreasing, rotationsNeeded);
            }
            
            // Vérifier que la nouvelle position est sûre (pas dans un mur)
            if (!isPositionSafe(circleX, circleY)) {
                findNearestSafePosition();
            }
        }
    }

    /**
     * Trouve la position sûre la plus proche
     */
    private void findNearestSafePosition() {
        // Convertir en coordonnées de grille
        int gridX = (int)((circleX - mazeOffsetX) / cellSize);
        int gridY = (int)((circleY - mazeOffsetY) / cellSize);
        
        // Limiter les indices dans les bornes de la grille
        gridX = Math.max(0, Math.min(gridX, mazeGrid[0].length - 1));
        gridY = Math.max(0, Math.min(gridY, mazeGrid.length - 1));
        
        // Si la position actuelle est déjà sûre, ne rien faire
        if (mazeGrid[gridY][gridX] == 0) {
            circleX = mazeOffsetX + (gridX + 0.5f) * cellSize;
            circleY = mazeOffsetY + (gridY + 0.5f) * cellSize;
            return;
        }
        
        // Rechercher la position sûre la plus proche
        int maxDistance = Math.max(mazeGrid.length, mazeGrid[0].length);
        for (int distance = 1; distance < maxDistance; distance++) {
            // Vérifier toutes les cellules à cette distance
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance; dy <= distance; dy++) {
                    // Ne vérifier que les cellules sur le périmètre du carré
                    if (Math.abs(dx) == distance || Math.abs(dy) == distance) {
                        int checkX = gridX + dx;
                        int checkY = gridY + dy;
                        
                        // Vérifier que la cellule est dans les limites
                        if (checkX >= 0 && checkX < mazeGrid[0].length && 
                            checkY >= 0 && checkY < mazeGrid.length) {
                            // Si la cellule est un passage, l'utiliser
                            if (mazeGrid[checkY][checkX] == 0) {
                                circleX = mazeOffsetX + (checkX + 0.5f) * cellSize;
                                circleY = mazeOffsetY + (checkY + 0.5f) * cellSize;
                                return;
                            }
                        }
                    }
                }
            }
        }
        
        // Si aucune position sûre n'est trouvée, placer la balle dans la première cellule libre
        placeBallInMaze();
    }

    /**
     * Vérifie si une position est sûre (pas dans un mur)
     */
    private boolean isPositionSafe(float x, float y) {
        // Convertir les coordonnées en indices de grille
        int gridX = (int)((x - mazeOffsetX) / cellSize);
        int gridY = (int)((y - mazeOffsetY) / cellSize);
        
        // Vérifier si les indices sont valides et si la cellule est un passage
        return gridX >= 0 && gridX < mazeGrid[0].length && 
               gridY >= 0 && gridY < mazeGrid.length && 
               mazeGrid[gridY][gridX] == 0;
    }
}
