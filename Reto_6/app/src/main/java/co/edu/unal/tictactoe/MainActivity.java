package co.edu.unal.tictactoe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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

    // Sonidos
    private android.media.MediaPlayer sMoveHuman, sMoveCpu;

    // Estado
    private boolean mGameOver = false;
    private boolean mAndroidThinking = false;
    private int mHumanWins = 0, mAndroidWins = 0, mTies = 0;
    private boolean mHumanStartsNext = true;

    // Marcadores
    private TextView tvScoreHuman, tvScoreTies, tvScoreAndroid;

    // Claves estado
    private static final String STATE_BOARD        = "board";
    private static final String STATE_GAME_OVER    = "game_over";
    private static final String STATE_HUMAN_WINS   = "human_wins";
    private static final String STATE_ANDROID_WINS = "android_wins";
    private static final String STATE_TIES         = "ties";
    private static final String STATE_HUMAN_STARTS = "human_starts_next";
    private static final String STATE_INFO_TEXT    = "info.text";
    private static final String STATE_DIFFICULTY   = "difficulty";
    private static final String STATE_CPU_PENDING  = "cpu_pending";

    // SharedPreferences
    private static final String PREFS      = "scores_prefs";
    private static final String P_HUMAN    = "p_human";
    private static final String P_ANDROID  = "p_android";
    private static final String P_TIES     = "p_ties";
    private static final String P_STARTS   = "p_starts";
    private static final String P_DIFF     = "p_diff";


    //mover CPU con retraso reprogramable
    private Runnable mComputerMoveTask;
    private boolean mCpuMovePending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar
        MaterialToolbar tb = findViewById(R.id.topAppBar);
        tb.setTitle(R.string.app_name);
        tb.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_clear_scores) { clearScores(); return true; }
            if (item.getItemId() == R.id.action_about) { showAboutDialog(); return true; }
            return false;
        });

        // UI
        mInfoTextView  = findViewById(R.id.information);
        mBoardView     = findViewById(R.id.board);
        MaterialButton btnNew  = findViewById(R.id.btn_new_game);
        MaterialButton btnDiff = findViewById(R.id.btn_difficulty);
        MaterialButton btnQuit = findViewById(R.id.btn_quit);

        tvScoreHuman   = findViewById(R.id.tv_score_human);
        tvScoreTies    = findViewById(R.id.tv_score_ties);
        tvScoreAndroid = findViewById(R.id.tv_score_android);

        int diffOrdPref;  // guardamos aquí el ordinal de dificultad
        {
            android.content.SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            mHumanWins       = sp.getInt(P_HUMAN,   0);
            mAndroidWins     = sp.getInt(P_ANDROID, 0);
            mTies            = sp.getInt(P_TIES,    0);
            mHumanStartsNext = sp.getBoolean(P_STARTS, true);
            diffOrdPref      = sp.getInt(P_DIFF, TicTacToeGame.DifficultyLevel.Expert.ordinal());
        }


        btnNew.setOnClickListener(this::onNewGameClicked);
        btnDiff.setOnClickListener(v -> showDifficultyDialog());
        btnQuit.setOnClickListener(v -> confirmQuit());

        // Motor
        mGame = new TicTacToeGame();
        mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[diffOrdPref]);

        if (mBoardView != null) {
            mBoardView.setGame(mGame);
            mBoardView.setOnTouchListener((v, event) -> {
                if (mGameOver || mAndroidThinking) return false;
                if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
                int col = (int) (event.getX() / (mBoardView.getWidth() / 3f));
                int row = (int) (event.getY() / (mBoardView.getHeight() / 3f));
                int pos = row * 3 + col;
                if (pos < 0 || pos >= TicTacToeGame.BOARD_SIZE) return false;
                onCellClicked(pos);
                return true;
            });
        }


        // Runnable único para la jugada de CPU
        mComputerMoveTask = () -> {
            int move = mGame.getComputerMove();
            setMove(TicTacToeGame.COMPUTER_PLAYER, move);
            play(sMoveCpu);

            int w = mGame.checkForWinner();
            mAndroidThinking = false;
            mCpuMovePending = false;

            if (w == 0) mInfoTextView.setText(R.string.turn_human);
            else        finishGameWithResult(w);
        };

        /* ===== Restauración ===== */
        if (savedInstanceState == null) {
            startNewGame();
        } else {
            char[] board = savedInstanceState.getCharArray(STATE_BOARD);
            if (board != null) mGame.setBoardState(board);

            int diffOrd = savedInstanceState.getInt(
                    STATE_DIFFICULTY,
                    TicTacToeGame.DifficultyLevel.Expert.ordinal());
            mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.values()[diffOrd]);

            mGameOver        = savedInstanceState.getBoolean(STATE_GAME_OVER, false);
            mHumanStartsNext = savedInstanceState.getBoolean(STATE_HUMAN_STARTS, true);
            mHumanWins       = savedInstanceState.getInt(STATE_HUMAN_WINS, 0);
            mAndroidWins     = savedInstanceState.getInt(STATE_ANDROID_WINS, 0);
            mTies            = savedInstanceState.getInt(STATE_TIES, 0);

            CharSequence info = savedInstanceState.getCharSequence(STATE_INFO_TEXT);
            if (info != null) mInfoTextView.setText(info);

            mCpuMovePending = savedInstanceState.getBoolean(STATE_CPU_PENDING, false);

            mAndroidThinking = false;

            // Reprograma jugada CPU si quedó pendiente al rotar
            if (!mGameOver && mCpuMovePending) {
                mAndroidThinking = true;
                mInfoTextView.postDelayed(mComputerMoveTask, 600);
            }
        }

