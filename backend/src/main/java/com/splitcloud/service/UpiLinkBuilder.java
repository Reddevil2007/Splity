package com.splitcloud.service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Builds a standard UPI (Unified Payments Interface) deep link. Opening it on a
 * phone launches Google Pay / PhonePe / Paytm with the payee and amount filled in.
 *
 * The payment itself happens directly between the two people. SplitCloud never
 * receives or holds money, so no payment-gateway onboarding or KYC is involved.
 */
public final class UpiLinkBuilder {

    private UpiLinkBuilder() {
    }

    /** @return the upi:// link, or null if the payee has no UPI ID on file */
    public static String build(String payeeUpiId, String payeeName, long amountPaise, String note) {
        if (payeeUpiId == null || payeeUpiId.isBlank() || amountPaise <= 0) {
            return null;
        }
        String amount = BigDecimal.valueOf(amountPaise).movePointLeft(2).setScale(2).toPlainString();
        return "upi://pay"
                + "?pa=" + encode(payeeUpiId.trim())
                + "&pn=" + encode(payeeName)
                + "&am=" + amount
                + "&cu=INR"
                + "&tn=" + encode(note);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
