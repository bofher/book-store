package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/client")
public class ClientController {

    private final OrderService orderService;

    @GetMapping("/orders")
    public String myOrders(Authentication authentication, Model model) {
        model.addAttribute("orders", orderService.getOrdersByClient(authentication.getName()));
        return "client-orders";
    }

    @PostMapping("/orders/cancel")
    public String cancelOrder(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime orderDate,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        orderService.cancelClientOrder(authentication.getName(), orderDate, authentication.getName());
        redirectAttributes.addFlashAttribute("orderCanceled", true);
        return "redirect:/client/orders";
    }

    @PostMapping("/orders/delete")
    public String deleteOrder(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime orderDate,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        orderService.deleteClientOrder(authentication.getName(), orderDate, authentication.getName());
        redirectAttributes.addFlashAttribute("orderDeleted", true);
        return "redirect:/client/orders";
    }
}
