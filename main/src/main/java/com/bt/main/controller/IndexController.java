package com.bt.main.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class IndexController {

    @GetMapping(value = {"/", "/index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> index() throws IOException {
        Resource resource = new ClassPathResource("static/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }

    // Handle SPA routing - serve index.html for all frontend routes
    @GetMapping(value = {
        "/login", "/register", "/dashboard", "/profile", 
        "/products", "/products/**", "/fd-calculator", 
        "/accounts", "/accounts/**"
    }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> spaRoutes() throws IOException {
        Resource resource = new ClassPathResource("static/index.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(resource);
    }
}
