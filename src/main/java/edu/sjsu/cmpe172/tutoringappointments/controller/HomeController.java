package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.dto.ServiceDto;
import edu.sjsu.cmpe172.tutoringappointments.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/api/services")
    public List<ServiceDto> home() {
        return homeService.getServices();
    }
}
