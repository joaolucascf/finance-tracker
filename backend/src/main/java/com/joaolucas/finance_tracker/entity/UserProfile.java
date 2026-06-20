package com.joaolucas.finance_tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String nickname;

    private LocalDate birthDate;

    private BigDecimal monthlyIncome;

    @Column(length = 20)
    private String maritalStatus;

    @Column(columnDefinition = "BYTEA")
    private byte[] photo;

    @Column(name = "photo_type", length = 50)
    private String photoType;
}
