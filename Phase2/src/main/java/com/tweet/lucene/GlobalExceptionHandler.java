package com.tweet.lucene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

public class GlobalExceptionHandler {
    private static Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public org.springframework.web.servlet.ModelAndView exception(final Throwable throwable) {

        logger.error("Exception ocurred", throwable);

        org.springframework.web.servlet.ModelAndView modelAndView = new org.springframework.web.servlet.ModelAndView("/error");
        String errorMessage = (throwable != null ? throwable.toString() : "Unknown error");
        modelAndView.addObject("errorMessage", errorMessage);
        return modelAndView;
    }
}
