package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByClient_Email(String clientEmail);

    Page<Order> findByClient_Email(String clientEmail, Pageable pageable);

    List<Order> findByEmployee_Email(String employeeEmail);

    Page<Order> findByEmployee_Email(String employeeEmail, Pageable pageable);
}
