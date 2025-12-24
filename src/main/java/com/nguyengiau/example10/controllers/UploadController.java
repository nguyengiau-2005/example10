package com.nguyengiau.example10.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.nio.file.*;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:3000")
public class UploadController {

    private final String UPLOAD_DIR = "uploads/images";

    @PostMapping("/images")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            Files.write(filePath, file.getBytes());

            String imageUrl = "http://localhost:8080/images/" + fileName; // URL đầy đủ
            return ResponseEntity.ok(Map.of("url", imageUrl));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Upload thất bại: " + e.getMessage()));
        }
    }
}
