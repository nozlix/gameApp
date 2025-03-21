package com.example.gameapp.utils;

import android.content.Context;

public class MazePainter {
    public int[][] getLabyrinth() {
        return labyrinth;
    }

    private int[][] labyrinth;


    public MazePainter(Context context, int width, int height) {
        MazeGenerator generator = new MazeGenerator(width, height);
        labyrinth = generator.getMaze();
    }

}
