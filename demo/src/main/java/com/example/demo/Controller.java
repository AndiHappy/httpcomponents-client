package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@RestController
public class Controller {

    @PostMapping("/p")
    public String debugPost(HttpServletRequest req, HttpServletResponse res) {
            return "server " + UUID.randomUUID().toString()+ " Post ok";
    }

    @GetMapping("/g")
    public String debugGet(HttpServletRequest req, HttpServletResponse res) {
        return "server " + UUID.randomUUID().toString()+ " Get ok";
    }
    }
