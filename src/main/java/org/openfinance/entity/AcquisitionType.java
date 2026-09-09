package org.openfinance.entity;

/** Purchase and gift are owned positions; a plan enters holdings only when acquired. */
public enum AcquisitionType {
    PURCHASE,
    GIFT,
    PLANNED
}
