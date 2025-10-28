package co.edu.unal.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameListActivity extends AppCompatActivity {

    private ListView listView;
    private MaterialButton btnCreate;

    private final List<NetGame> games = new ArrayList<>();
    private final List<String> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private FirebaseAuth auth;
    private DatabaseReference gamesRef;
    private ChildEventListener gamesListener;

    private String uid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_list);

        listView  = findViewById(R.id.list_games);
        btnCreate = findViewById(R.id.btn_create_game);

        // 1) Auth anónima
        auth = FirebaseAuth.getInstance();
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) {
            auth.signInAnonymously().addOnSuccessListener(r -> {
                uid = r.getUser().getUid();
                initDbAndUi();
            }).addOnFailureListener(e -> btnCreate.setEnabled(false));
        } else {
            uid = u.getUid();
            initDbAndUi();
        }
    }

    private void initDbAndUi() {
        gamesRef = FirebaseDatabase.getInstance().getReference("games");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, rows);
        listView.setAdapter(adapter);

        // 2) Escuchar lista
        gamesListener = new ChildEventListener() {
            @Override public void onChildAdded(@NonNull DataSnapshot snap, String prevKey) { addOrUpdate(snap.getValue(NetGame.class)); }
            @Override public void onChildChanged(@NonNull DataSnapshot snap, String prevKey) { addOrUpdate(snap.getValue(NetGame.class)); }
            @Override public void onChildRemoved(@NonNull DataSnapshot snap) { NetGame g = snap.getValue(NetGame.class); if (g!=null) remove(g.id); }
            @Override public void onChildMoved(@NonNull DataSnapshot snap, String prevKey) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        gamesRef.addChildEventListener(gamesListener);

        // 3) Crear juego
        btnCreate.setOnClickListener(v -> {
            if (uid == null) return;
            String key = gamesRef.push().getKey();
            if (key == null) return;

            Map<String, Object> data = new HashMap<>();
            data.put("id", key);
            data.put("hostId", uid);
            data.put("guestId", null);
            data.put("status", "waiting");
            data.put("createdAt", ServerValue.TIMESTAMP);
            data.put("board", "         ");  // 9 celdas vacías
            data.put("turn", "X");           // host X comienza

            gamesRef.child(key).updateChildren(data).addOnSuccessListener(ok -> {
                goOnlineGame(key, true);
            });
        });

        // 4) Unirse a un juego existente
        listView.setOnItemClickListener((p, view, pos, id) -> {
            NetGame g = games.get(pos);
            if (g == null) return;

            if ("waiting".equals(g.status) && (g.guestId == null || g.guestId.isEmpty()) && !uid.equals(g.hostId)) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("guestId", uid);
                updates.put("status", "active");
                gamesRef.child(g.id).updateChildren(updates).addOnSuccessListener(ok -> {
                    goOnlineGame(g.id, false);
                });
            } else {
                goOnlineGame(g.id, false);
            }
        });
    }

    private void goOnlineGame(String gameId, boolean isHost) {
        Intent it = new Intent(this, OnlineGameActivity.class);
        it.putExtra("gameId", gameId);
        it.putExtra("isHost", isHost);
        startActivity(it);
    }

    private void addOrUpdate(NetGame g) {
        if (g == null || g.id == null) return;
        int idx = indexOf(g.id);
        String label = "Juego " + g.id.substring(0, Math.min(5, g.id.length())) + " — " + g.status;
        if (idx >= 0) { games.set(idx, g); rows.set(idx, label); }
        else          { games.add(g);      rows.add(label); }
        adapter.notifyDataSetChanged();
    }

    private void remove(String id) {
        int idx = indexOf(id);
        if (idx >= 0) { games.remove(idx); rows.remove(idx); adapter.notifyDataSetChanged(); }
    }

    private int indexOf(String id) {
        for (int i = 0; i < games.size(); i++)
            if (id.equals(games.get(i).id)) return i;
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gamesRef != null && gamesListener != null) {
            gamesRef.removeEventListener(gamesListener);
        }
    }
}

