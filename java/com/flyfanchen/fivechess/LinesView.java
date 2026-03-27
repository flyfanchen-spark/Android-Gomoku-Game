package com.flyfanchen.fivechess;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LinesView extends View {
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int[][] board;
    private int selX = -1, selY = -1;
    private int stepSoundId;
    private int stepStreamId = 0; // 记录正在播放的步进音效流
    private int lastMilestoneReached = 0; // 记录上一次达到的里程碑 (500, 1000...)

    // 重新定义颜色：使用更深、更饱和的颜色，黄色改为深琥珀色
    private int[] ballColors = {
            Color.parseColor("#E53935"), // 深红
            Color.parseColor("#1E88E5"), // 深蓝
            Color.parseColor("#FFB300"), // 深黄 (琥珀色)
            Color.parseColor("#43A047"), // 深绿
            Color.parseColor("#8E24AA")  // 深紫
    };

    private android.media.SoundPool soundPool;
    private int clearSoundId;
    private boolean isAnimating = false;

    public interface OnScoreChangeListener {
        void onScoreChanged(int score);
        void onGameOver();
    }
    private OnScoreChangeListener scoreListener;

    public void setOnScoreChangeListener(OnScoreChangeListener listener) {
        this.scoreListener = listener;
    }

    public LinesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        soundPool = new android.media.SoundPool.Builder().setMaxStreams(10).build();
        clearSoundId = soundPool.load(context, R.raw.clear_pop, 1);
        stepSoundId = soundPool.load(context, R.raw.step_tick, 1); // 加载脚步声
        initGame();
    }

    public void initGame() {
        int size = GameSettings.boardSize;
        board = new int[size][size];
        GameSettings.score = 0;
        selX = -1; selY = -1;
        isAnimating = false;
        spawnBalls(3);
        invalidate();
    }

    private void spawnBalls(int count) {
        List<int[]> emptyCells = new ArrayList<>();
        for (int i = 0; i < GameSettings.boardSize; i++) {
            for (int j = 0; j < GameSettings.boardSize; j++) {
                if (board[i][j] == 0) emptyCells.add(new int[]{i, j});
            }
        }
        if (emptyCells.isEmpty()) return;

        Collections.shuffle(emptyCells);
        int actualCount = Math.min(count, emptyCells.size());
        for (int i = 0; i < actualCount; i++) {
            int[] pos = emptyCells.get(i);
            board[pos[0]][pos[1]] = (int) (Math.random() * GameSettings.colorCount) + 1;
            checkAndClear(pos[0], pos[1]);
        }

        if (emptyCells.size() <= actualCount) {
            if (scoreListener != null) scoreListener.onGameOver();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float viewSize = getWidth();
        float cellW = viewSize / GameSettings.boardSize;

        // canvas.drawColor(Color.WHITE); // 删掉这一行
        canvas.drawColor(GameSettings.currentBgColor); // 改为跟随设置

        // 1. 绘制网格线
        paint.setColor(Color.parseColor("#EEEEEE")); // 更淡的线，突出球体
        paint.setStrokeWidth(2);
        for (int i = 0; i <= GameSettings.boardSize; i++) {
            canvas.drawLine(0, i * cellW, viewSize, i * cellW, paint);
            canvas.drawLine(i * cellW, 0, i * cellW, viewSize, paint);
        }

        // 2. 遍历绘制内容
        for (int i = 0; i < GameSettings.boardSize; i++) {
            for (int j = 0; j < GameSettings.boardSize; j++) {
                float centerX = i * cellW + cellW / 2;
                float centerY = j * cellW + cellW / 2;

                // --- 选中效果：格子背景变深 ---
                if (i == selX && j == selY) {
                    paint.setColor(Color.parseColor("#E0E0E0"));
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(i * cellW + 4, j * cellW + 4, (i + 1) * cellW - 4, (j + 1) * cellW - 4, paint);
                }

                if (board[i][j] > 0) {
                    int ballColor = ballColors[board[i][j] - 1];
                    float radius = cellW * 0.38f;

                    // --- 增强版选中效果：扩散环和放大 ---
                    if (i == selX && j == selY) {
                        // 绘制半透明扩散环
                        paint.setColor(ballColor);
                        paint.setAlpha(60);
                        canvas.drawCircle(centerX, centerY, cellW * 0.48f, paint);

                        // 绘制外层实心描边圈
                        paint.setStyle(Paint.Style.STROKE);
                        paint.setStrokeWidth(6);
                        paint.setAlpha(255);
                        canvas.drawCircle(centerX, centerY, cellW * 0.44f, paint);

                        // 选中球体变大
                        radius = cellW * 0.42f;
                    }

                    // 绘制球体主体
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(ballColor);
                    paint.setAlpha(255);
                    canvas.drawCircle(centerX, centerY, radius, paint);

                    // 绘制立体感高光 (白色小圆点)
                    paint.setColor(Color.WHITE);
                    paint.setAlpha(180);
                    canvas.drawCircle(centerX - radius / 3, centerY - radius / 3, radius / 4, paint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isAnimating || event.getAction() != MotionEvent.ACTION_UP) return true;

        float cellW = getWidth() / (float) GameSettings.boardSize;
        int x = (int) (event.getX() / cellW);
        int y = (int) (event.getY() / cellW);

        if (x < 0 || x >= GameSettings.boardSize || y < 0 || y >= GameSettings.boardSize) return true;

        if (board[x][y] > 0) {
            // 如果点的是球，切换选中目标
            selX = x; selY = y;
        } else if (selX != -1) {
            // 如果点的是空格，且已有选中，尝试移动
            List<int[]> path = GameLogic.findPath(board, selX, selY, x, y, GameSettings.boardSize);
            if (path != null) {
                startMoveAnimation(path);
            } else {
                Toast.makeText(getContext(), "无法到达该位置", Toast.LENGTH_SHORT).show();
            }
        }
        invalidate();
        return true;
    }

    private void startMoveAnimation(final List<int[]> path) {
        isAnimating = true;
        final int ballColorIndex = board[selX][selY];
        final int startX = selX;
        final int startY = selY;

        selX = -1; selY = -1; // 移动开始，立即取消选中
        board[startX][startY] = 0;

        // 优化点1：放慢速度，从 60ms 改为 150ms 或 200ms
        int moveDelay = 150;

        final android.os.Handler handler = new android.os.Handler();
        for (int i = 1; i < path.size(); i++) {
            final int index = i;
            handler.postDelayed(() -> {
                // --- 修订逻辑 1：如果上一步的声音还在响，先把它停掉 ---
                if (stepStreamId != 0) {
                    soundPool.stop(stepStreamId);
                }

                int[] curr = path.get(index);
                int[] prev = path.get(index - 1);

                board[prev[0]][prev[1]] = 0;
                board[curr[0]][curr[1]] = ballColorIndex;

                // --- 修订逻辑 2：播放声音并记录 streamId ---
                // 最后的参数 1.0f 是播放速度。如果声音长，可以略微调快速度如 1.2f
                stepStreamId = soundPool.play(stepSoundId, 1.0f, 1.0f, 0, 0, 1.2f);

                invalidate();

                if (index == path.size() - 1) {
                    // --- 修订逻辑 3：到达终点，立刻强行停止步进音效 ---
                    handler.postDelayed(() -> {
                        if (stepStreamId != 0) {
                            soundPool.stop(stepStreamId);
                            stepStreamId = 0;
                        }
                    }, 100); // 100ms 后掐断，给最后一下留点余味

                    boolean cleared = checkAndClear(curr[0], curr[1]);
                    if (!cleared) {
                        spawnBalls(3);
                    } else {
                        soundPool.play(clearSoundId, 1, 1, 0, 0, 1);
                    }
                    isAnimating = false;
                }
            }, i * moveDelay); // 移动速度稍微加快，手感更爽快, 50->30
        }
    }

    private boolean checkAndClear(int x, int y) {
        int color = board[x][y];
        if (color == 0) return false;

        List<int[]> allToClear = new ArrayList<>();
        int[][] dirs = {{1, 0}, {0, 1}, {1, 1}, {1, -1}};

        for (int[] d : dirs) {
            List<int[]> line = new ArrayList<>();
            line.add(new int[]{x, y});

            for (int i = 1; i < GameSettings.boardSize; i++) {
                int nx = x + d[0] * i, ny = y + d[1] * i;
                if (nx >= 0 && nx < GameSettings.boardSize && ny >= 0 && ny < GameSettings.boardSize && board[nx][ny] == color) {
                    line.add(new int[]{nx, ny});
                } else break;
            }
            for (int i = 1; i < GameSettings.boardSize; i++) {
                int nx = x - d[0] * i, ny = y - d[1] * i;
                if (nx >= 0 && nx < GameSettings.boardSize && ny >= 0 && ny < GameSettings.boardSize && board[nx][ny] == color) {
                    line.add(new int[]{nx, ny});
                } else break;
            }

            if (line.size() >= 5) {
                for (int[] p : line) {
                    boolean alreadyIn = false;
                    for (int[] existing : allToClear) {
                        if (existing[0] == p[0] && existing[1] == p[1]) {
                            alreadyIn = true;
                            break;
                        }
                    }
                    if (!alreadyIn) allToClear.add(p);
                }
            }
        }

        if (!allToClear.isEmpty()) {
            int count = allToClear.size();
            for (int[] pos : allToClear) {
                board[pos[0]][pos[1]] = 0;
            }
            GameSettings.score += GameLogic.calculateScore(count);
            if (scoreListener != null) scoreListener.onScoreChanged(GameSettings.score);
            invalidate();
            return true;
        }
        return false;
    }
}