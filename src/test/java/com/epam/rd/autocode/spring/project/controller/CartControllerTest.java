package com.epam.rd.autocode.spring.project.controller;

import com.epam.rd.autocode.spring.project.dto.BookDTO;
import com.epam.rd.autocode.spring.project.model.enums.AgeGroup;
import com.epam.rd.autocode.spring.project.model.enums.Language;
import com.epam.rd.autocode.spring.project.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private BookService bookService;

    private CartController controller;

    @BeforeEach
    void setUp() {
        controller = new CartController(bookService);
    }

    @Test
    void addToCartCreatesSessionCartAndMergesQuantity() {
        MockHttpSession session = new MockHttpSession();

        String view = controller.addToCart("Clean Code", session, -2);
        controller.addToCart("Clean Code", session, 3);

        assertEquals("redirect:/cart", view);
        assertEquals(4, CartController.getCart(session).get("Clean Code"));
    }

    @Test
    void cartCalculatesTotal() {
        MockHttpSession session = new MockHttpSession();
        Map<String, Integer> cart = CartController.getCart(session);
        cart.put("Clean Code", 2);

        when(bookService.getBookByName("Clean Code")).thenReturn(book("Clean Code", new BigDecimal("10.00")));

        Model model = new ExtendedModelMap();
        String view = controller.cart(cart, model);

        assertEquals("cart", view);
        assertEquals(new BigDecimal("20.00"), model.getAttribute("total"));
        assertEquals(1, ((java.util.List<?>) model.getAttribute("lines")).size());
        verify(bookService).getBookByName("Clean Code");
    }

    private static BookDTO book(String name, BigDecimal price) {
        return new BookDTO(
                name,
                "Programming",
                AgeGroup.ADULT,
                price,
                LocalDate.of(2020, 1, 1),
                "Author",
                100,
                "Hardcover",
                "Description",
                Language.ENGLISH
        );
    }
}
