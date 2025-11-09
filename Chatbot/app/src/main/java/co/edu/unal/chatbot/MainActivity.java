package co.edu.unal.chatbot;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private MessageAdapter adapter;
    private AIClient ai;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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

            // Agregar el mensaje del usuario
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
}
