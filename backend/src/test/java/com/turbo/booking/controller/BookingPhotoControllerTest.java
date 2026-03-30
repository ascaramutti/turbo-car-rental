package com.turbo.booking.controller;

import com.turbo.booking.model.BookingPhoto;
import com.turbo.booking.service.BookingService;
import com.turbo.document.service.FileStorageService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.GlobalExceptionHandler;
import com.turbo.exception.error.BookingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingPhotoController")
class BookingPhotoControllerTest {

    @Mock private BookingService bookingService;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private BookingPhotoController bookingPhotoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingPhotoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/bookings/photos/{id} - returns PNG image")
    void viewBookingPhoto_png_returns200() throws Exception {
        BookingPhoto photo = new BookingPhoto();
        photo.setPhotoId(1L);
        photo.setFileUrl("./uploads/2/bookings/10/pickup_uuid.png");
        photo.setFileName("pickup.png");

        when(bookingService.getBookingPhoto(1L)).thenReturn(photo);
        when(fileStorageService.load(photo.getFileUrl()))
                .thenReturn(new ByteArrayResource(new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/bookings/photos/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        verify(bookingService).getBookingPhoto(1L);
        verify(fileStorageService).load(photo.getFileUrl());
    }

    @Test
    @DisplayName("GET /api/bookings/photos/{id} - returns JPEG image")
    void viewBookingPhoto_jpg_returns200() throws Exception {
        BookingPhoto photo = new BookingPhoto();
        photo.setPhotoId(2L);
        photo.setFileUrl("./uploads/2/bookings/10/return_uuid.jpg");
        photo.setFileName("return.jpg");

        when(bookingService.getBookingPhoto(2L)).thenReturn(photo);
        when(fileStorageService.load(photo.getFileUrl()))
                .thenReturn(new ByteArrayResource(new byte[]{4, 5, 6}));

        mockMvc.perform(get("/api/bookings/photos/2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    @DisplayName("GET /api/bookings/photos/{id} - photo not found returns 404")
    void viewBookingPhoto_notFound_returns404() throws Exception {
        when(bookingService.getBookingPhoto(999L))
                .thenThrow(new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND));

        mockMvc.perform(get("/api/bookings/photos/999"))
                .andExpect(status().isNotFound());
    }
}
