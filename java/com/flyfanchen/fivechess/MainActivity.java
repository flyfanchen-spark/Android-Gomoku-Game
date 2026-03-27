package com.flyfanchen.fivechess;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvUsername, tvScore, tvHighScore;
    private LinesView linesView;
    private View mainLayout; // 用于更改背景颜色

    private int milestoneThreshold = 500;
    private android.media.SoundPool milestonePool; // 专门给大奖用的音效池
    private int cheerSoundId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 0. 加载持久化设置（颜色等）
        GameSettings.loadSettings(this);
        milestonePool = new android.media.SoundPool.Builder().setMaxStreams(1).build();
        cheerSoundId = milestonePool.load(this, R.raw.cheer_milestone, 1);

        // 1. 初始化界面组件
        mainLayout = findViewById(R.id.main); // 请确保XML根布局ID为main
        tvUsername = findViewById(R.id.tvUsername);
        tvScore = findViewById(R.id.tvScore);
        tvHighScore = findViewById(R.id.tvHighScore); // 请确保XML中有此ID
        linesView = findViewById(R.id.linesView);

        // 应用保存的背景色
        mainLayout.setBackgroundColor(GameSettings.currentBgColor);
        updateHighScoreDisplay();

        // 2. 设置 LinesView 的监听器
        linesView.setOnScoreChangeListener(new LinesView.OnScoreChangeListener() {
            @Override
            public void onScoreChanged(int score) {
                tvScore.setText("Score: " + score);
                GameSettings.score = score; // 确保全局变量同步
                checkMilestones(score);
                // 实时检查是否破纪录
                if (score > GameSettings.getHighScore(MainActivity.this)) {
                    tvHighScore.setText("Best: " + score);
                }
            }

            @Override
            public void onGameOver() {
                // 游戏结束保存分数到历史记录
                // 棋盘满时保存
                GameSettings.saveScore(MainActivity.this, GameSettings.score);
                updateHighScoreDisplay();
                milestoneThreshold = 500; // 游戏结束重置里程碑

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("游戏结束")
                        .setMessage("棋盘已满！\n您的最终得分：" + GameSettings.score)
                        .setPositiveButton("再来一局", (d, w) -> {
                            linesView.initGame();
                            tvScore.setText("Score: 0");
                        })
                        .setCancelable(false)
                        .show();
            }
        });

        // 3. 按钮逻辑：开始
        findViewById(R.id.btnStart).setOnClickListener(v -> {
            linesView.initGame();
            tvScore.setText("Score: 0");
            updateHighScoreDisplay();
            Toast.makeText(this, "新游戏开始", Toast.LENGTH_SHORT).show();
        });

        // 4. 按钮逻辑：结束
        findViewById(R.id.btnEnd).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("退出确认")
                .setMessage("确定要结束当前游戏吗？系统将保存您的当前积分。")
                .setPositiveButton("确定", (d, w) -> {
                    // --- 关键点：退出前先存分 ---
                    GameSettings.saveScore(this, GameSettings.score);
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
        });

        // 5. 按钮逻辑：配置 (子菜单：难度、界面颜色)
        findViewById(R.id.btnConfig).setOnClickListener(v -> {
            String[] configOptions = {"难度设置", "界面颜色自定义"};
            new AlertDialog.Builder(this)
                    .setTitle("游戏配置")
                    .setItems(configOptions, (dialog, which) -> {
                        if (which == 0) showDifficultyDialog();
                        else showColorPickerDialog();
                    }).show();
        });

        // 6. 按钮逻辑：关于 (子菜单：说明、历史排名、关于)
        findViewById(R.id.btnAbout).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            // 动态添加菜单项
            popup.getMenu().add(0, 1, 0, "游戏说明");
            popup.getMenu().add(0, 2, 0, "历史前10名战绩");
            popup.getMenu().add(0, 3, 0, "关于作者");

            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 1: showHelpDialog(); break;
                    case 2: showRankingsDialog(); break;
                    case 3: showAboutDialog(); break;
                }
                return true;
            });
            popup.show();
        });

        // 7. 启动时弹出登录改名对话框
        showLoginDialog();
    }

    // --- 功能辅助方法 ---

    private void updateHighScoreDisplay() {
        if (tvHighScore != null) {
            tvHighScore.setText("Best: " + GameSettings.getHighScore(this));
        }
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("欢迎进入五子连珠");
        final EditText input = new EditText(this);
        input.setText(GameSettings.username);
        builder.setView(input);
        builder.setPositiveButton("确定", (dialog, which) -> {
            GameSettings.username = input.getText().toString();
            tvUsername.setText("用户: " + GameSettings.username);
        });
        builder.show();
    }

    private void showDifficultyDialog() {
        String[] levels = {"入门 (3色)", "标准 (4色) - 推荐", "高手 (5色)"};
        new AlertDialog.Builder(this)
                .setTitle("设置颜色数量（难度）")
                .setItems(levels, (dialog, which) -> {
                    GameSettings.colorCount = which + 3;
                    linesView.initGame();
                    tvScore.setText("Score: 0");
                    Toast.makeText(this, "难度已更新", Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void showColorPickerDialog() {
        new AlertDialog.Builder(this)
                .setTitle("自定义界面背景色")
                .setItems(GameSettings.COLOR_NAMES, (dialog, which) -> {
                    int color = Color.parseColor(GameSettings.COLOR_VALUES[which]);
                    GameSettings.saveBgColor(this, color); // 保存选择
                    mainLayout.setBackgroundColor(color); // 立即应用
                    linesView.invalidate(); // 刷新棋盘背景
                }).show();
    }

    private void showRankingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🏆 历史最高前10名")
                .setMessage(GameSettings.getHistoryDisplay(this))
                .setPositiveButton("返回", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("游戏说明")
                .setMessage("1. 点击小球选中，再次点击空格移动。\n" +
                        "2. 路径必须通畅（上下左右连通）方可移动。\n" +
                        "3. 5个及以上同色球连成一线即可消除。\n" +
                        "4. 默认难度为4色，挑战更高积分吧！")
                .setPositiveButton("知道了", null)
                .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("关于五子连珠")
                .setMessage("版本: v1.2 (正式版)\n作者: spark.chen@hotmail.com，陈凡\n日期: 2026年3月\n状态: 已打磨完成")
                .setPositiveButton("确定", null)
                .show();
    }
    // 3. 祝贺逻辑方法
    private void checkMilestones(int currentScore) {
        if (currentScore >= milestoneThreshold) {
            // 播放祝贺音效
            milestonePool.play(cheerSoundId, 1, 1, 1, 0, 1);

            // 弹出祝贺通知 (带动画样式的 Toast 或者自定义弹窗)
            Toast.makeText(this, "🎉 太棒了！您已突破 " + milestoneThreshold + " 分大关！继续保持！", Toast.LENGTH_LONG).show();

            // 设置下一个里程碑
            milestoneThreshold += 500;
        }
    }
}