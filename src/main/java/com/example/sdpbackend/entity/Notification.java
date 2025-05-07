package com.example.sdpbackend.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = true)
    private String userType;

    @Column(columnDefinition = "TEXT")
    private String subscription;

    @ElementCollection
    @CollectionTable(name = "notification_records",
            joinColumns = @JoinColumn(name = "notification_id"))
    private List<NotificationRecord> notificationRecords = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class NotificationRecord {
        private String type;
        private String title;
        private String body;
        private String orderId;

        @Column(name = "`read`") // Escape the reserved keyword
        private boolean read;

        private LocalDateTime createdAt;
        private LocalDateTime readAt;
    }
}
