package com.ramteja.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Returned when a circuit breaker is open or a downstream call fails, instead of a raw 500. */
@RestController
public class FallbackController {

    @RequestMapping("/fallback/{service}")
    public ResponseEntity<ProblemDetail> fallback(@PathVariable String service) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The " + service + " service is temporarily unavailable. Please retry shortly.");
        pd.setProperty("service", service);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }
}
