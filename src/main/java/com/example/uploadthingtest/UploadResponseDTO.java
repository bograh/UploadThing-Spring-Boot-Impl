package com.example.uploadthingtest;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class UploadResponseDTO {
    private String status;
    private String uploadId;
    private String name;
    private List<String> uploadUrls;
    private LocalDateTime uploadedAt;
}
