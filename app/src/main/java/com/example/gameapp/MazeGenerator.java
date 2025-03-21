package com.example.gameapp;

import java.util.Random;

public class MazeGenerator {
    private int width, height;
    private int[][] maze;
    private final int WALL = 1, PATH = 0;
    private Random random = new Random();

    public MazeGenerator(int width, int height) {
        // Augmenter la taille pour avoir des allées larges
        this.width = width * 2 + 1;  // Largeur du labyrinthe
        this.height = height * 2 + 1; // Hauteur du labyrinthe
        maze = new int[this.height][this.width];

        // Initialiser le labyrinthe avec des murs partout
        for (int i = 0; i < this.height; i++) {
            for (int j = 0; j < this.width; j++) {
                maze[i][j] = WALL;
            }
        }

        // Générer le labyrinthe avec des allées plus larges
        generateMaze(1, 1);
        placeExit();
    }

    private void generateMaze(int x, int y) {
        maze[y][x] = PATH; // Marquer la cellule de départ comme un chemin
        int[] directions = {0, 1, 2, 3};  // Haut, Droite, Bas, Gauche
        shuffleArray(directions);

        // Définir les mouvements à 2 cellules de largeur pour générer des chemins larges
        int[] dx = {0, 2, 0, -2};  // Déplacer de 2 pour s'assurer d'avoir une largeur de 2
        int[] dy = {-2, 0, 2, 0};  // Déplacer de 2 pour s'assurer d'avoir une largeur de 2

        for (int i = 0; i < 4; i++) {
            int nx = x + dx[directions[i]];
            int ny = y + dy[directions[i]];

            if (nx > 0 && ny > 0 && nx < width - 1 && ny < height - 1 && maze[ny][nx] == WALL) {
                // Creuser un chemin large (2 cellules)
                maze[y + dy[directions[i]] / 2][x + dx[directions[i]] / 2] = PATH; // Une cellule au milieu pour un passage de 2 cellules
                maze[ny][nx] = PATH; // Marquer la nouvelle cellule comme un chemin
                generateMaze(nx, ny);  // Appel récursif pour continuer à creuser
            }
        }
    }

    private void placeExit() {
        // Placer la sortie en bas du labyrinthe
        for (int x = width - 2; x > 0; x--) {
            if (maze[height - 2][x] == PATH) {
                maze[height - 1][x] = PATH;  // Sortie
                return;
            }
        }
    }

    private void shuffleArray(int[] array) {
        // Mélanger les directions pour obtenir un labyrinthe aléatoire
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            int temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    public int[][] getMaze() {
        return maze;
    }
}
