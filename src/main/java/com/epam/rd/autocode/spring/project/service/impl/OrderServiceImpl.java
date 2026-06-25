package com.epam.rd.autocode.spring.project.service.impl;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.exception.NotEnoughMoneyException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.BookItem;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.model.Order;
import com.epam.rd.autocode.spring.project.model.enums.OrderStatus;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final BookRepository bookRepository;
    private final ModelMapper modelMapper;

    @Override
    @PreAuthorize("hasRole('CLIENT')")
    public List<OrderDTO> getOrdersByClient(String clientEmail) {
        return orderRepository.findByClient_EmailOrderByOrderDateDesc(clientEmail)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('CLIENT')")
    public Page<OrderDTO> getOrdersByClient(String clientEmail, Pageable pageable) {
        return orderRepository.findByClient_Email(clientEmail, pageable)
                .map(this::toDto);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<OrderDTO> getOrdersByEmployee(String employeeEmail) {
        return orderRepository.findByEmployee_EmailOrderByOrderDateDesc(employeeEmail)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Page<OrderDTO> getOrdersByEmployee(String employeeEmail, Pageable pageable) {
        return orderRepository.findByEmployee_Email(employeeEmail, pageable)
                .map(this::toDto);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::toDto);
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public Page<OrderDTO> searchOrdersByClient(String search, Pageable pageable) {
        if (!StringUtils.hasText(search)) {
            return getAllOrders(pageable);
        }
        return orderRepository.searchOrdersByClient(search.trim(), pageable)
                .map(this::toDto);
    }

    @Override
    public OrderDTO addOrder(OrderDTO order) {
        Order entity = modelMapper.map(order, Order.class);
        if (entity.getStatus() == null) {
            entity.setStatus(OrderStatus.PENDING);
        }
        Order saved = orderRepository.save(entity);

        return toDto(saved);
    }

    @Override
    @PreAuthorize("hasRole('CLIENT')")
    public OrderDTO placeOrder(String clientEmail, Map<String, Integer> cart) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        Client client = clientRepository.findByEmail(clientEmail)
                .orElseThrow(() -> new NotFoundException("Client not found by email: " + clientEmail));

        List<BookItem> bookItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Book book = bookRepository.findByName(entry.getKey())
                    .orElseThrow(() -> new NotFoundException("Book not found by name: " + entry.getKey()));

            Integer quantity = entry.getValue();
            BigDecimal linePrice = book.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(linePrice);
            bookItems.add(new BookItem(null, null, book, quantity));
        }

        if (client.getBalance().compareTo(total) < 0) {
            throw new NotEnoughMoneyException(
                    "Not enough money. Required: " + total + ", available: " + client.getBalance()
            );
        }

        log.debug("Client {} is placing order with total {}", clientEmail, total);
        Order order = new Order();
        order.setClient(client);
        order.setEmployee(null);
        order.setOrderDate(LocalDateTime.now());
        order.setPrice(total);
        order.setStatus(OrderStatus.PENDING);
        order.setBookItems(bookItems);
        bookItems.forEach(item -> item.setOrder(order));

        client.setBalance(client.getBalance().subtract(total));
        clientRepository.save(client);

        Order saved = orderRepository.save(order);
        OrderDTO result = toDto(saved);
        log.debug("Client {} placed order successfully with total {}", clientEmail, total);
        return result;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('EMPLOYEE')")
    public OrderDTO confirmOrder(String clientEmail, LocalDateTime orderDate, String employeeEmail) {
        Order order = orderRepository.findByClient_EmailAndOrderDate(clientEmail, orderDate)
                .orElseThrow(() -> new NotFoundException("Order not found by client and date: " + clientEmail));
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            return toDto(order);
        }
        if (order.getStatus() == OrderStatus.CANCELED) {
            return toDto(order);
        }
        Employee employee = employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found by email: " + employeeEmail));

        log.debug("Employee {} is confirming order for client {} at {}", employeeEmail, clientEmail, orderDate);
        order.setEmployee(employee);
        order.setStatus(OrderStatus.CONFIRMED);
        OrderDTO result = toDto(orderRepository.save(order));
        log.debug("Employee {} confirmed order for client {} at {} successfully", employeeEmail, clientEmail, orderDate);
        return result;
    }

    @Override
    @PreAuthorize("hasRole('EMPLOYEE')")
    public OrderDTO cancelOrder(String clientEmail, LocalDateTime orderDate, String employeeEmail) {
        Order order = orderRepository.findByClient_EmailAndOrderDate(clientEmail, orderDate)
                .orElseThrow(() -> new NotFoundException("Order not found by client and date: " + clientEmail));
        if (order.getStatus() != OrderStatus.PENDING) {
            return toDto(order);
        }
        employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found by email: " + employeeEmail));

        log.debug("Employee {} is canceling order for client {} at {}", employeeEmail, clientEmail, orderDate);
        OrderDTO result = cancelPendingOrder(order);
        log.debug("Employee {} canceled order for client {} at {} successfully", employeeEmail, clientEmail, orderDate);
        return result;
    }

    @Override
    @PreAuthorize("hasRole('CLIENT')")
    public OrderDTO cancelClientOrder(String clientEmail, LocalDateTime orderDate, String currentClientEmail) {
        Order order = orderRepository.findByClient_EmailAndOrderDate(clientEmail, orderDate)
                .orElseThrow(() -> new NotFoundException("Order not found by client and date: " + clientEmail));
        if (order.getStatus() != OrderStatus.PENDING) {
            return toDto(order);
        }

        if (!order.getClient().getEmail().equals(currentClientEmail)) {
            throw new NotFoundException("Order not found by client and date: " + clientEmail);
        }

        log.debug("Client {} is canceling own order at {}", currentClientEmail, orderDate);
        OrderDTO result = cancelPendingOrder(order);
        log.debug("Client {} canceled own order at {} successfully", currentClientEmail, orderDate);
        return result;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('EMPLOYEE')")
    public void deleteOrder(String clientEmail, LocalDateTime orderDate, String employeeEmail) {
        Order order = orderRepository.findByClient_EmailAndOrderDate(clientEmail, orderDate)
                .orElseThrow(() -> new NotFoundException("Order not found by client and date: " + clientEmail));
        employeeRepository.findByEmail(employeeEmail)
                .orElseThrow(() -> new NotFoundException("Employee not found by email: " + employeeEmail));

        log.debug("Employee {} is deleting order for client {} at {}", employeeEmail, clientEmail, orderDate);
        refundIfPending(order);
        orderRepository.delete(order);
        log.debug("Employee {} deleted order for client {} at {} successfully", employeeEmail, clientEmail, orderDate);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('CLIENT')")
    public void deleteClientOrder(String clientEmail, LocalDateTime orderDate, String currentClientEmail) {
        Order order = orderRepository.findByClient_EmailAndOrderDate(clientEmail, orderDate)
                .orElseThrow(() -> new NotFoundException("Order not found by client and date: " + clientEmail));

        if (!order.getClient().getEmail().equals(currentClientEmail)) {
            throw new NotFoundException("Order not found by client and date: " + clientEmail);
        }

        log.debug("Client {} is deleting own order at {}", currentClientEmail, orderDate);
        refundIfPending(order);
        orderRepository.delete(order);
        log.debug("Client {} deleted own order at {} successfully", currentClientEmail, orderDate);
    }

    private OrderDTO cancelPendingOrder(Order order) {
        refundIfPending(order);
        order.setStatus(OrderStatus.CANCELED);
        order.setEmployee(null);
        return toDto(orderRepository.save(order));
    }

    private void refundIfPending(Order order) {
        if (order.getStatus() != OrderStatus.PENDING) {
            return;
        }

        Client client = order.getClient();
        client.setBalance(client.getBalance().add(order.getPrice()));
        clientRepository.save(client);
    }

    private OrderDTO toDto(Order order) {
        List<BookItemDTO> items = order.getBookItems().stream()
                .map(item -> new BookItemDTO(item.getBook().getName(), item.getQuantity()))
                .toList();

        return new OrderDTO(
                order.getClient().getEmail(),
                order.getEmployee() != null ? order.getEmployee().getEmail() : null,
                order.getOrderDate(),
                order.getPrice(),
                order.getStatus() != null ? order.getStatus().name() : null,
                items
        );
    }
}
