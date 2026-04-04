package com.finresearch.service;

import com.finresearch.config.FinResearchProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FinResearchProperties props;
    private final EncryptionService encryptionService;

    public Path userDir(Long userId) throws IOException {
        Path base = Path.of(props.getDataDir(), "uploads", String.valueOf(userId));
        Files.createDirectories(base);
        return base;
    }

    /** 原始文件加密落盘，返回相对路径标识 */
    public String saveEncrypted(Long userId, MultipartFile file) throws IOException {
        Path dir = userDir(userId);
        String name = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        Path target = dir.resolve(name);
        byte[] plain = file.getBytes();
        byte[] enc = encryptionService.encrypt(plain);
        Files.write(target, enc);
        return target.toString();
    }

    public byte[] readEncryptedAbsolutePath(String absolutePath) throws IOException {
        byte[] enc = Files.readAllBytes(Path.of(absolutePath));
        return encryptionService.decrypt(enc);
    }

    public String savePlainInUserDir(Long userId, String suffix, byte[] plain) throws IOException {
        Path dir = userDir(userId);
        String name = UUID.randomUUID() + suffix;
        Path target = dir.resolve(name);
        byte[] enc = encryptionService.encrypt(plain);
        Files.write(target, enc);
        return target.toString();
    }

    private static String sanitize(String name) {
        if (name == null) {
            return "file";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
