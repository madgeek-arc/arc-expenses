package arc.expenses.controller;


import eu.openminted.registry.core.exception.ResourceNotFoundException;
import eu.openminted.registry.core.exception.ServerError;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GenericControllerAdvice {

    private Logger logger = LogManager.getLogger(GenericControllerAdvice.class);


    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    ServerError securityException(HttpServletRequest req, Exception ex) {
        return new ServerError(req.getRequestURL().toString(),ex);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    ServerError accessDeniedException(HttpServletRequest req, Exception ex) {
        return new ServerError(req.getRequestURL().toString(),ex);
    }


}
