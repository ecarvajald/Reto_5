package co.edu.unal.maps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.Locale;

import co.edu.unal.maps.R;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 1;
    private MapView map;
    private GeoPoint currentLocation;
    private Button btnPoi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().load(this,
                PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        btnPoi = findViewById(R.id.btnPoi);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        Spinner spinner = findViewById(R.id.spinnerPoiType);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.poi_categories,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);


        btnPoi.setOnClickListener(v -> {
            Toast.makeText(this, "Buscando lugares...", Toast.LENGTH_SHORT).show();
            String selected = spinner.getSelectedItem().toString();
            loadPOIs(selected);
        });



        checkPermissions();
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS);

        } else {
            enableLocation(); // solo si ya hay permiso
        }
    }


    @SuppressLint("MissingPermission")
    private void enableLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // Si no hay permiso, simplemente no sigue
        }

        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (loc != null) {
            currentLocation = new GeoPoint(loc.getLatitude(), loc.getLongitude());

            IMapController controller = map.getController();
            controller.setZoom(18.0);
            controller.setCenter(currentLocation);

            addMarker("Mi ubicación", currentLocation);
        } else {
            Toast.makeText(this, "Activa el GPS y abre la app en exteriores", Toast.LENGTH_SHORT).show();
        }
    }


    private void addMarker(String title, GeoPoint point) {
        try {
            Marker marker = new Marker(map);
            marker.setPosition(point);

            String address = getAddress(point.getLatitude(), point.getLongitude());
            marker.setTitle(title);
            marker.setSnippet(address); // Dirección en el popup

            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            // Si quieres que se abra de una vez el popup:
            // marker.showInfoWindow();

            map.getOverlays().add(marker);
            map.invalidate();
        } catch (Exception e) {
            Toast.makeText(this, "Error al agregar marcador: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void loadPOIs(String type) {

        Toast.makeText(this, "Cargando...", Toast.LENGTH_SHORT).show();

        if (currentLocation == null) {
            Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int radius = Integer.parseInt(prefs.getString("radio", "1")) * 1500;



        String lat = String.valueOf(currentLocation.getLatitude());
        String lon = String.valueOf(currentLocation.getLongitude());
        String amenityFilter = "";

        switch (type.trim().toLowerCase()) {

            case "hospitales":
                amenityFilter =
                        "node(around:" + radius + "," + lat + "," + lon + ")[amenity=hospital];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[amenity=clinic];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[amenity=doctors];";
                break;

            case "lugares turísticos":
                amenityFilter =
                        "node(around:" + radius + "," + lat + "," + lon + ")[tourism=attraction];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[tourism=museum];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[tourism=viewpoint];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[tourism=artwork];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[tourism=theme_park];";
                break;

            case "restaurantes":
                amenityFilter =
                        "node(around:" + radius + "," + lat + "," + lon + ")[amenity=restaurant];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[amenity=fast_food];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[amenity=cafe];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[amenity=bar];";
                break;

            case "gasolineras":
                amenityFilter =
                        "node(around:" + radius + "," + lat + "," + lon + ")[amenity=fuel];" +
                                "node(around:" + radius + "," + lat + "," + lon + ")[amenity=charging_station];";
                break;
        }


        String query =
                "[out:json][timeout:25];(" + amenityFilter + ");out;";

        String url;
        try {
            url = "https://overpass-api.de/api/interpreter?data=" +
                    java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            Toast.makeText(this, "Error preparando consulta: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }


        map.getOverlayManager().clear();
        addMarker("Mi ubicación", currentLocation);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    JSONArray elements = response.optJSONArray("elements");
                    if (elements == null || elements.length() == 0) {
                        Toast.makeText(this, "No se encontraron puntos de interés cercanos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int total = elements.length();
                    Toast.makeText(this, "Se encontraron " + total + " lugares cercanos", Toast.LENGTH_LONG).show();

                    // Mostrar máximo 40 para evitar sobrecarga
                    int limit = Math.min(elements.length(), 40);
                    for (int i = 0; i < limit; i++) {
                        double la = elements.optJSONObject(i).optDouble("lat");
                        double lo = elements.optJSONObject(i).optDouble("lon");

                        // 🔹 Nombre base según la categoría seleccionada
                        String baseName;
                        switch (type.toLowerCase()) {
                            case "hospitales":
                                baseName = "Hospital cercano";
                                break;
                            case "restaurantes":
                                baseName = "Restaurante cercano";
                                break;
                            case "lugares turísticos":
                                baseName = "Lugar turístico cercano";
                                break;
                            case "gasolineras":
                                baseName = "Gasolinera cercana";
                                break;
                            default:
                                baseName = "Lugar cercano";
                                break;
                        }

                        // 🔹 Nombre final del marcador
                        String name = baseName;

                        // Si tiene etiqueta "tags" con nombre, usamos ese nombre
                        if (elements.optJSONObject(i).has("tags") &&
                                elements.optJSONObject(i).optJSONObject("tags").has("name")) {

                            name = elements.optJSONObject(i)
                                    .optJSONObject("tags")
                                    .optString("name", baseName);

                        } else if (elements.optJSONObject(i).has("tags")) {
                            // Si no tiene "name", tratamos de deducir el tipo
                            String tipo = elements.optJSONObject(i)
                                    .optJSONObject("tags")
                                    .optString("amenity", baseName);
                            tipo = tipo.substring(0, 1).toUpperCase() + tipo.substring(1);
                            name = tipo;
                        }

                        addMarker(name, new GeoPoint(la, lo));
                    }


                    map.invalidate();
                },
                error -> Toast.makeText(this, "Error al consultar puntos cercanos", Toast.LENGTH_SHORT).show()
        );

        request.setShouldCache(false);
        Volley.newRequestQueue(this).add(request);

    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                enableLocation();

            } else {
                Toast.makeText(this,
                        "Se requiere el permiso de ubicación para continuar",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    private String getAddress(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.getAddressLine(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Dirección desconocida";
    }


}
