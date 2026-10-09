package com.fudn.customerservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * JPA Entity mapping bảng 'customer' trong SQL Server (TODO 2.2).
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "telephone", length = 15)
    private String telephone;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "customer_birthday")
    private LocalDate customerBirthday;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_status", nullable = false, length = 20)
    private CustomerStatus customerStatus;

    @Column(name = "password", nullable = false, length = 100)
    private String password;
}