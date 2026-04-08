package com.turbo.auth.service.command;

import lombok.Data;

@Data
public class AddressCommand {

    private String streetAddress;
    private String city;
    private String province;
    private String postalCode;
    private String country;
}
