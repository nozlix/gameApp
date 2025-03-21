package com.example.gameapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private GameView gameView;
    private float accelerometerX, accelerometerY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Initialisation du gestionnaire de capteurs
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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

        // Créer et configurer GameView avec la valeur de y mise à jour
        gameView = new GameView(this, valeur_y);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Enregistrer l'écouteur du capteur lorsque l'application reprend
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Désactiver l'écouteur du capteur lorsque l'application est en pause
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Obtenir la rotation actuelle de l'écran
            int rotation = getWindowManager().getDefaultDisplay().getRotation();

            // Adapter les valeurs de l'accéléromètre en fonction de l'orientation
            switch (rotation) {
                case Surface.ROTATION_0: // Portrait, orientation normale
                    // Les valeurs standards
                    accelerometerX = -event.values[0];
                    accelerometerY = event.values[1];
                    break;
                case Surface.ROTATION_90: // Paysage, rotation à gauche
                    // Inverser les axes et le signe de Y
                    accelerometerX = event.values[1];
                    accelerometerY = event.values[0];
                    break;
                case Surface.ROTATION_180: // Portrait inversé
                    // Inverser les deux signes
                    accelerometerX = event.values[0];
                    accelerometerY = -event.values[1];
                    break;
                case Surface.ROTATION_270: // Paysage, rotation à droite
                    // Inverser les axes et le signe de X
                    accelerometerX = -event.values[1];
                    accelerometerY = -event.values[0];
                    break;
            }

            // Mettre à jour les données dans GameView
            if (gameView != null) {
                gameView.updateBallPosition(accelerometerX, accelerometerY);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Non utilisé pour l'instant, mais doit être implémenté
    }
}