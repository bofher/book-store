package com.epam.rd.autocode.spring.project.service;

import com.epam.rd.autocode.spring.project.dto.BookItemDTO;
import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.exception.NotEnoughMoneyException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import com.epam.rd.autocode.spring.project.model.Book;
import com.epam.rd.autocode.spring.project.model.BookItem;
import com.epam.rd.autocode.spring.project.model.Client;
import com.epam.rd.autocode.spring.project.model.Employee;
import com.epam.rd.autocode.spring.project.model.Order;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.model.enums.OrderStatus;
import com.epam.rd.autocode.spring.project.repo.BookRepository;
import com.epam.rd.autocode.spring.project.repo.ClientRepository;
import com.epam.rd.autocode.spring.project.repo.EmployeeRepository;
import com.epam.rd.autocode.spring.project.repo.OrderRepository;
import com.epam.rd.autocode.spring.project.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BookRepository bookRepository;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(
                orderRepository,
                clientRepository,
                employeeRepository,
                bookRepository,
                new ModelMapper()
        );
    }

    @Test
    void getOrdersByClientReturnsMappedDtos() {
        when(orderRepository.findByClient_EmailOrderByOrderDateDesc("client@example.com"))
                .thenReturn(List.of(order(OrderStatus.PENDING)));

        List<OrderDTO> result = orderService.getOrdersByClient("client@example.com");

        assertEquals(1, result.size());
        assertEquals("client@example.com", result.get(0).getClientEmail());
    }

    @Test
    void getOrdersByClientPageReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 5);
        when(orderRepository.findByClient_Email("client@example.com", pageable))
                .thenReturn(new PageImpl<>(List.of(order(OrderStatus.PENDING))));

        Page<OrderDTO> result = orderService.getOrdersByClient("client@example.com", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrdersByEmployeeReturnsMappedDtos() {
        when(orderRepository.findByEmployee_EmailOrderByOrderDateDesc("employee@example.com"))
                .thenReturn(List.of(order(OrderStatus.CONFIRMED)));

        List<OrderDTO> result = orderService.getOrdersByEmployee("employee@example.com");

        assertEquals("employee@example.com", result.get(0).getEmployeeEmail());
    }

    @Test
    void getAllOrdersReturnsMappedDtos() {
        when(orderRepository.findAll()).thenReturn(List.of(order(OrderStatus.PENDING)));

        List<OrderDTO> result = orderService.getAllOrders();

        assertEquals(1, result.size());
    }

    @Test
    void searchOrdersByClientWithoutTextFallsBackToFindAll() {
        Pageable pageable = PageRequest.of(0, 5);
        when(orderRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(order(OrderStatus.PENDING))));

        Page<OrderDTO> result = orderService.searchOrdersByClient("  ", pageable);

        assertEquals(1, result.getContent().size());
        verify(orderRepository).findAll(pageable);
    }

    @Test
    void addOrderSetsPendingStatusWhenMissing() {
        OrderDTO input = new OrderDTO("client@example.com", null, LocalDateTime.now(), new BigDecimal("20.00"), null, List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO result = orderService.addOrder(input);

        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void placeOrderCreatesPendingOrderAndSubtractsBalance() {
        Client client = client(new BigDecimal("100.00"));
        Book book = book("Dune", new BigDecimal("25.00"));
        when(clientRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client));
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book));
        when(clientRepository.save(client)).thenReturn(client);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO result = orderService.placeOrder("client@example.com", Map.of("Dune", 2));

        assertEquals("PENDING", result.getStatus());
        assertEquals(new BigDecimal("50.00"), result.getPrice());
        assertEquals(new BigDecimal("50.00"), client.getBalance());
        assertEquals(1, result.getBookItems().size());
    }

    @Test
    void placeOrderThrowsWhenClientHasNotEnoughMoney() {
        when(clientRepository.findByEmail("client@example.com")).thenReturn(Optional.of(client(new BigDecimal("10.00"))));
        when(bookRepository.findByName("Dune")).thenReturn(Optional.of(book("Dune", new BigDecimal("25.00"))));

        assertThrows(NotEnoughMoneyException.class, () -> orderService.placeOrder("client@example.com", Map.of("Dune", 1)));
    }

    @Test
    void placeOrderThrowsWhenCartEmpty() {
        assertThrows(IllegalArgumentException.class, () -> orderService.placeOrder("client@example.com", Map.of()));
    }

    @Test
    void confirmOrderUpdatesPendingOrder() {
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);
        Order order = order(OrderStatus.PENDING);
        order.setOrderDate(orderDate);
        when(orderRepository.findByClient_EmailAndOrderDate("client@example.com", orderDate)).thenReturn(Optional.of(order));
        when(employeeRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee()));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDTO result = orderService.confirmOrder("client@example.com", orderDate, "employee@example.com");

        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("employee@example.com", result.getEmployeeEmail());
    }

    @Test
    void confirmOrderReturnsCurrentDtoWhenAlreadyCanceled() {
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);
        Order order = order(OrderStatus.CANCELED);
        order.setOrderDate(orderDate);
        when(orderRepository.findByClient_EmailAndOrderDate("client@example.com", orderDate)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.confirmOrder("client@example.com", orderDate, "employee@example.com");

        assertEquals("CANCELED", result.getStatus());
    }

    @Test
    void cancelOrderRefundsBalanceAndClearsEmployee() {
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);
        Order order = order(OrderStatus.PENDING);
        order.setOrderDate(orderDate);
        order.setEmployee(employee());
        Client client = order.getClient();
        client.setBalance(new BigDecimal("40.00"));
        when(orderRepository.findByClient_EmailAndOrderDate("client@example.com", orderDate)).thenReturn(Optional.of(order));
        when(employeeRepository.findByEmail("employee@example.com")).thenReturn(Optional.of(employee()));
        when(clientRepository.save(client)).thenReturn(client);
        when(orderRepository.save(order)).thenReturn(order);

        OrderDTO result = orderService.cancelOrder("client@example.com", orderDate, "employee@example.com");

        assertEquals("CANCELED", result.getStatus());
        assertNull(result.getEmployeeEmail());
        assertEquals(new BigDecimal("90.00"), client.getBalance());
    }

    @Test
    void cancelClientOrderThrowsForForeignClient() {
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);
        Order order = order(OrderStatus.PENDING);
        order.setOrderDate(orderDate);
        when(orderRepository.findByClient_EmailAndOrderDate("client@example.com", orderDate)).thenReturn(Optional.of(order));

        assertThrows(NotFoundException.class,
                () -> orderService.cancelClientOrder("client@example.com", orderDate, "other@example.com"));
    }

    @Test
    void cancelClientOrderRefundsOwnPendingOrder() {
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);
        Order order = order(OrderStatus.PENDING);
        order.setOrderDate(orderDate);
        Client client = order.getClient();
        client.setBalance(new BigDecimal("10.00"));
        when(orderRepository.findByClient_EmailAndOrderDate("client@example.com", orderDate)).thenReturn(Optional.of(order));
        when(clientRepository.save(client)).thenReturn(client);
        when(orderRepository.save(order)).thenReturn(order);

        OrderDTO result = orderService.cancelClientOrder("client@example.com", orderDate, "client@example.com");

        assertEquals("CANCELED", result.getStatus());
        assertEquals(new BigDecimal("60.00"), client.getBalance());
    }

    @Test
    void searchOrdersByClientWithTextUsesSearchRepositoryMethod() {
        Pageable pageable = PageRequest.of(0, 5);
        when(orderRepository.searchOrdersByClient("alice", pageable))
                .thenReturn(new PageImpl<>(List.of(order(OrderStatus.PENDING))));

        Page<OrderDTO> result = orderService.searchOrdersByClient(" alice ", pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void cancelOrderReturnsCurrentDtoWhenOrderNotPending() {
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);
        Order order = order(OrderStatus.CONFIRMED);
        order.setOrderDate(orderDate);
        when(orderRepository.findByClient_EmailAndOrderDate("client@example.com", orderDate)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.cancelOrder("client@example.com", orderDate, "employee@example.com");

        assertEquals("CONFIRMED", result.getStatus());
    }

    private static Client client(BigDecimal balance) {
        return new Client(1L, "client@example.com", "pass", "Client", balance);
    }

    private static Employee employee() {
        return new Employee(1L, "employee@example.com", "pass", "Employee", "+380000000000", LocalDate.of(1990, 1, 1));
    }

    private static Book book(String name, BigDecimal price) {
        return new Book(
                1L,
                name,
                "Novel",
                AgeGroup.ADULT,
                price,
                LocalDate.of(2020, 1, 1),
                "Author",
                200,
                "Hardcover",
                "Description",
                Language.ENGLISH
        );
    }

    private static Order order(OrderStatus status) {
        Client client = client(new BigDecimal("100.00"));
        Employee employee = employee();
        Book book = book("Dune", new BigDecimal("50.00"));
        Order order = new Order();
        order.setId(1L);
        order.setClient(client);
        order.setEmployee(employee);
        order.setOrderDate(LocalDateTime.of(2026, 6, 25, 10, 0));
        order.setPrice(new BigDecimal("50.00"));
        order.setStatus(status);
        BookItem item = new BookItem(1L, order, book, 1);
        order.setBookItems(List.of(item));
        return order;
    }
}
