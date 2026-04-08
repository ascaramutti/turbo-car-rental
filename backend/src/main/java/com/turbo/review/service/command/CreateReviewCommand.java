package com.turbo.review.service.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReviewCommand {

    private Long bookingId;
    private Long userId;
    private Integer rating;
    private String comment;
}
