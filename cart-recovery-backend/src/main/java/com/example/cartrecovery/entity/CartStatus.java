package com.example.cartrecovery.entity;

public enum CartStatus {

    ACTIVE,

    ABANDONED,

    REMINDER_SENT,

    CHECKED_OUT,

    EXPIRED,

    /*
     * Retained for carts written by earlier versions. New normal checkouts use
     * CHECKED_OUT.
     */
    COMPLETED,

    RECOVERED
}
