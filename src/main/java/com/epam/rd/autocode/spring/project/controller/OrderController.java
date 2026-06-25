package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.service.OrderService;
import com.epam.rd.autocode.spring.project.exception.NotEnoughMoneyException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public String checkout(Authentication authentication,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        var cart = CartController.getCart(session);
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        try {
            orderService.placeOrder(authentication.getName(), cart);
            CartController.clearCart(session);
            redirectAttributes.addFlashAttribute("checkoutSuccess", true);
        } catch (NotEnoughMoneyException ex) {
            redirectAttributes.addFlashAttribute("checkoutError", true);
        }
        return "redirect:/cart";
    }
}
