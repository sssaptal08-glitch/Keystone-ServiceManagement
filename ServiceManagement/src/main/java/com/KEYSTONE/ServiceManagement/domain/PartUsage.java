package com.KEYSTONE.ServiceManagement.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "part_usages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(nullable = false)
    private Integer quantity;
}
