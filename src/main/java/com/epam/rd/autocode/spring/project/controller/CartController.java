package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.service.BookService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cart")
public class CartController {

    public static final String CART_SESSION_ATTRIBUTE = "cart";
    private final BookService bookService;

    @ModelAttribute("cart")
    public Map<String, Integer> cart(HttpSession session) {
        return getCart(session);
    }

    @GetMapping
    public String cart(@ModelAttribute("cart") Map<String, Integer> cart, Model model) {
        List<Map.Entry<BookDTO, Integer>> lines = cart.entrySet().stream()
                .map(entry -> {
                    BookDTO book = bookService.getBookByName(entry.getKey());
                    return Map.entry(book, entry.getValue());
                })
                .toList();

        BigDecimal total = lines.stream()
                .map(line -> line.getKey().getPrice().multiply(BigDecimal.valueOf(line.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("lines", lines);
        model.addAttribute("total", total);
        return "cart";
    }

    @PostMapping("/items/{name}")
    public String addToCart(@PathVariable String name,
                            HttpSession session,
                            @RequestParam(defaultValue = "1") int quantity) {
        int itemsToAdd = Math.max(quantity, 1);
        getCart(session).merge(name, itemsToAdd, Integer::sum);
        return "redirect:/cart";
    }


    public static Map<String, Integer> getCart(HttpSession session) {
        Map<String, Integer> cart = (Map<String, Integer>) session.getAttribute(CART_SESSION_ATTRIBUTE);
        if (cart == null) {
            cart = new LinkedHashMap<>();
            session.setAttribute(CART_SESSION_ATTRIBUTE, cart);
        }
        return cart;
    }

    public static void clearCart(HttpSession session) {
        session.removeAttribute(CART_SESSION_ATTRIBUTE);
    }
}
