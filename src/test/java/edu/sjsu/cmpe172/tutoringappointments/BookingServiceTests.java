package edu.sjsu.cmpe172.tutoringappointments;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.sjsu.cmpe172.tutoringappointments.repository.BookingRepository;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingException;
import edu.sjsu.cmpe172.tutoringappointments.service.BookingService;
class BookingServiceTests {
    BookingRepository repository;
    BookingService service;
    LocalDateTime future=LocalDateTime.now().plusDays(5);

    @BeforeEach void setup() { 
        repository=mock(BookingRepository.class);service=new BookingService(repository);
    }

    BookingRepository.LockedSlot slot(String status) { return new BookingRepository.LockedSlot(10,1,2,future,future.plusHours(1),status,60); }
    
    @Test void booksAvailableSlot() {
        when(repository.lockSlot(10)).thenReturn(Optional.of(slot("AVAILABLE")));
        when(repository.insertBooking(1,10)).thenReturn(20L);
        assertEquals(20,service.book(1,10));verify(repository).slotStatus(10,"BOOKED");
    }

    @Test void rejectsConflict() {
        when(repository.lockSlot(10)).thenReturn(Optional.of(slot("BOOKED")));
        assertEquals(409,assertThrows(BookingException.class,()->service.book(1,10)).status());
        verify(repository,never()).insertBooking(anyLong(),anyLong());
    }

    @Test void rejectsInvalidIds() {
        assertEquals(400,assertThrows(BookingException.class,()->service.book(1,0)).status());verifyNoInteractions(repository);
    }

    @Test void rejectsNonOwnerCancellation() {
        when(repository.booking(20)).thenReturn(Optional.of(new BookingRepository.Booking(20,2,10,"BOOKED")));
        assertEquals(403,assertThrows(BookingException.class,()->service.cancel(1,20)).status());verify(repository,never()).cancel(anyLong());
    }

    @Test void ownerCancellationReopensSlot() {
        when(repository.booking(20)).thenReturn(Optional.of(new BookingRepository.Booking(20,1,10,"BOOKED")));
        when(repository.lockSlot(10)).thenReturn(Optional.of(slot("BOOKED")));
        service.cancel(1,20);verify(repository).cancel(20);verify(repository).slotStatus(10,"AVAILABLE");
    }

    @Test void validatesProviderSlotTimes() {
        assertEquals(400,assertThrows(BookingException.class,()->service.create(3,2,future,future.minusMinutes(1))).status());
        verifyNoInteractions(repository);
    }
    
    @Test void validatesServiceDuration() {
        when(repository.provider(3)).thenReturn(Optional.of(1L));
        when(repository.serviceDuration(2,1)).thenReturn(Optional.of(60));
        assertEquals(400,assertThrows(BookingException.class,()->service.create(3,2,future,future.plusMinutes(30))).status());
        verify(repository,never()).createSlot(anyLong(),anyLong(),any(),any());
    }
}
