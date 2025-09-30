package co.edu.unal.tictactoe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BoardView extends View {

    public static final int GRID_WIDTH = 6;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mHumanBitmap;
    private Bitmap mComputerBitmap;

    private TicTacToeGame mGame;

    public BoardView(Context context) {
        super(context);
        init();
    }

    public BoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gridPaint.setColor(Color.parseColor("#B39DDB"));
        gridPaint.setStrokeWidth(GRID_WIDTH);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        // Intenta cargar bitmaps (opcionales). Si no existen, dibujará texto.
        try { mHumanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.x_img); } catch (Exception ignore) {}
        try { mComputerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.o_img); } catch (Exception ignore) {}
    }

    public void setGame(TicTacToeGame game) {
        this.mGame = game;
        invalidate();
    }

    public int getBoardCellWidth()  { return getWidth() / 3; }
    public int getBoardCellHeight() { return getHeight() / 3; }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int w = getWidth();
        final int h = getHeight();
        final int cellW = getBoardCellWidth();
        final int cellH = getBoardCellHeight();

        // Ajusta el tamaño de letra para el fallback de texto
        textPaint.setTextSize(Math.min(cellW, cellH) * 0.6f);

        // Líneas de la grilla
        canvas.drawLine(cellW, 0, cellW, h, gridPaint);
        canvas.drawLine(2 * cellW, 0, 2 * cellW, h, gridPaint);
        canvas.drawLine(0, cellH, w, cellH, gridPaint);
        canvas.drawLine(0, 2 * cellH, w, 2 * cellH, gridPaint);

        if (mGame == null) return;

        // Dibuja X/O por celda
        final int pad = GRID_WIDTH; // pequeño margen para no tocar las líneas
        for (int i = 0; i < TicTacToeGame.BOARD_SIZE; i++) {
            int col = i % 3, row = i / 3;

            int left   = col * cellW + pad;
            int top    = row * cellH + pad;
            int right  = (col + 1) * cellW - pad;
            int bottom = (row + 1) * cellH - pad;

            char occ = mGame.getBoardOccupant(i);

            if (occ == TicTacToeGame.HUMAN_PLAYER) {
                if (mHumanBitmap != null) {
                    canvas.drawBitmap(mHumanBitmap, null, new Rect(left, top, right, bottom), null);
                } else {
                    // Fallback: dibuja letra “X”
                    float cx = left + (right - left) / 2f;
                    float cy = top + (bottom - top) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                    canvas.drawText("X", cx, cy, textPaint);
                }
            } else if (occ == TicTacToeGame.COMPUTER_PLAYER) {
                if (mComputerBitmap != null) {
                    canvas.drawBitmap(mComputerBitmap, null, new Rect(left, top, right, bottom), null);
                } else {
                    // Fallback: dibuja letra “O”
                    float cx = left + (right - left) / 2f;
                    float cy = top + (bottom - top) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                    canvas.drawText("O", cx, cy, textPaint);
                }
            }
        }
    }
}
