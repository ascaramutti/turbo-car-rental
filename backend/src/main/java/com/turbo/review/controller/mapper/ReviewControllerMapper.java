package com.turbo.review.controller.mapper;

import com.turbo.review.dto.CreateReviewRequest;
import com.turbo.review.service.command.CreateReviewCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewControllerMapper {

    @Mapping(source = "request.bookingId", target = "bookingId")
    @Mapping(source = "request.rating", target = "rating")
    @Mapping(source = "request.comment", target = "comment")
    @Mapping(source = "userId", target = "userId")
    CreateReviewCommand toCreateReviewCommand(CreateReviewRequest request, Long userId);
}
