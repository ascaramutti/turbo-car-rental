package com.turbo.booking.controller;

import com.turbo.booking.model.BookingPhoto;
import com.turbo.booking.service.BookingService;
import com.turbo.document.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings/photos")
@RequiredArgsConstructor
public class BookingPhotoController {

    private final BookingService bookingService;
    private final FileStorageService fileStorageService;

    @GetMapping("/{photoId}")
    public ResponseEntity<Resource> viewBookingPhoto(@PathVariable Long photoId) {
        BookingPhoto photo = bookingService.getBookingPhoto(photoId);
        Resource resource = fileStorageService.load(photo.getFileUrl());
        String extension = photo.getFileName().substring(photo.getFileName().lastIndexOf('.') + 1).toLowerCase();
        MediaType mediaType = "png".equals(extension) ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
