package co.edu.unal.tictactoe;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TempActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("¡TempActivity abierta! ✅");
        tv.setPadding(40, 80, 40, 80);
        setContentView(tv);
    }
}
