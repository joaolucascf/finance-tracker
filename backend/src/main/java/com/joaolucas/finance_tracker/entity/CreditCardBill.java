package com.joaolucas.finance_tracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_card_bill")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCardBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private FinancialAccount account;

    @Column(nullable = false)
    private String provider;

    /** Provider bill id; NULL for the open transition bill (the current cycle has no provider bill yet). */
    @Column(name = "external_bill_id")
    private String externalBillId;

    /** NULL only for a transition bill on a card that has no closed provider bill to reference yet. */
    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillStatus status;

    /** Sequential number among the user's closed bills; NULL for the transition bill. */
    @Column(name = "bill_sequence")
    private Integer sequence;

    @Column(name = "custom_name")
    private String customName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
