package com.example.gameapp;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
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

    // Variable pour sauvegarder la lucidité entre les changements d'orientation
    private float savedLucidity = 1.0f;
    private static final String KEY_LUCIDITY = "lucidity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Récupérer la lucidité sauvegardée si disponible
        if (savedInstanceState != null) {
            savedLucidity = savedInstanceState.getFloat(KEY_LUCIDITY, 1.0f);
        }

        // Initialisation du gestionnaire de capteurs
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Créer et configurer GameView avec la lucidité sauvegardée
        gameView = new GameView(this, savedLucidity);
        setContentView(gameView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Sauvegarder la lucidité actuelle
        if (gameView != null) {
            savedLucidity = gameView.getLucidityValue();
            outState.putFloat(KEY_LUCIDITY, savedLucidity);
        }
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

        // Sauvegarder la lucidité actuelle
        if (gameView != null) {
            savedLucidity = gameView.getLucidityValue();
        }
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
    
    /**
     * Sauvegarde la lucidité actuelle
     * @param lucidity Valeur de lucidité à sauvegarder
     */
    public void saveCurrentLucidity(float lucidity) {
        // Sauvegarder la lucidité
        this.savedLucidity = lucidity;
        
        // Sauvegarder dans les préférences partagées pour la persistance
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_LUCIDITY, lucidity);
        editor.apply();
    }
}