package com.example.gameapp.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

public class MazePainter {
    public int[][] getLabyrinth() {
        return labyrinth;
    }

    private int[][] labyrinth;

    private int cellSize;
    private int startX;
    private int startY;


    private Context context;

    public MazePainter(Context context, int width, int height) {
        this.context = context;
        MazeGenerator generator = new MazeGenerator(width, height);
        labyrinth = generator.getMaze();
    }

    // Function to draw the labyrinth
//    public void drawLabyrinth(Canvas canvas) {
//        this.calculateLabyrinthDimensions();
//        if (labyrinth == null){
//            return;
//        }
//        Paint wallPaint = new Paint();
//        wallPaint.setColor(Color.BLACK);
//
//        Paint pathPaint = new Paint();
//        pathPaint.setColor(Color.GREEN);  // Blanc pour les chemins
//        pathPaint.setStyle(Paint.Style.FILL);
//
//        for (int i = 0; i < labyrinth.length; i++) {
//            for (int j = 0; j < labyrinth[i].length; j++) {
//                if (labyrinth[i][j] == 1) { // 1 represents a wall
//                    canvas.drawRect(startX + j * cellSize, startY + i * cellSize,
//                            startX + (j + 1) * cellSize, startY + (i + 1) * cellSize, wallPaint);
//                }
//            }
//        }
//    }
//
//    public void calculateLabyrinthDimensions() {
//        // Get the screen dimensions
//        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        Display display = wm.getDefaultDisplay();
//        Point size = new Point();
//        display.getSize(size);
//        int screenWidth = size.x;
//        int screenHeight = size.y;
//
//        // Calculate the maximum possible cell size based on the smaller dimension
//        int maxCellSize = Math.min(screenWidth / labyrinth[0].length, screenHeight / labyrinth.length);
//
//        // Choose a cell size that is a bit smaller than the maximum to add some padding
//        cellSize = (int) (maxCellSize * 1); // 80% of the maximum size
//
//        // Calculate the total width and height of the labyrinth
//        int labyrinthWidth = labyrinth[0].length * cellSize;
//        int labyrinthHeight = labyrinth.length * cellSize;
//
//        // Calculate the starting position to center the labyrinth
//        startX = (screenWidth - labyrinthWidth) / 2;
//        startY = (screenHeight - labyrinthHeight) / 2;
//    }
}
