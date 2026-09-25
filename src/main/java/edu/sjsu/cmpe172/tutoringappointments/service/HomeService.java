package edu.sjsu.cmpe172.tutoringappointments.service;

import edu.sjsu.cmpe172.tutoringappointments.dto.ServiceDto;
import edu.sjsu.cmpe172.tutoringappointments.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HomeService {

    private final ServiceRepository serviceRepository;

    public HomeService(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    public List<ServiceDto> getServices() {
        return serviceRepository.findAll();
    }
}