package com.example.gameapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Récupérer les préférences partagées
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);

        // Récupérer le nombre de lancements précédents (par défaut 0)
        int nb_lancements = sharedPref.getInt("nb_lancements", 0);

        // Incrémenter le compteur de lancements
        nb_lancements++;

        // Calculer la nouvelle valeur de y
        int valeur_y = (100 * nb_lancements) % 400;

        // Sauvegarder les nouvelles valeurs
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("nb_lancements", nb_lancements);
        editor.putInt("valeur_y", valeur_y);
        editor.apply();

        // Afficher GameView avec la valeur de y mise à jour
        setContentView(new GameView(this, valeur_y));


    }
}