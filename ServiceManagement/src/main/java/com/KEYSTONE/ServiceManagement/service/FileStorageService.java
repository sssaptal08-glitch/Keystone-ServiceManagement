package com.KEYSTONE.ServiceManagement.service;

import com.KEYSTONE.ServiceManagement.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stores uploaded work order attachments (photos, documents) on local disk under
 * app.upload.dir. Filenames are randomized to prevent collisions and path traversal;
 * the original filename is preserved separately in the database for display/download.
 *
 * For a larger production deployment this would be swapped for S3/GCS-backed storage,
 * but the local-disk approach keeps the Docker Compose setup dependency-free.
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory: " + uploadRoot, e);
        }
    }

    public String store(MultipartFile file) {
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }
        String storedFilename = UUID.randomUUID() + extension;

        try {
            Path target = uploadRoot.resolve(storedFilename).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Invalid file path");
            }
            Files.copy(file.getInputStream(), target);
            return storedFilename;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file " + original, e);
        }
    }

    public Resource loadAsResource(String storedFilename) {
        try {
            Path file = uploadRoot.resolve(storedFilename).normalize();
            if (!file.startsWith(uploadRoot)) {
                throw new NotFoundException("File not found: " + storedFilename);
            }
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("File not found: " + storedFilename);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("File not found: " + storedFilename);
        }
    }

    public void delete(String storedFilename) {
        try {
            Path file = uploadRoot.resolve(storedFilename).normalize();
            if (file.startsWith(uploadRoot)) {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            log.warn("Failed to delete stored file {}", storedFilename, e);
        }
    }
}
