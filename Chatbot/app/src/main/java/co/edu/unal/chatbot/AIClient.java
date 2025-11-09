package co.edu.unal.chatbot;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.util.*;

public class AIClient {

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface Callback {
        void onSuccess(String text);
        void onError(String error);
    }

    public void sendMessage(String userMessage, Callback callback) {

        // Endpoint Groq
        String url = "https://api.groq.com/openai/v1/chat/completions";

        // Cuerpo de la solicitud
        Map<String, Object> json = new HashMap<>();
        // Modelo
        json.put("model", "llama-3.1-8b-instant");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", "Eres un asistente útil en español."));
        messages.add(Map.of("role", "user", "content", userMessage));
        json.put("messages", messages);

        RequestBody body = RequestBody.create(
                gson.toJson(json),
                MediaType.parse("application/json")
        );

        // Autenticación con tu GROQ_API_KEY desde BuildConfig
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                .post(body)
                .build();

        http.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                // Leer SIEMPRE el body (haya o no error)
                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    // Muestra el detalle del 4xx/5xx para depurar rápido
                    callback.onError("Error HTTP: " + response.code() + "\n" + responseBody);
                    return;
                }

                try {
                    JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonArray choices = root.getAsJsonArray("choices");

                    String text = choices.get(0)
                            .getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content")
                            .getAsString();

                    callback.onSuccess(text);

                } catch (Exception ex) {
                    callback.onError("Error parseando respuesta: " + ex.getMessage());
                }
            }
        });
    }
}
