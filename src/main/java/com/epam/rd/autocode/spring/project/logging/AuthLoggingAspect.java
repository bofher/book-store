package com.epam.rd.autocode.spring.project.logging;

import com.epam.rd.autocode.spring.project.dto.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class AuthLoggingAspect {

    @AfterReturning(
            pointcut = "execution(* com.epam.rd.autocode.spring.project.controller.AuthController.login(..))" +
                    " && args(loginRequest, ..)",
            returning = "result",
            argNames = "loginRequest,result")
    public void logLogin(LoginRequest loginRequest, Object result) {
        if ("redirect:/".equals(result)) {
            log.info("User logged in: {}", loginRequest.getEmail());
        }
    }

    @Before("execution(* com.epam.rd.autocode.spring.project.controller.AuthController.logout(..))")
    public void logLogout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication != null ? authentication.getName() : "unknown";
        log.info("User logged out: {}", email);
    }
}
