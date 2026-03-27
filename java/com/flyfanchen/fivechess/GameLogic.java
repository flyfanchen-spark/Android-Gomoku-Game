package com.flyfanchen.fivechess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class GameLogic {
    // 1. BFS 算法判断路径是否存在
    public static boolean hasPath(int[][] board, int startX, int startY, int endX, int endY, int size) {
        if (startX == endX && startY == endY) return true;
        boolean[][] visited = new boolean[size][size];
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        visited[startX][startY] = true;

        int[][] dirs = {{0,1}, {0,-1}, {1,0}, {-1,0}};

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            for (int[] d : dirs) {
                int nx = curr[0] + d[0];
                int ny = curr[1] + d[1];
                if (nx >= 0 && nx < size && ny >= 0 && ny < size && board[nx][ny] == 0 && !visited[nx][ny]) {
                    if (nx == endX && ny == endY) return true;
                    visited[nx][ny] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }
        return false;
    }
    // 将原来的 hasPath 替换为 findPath
    public static List<int[]> findPath(int[][] board, int startX, int startY, int endX, int endY, int size) {
        int[][] parentX = new int[size][size];
        int[][] parentY = new int[size][size];
        for (int[] row : parentX) Arrays.fill(row, -1);

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{startX, startY});
        boolean[][] visited = new boolean[size][size];
        visited[startX][startY] = true;

        int[][] dirs = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};
        boolean found = false;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            if (curr[0] == endX && curr[1] == endY) {
                found = true;
                break;
            }
            for (int[] d : dirs) {
                int nx = curr[0] + d[0], ny = curr[1] + d[1];
                if (nx >= 0 && nx < size && ny >= 0 && ny < size && board[nx][ny] == 0 && !visited[nx][ny]) {
                    visited[nx][ny] = true;
                    parentX[nx][ny] = curr[0];
                    parentY[nx][ny] = curr[1];
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        if (!found) return null;

        // 回溯路径
        List<int[]> path = new ArrayList<>();
        int cx = endX, cy = endY;
        while (cx != -1) {
            path.add(0, new int[]{cx, cy});
            int tempX = parentX[cx][cy];
            int tempY = parentY[cx][cy];
            cx = tempX; cy = tempY;
        }
        return path;
    }

    // 2. 积分计算公式: 5+2+4+6...
    public static int calculateScore(int count) {
        if (count < 5) return count; // 用户要求的特殊规则
        int total = 5;
        for (int i = 1; i <= (count - 5); i++) {
            total += (2 * i);
        }
        return total;
    }
}