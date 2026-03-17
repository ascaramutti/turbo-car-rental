package com.turbo.booking.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Constants and helpers for the platform pricing model.
 *
 * <p>Pricing model: the owner sets a net hourly rate they want to receive. The platform applies
 * a fixed markup on top of that rate when displaying it to drivers and when computing the total
 * price the driver pays. The owner always receives exactly their net rate; the markup becomes
 * the platform's revenue.</p>
 *
 * <p>Example: owner sets $10/hr → driver sees $12/hr → 5h booking → driver pays $60 →
 * owner receives $50 → platform earns $10.</p>
 */
public final class PricingConstants {

    private PricingConstants() {
    }

    /** Markup applied on top of the owner's net hourly rate when shown to drivers. */
    public static final BigDecimal PLATFORM_MARKUP_PERCENTAGE = new BigDecimal("0.20");

    /** Multiplier (1 + markup) used to convert an owner net rate into a driver-facing rate. */
    public static final BigDecimal MARKUP_MULTIPLIER = BigDecimal.ONE.add(PLATFORM_MARKUP_PERCENTAGE);

    /** Decimal scale used for monetary values displayed to users. */
    private static final int DISPLAY_SCALE = 2;

    /**
     * Applies the platform markup on top of an owner's net hourly rate.
     *
     * @param netHourlyRate the owner's net rate (what the owner receives per hour); may be null
     * @return the rate that drivers see and pay; null if input is null
     */
    public static BigDecimal applyMarkup(BigDecimal netHourlyRate) {
        if (netHourlyRate == null) {
            return null;
        }
        return netHourlyRate.multiply(MARKUP_MULTIPLIER).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}
