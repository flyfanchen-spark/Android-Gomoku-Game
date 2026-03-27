package com.flyfanchen.fivechess;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameSettings {
    public static String username = "Player_" + (int)(Math.random() * 1000);
    public static int boardSize = 10;
    public static int colorCount = 4; // 3,4,5 color types
    public static int score = 0;

    // 改进点2：默认底色
    public static int currentBgColor = Color.parseColor("#FAF8EF");

    // 预设的8种淡雅颜色
    public static final String[] COLOR_NAMES = {"象牙白", "淡苹果绿", "浅天蓝", "薰衣草紫", "樱花粉", "柠檬绸缎", "薄荷绿", "香槟金"};
    public static final String[] COLOR_VALUES = {"#FAF8EF", "#F0FFF0", "#F0F8FF", "#E6E6FA", "#FFF0F5", "#FFFACD", "#F5FFFA", "#FAF0E6"};

    // --- 数据存储逻辑 ---
    private static final String PREF_NAME = "GameData";

    // 保存分数到历史前10名
    public static void saveScore(Context context, int score) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String history = prefs.getString("score_history", "");
        long time = System.currentTimeMillis();
        // 格式：分数|用户名|时间;
        history += score + "|" + username + "|" + time + ";";

        // 排序逻辑：取前10名
        String[] entries = history.split(";");
        List<ScoreEntry> list = new ArrayList<>();
        for (String s : entries) {
            if (!s.isEmpty()) {
                String[] p = s.split("\\|");
                list.add(new ScoreEntry(Integer.parseInt(p[0]), p[1], Long.parseLong(p[2])));
            }
        }
        Collections.sort(list, (a, b) -> b.score - a.score); // 降序

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(list.size(), 10); i++) {
            sb.append(list.get(i).toString()).append(";");
        }
        prefs.edit().putString("score_history", sb.toString()).apply();
    }

    // 获取最高分
    public static int getHighScore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String history = prefs.getString("score_history", "");
        if (history.isEmpty()) return 0;
        return Integer.parseInt(history.split("\\|")[0]);
    }

    // 获取历史记录字符串
    public static String getHistoryDisplay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String history = prefs.getString("score_history", "");
        if (history.isEmpty()) return "暂无记录";
        String[] entries = history.split(";");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.length; i++) {
            String[] p = entries[i].split("\\|");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
            sb.append(String.format("%d. %s: %s分 (%s)\n", i+1, p[1], p[0], sdf.format(new java.util.Date(Long.parseLong(p[2])))));
        }
        return sb.toString();
    }

    // 保存和加载背景颜色
    public static void saveBgColor(Context context, int color) {
        currentBgColor = color;
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putInt("bg_color", color).apply();
    }

    public static void loadSettings(Context context) {
        currentBgColor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt("bg_color", Color.parseColor("#FAF8EF"));
    }

    static class ScoreEntry {
        int score; String name; long time;
        ScoreEntry(int s, String n, long t) { score=s; name=n; time=t; }
        public String toString() { return score + "|" + name + "|" + time; }
    }
}