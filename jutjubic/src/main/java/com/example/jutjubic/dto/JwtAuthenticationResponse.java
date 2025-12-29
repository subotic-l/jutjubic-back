package com.example.jutjubic.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthenticationResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private Long expiresIn;

    public JwtAuthenticationResponse(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
}
