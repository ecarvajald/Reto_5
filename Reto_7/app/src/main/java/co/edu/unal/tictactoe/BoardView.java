package co.edu.unal.tictactoe;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class BoardView extends View {

    public interface OnCellTapListener {
        void onCellTap(int pos);
    }

    private OnCellTapListener onCellTapListener;
    public void setOnCellTapListener(OnCellTapListener l) { this.onCellTapListener = l; }

    public static final int GRID_WIDTH = 6;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap mHumanBitmap, mComputerBitmap;
    private TicTacToeGame mGame;

    public BoardView(Context context) { super(context); init(); }
    public BoardView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public BoardView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        setClickable(true);
        setFocusable(true);

        gridPaint.setColor(Color.parseColor("#B39DDB"));
        gridPaint.setStrokeWidth(GRID_WIDTH);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        try { mHumanBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.x_img); } catch (Exception ignore) {}
        try { mComputerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.o_img); } catch (Exception ignore) {}
    }

    @Override protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec), h = MeasureSpec.getSize(hSpec);
        int size = Math.min(w, h);
        setMeasuredDimension(size, size);
    }

    public void setGame(TicTacToeGame game) { this.mGame = game; invalidate(); }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (ev.getAction() != MotionEvent.ACTION_DOWN) return true;

        performClick();

        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return true;

        int col = (int) (ev.getX() / (w / 3f));
        int row = (int) (ev.getY() / (h / 3f));
        int pos = row * 3 + col;
        if (pos < 0 || pos >= TicTacToeGame.BOARD_SIZE) return true;

        if (onCellTapListener != null) onCellTapListener.onCellTap(pos);
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int w = getWidth(), h = getHeight();
        final int cellW = w / 3, cellH = h / 3;

        textPaint.setTextSize(Math.min(cellW, cellH) * 0.6f);

        // Grilla
        canvas.drawLine(cellW, 0, cellW, h, gridPaint);
        canvas.drawLine(2 * cellW, 0, 2 * cellW, h, gridPaint);
        canvas.drawLine(0, cellH, w, cellH, gridPaint);
        canvas.drawLine(0, 2 * cellH, w, 2 * cellH, gridPaint);

        if (mGame == null) return;

        // X/O
        final int pad = GRID_WIDTH;
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
                    float cx = (left + right) / 2f;
                    float cy = (top + bottom) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                    canvas.drawText("X", cx, cy, textPaint);
                }
            } else if (occ == TicTacToeGame.COMPUTER_PLAYER) {
                if (mComputerBitmap != null) {
                    canvas.drawBitmap(mComputerBitmap, null, new Rect(left, top, right, bottom), null);
                } else {
                    float cx = (left + right) / 2f;
                    float cy = (top + bottom) / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
                    canvas.drawText("O", cx, cy, textPaint);
                }
            }
        }
    }
}
