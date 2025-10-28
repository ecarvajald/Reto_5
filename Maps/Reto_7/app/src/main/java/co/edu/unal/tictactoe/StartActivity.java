package co.edu.unal.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class StartActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        MaterialButton btnVsAndroid = findViewById(R.id.btn_vs_android);
        MaterialButton btnOnline    = findViewById(R.id.btn_online);

        btnVsAndroid.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class))
        );

        btnOnline.setOnClickListener(v ->
                startActivity(new Intent(this, GameListActivity.class))
        );
    }
}
