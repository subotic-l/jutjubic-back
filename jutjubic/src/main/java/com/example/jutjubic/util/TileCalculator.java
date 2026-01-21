package com.example.jutjubic.util;

import com.example.jutjubic.dto.TileCoordinate;

public class TileCalculator {
    
    public static TileCoordinate getTileForLocation(double lat, double lon, int zoom) {
        int tiles = 1 << zoom;

        int x = (int) Math.floor((lon + 180.0) / 360.0 * tiles);

        double latRad = Math.toRadians(lat);
        int y = (int) Math.floor(
            (1.0 - Math.log(Math.tan(latRad) + 1 / Math.cos(latRad)) / Math.PI)
            / 2.0 * tiles
        );

        return new TileCoordinate(zoom, x, y);
    }
}
