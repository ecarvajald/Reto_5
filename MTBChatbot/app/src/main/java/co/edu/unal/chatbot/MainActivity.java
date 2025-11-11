package co.edu.unal.chatbot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private MessageAdapter adapter;
    private AIClient ai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("MTB Chat Bot");
        }

        Log.d("GROQ_DEBUG", "KEY=" + BuildConfig.GROQ_API_KEY);

        RecyclerView rv = findViewById(R.id.recyclerChat);
        EditText et = findViewById(R.id.editMessage);
        Button btn = findViewById(R.id.btnSend);

        adapter = new MessageAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        ai = new AIClient();

        btn.setOnClickListener(v -> {
            String prompt = et.getText().toString().trim();
            if (prompt.isEmpty()) return;

            // Agregar mensaje del usuario
            adapter.addMessage(new Message(prompt, true));
            et.setText("");

            // Enviar a Mixtral (Groq)
            ai.sendMessage(prompt, new AIClient.Callback() {
                @Override
                public void onSuccess(String text) {
                    runOnUiThread(() -> {
                        adapter.addMessage(new Message(text, false));
                        rv.scrollToPosition(adapter.getItemCount() - 1);
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        adapter.addMessage(new Message("Error: " + error, false));
                        rv.scrollToPosition(adapter.getItemCount() - 1);
                    });
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.menu_info) {
            mostrarDialogoInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void mostrarDialogoInfo() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sobre MTB Chatbot")
                .setMessage(
                        "MTB Chatbot es un asistente especializado en Mycobacterium tuberculosis. " +
                                "Explica microbiología, genómica comparativa, resistencia a fármacos, " +
                                "mutaciones, WGS, SNPs e infecciones mixtas. Está pensado para apoyar " +
                                "a estudiantes y profesionales en temas de TB."
                )
                .setPositiveButton("Cerrar", null)
                .show();
    }

}
