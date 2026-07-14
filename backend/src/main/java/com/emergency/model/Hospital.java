package com.emergency.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "hospitals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private Double lat;

    @Column(nullable = false)
    private Double lng;

    private String phone;

    // Bed tracking — kept in sync via REST updates and cached in Redis
    @Column(nullable = false)
    @Builder.Default
    private Integer totalBeds = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer availableBeds = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer icuTotal = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer icuAvailable = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean traumaCenter = false;

    // JSON array of strings e.g. ["cardiac","stroke","burn"]
    @ElementCollection
    @CollectionTable(name = "hospital_specializations",
                     joinColumns = @JoinColumn(name = "hospital_id"))
    @Column(name = "specialization")
    @Builder.Default
    private List<String> specializations = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