//  Refresca SIEMPRE aquí (una sola vez)
        updateScores();
        if (mBoardView != null) mBoardView.invalidate();

    }

    @Override
    protected void onStop() {
        super.onStop();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putInt(P_HUMAN,   mHumanWins)
                .putInt(P_ANDROID, mAndroidWins)
                .putInt(P_TIES,    mTies)
                .putBoolean(P_STARTS, mHumanStartsNext)
                .putInt(P_DIFF, mGame.getDifficultyLevel().ordinal())
                .apply();
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharArray(STATE_BOARD, mGame.getBoardState());
        outState.putInt(STATE_DIFFICULTY, mGame.getDifficultyLevel().ordinal());
        outState.putBoolean(STATE_GAME_OVER, mGameOver);
        outState.putBoolean(STATE_HUMAN_STARTS, mHumanStartsNext);
        outState.putInt(STATE_HUMAN_WINS, mHumanWins);
        outState.putInt(STATE_ANDROID_WINS, mAndroidWins);
        outState.putInt(STATE_TIES, mTies);
        outState.putCharSequence(STATE_INFO_TEXT, mInfoTextView.getText());
        outState.putBoolean(STATE_CPU_PENDING, mCpuMovePending);
    }

    private void startNewGame() {
        mGame.clearBoard();
        mGameOver = false;
        mAndroidThinking = false;
        if (mBoardView != null) mBoardView.invalidate();

        if (mHumanStartsNext) {
            mInfoTextView.setText(R.string.you_go_first);
        } else {
            mInfoTextView.setText(R.string.turn_android);
            mAndroidThinking = true;
            mCpuMovePending = true;
            mInfoTextView.postDelayed(mComputerMoveTask, 600);
        }
    }

    private void onCellClicked(int location) {
        if (mGameOver || mAndroidThinking) return;

        setMove(TicTacToeGame.HUMAN_PLAYER, location);

        int winner = mGame.checkForWinner();
        if (winner != 0) { finishGameWithResult(winner); return; }

        mInfoTextView.setText(R.string.turn_android);
        mAndroidThinking = true;
        mCpuMovePending = true;
        mInfoTextView.postDelayed(mComputerMoveTask, 1000);
    }

    private void setMove(char player, int location) {
        if (!mGame.setMove(player, location)) return;
        if (mBoardView != null) mBoardView.invalidate();

        if (player == TicTacToeGame.HUMAN_PLAYER) play(sMoveHuman);
        else                                       play(sMoveCpu);

        if (mBoardView != null) mBoardView.invalidate();
    }

    private void updateScores() {
        tvScoreHuman.setText(getString(R.string.score_human, mHumanWins));
        tvScoreTies.setText(getString(R.string.score_ties, mTies));
        tvScoreAndroid.setText(getString(R.string.score_android, mAndroidWins));
    }

    private void finishGameWithResult(int winner) {
        if (winner == 1) { mInfoTextView.setText(R.string.result_tie);          mTies++;        }
        else if (winner == 2) { mInfoTextView.setText(R.string.result_human_wins); mHumanWins++;   }
        else { mInfoTextView.setText(R.string.result_android_wins); mAndroidWins++; }

        mGameOver = true;
        updateScores();
        mHumanStartsNext = !mHumanStartsNext;
    }

    private void clearScores() {
        mHumanWins = 0; mAndroidWins = 0; mTies = 0;
        updateScores();
        mInfoTextView.setText(R.string.scores_cleared);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear_scores) { clearScores(); return true; }
        if (id == R.id.action_about) { showAboutDialog(); return true; }
        return super.onOptionsItemSelected(item);
    }

    public void onNewGameClicked(View v) { startNewGame(); }

    private void showDifficultyDialog() {
        final CharSequence[] levels = {
                getString(R.string.level_easy),
                getString(R.string.level_harder),
                getString(R.string.level_expert)
        };
        final int checked = mGame.getDifficultyLevel().ordinal();
        final int[] selectedIndex = { checked };

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.choose_level)
                .setSingleChoiceItems(levels, checked, (d, which) -> selectedIndex[0] = which)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    TicTacToeGame.DifficultyLevel sel =
                            TicTacToeGame.DifficultyLevel.values()[selectedIndex[0]];
                    mGame.setDifficultyLevel(sel);
                    mInfoTextView.setText(getString(R.string.choose_level) + ": " + levels[selectedIndex[0]]);
                })
                .show();
    }

    private void confirmQuit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.quit)
                .setMessage(R.string.confirm_quit)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.exit, (d, w) -> finishAffinity())
                .show();
    }

    private void showAboutDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View content = inflater.inflate(R.layout.about_dialog, null, false);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setView(content)
                .setPositiveButton(R.string.about_ok, null)
                .show();
    }

    private void play(android.media.MediaPlayer mp) {
        if (mp == null) return;
        try { mp.seekTo(0); mp.start(); } catch (Exception ignore) {}
    }

    private void loadSounds() {
        sMoveHuman = android.media.MediaPlayer.create(getApplicationContext(), R.raw.move_human);
        sMoveCpu   = android.media.MediaPlayer.create(getApplicationContext(), R.raw.move_cpu);
    }

    private void releaseSounds() {
        if (sMoveHuman != null) { try { sMoveHuman.release(); } catch (Exception ignore) {} sMoveHuman = null; }
        if (sMoveCpu   != null) { try { sMoveCpu.release(); }   catch (Exception ignore) {} sMoveCpu   = null; }
    }

    @Override protected void onResume() { super.onResume(); loadSounds(); }
    @Override protected void onPause()  {
        super.onPause();
        // Cancela runnable pendiente para evitar crash tras rotación
        if (mInfoTextView != null && mComputerMoveTask != null) {
            mInfoTextView.removeCallbacks(mComputerMoveTask);
        }
        releaseSounds();
    }
}
