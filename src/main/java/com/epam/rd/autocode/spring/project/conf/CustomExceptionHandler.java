package com.epam.rd.autocode.spring.project.conf;

import com.epam.rd.autocode.spring.project.exception.AlreadyExistException;
import com.epam.rd.autocode.spring.project.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@Slf4j
public class CustomExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException ex,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        return buildErrorView(HttpStatus.NOT_FOUND, ex.getMessage(), request, response);
    }

    @ExceptionHandler(AlreadyExistException.class)
    public ModelAndView handleAlreadyExists(AlreadyExistException ex,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        return buildErrorView(HttpStatus.CONFLICT, ex.getMessage(), request, response);
    }

    private ModelAndView buildErrorView(HttpStatus status,
                                        String message,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        log.warn("Handled {} for path {}: {}", status.value(), request.getRequestURI(), message);
        response.setStatus(status.value());

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("path", request.getRequestURI());
        return mav;
    }
}
