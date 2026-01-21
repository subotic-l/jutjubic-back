package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MapBoundsRequest {
    private Double minLatitude;
    private Double maxLatitude;
    private Double minLongitude;
    private Double maxLongitude;
}
