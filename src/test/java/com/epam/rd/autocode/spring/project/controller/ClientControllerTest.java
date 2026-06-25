package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.OrderDTO;
import com.epam.rd.autocode.spring.project.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientControllerTest {

    @Mock
    private OrderService orderService;

    private ClientController controller;

    @BeforeEach
    void setUp() {
        controller = new ClientController(orderService);
    }

    @Test
    void myOrdersAddsOrdersToModel() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        Model model = new ExtendedModelMap();

        when(orderService.getOrdersByClient("client@example.com")).thenReturn(List.<OrderDTO>of());

        String view = controller.myOrders(authentication, model);

        assertEquals("client-orders", view);
        assertEquals(List.of(), model.getAttribute("orders"));
    }

    @Test
    void cancelOrderRedirectsAndAddsFlashAttribute() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        LocalDateTime orderDate = LocalDateTime.of(2026, 6, 25, 10, 0);

        String view = controller.cancelOrder(orderDate, authentication, redirectAttributes);

        assertEquals("redirect:/client/orders", view);
        assertTrue((Boolean) redirectAttributes.getFlashAttributes().get("orderCanceled"));
        verify(orderService).cancelClientOrder("client@example.com", orderDate, "client@example.com");
    }
}
