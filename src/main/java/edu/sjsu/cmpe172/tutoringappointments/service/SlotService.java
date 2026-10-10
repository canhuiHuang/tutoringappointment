package edu.sjsu.cmpe172.tutoringappointments.service;

import edu.sjsu.cmpe172.tutoringappointments.dto.SlotDto;
import edu.sjsu.cmpe172.tutoringappointments.repository.SlotRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SlotService {

    private final SlotRepository slotRepository;

    public SlotService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public List<SlotDto> getAvailableSlots() {
        return slotRepository.findAllAvailable();
    }

    public List<SlotDto> getAvailableSlots(Long providerId, Long serviceId, LocalDate date, int page, int size) {
        return slotRepository.findAvailable(providerId, serviceId, date, page, size);
    }

    public List<SlotDto> getAvailableSlots(Long providerId, Long serviceId, LocalDate date, int page, int size, LocalDate endDate) {
        return slotRepository.findAvailable(providerId, serviceId, date, page, size, endDate);
    }
}
