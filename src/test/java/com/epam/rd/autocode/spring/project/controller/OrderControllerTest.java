package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.exception.NotEnoughMoneyException;
import com.epam.rd.autocode.spring.project.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private OrderController controller;

    @BeforeEach
    void setUp() {
        controller = new OrderController(orderService);
    }

    @Test
    void checkoutRedirectsToCartWhenCartIsEmpty() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        HttpSession session = new MockHttpSession();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.checkout(authentication, session, redirectAttributes);

        assertEquals("redirect:/cart", view);
        verify(orderService, org.mockito.Mockito.never()).placeOrder(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    void checkoutPlacesOrderAndClearsCart() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        MockHttpSession session = new MockHttpSession();
        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("Clean Code", 1);
        session.setAttribute(CartController.CART_SESSION_ATTRIBUTE, cart);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.checkout(authentication, session, redirectAttributes);

        assertEquals("redirect:/cart", view);
        assertTrue((Boolean) redirectAttributes.getFlashAttributes().get("checkoutSuccess"));
        assertFalse(session.getAttributeNames().hasMoreElements());
        verify(orderService).placeOrder("client@example.com", cart);
    }

    @Test
    void checkoutAddsErrorFlashWhenMoneyIsInsufficient() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("client@example.com", "pass");
        MockHttpSession session = new MockHttpSession();
        Map<String, Integer> cart = new LinkedHashMap<>();
        cart.put("Clean Code", 1);
        session.setAttribute(CartController.CART_SESSION_ATTRIBUTE, cart);
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        doThrow(new NotEnoughMoneyException("not enough")).when(orderService)
                .placeOrder("client@example.com", cart);

        String view = controller.checkout(authentication, session, redirectAttributes);

        assertEquals("redirect:/cart", view);
        assertTrue((Boolean) redirectAttributes.getFlashAttributes().get("checkoutError"));
        assertInstanceOf(Map.class, session.getAttribute(CartController.CART_SESSION_ATTRIBUTE));
    }
}
