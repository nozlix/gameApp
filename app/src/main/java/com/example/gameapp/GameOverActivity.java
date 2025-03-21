package com.example.gameapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;

public class GameOverActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);



        ConstraintLayout layout = findViewById(R.id.game_over);
        AnimationDrawable animationDrawable = (AnimationDrawable) layout.getBackground();
        animationDrawable.setEnterFadeDuration(500);
        animationDrawable.setExitFadeDuration(500);
        animationDrawable.start();

        ImageView imageGameOver= findViewById(R.id.imageGameOver);

        // Charger et appliquer l'animation
        Animation moveTitleAnimation = AnimationUtils.loadAnimation(this, R.anim.move_title);
        imageGameOver.startAnimation(moveTitleAnimation);
    }

    public void startMainActivity(View view){
        Intent intent = new Intent(GameOverActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
