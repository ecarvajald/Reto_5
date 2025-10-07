package co.edu.unal.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class OnlineGameActivity extends AppCompatActivity {

    // Intent extras
    private String gameId;

    // Firebase
    private DatabaseReference gameRef;
    private ValueEventListener gameListener;

    // UI
    private BoardView boardView;
    private TextView tvInfo, tvStatus;

    // Motor local
    private TicTacToeGame game;

    // Cache de estado remoto
    private String lastStatus = "waiting";   // waiting | active | finished
    private String lastTurn   = "X";         // "X" | "O" | "-"
    private String lastBoard  = "         "; // 9 espacios
    private String lastMine   = "";          // "X" | "O" | ""

    // Sonidos
    private android.media.MediaPlayer sMoveMine, sMoveOpp;

    // Evitar doble sonido (tap + snapshot)
    private String prevBoard = "         ";
    private boolean justPlayedLocal = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_game);

        // UI
        tvInfo    = findViewById(R.id.tv_info_online);
        tvStatus  = findViewById(R.id.tv_status);
        boardView = findViewById(R.id.board);
        tvStatus.setVisibility(View.GONE); // ocultar fila de estado si no la quieres

        // Botón inicio
        View btnGoHome = findViewById(R.id.btn_go_home);
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                if (gameRef != null && gameListener != null) {
                    gameRef.removeEventListener(gameListener);
                }
                Intent i = new Intent(this, StartActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            });
        }

        // Motor
        game = new TicTacToeGame();
        boardView.setGame(game);

        // Params
        gameId = getIntent().getStringExtra("gameId");
        if (gameId == null || gameId.isEmpty()) {
            Toast.makeText(this, "Falta gameId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        prevBoard = lastBoard;

        // Autenticación
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(res -> attachGame())
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        } else {
            attachGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameRef != null && gameListener != null) {
            gameRef.removeEventListener(gameListener);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        try { sMoveMine = android.media.MediaPlayer.create(getApplicationContext(), R.raw.move_human); } catch (Exception ignore) {}
        try { sMoveOpp  = android.media.MediaPlayer.create(getApplicationContext(), R.raw.move_cpu);   } catch (Exception ignore) {}
    }

    @Override protected void onPause() {
        super.onPause();
        releaseSounds();
    }

    // Adjuntar listener y touch
    private void attachGame() {
        gameRef = FirebaseDatabase.getInstance().getReference("games").child(gameId);

        // Listener principal del juego
        gameListener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) {
                // Lee estado remoto normalizado
                String status = getStringField(s, "status", "waiting");
                String turn   = normTurn(s.child("turn").getValue()); // "X"/"O"/""
                String board  = getStringField(s, "board", "         ");
                if (board.length() != 9) board = "         ";
                if (!("waiting".equals(status) || "active".equals(status) || "finished".equals(status))) {
                    status = "waiting";
                }

                // Calcula mi marca desde hostId/guestId del snapshot
                lastMine = myMarkFromSnapshot(s); // "X" | "O" | ""

                lastStatus = status;
                lastTurn   = turn.isEmpty() ? "X" : turn;
                lastBoard  = board;

                // --- SONIDO: detectar quién movió entre prevBoard y board ---
                if (board != null && board.length() == 9 && !board.equals(prevBoard)) {
                    char mover = ' ';
                    for (int i = 0; i < 9; i++) {
                        char a = prevBoard.charAt(i), b = board.charAt(i);
                        if (a != b && b != ' ') { mover = b; break; }
                    }
                    if (mover != ' ') {
                        boolean movedIsMine = (lastMine != null && !lastMine.isEmpty() && lastMine.charAt(0) == mover);
                        if (movedIsMine) {
                            if (!justPlayedLocal) play(sMoveMine);
                        } else {
                            play(sMoveOpp);
                        }
                    }
                    prevBoard = board;
                    justPlayedLocal = false;
                }
                // --- FIN SONIDO ---

                // Refresca motor
                game.setBoardFromString(board);
                boardView.invalidate();

                // UI (👉 ahora muestra quién ganó/empate cuando finished)
                if ("waiting".equals(status)) {
                    tvInfo.setText("Esperando oponente…");
                } else if ("finished".equals(status)) {
                    tvInfo.setText(resultTextForBoard(board));
                } else { // active
                    boolean myTurn = !lastMine.isEmpty() && lastMine.equals(lastTurn);
                    tvInfo.setText(myTurn ? "Tu turno" : "Turno del oponente");
                }

                // (oculto en UI, pero lo dejamos por si lo vuelves a mostrar)
                tvStatus.setText("Estado: " + status + " | Turn: " + lastTurn /* + " | Board: " + board */);

                maybeAutoJoinAsGuestIfWaiting(s);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                tvStatus.setText("Error: " + error.getMessage());
            }
        };
        gameRef.addValueEventListener(gameListener);

        // Tap del tablero
        boardView.setOnCellTapListener(pos -> {
            // Validaciones
            if (!"active".equals(lastStatus)) {
                if ("finished".equals(lastStatus)) {
                    Toast.makeText(this, resultTextForBoard(lastBoard), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "La partida no está lista (status=" + lastStatus + ")", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (lastMine == null || lastMine.isEmpty()) {
                Toast.makeText(this, "No estás dentro de esta partida.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!lastMine.equals(lastTurn)) {
                Toast.makeText(this, "No es tu turno (turno=" + lastTurn + ")", Toast.LENGTH_SHORT).show();
                return;
            }
            if (lastBoard.charAt(pos) != ' ') {
                Toast.makeText(this, "Celda ocupada", Toast.LENGTH_SHORT).show();
                return;
            }

            // Sonido inmediato
            play(sMoveMine);
            justPlayedLocal = true;

            // Transacción atómica
            doMoveTransaction(pos);
        });
    }

    // ==== Helper para mostrar resultado según tablero ====
    private String resultTextForBoard(String board) {
        int w = game.checkForWinner(board); // 0: sigue, 1: empate, 2: gana X, 3: gana O
        if (w == 1) return "Partida terminada: ¡Empate!";
        if (w == 2) return "Partida terminada: Ganó X";
        if (w == 3) return "Partida terminada: Ganó O";
        return "Partida terminada";
    }

    // ==== Helpers de normalización / Firebase ====
    @NonNull
    private String getStringField(@NonNull DataSnapshot s, @NonNull String key, @NonNull String def) {
        Object v = s.child(key).getValue();
        if (v == null) return def;
        String out = String.valueOf(v);
        return out == null ? def : out;
    }

    @NonNull
    private String normTurn(@Nullable Object t) {
        if (t == null) return "";
        String s = String.valueOf(t).trim();
        return ("X".equals(s) || "O".equals(s)) ? s : "";
    }

    @NonNull
    private String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return (u != null) ? u.getUid() : "";
    }

    @NonNull
    private String myMarkFromSnapshot(@NonNull DataSnapshot s) {
        String hostId  = getStringField(s, "hostId", "");
        String guestId = getStringField(s, "guestId", "");
        String me = uid();
        if (me.equals(hostId))  return "X";
        if (me.equals(guestId)) return "O";
        return "";
    }

    // ==== Transacción de jugada ====
    private void doMoveTransaction(final int pos) {
        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData d) {
                String status = valStr(d.child("status"), "waiting");
                String turn   = normTurn(d.child("turn").getValue());
                String board  = valStr(d.child("board"), "         ");
                if (board.length() != 9) board = "         ";

                String hostId  = valStr(d.child("hostId"), "");
                String guestId = valStr(d.child("guestId"), "");
                String me = uid();
                String mine = me.equals(hostId) ? "X" : (me.equals(guestId) ? "O" : "");

                if (!"active".equals(status)) return Transaction.abort();
                if (mine.isEmpty() || !mine.equals(turn)) return Transaction.abort();

                char[] arr = board.toCharArray();
                if (arr[pos] != ' ') return Transaction.abort();

                arr[pos] = mine.charAt(0);
                String newBoard = new String(arr);

                int w = game.checkForWinner(newBoard); // 0 sigue, 1 empate, 2 X, 3 O

                d.child("board").setValue(newBoard);
                if (w == 0) {
                    d.child("turn").setValue(mine.equals("X") ? "O" : "X");
                    d.child("status").setValue("active");
                } else {
                    d.child("turn").setValue("-");
                    d.child("status").setValue("finished");
                }

                return Transaction.success(d);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Toast.makeText(OnlineGameActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void maybeAutoJoinAsGuestIfWaiting(@NonNull DataSnapshot s) {
        String status = getStringField(s, "status", "waiting");
        if (!"waiting".equals(status)) return;

        String hostId  = getStringField(s, "hostId", "");
        String guestId = getStringField(s, "guestId", "");
        String me = uid();

        if (me.equals(hostId) || (guestId != null && !guestId.isEmpty())) return;

        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull @Override
            public Transaction.Result doTransaction(@NonNull MutableData d) {
                String st = valStr(d.child("status"), "waiting");
                String h  = valStr(d.child("hostId"), "");
                String g  = valStr(d.child("guestId"), "");

                if (!"waiting".equals(st)) return Transaction.abort();
                if (g != null && !g.isEmpty()) return Transaction.abort();
                if (uid().equals(h)) return Transaction.abort();

                d.child("guestId").setValue(uid());
                String turn = normTurn(d.child("turn").getValue());
                if (turn.isEmpty()) d.child("turn").setValue("X");
                String board = valStr(d.child("board"), "         ");
                if (board.length() != 9) d.child("board").setValue("         ");
                d.child("status").setValue("active");
                return Transaction.success(d);
            }
            @Override public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {}
        });
    }

    @NonNull
    private String valStr(@NonNull MutableData d, @NonNull String def) {
        Object v = d.getValue();
        if (v == null) return def;
        String s = String.valueOf(v);
        return s == null ? def : s;
    }

    private void play(android.media.MediaPlayer mp) {
        if (mp == null) return;
        try { mp.seekTo(0); mp.start(); } catch (Exception ignore) {}
    }

    private void releaseSounds() {
        if (sMoveMine != null) { try { sMoveMine.release(); } catch (Exception ignore) {} sMoveMine = null; }
        if (sMoveOpp  != null) { try { sMoveOpp.release();  } catch (Exception ignore) {} sMoveOpp  = null; }
    }
}
