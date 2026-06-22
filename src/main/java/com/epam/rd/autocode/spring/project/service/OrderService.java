package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.*;

public interface OrderService {

    List<OrderDTO> getOrdersByClient(String clientEmail);

    Page<OrderDTO> getOrdersByClient(String clientEmail, Pageable pageable);

    List<OrderDTO> getOrdersByEmployee(String employeeEmail);

    Page<OrderDTO> getOrdersByEmployee(String employeeEmail, Pageable pageable);

    OrderDTO addOrder(OrderDTO order);
}
