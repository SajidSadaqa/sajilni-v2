package com.sajilni.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.Collections;

@RestController
public class EncodingTestController {

    @GetMapping("/api/test/encoding")
    public Map<String, String> testEncoding() {
        // This is a hardcoded Arabic string.
        // We are not using the message source or database.
        String arabicMessage = "أهلاً بالعالم"; // "Hello, World"

        // We return it in a simple JSON map.
        return Collections.singletonMap("message", arabicMessage);
    }
}
