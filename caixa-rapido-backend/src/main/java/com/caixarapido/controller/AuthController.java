package com.caixarapido.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if ("admin".equals(username) && "1234".equals(password)) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Login bem-sucedido!"));
        } else {
            return ResponseEntity.status(401).body(Map.of("status", "error", "message", "Usuário ou senha incorretos"));
        }
    }
}
