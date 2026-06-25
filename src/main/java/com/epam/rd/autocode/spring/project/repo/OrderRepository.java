package com.epam.rd.autocode.spring.project.repo;

import com.epam.rd.autocode.spring.project.model.Order;
import com.epam.rd.autocode.spring.project.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByClient_Email(String clientEmail);

    List<Order> findByClient_EmailOrderByOrderDateDesc(String clientEmail);

    Page<Order> findByClient_Email(String clientEmail, Pageable pageable);

    Optional<Order> findByClient_EmailAndOrderDate(String clientEmail, LocalDateTime orderDate);

    List<Order> findByEmployee_Email(String employeeEmail);

    List<Order> findByEmployee_EmailOrderByOrderDateDesc(String employeeEmail);

    Page<Order> findByEmployee_Email(String employeeEmail, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("""
            SELECT o
            FROM Order o
            WHERE LOWER(o.client.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(o.client.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Order> searchOrdersByClient(@Param("search") String search, Pageable pageable);
}
