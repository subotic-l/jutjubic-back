package com.example.jutjubic.util;

public class TileRange {
    private final int xStart;
    private final int xEnd;
    private final int yStart;
    private final int yEnd;

    public TileRange(int xStart, int xEnd, int yStart, int yEnd) {
        this.xStart = xStart;
        this.xEnd = xEnd;
        this.yStart = yStart;
        this.yEnd = yEnd;
    }

    public int getXStart() { return xStart; }
    public int getXEnd()   { return xEnd; }
    public int getYStart() { return yStart; }
    public int getYEnd()   { return yEnd; }
}