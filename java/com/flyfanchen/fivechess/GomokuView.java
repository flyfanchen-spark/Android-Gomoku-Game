package com.flyfanchen.fivechess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class GomokuView extends View {
    private Paint paint = new Paint();
    private int gridCount = 15; // 15x15的棋盘
    private float gridWidth;
    private int[][] board = new int[gridCount][gridCount]; // 0:空, 1:黑子, 2:白子
    private boolean isBlackTurn = true;
    private boolean isGameOver = false;

    public GomokuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true); // 抗锯齿，让圆圈更圆滑
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 让棋盘始终是正方形
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        gridWidth = getWidth() / (float) gridCount;

        // 1. 画棋盘背景
        canvas.drawColor(Color.parseColor("#DDBB99"));

        // 2. 画棋盘线
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(2);
        for (int i = 0; i < gridCount; i++) {
            // 横线
            canvas.drawLine(gridWidth / 2, gridWidth / 2 + i * gridWidth,
                    getWidth() - gridWidth / 2, gridWidth / 2 + i * gridWidth, paint);
            // 纵线
            canvas.drawLine(gridWidth / 2 + i * gridWidth, gridWidth / 2,
                    gridWidth / 2 + i * gridWidth, getWidth() - gridWidth / 2, paint);
        }

        // 3. 画棋子
        for (int i = 0; i < gridCount; i++) {
            for (int j = 0; j < gridCount; j++) {
                if (board[i][j] != 0) {
                    paint.setColor(board[i][j] == 1 ? Color.BLACK : Color.WHITE);
                    canvas.drawCircle(gridWidth / 2 + i * gridWidth,
                            gridWidth / 2 + j * gridWidth,
                            gridWidth * 0.4f, paint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isGameOver || event.getAction() != MotionEvent.ACTION_UP) return true;

        // 将点击的像素坐标转换为棋盘行列索引
        int x = (int) (event.getX() / gridWidth);
        int y = (int) (event.getY() / gridWidth);

        if (x >= 0 && x < gridCount && y >= 0 && y < gridCount && board[x][y] == 0) {
            board[x][y] = isBlackTurn ? 1 : 2;
            if (checkWin(x, y)) {
                isGameOver = true;
                Toast.makeText(getContext(), (isBlackTurn ? "黑方" : "白方") + "赢了！", Toast.LENGTH_LONG).show();
            }
            isBlackTurn = !isBlackTurn;
            invalidate(); // 刷新界面，触发 onDraw
        }
        return true;
    }

    // 极其简易的胜负判断算法（水平/垂直/斜向）
    private boolean checkWin(int x, int y) {
        int color = board[x][y];
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            int count = 1;
            // 正向找
            for (int i = 1; i < 5; i++) {
                int nx = x + d[0] * i, ny = y + d[1] * i;
                if (nx >= 0 && nx < gridCount && ny >= 0 && ny < gridCount && board[nx][ny] == color) count++;
                else break;
            }
            // 反向找
            for (int i = 1; i < 5; i++) {
                int nx = x - d[0] * i, ny = y - d[1] * i;
                if (nx >= 0 && nx < gridCount && ny >= 0 && ny < gridCount && board[nx][ny] == color) count++;
                else break;
            }
            if (count >= 5) return true;
        }
        return false;
    }
}