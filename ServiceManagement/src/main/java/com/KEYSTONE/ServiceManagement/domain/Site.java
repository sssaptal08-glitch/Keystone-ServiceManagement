package com.KEYSTONE.ServiceManagement.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String city;

    private String state;

    @Column(name = "postal_code")
    private String postalCode;
}
