package com.ubique.prayer.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/kakao")
    public ResponseEntity<Void> startKakaoLogin() {

        System.out.println("AuthController.startKakaoLogin START");

        URI authorizationUri = URI.create("/oauth2/authorization/kakao");

        // Spring Security의 OAuth2 시작 경로로 브라우저를 리다이렉트
        // client-id와 callback URI를 사용한 인가 URL 생성은 Spring Security Filter가 처리
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(authorizationUri)
            .build();
    }
}
