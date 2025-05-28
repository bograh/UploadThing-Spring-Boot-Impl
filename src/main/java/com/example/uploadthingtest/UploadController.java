package com.example.uploadthingtest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    @PostMapping(consumes = {"multipart/form-data"}, path = "")
    public ResponseEntity<?> createUpload(
            @RequestParam("name") String name,
            @RequestParam("files") List<MultipartFile> files) {

        try {
            if (name == null || name.trim().isEmpty() || files == null || files.isEmpty()) {
                return new ResponseEntity<>("All fields are required.",
                        HttpStatus.BAD_REQUEST);
            }

            // Pass the data to the service layer
            UploadResponseDTO newUpload = uploadService.createUpload(name, files);
            return new ResponseEntity<>(newUpload, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping
    public ResponseEntity<List<Upload>> getUploads() {
        List<Upload> uploads = uploadService.getUploads();

        return new ResponseEntity<>(uploads, HttpStatus.OK);
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<?> deleteUpload(@PathVariable String uploadId) throws IOException, InterruptedException {
        uploadService.deleteUpload(uploadId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
