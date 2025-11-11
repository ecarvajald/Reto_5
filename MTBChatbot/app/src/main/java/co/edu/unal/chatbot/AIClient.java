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

        // URL para Groq API
        String url = "https://api.groq.com/openai/v1/chat/completions";

        // Parámetros del modelo
        Map<String, Object> json = new HashMap<>();
        json.put("model", "llama-3.1-8b-instant");

        // Mensajes del chat
        List<Map<String, String>> messages = new ArrayList<>();

        // SYSTEM PROMPT ESPECIALIZADO (microbiología y MTB)
        messages.add(Map.of(
                "role", "system",
                "content",
                "Eres un asistente experto en microbiología, genómica comparativa y tuberculosis. " +
                        "Puedes explicar mutaciones, variantes, resistencia a fármacos, WGS, SNPs, " +
                        "infecciones mixtas, pipelines bioinformáticos y conceptos epidemiológicos. " +
                        "Utiliza un lenguaje técnico pero claro."
        ));

        //Mensaje del usuario
        messages.add(Map.of("role", "user", "content", userMessage));

        json.put("messages", messages);

        //Cuerpo JSON del request
        RequestBody body = RequestBody.create(
                gson.toJson(json),
                MediaType.parse("application/json")
        );

        // Request con API KEY de Groq
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
                .post(body)
                .build();

        //Enviar request
        http.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Error de red: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseBody = response.body().string(); // leer respuesta

                if (!response.isSuccessful()) {
                    callback.onError(
                            "Error HTTP: " + response.code() + "\n" +
                                    "Respuesta: " + responseBody
                    );
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
