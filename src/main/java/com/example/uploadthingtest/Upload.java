package com.example.uploadthingtest;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String Id;

    @Column(nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "files", joinColumns = @JoinColumn(name = "uploadId"))
    @Column(name = "fileUrl")
    private List<String> files;

    private LocalDateTime uploadedAt;
}
