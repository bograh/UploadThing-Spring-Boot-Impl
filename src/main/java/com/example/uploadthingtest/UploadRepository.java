package com.example.uploadthingtest;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String> {
    @Modifying
    @Transactional
    @Query(value = """
        DELETE FROM files WHERE upload_id = :uploadId;
        DELETE FROM upload WHERE id = :uploadId;
        """, nativeQuery = true)
    void deleteUploadWithRelatedData(@Param("uploadId") String uploadId);
}
