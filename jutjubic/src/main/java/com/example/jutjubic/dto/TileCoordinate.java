package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TileCoordinate {
    private Integer zoom;
    private Integer x;
    private Integer y;
    
    public String toKey() {
        return String.format("%d/%d/%d", zoom, x, y);
    }
    
    public static TileCoordinate fromKey(String key) {
        String[] parts = key.split("/");
        return new TileCoordinate(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])
        );
    }
}
