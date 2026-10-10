package edu.sjsu.cmpe172.tutoringappointments.controller;

import edu.sjsu.cmpe172.tutoringappointments.dto.SlotDto;
import edu.sjsu.cmpe172.tutoringappointments.service.SlotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping("/slots")
    public List<SlotDto> getAvailableSlots(
        @RequestParam(required = false) Long providerId,
        @RequestParam(required = false) Long serviceId,
        @RequestParam(required = false) LocalDate date,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) LocalDate endDate
    ) {
        return slotService.getAvailableSlots(providerId, serviceId, date, page, size, endDate);
    }
}
