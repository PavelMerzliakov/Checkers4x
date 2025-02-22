package com.example.checkers4x;

public class Triangle {
    public static final int TYPE_ALTAR = 1;
    public static final int TYPE_CAGE = 2; // Загон
    
    private final int playerId; // 1-WR, 2-BR, 3-WF, 4-BF
    private final int type; // ALTAR или CAGE
    private final int position; // 1-8 (номер треугольника)
    
    public Triangle(int playerId, int type, int position) {
        this.playerId = playerId;
        this.type = type;
        this.position = position;
    }
    
    public int getPlayerId() {
        return playerId;
    }
    
    public int getType() {
        return type;
    }
    
    public int getPosition() {
        return position;
    }
    
    public boolean isAltar() {
        return type == TYPE_ALTAR;
    }
    
    public boolean isCage() {
        return type == TYPE_CAGE;
    }
} 