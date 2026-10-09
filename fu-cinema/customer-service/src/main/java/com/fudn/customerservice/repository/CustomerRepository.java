package com.fudn.customerservice.repository;

import com.fudn.customerservice.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository cho Customer (TODO 2.2).
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndCustomerIdNot(String email, Long customerId);

    List<Customer> findByCustomerNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCustomerIdAsc(
            String customerName, String email);
}