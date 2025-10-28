package co.edu.unal.tictactoe;

import java.util.Random;

public class TicTacToeGame {
    public static final int BOARD_SIZE = 9;
    public static final char HUMAN_PLAYER = 'X';
    public static final char COMPUTER_PLAYER = 'O';
    public static final char OPEN_SPOT = ' ';

    // Niveles de dificultad
    public enum DifficultyLevel {Easy, Harder, Expert}

    // Dificultad actual (por defecto Expert)
    private DifficultyLevel mDifficultyLevel = DifficultyLevel.Expert;

    public DifficultyLevel getDifficultyLevel() {
        return mDifficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        mDifficultyLevel = difficultyLevel;
    }

    private final char[] mBoard;
    private final Random mRand;

    public TicTacToeGame() {
        mBoard = new char[BOARD_SIZE];
        mRand = new Random();
        clearBoard();
    }

    // Limpia el tablero
    public void clearBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            mBoard[i] = OPEN_SPOT;
        }
    }

    public boolean setMove(char player, int location) {
        if (location >= 0 && location < BOARD_SIZE && mBoard[location] == OPEN_SPOT) {
            mBoard[location] = player;
            return true;
        }
        return false;
    }


    // ------- IA por funciones auxiliares -------
    private int getRandomMove() {
        int move;
        do {
            move = mRand.nextInt(BOARD_SIZE);
        } while (mBoard[move] != OPEN_SPOT);
        return move;
    }

    // Devuelve una jugada ganadora para 'O' si existe; -1 si no hay
    private int getWinningMove() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = COMPUTER_PLAYER;
                int w = checkForWinner();
                mBoard[i] = OPEN_SPOT;
                if (w == 3) return i; // O ganaría aquí
            }
        }
        return -1;
    }

    // Devuelve una jugada que bloquee victoria de 'X'; -1 si no hay
    private int getBlockingMove() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) {
                mBoard[i] = HUMAN_PLAYER;
                int w = checkForWinner();
                mBoard[i] = OPEN_SPOT;
                if (w == 2) return i; // X ganaría; bloquear aquí
            }
        }
        return -1;
    }

    // Selección de jugada según dificultad
    public int getComputerMove() {
        int move = -1;
        if (mDifficultyLevel == DifficultyLevel.Easy) {
            move = getRandomMove();
        } else if (mDifficultyLevel == DifficultyLevel.Harder) {
            move = getWinningMove();
            if (move == -1) move = getRandomMove();
        } else if (mDifficultyLevel == DifficultyLevel.Expert) {
            move = getWinningMove();
            if (move == -1) move = getBlockingMove();
            if (move == -1) move = getRandomMove();
        }
        return move;
    }

    //  0 = sin ganador / en juego
    //  1 = empate
    //  2 = gana X
    //  3 = gana O
    public int checkForWinner() {
        // Horizontales
        for (int i = 0; i <= 6; i += 3) {
            if (mBoard[i] == HUMAN_PLAYER && mBoard[i + 1] == HUMAN_PLAYER && mBoard[i + 2] == HUMAN_PLAYER)
                return 2;
            if (mBoard[i] == COMPUTER_PLAYER && mBoard[i + 1] == COMPUTER_PLAYER && mBoard[i + 2] == COMPUTER_PLAYER)
                return 3;
        }
        // Verticales
        for (int i = 0; i <= 2; i++) {
            if (mBoard[i] == HUMAN_PLAYER && mBoard[i + 3] == HUMAN_PLAYER && mBoard[i + 6] == HUMAN_PLAYER)
                return 2;
            if (mBoard[i] == COMPUTER_PLAYER && mBoard[i + 3] == COMPUTER_PLAYER && mBoard[i + 6] == COMPUTER_PLAYER)
                return 3;
        }
        // Diagonales
        if ((mBoard[0] == HUMAN_PLAYER && mBoard[4] == HUMAN_PLAYER && mBoard[8] == HUMAN_PLAYER) ||
                (mBoard[2] == HUMAN_PLAYER && mBoard[4] == HUMAN_PLAYER && mBoard[6] == HUMAN_PLAYER))
            return 2;
        if ((mBoard[0] == COMPUTER_PLAYER && mBoard[4] == COMPUTER_PLAYER && mBoard[8] == COMPUTER_PLAYER) ||
                (mBoard[2] == COMPUTER_PLAYER && mBoard[4] == COMPUTER_PLAYER && mBoard[6] == COMPUTER_PLAYER))
            return 3;

        // ¿Aún hay espacios?
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (mBoard[i] == OPEN_SPOT) return 0;
        }
        return 1; // empate
    }
    public char getBoardOccupant(int i) {
        if (i >= 0 && i < BOARD_SIZE) return mBoard[i];
        return OPEN_SPOT;
    }

}
