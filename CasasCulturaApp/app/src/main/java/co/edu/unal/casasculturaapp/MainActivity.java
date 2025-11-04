package co.edu.unal.casasculturaapp;

import androidx.appcompat.app.AppCompatActivity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    private Spinner spMunicipio, spCategoria;
    private Button btnFiltrar;
    private ListView listView;

    private final ArrayList<String> listaCasas = new ArrayList<>();
    private final ArrayList<String> listaMunicipios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referencias UI
        spMunicipio = findViewById(R.id.spMunicipio);
        spCategoria = findViewById(R.id.spCategoria);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        listView = findViewById(R.id.listViewCasas);

        // Cargar lista de categorías manualmente
        cargarCategorias();

        // Cargar municipios desde el webservice
        cargarMunicipios();

        // Acción del botón
        btnFiltrar.setOnClickListener(v -> {
            String municipio = spMunicipio.getSelectedItem().toString();
            String categoria = spCategoria.getSelectedItem().toString();
            cargarDatos(municipio, categoria);
        });
    }

    /**
     * 🔹 Carga las categorías culturales fijas
     */
    private void cargarCategorias() {
        ArrayList<String> listaCategorias = new ArrayList<>();
        listaCategorias.add("TODAS");
        listaCategorias.add("Cinematografía");
        listaCategorias.add("Patrimonio");
        listaCategorias.add("Teatro");
        listaCategorias.add("Danza");
        listaCategorias.add("Música");
        listaCategorias.add("Artes plásticas");
        listaCategorias.add("Literatura");
        listaCategorias.add("Museo");
        listaCategorias.add("Biblioteca");

        ArrayAdapter<String> adapterCat = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaCategorias);
        adapterCat.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategoria.setAdapter(adapterCat);
    }

    /**
     * 🔹 Carga los municipios únicos desde la API
     */
    private void cargarMunicipios() {
        new Thread(() -> {
            try {
                Uri uri = Uri.parse("https://www.datos.gov.co/resource/jpr9-4ew8.json")
                        .buildUpon()
                        .appendQueryParameter("$limit", "5000")
                        .build();

                URL url = new URL(uri.toString());
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                con.disconnect();

                JSONArray jsonArray = new JSONArray(sb.toString());
                HashSet<String> munSet = new HashSet<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject casa = jsonArray.getJSONObject(i);
                    String mun = casa.optString("municipio", "").trim();
                    if (!mun.isEmpty()) munSet.add(mun);
                }

                listaMunicipios.clear();
                listaMunicipios.add("TODOS");
                listaMunicipios.addAll(munSet);

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapterMun = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, listaMunicipios);
                    adapterMun.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spMunicipio.setAdapter(adapterMun);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error al cargar municipios: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void cargarDatos(String municipio, String categoria) {
        new Thread(() -> {
            HttpURLConnection con = null;
            BufferedReader reader = null;
            try {
                // 1) Construye $where SOLO para municipio (case-insensitive)
                String whereClause = "";
                if (!"TODOS".equals(municipio)) {
                    // Escapar comillas simples por seguridad en SoQL
                    String muniSafe = municipio.toUpperCase().replace("'", "''");
                    whereClause = "upper(municipio) like '%" + muniSafe + "%'";
                }

                // 2) Armar URL con Uri.Builder (codifica todo)
                Uri.Builder builder = Uri.parse("https://www.datos.gov.co/resource/jpr9-4ew8.json").buildUpon();
                if (!whereClause.isEmpty()) {
                    builder.appendQueryParameter("$where", whereClause);
                }
                // Para la categoría usamos $q (full-text, evita errores con tildes y %)
                if (!"TODAS".equals(categoria)) {
                    builder.appendQueryParameter("$q", categoria);
                }
                builder.appendQueryParameter("$limit", "5000");

                String finalUrl = builder.build().toString();
                System.out.println("URL de consulta: " + finalUrl);

                // 3) Conexión y manejo de errores HTTP
                URL url = new URL(finalUrl);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setConnectTimeout(10000);
                con.setReadTimeout(10000);

                int code = con.getResponseCode();
                if (code >= 400) {
                    // Leer cuerpo de error para ver el mensaje que da Socrata
                    BufferedReader err = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                    StringBuilder esb = new StringBuilder();
                    String el;
                    while ((el = err.readLine()) != null) esb.append(el);
                    err.close();
                    String errorBody = esb.toString();
                    System.out.println("HTTP " + code + " - Error body: " + errorBody);

                    String finalError = "HTTP " + code + " - " + errorBody;
                    runOnUiThread(() -> Toast.makeText(this, "Error en consulta: " + finalError, Toast.LENGTH_LONG).show());
                    return;
                }

                // 4) Leer respuesta OK
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONArray jsonArray = new JSONArray(sb.toString());
                ArrayList<String> temp = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject casa = jsonArray.getJSONObject(i);

                    // Nombre con fallbacks
                    String nombre = casa.optString("nombre_de_la_entidad", "");
                    if (nombre.isEmpty()) nombre = casa.optString("raz_n_social", "");
                    if (nombre.isEmpty()) nombre = casa.optString("direcci_n", "Sin dirección");

                    String muni = casa.optString("municipio", "Sin municipio");
                    String dep = casa.optString("departamento", "Sin departamento");

                    temp.add(nombre + " - " + muni + " (" + dep + ")");
                }

                runOnUiThread(() -> {
                    listaCasas.clear();
                    listaCasas.addAll(temp);
                    listView.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_list_item_1, listaCasas));
                    Toast.makeText(this, "Resultados: " + listaCasas.size(), Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Excepción: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                try {
                    if (reader != null) reader.close();
                } catch (Exception ignored) {
                }
                if (con != null) con.disconnect();
            }
        }).start();
    }
}
