package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

public interface OrderService {

    List<OrderDTO> getOrdersByClient(String clientEmail);

    Page<OrderDTO> getOrdersByClient(String clientEmail, Pageable pageable);

    List<OrderDTO> getOrdersByEmployee(String employeeEmail);

    Page<OrderDTO> getOrdersByEmployee(String employeeEmail, Pageable pageable);

    List<OrderDTO> getAllOrders();

    Page<OrderDTO> getAllOrders(Pageable pageable);

    Page<OrderDTO> searchOrdersByClient(String search, Pageable pageable);

    OrderDTO addOrder(OrderDTO order);

    OrderDTO placeOrder(String clientEmail, Map<String, Integer> cart);

    OrderDTO confirmOrder(String clientEmail, LocalDateTime orderDate, String employeeEmail);

    OrderDTO cancelOrder(String clientEmail, LocalDateTime orderDate, String employeeEmail);

    OrderDTO cancelClientOrder(String clientEmail, LocalDateTime orderDate, String currentClientEmail);

    void deleteOrder(String clientEmail, LocalDateTime orderDate, String employeeEmail);

    void deleteClientOrder(String clientEmail, LocalDateTime orderDate, String currentClientEmail);
}
