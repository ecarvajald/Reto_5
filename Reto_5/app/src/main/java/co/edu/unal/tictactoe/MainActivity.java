package co.edu.unal.tictactoe;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TicTacToeGame mGame;

    // UI
    private TextView mInfoTextView;

    private BoardView mBoardView;


    // Añadir sonidos
    private android.media.MediaPlayer sMoveHuman, sMoveCpu;

    // Estado
    private boolean mGameOver = false;
    private boolean mAndroidThinking = false;
    //marcadores
    private int mHumanWins = 0, mAndroidWins = 0, mTies = 0;
    // Quién empieza la próxima partida
    private boolean mHumanStartsNext = true;

    // TextViews de marcadores
    private TextView tvScoreHuman, tvScoreTies, tvScoreAndroid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar (debe existir en el XML con id @+id/topAppBar)
        MaterialToolbar tb = findViewById(R.id.topAppBar);
        tb.setTitle(R.string.app_name);
        tb.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_scores) {
                clearScores();  // ← se mantiene tal cual
                return true;
            } else if (item.getItemId() == R.id.action_about) {
                showAboutDialog();  // ← abre tu diálogo About
                return true;
            }
            return false;
        });


        // Enlazar UI
        mInfoTextView = findViewById(R.id.information);
        mBoardView = findViewById(R.id.board);



        // Botones inferiores

        MaterialButton btnNew  = findViewById(R.id.btn_new_game);
        MaterialButton btnDiff = findViewById(R.id.btn_difficulty);
        MaterialButton btnQuit = findViewById(R.id.btn_quit);


        tvScoreHuman   = findViewById(R.id.tv_score_human);
        tvScoreTies    = findViewById(R.id.tv_score_ties);
        tvScoreAndroid = findViewById(R.id.tv_score_android);
        updateScores();

        
        btnDiff.setOnClickListener(v -> showDifficultyDialog());
        btnQuit.setOnClickListener(v -> confirmQuit());




        // Motor
        mGame = new TicTacToeGame();

        if (mBoardView != null) {
            mBoardView.setGame(mGame);

            mBoardView.setOnTouchListener((v, event) -> {
                if (mGameOver || mAndroidThinking) return false;
                if (event.getAction() != android.view.MotionEvent.ACTION_DOWN) return false;

                int col = (int) (event.getX() / (mBoardView.getWidth() / 3f));
                int row = (int) (event.getY() / (mBoardView.getHeight() / 3f));
                int pos = row * 3 + col;
                if (pos < 0 || pos >= TicTacToeGame.BOARD_SIZE) return false;

                onCellClicked(pos);
                return true;
            });
        }


        // Arrancar
        startNewGame();
    }


    private void startNewGame() {
        // reiniciar estado
        mGame.clearBoard();
        mGameOver = false;
        mAndroidThinking = false;
        if (mBoardView != null) mBoardView.invalidate();

        // mostrar quién arranca
        if (mHumanStartsNext) {
            mInfoTextView.setText(R.string.you_go_first);
        } else {
            // si empieza Android, muéstralo y haz su primera jugada tras una breve pausa
            mInfoTextView.setText(R.string.turn_android);
            mAndroidThinking = true;
            mInfoTextView.postDelayed(() -> {
                int move = mGame.getComputerMove();
                setMove(TicTacToeGame.COMPUTER_PLAYER, move);
                mAndroidThinking = false;

                int w = mGame.checkForWinner();
                if (w == 0) mInfoTextView.setText(R.string.turn_human);
                else
                    // si ganara o empatara en su primer movimiento
                    finishGameWithResult(w);
            }, 600);
        }
    }


    /** Maneja el toque de una casilla */
    /** Maneja el toque de una casilla (con pausa para mostrar "Android's turn.") */
    private void onCellClicked(int location) {
        // No permitir toques si terminó o si Android está "pensando"
        if (mGameOver || mAndroidThinking) return;


        // Juega humano
        setMove(TicTacToeGame.HUMAN_PLAYER, location);

        int winner = mGame.checkForWinner();
        if (winner != 0) {
            finishGameWithResult(winner);   // suma marcador y alterna el próximo inicio
            return;
        }


        // Mostrar turno de Android y esperar un poco antes de que juegue
        mInfoTextView.setText(R.string.turn_android);
        mAndroidThinking = true;

        // Ejecuta la jugada de Android tras 1 segundo
        mInfoTextView.postDelayed(() -> {
            int move = mGame.getComputerMove();
            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
            play(sMoveCpu);

            int w = mGame.checkForWinner();
            mAndroidThinking = false;


            if (w == 0) {
                mInfoTextView.setText(R.string.turn_human);
            } else {
                finishGameWithResult(w);
            }
        }, 1000);
    }




    /** Pinta X/O en el botón y deshabilita la casilla, SOLO si la jugada fue válida */
    private void setMove(char player, int location) {
        if (!mGame.setMove(player, location)) return; // si la casilla está ocupada, no hace nada
        if (mBoardView != null) mBoardView.invalidate(); // redibuja el tablero

        // Sonido de movimiento
        if (player == TicTacToeGame.HUMAN_PLAYER) play(sMoveHuman);
        else                                      play(sMoveCpu);

        if (mBoardView != null) mBoardView.invalidate();

    }





    private void updateScores() {
        tvScoreHuman.setText(getString(R.string.score_human, mHumanWins));
        tvScoreTies.setText(getString(R.string.score_ties, mTies));
        tvScoreAndroid.setText(getString(R.string.score_android, mAndroidWins));
    }

    private void finishGameWithResult(int winner) {
        if (winner == 1) {
            mInfoTextView.setText(R.string.result_tie);
            mTies++;
        } else if (winner == 2) {
            mInfoTextView.setText(R.string.result_human_wins);
            mHumanWins++;
        } else { // winner == 3
            mInfoTextView.setText(R.string.result_android_wins);
            mAndroidWins++;
        }
        mGameOver = true;
        updateScores();
        // alternar el que empieza para la próxima
        mHumanStartsNext = !mHumanStartsNext;
    }

    private void clearScores() {
        mHumanWins = 0;
        mAndroidWins = 0;
        mTies = 0;
        updateScores();                          // refresca la barra
        mInfoTextView.setText(R.string.scores_cleared); // mensaje opcional

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear_scores) {
            clearScores();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();   // abre diálogo About
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    public void onNewGameClicked(View v) {
        startNewGame();  // solo nuevo juego, sin borrar marcadores
    }


    // Diálogo de dificultad
    private void showDifficultyDialog() {
        final CharSequence[] levels = {
                getString(R.string.level_easy),
                getString(R.string.level_harder),
                getString(R.string.level_expert)
        };

        final int checked = mGame.getDifficultyLevel().ordinal();
        final int[] selectedIndex = { checked };

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_level)
                .setSingleChoiceItems(levels, checked, (dialog, which) -> {
                    selectedIndex[0] = which;
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    TicTacToeGame.DifficultyLevel sel =
                            TicTacToeGame.DifficultyLevel.values()[selectedIndex[0]];
                    mGame.setDifficultyLevel(sel);
                    if (mInfoTextView != null) {
                        mInfoTextView.setText(
                                getString(R.string.choose_level) + ": " + levels[selectedIndex[0]]
                        );
                    }
                    // startNewGame(); // opcional
                })
                .show();
    } // CIERRE de showDifficultyDialog()

    // Confirmar salida
    private void confirmQuit() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quit)
                .setMessage(R.string.confirm_quit)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.exit, (d, w) -> {
                    finishAffinity(); // o finish();
                })
                .show();
    } //Cierre de confirmQuit()


    private void showAboutDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.about_dialog, null, false);


        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setView(content)
                .setPositiveButton(R.string.about_ok, null)
                .show();
    }

    // Reproduce seguro
    private void play(android.media.MediaPlayer mp) {
        if (mp == null) return;
        try { mp.seekTo(0); mp.start(); } catch (Exception ignore) {}
    }

    // Cargar
    private void loadSounds() {
        sMoveHuman = android.media.MediaPlayer.create(getApplicationContext(), R.raw.move_human);
        sMoveCpu   = android.media.MediaPlayer.create(getApplicationContext(), R.raw.move_cpu);
    }

    // Liberar
    private void releaseSounds() {
        if (sMoveHuman != null) { try { sMoveHuman.release(); } catch (Exception ignore) {} sMoveHuman = null; }
        if (sMoveCpu   != null) { try { sMoveCpu.release(); }   catch (Exception ignore) {} sMoveCpu   = null; }
    }

    @Override protected void onResume() { super.onResume(); loadSounds(); }
    @Override protected void onPause()  { super.onPause();  releaseSounds(); }



} // Cierre final de la clase MainActivity

