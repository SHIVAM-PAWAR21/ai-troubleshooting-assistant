package org.assignment.troubleshooter.web;

import org.assignment.troubleshooter.dto.TroubleshootResponse;
import org.assignment.troubleshooter.service.TroubleshootingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class TroubleshootingController {

    private static final Logger log = LoggerFactory.getLogger(TroubleshootingController.class);

    public record Request(String searchString, Boolean semanticOnly) {
    }

    private final TroubleshootingService service;

    public TroubleshootingController(TroubleshootingService service) {
        this.service = service;
    }

    @PostMapping("/troubleshoot")
    public TroubleshootResponse troubleshoot(@RequestBody Request request) {

        String query = request.searchString() == null ? "" : request.searchString().trim();

        if (query.isEmpty() || query.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Describe the issue in 1 to 1000 characters.");
        }
        return service.troubleshoot(query, Boolean.TRUE.equals(request.semanticOnly()));
    }
}
