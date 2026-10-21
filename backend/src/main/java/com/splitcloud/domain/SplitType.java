package com.splitcloud.domain;

/** How an expense is divided between its participants. */
public enum SplitType {
    /** Everyone pays the same amount. */
    EQUAL,
    /** Each participant's amount (in paise) is given explicitly; must add up to the total. */
    EXACT,
    /** Each participant's percentage is given; must add up to 100. */
    PERCENT,
    /** Each participant gets a weight, e.g. 2 shares vs 1 share. */
    SHARES
}
