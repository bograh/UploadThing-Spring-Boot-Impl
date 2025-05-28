package com.example.uploadthingtest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class UploadService {

    private final UploadThingService uploadThingService;
    private final UploadRepository uploadRepository;

    public List<Upload> getUploads() {
        return uploadRepository.findAll();
    }

    public UploadResponseDTO createUpload(String name, List<MultipartFile> files) throws IOException, InterruptedException {
        List<String> fileUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileUrl = uploadThingService.uploadFile(file);
            fileUrls.add(fileUrl);
        }

        LocalDateTime now = LocalDateTime.now();
        Upload upload = new Upload();
        upload.setName(name);
        upload.setFiles(fileUrls);
        upload.setUploadedAt(now);
        uploadRepository.save(upload);

        UploadResponseDTO uploadResponseDTO = new UploadResponseDTO();
        uploadResponseDTO.setUploadId(upload.getId());
        uploadResponseDTO.setStatus("success");
        uploadResponseDTO.setName(name);
        uploadResponseDTO.setUploadUrls(fileUrls);
        uploadResponseDTO.setUploadedAt(now);
        return uploadResponseDTO;
    }

    public void deleteUpload(String uploadId) throws IOException, InterruptedException {
        Upload upload = uploadRepository.findById(uploadId).orElse(null);
        if(upload != null) {
            if(upload.getFiles().isEmpty()) {
                uploadRepository.delete(upload);
                throw new  RuntimeException("No upload files found");
            }
            for (String fileUrl : upload.getFiles()) {
                uploadThingService.deleteFile(fileUrl);
            }
            uploadRepository.delete(upload);
//            uploadRepository.deleteUploadWithRelatedData(upload.getId());
        }

    }

}
