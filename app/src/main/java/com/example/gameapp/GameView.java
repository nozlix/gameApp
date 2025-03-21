package com.example.gameapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private GameThread thread;
    private int y;
    private int x=0;
    private int[][] labyrinth;
    private int cellSize;
    private int startX;
    private int startY;

    private Context context;

    public GameView(Context context, int valeur_y) {
        super(context);
        this.context = context;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        this.y = valeur_y;
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
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
        }

        initializeLabyrinth();
        drawLabyrinth(canvas);
    }
    public void update() {
        x = (x + 1) % 300;
    }

    // Function to initialize the labyrinth (example)
    private void initializeLabyrinth() {
        // Example: a simple 15x15 labyrinth
        labyrinth = new int[][]{
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1},
                {1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
                {1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1},
                {1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 1, 1, 1},
                {1, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1},
                {1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
        };
    }

    // Function to draw the labyrinth
    public void drawLabyrinth(Canvas canvas) {
        this.initializeLabyrinth();
        this.calculateLabyrinthDimensions(canvas.getWidth(), canvas.getHeight());
        if (labyrinth == null) return;

        Paint wallPaint = new Paint();
        wallPaint.setColor(Color.BLACK);

        for (int i = 0; i < labyrinth.length; i++) {
            for (int j = 0; j < labyrinth[i].length; j++) {
                if (labyrinth[i][j] == 1) { // 1 represents a wall
                    canvas.drawRect(startX + j * cellSize, startY + i * cellSize,
                            startX + (j + 1) * cellSize, startY + (i + 1) * cellSize, wallPaint);
                }
            }
        }
    }

    private void calculateLabyrinthDimensions(int surfaceWidth, int surfaceHeight) {
        // Get the screen dimensions
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;

        // Calculate the maximum possible cell size based on the smaller dimension
        int maxCellSize = Math.min(screenWidth / labyrinth[0].length, screenHeight / labyrinth.length);

        // Choose a cell size that is a bit smaller than the maximum to add some padding
        cellSize = (int) (maxCellSize * 0.8); // 80% of the maximum size

        // Calculate the total width and height of the labyrinth
        int labyrinthWidth = labyrinth[0].length * cellSize;
        int labyrinthHeight = labyrinth.length * cellSize;

        // Calculate the starting position to center the labyrinth
        startX = (screenWidth - labyrinthWidth) / 2;
        startY = (screenHeight - labyrinthHeight) / 2;
    }

}
