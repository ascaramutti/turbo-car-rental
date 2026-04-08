package com.turbo.booking.controller.mapper;

import com.turbo.booking.dto.OwnerDashboardResponse;
import com.turbo.booking.service.result.OwnerDashboardStats;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OwnerDashboardControllerMapper {

    OwnerDashboardResponse toOwnerDashboardResponse(OwnerDashboardStats stats);
}
