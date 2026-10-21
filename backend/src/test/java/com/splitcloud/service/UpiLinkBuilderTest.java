package com.splitcloud.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpiLinkBuilderTest {

    @Test
    void buildsStandardUpiLink() {
        String link = UpiLinkBuilder.build("diya@oksbi", "Diya Rao", 123_456, "SplitCloud - Goa Trip");
        assertEquals("upi://pay?pa=diya%40oksbi&pn=Diya%20Rao&am=1234.56&cu=INR&tn=SplitCloud%20-%20Goa%20Trip",
                link);
    }

    @Test
    void wholeRupeesStillShowTwoDecimals() {
        assertTrue(UpiLinkBuilder.build("a@ybl", "A", 50_000, "x").contains("&am=500.00&"));
    }

    @Test
    void noLinkWithoutUpiId() {
        assertNull(UpiLinkBuilder.build(null, "A", 100, "x"));
        assertNull(UpiLinkBuilder.build("  ", "A", 100, "x"));
    }
}
