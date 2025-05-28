package com.example.uploadthingtest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class UploadThingService {

    @Value("${uploadthing.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String uploadFile(MultipartFile file) throws IOException {
        try {
            // Step 1: Get presigned URL from UploadThing
            String presignedUrlData = getPresignedUrl(file);
            JsonNode urlResponse = objectMapper.readTree(presignedUrlData);

            // Step 2: Upload file to presigned URL
            return uploadToPresignedUrl(urlResponse, file);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload interrupted", e);
        }
    }

    private String getPresignedUrl(MultipartFile file) throws IOException, InterruptedException {
        String fileName = UUID.randomUUID() +
                          Objects.requireNonNull(file.getOriginalFilename())
                                  .substring(file.getOriginalFilename().lastIndexOf("."));

        // Create the request body for getting presigned URL
        Map<String, Object> requestBody = new HashMap<>();

        List<Map<String, Object>> files = new ArrayList<>();
        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("name", fileName);
        fileInfo.put("size", file.getSize());
        fileInfo.put("type", file.getContentType());
        fileInfo.put("customId", null);
        files.add(fileInfo);

        requestBody.put("files", files);
        requestBody.put("acl", "public-read");
        requestBody.put("metadata", null);
        requestBody.put("contentDisposition", "inline");

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.uploadthing.com/v6/uploadFiles"))
                .header("X-Uploadthing-Api-Key", apiKey)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get presigned URL: " + response.body());
        }

        return response.body();
    }

    private String uploadToPresignedUrl(JsonNode urlResponse, MultipartFile file)
            throws IOException, InterruptedException {

        JsonNode dataArray = urlResponse.get("data");
        if (dataArray == null || !dataArray.isArray() || dataArray.isEmpty()) {
            throw new RuntimeException("Invalid response from UploadThing API");
        }

        JsonNode fileData = dataArray.get(0);
        String uploadUrl = fileData.get("url").asText();
        JsonNode fields = fileData.get("fields");

        // Create multipart form data
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString().replace("-", "");
        StringBuilder formData = new StringBuilder();

        // Add all the fields from UploadThing response
        if (fields != null) { // Assuming 'fields' is an ObjectNode
            fields.properties().forEach(entry -> {
                formData.append("--").append(boundary).append("\r\n");
                formData.append("Content-Disposition: form-data; name=\"")
                        .append(entry.getKey()).append("\"\r\n\r\n");
                formData.append(entry.getValue().asText()).append("\r\n");
            });
        }


        // Add the file
        formData.append("--").append(boundary).append("\r\n");
        formData.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(file.getOriginalFilename()).append("\"\r\n");
        formData.append("Content-Type: ").append(file.getContentType()).append("\r\n\r\n");

        // Convert to bytes and add file content
        byte[] formDataBytes = formData.toString().getBytes();
        byte[] fileBytes = file.getBytes();
        byte[] endBoundary = ("\r\n--" + boundary + "--\r\n").getBytes();

        byte[] requestBody = new byte[formDataBytes.length + fileBytes.length + endBoundary.length];
        System.arraycopy(formDataBytes, 0, requestBody, 0, formDataBytes.length);
        System.arraycopy(fileBytes, 0, requestBody, formDataBytes.length, fileBytes.length);
        System.arraycopy(endBoundary, 0, requestBody, formDataBytes.length + fileBytes.length, endBoundary.length);

        HttpRequest uploadRequest = HttpRequest.newBuilder()
                .uri(URI.create(uploadUrl))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .method("POST", HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();

        HttpResponse<String> uploadResponse = httpClient.send(uploadRequest, HttpResponse.BodyHandlers.ofString());

        if (uploadResponse.statusCode() < 200 || uploadResponse.statusCode() >= 300) {
            throw new RuntimeException("Failed to upload file: " + uploadResponse.body());
        }

        // Return the public URL (you might need to construct this based on UploadThing's response)
        String fileKey = fileData.get("key").asText();
        return "https://utfs.io/f/" + fileKey;
    }

    public void deleteFile(String fileKey) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("fileKeys", List.of(fileKey));

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.uploadthing.com/v6/deleteFiles"))
                .header("X-Uploadthing-Api-Key", apiKey)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to delete file: " + response.body());
        }
    }
}
