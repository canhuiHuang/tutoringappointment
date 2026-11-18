package edu.sjsu.cmpe172.tutoringappointments.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/login", "/customer", "/customer/appointments", "/provider", "/provider/slots"})
    public String index() {
        return "forward:/index.html";
    }
}
