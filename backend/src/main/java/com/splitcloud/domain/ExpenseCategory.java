package com.splitcloud.domain;

import java.util.List;
import java.util.Locale;

/** What an expense was for. Drives the colours of the spending chart. */
public enum ExpenseCategory {
    FOOD(List.of("food", "dinner", "lunch", "breakfast", "meal", "restaurant", "shack", "pizza", "snack",
            "grocer", "cafe", "coffee", "biryani", "swiggy", "zomato")),
    DRINKS(List.of("drink", "beer", "tequila", "vodka", "wine", "bar", "pub", "cocktail", "whisky", "rum")),
    TRAVEL(List.of("cab", "taxi", "uber", "auto", "bus", "train", "flight", "fuel", "petrol",
            "scooter", "bike", "rental", "metro", "ticket", "toll", "airport")),
    STAY(List.of("hotel", "stay", "hostel", "airbnb", "rent", "room", "resort", "booking", "villa")),
    ACTIVITIES(List.of("parasail", "ticket", "museum", "movie", "trek", "tour", "game", "club", "party",
            "entry", "activity", "scuba", "surf")),
    SHOPPING(List.of("shop", "souvenir", "clothes", "gift", "market", "mall")),
    OTHER(List.of());

    private final List<String> keywords;

    ExpenseCategory(List<String> keywords) {
        this.keywords = keywords;
    }

    /** Best guess from the description, e.g. "Cab to airport" is TRAVEL. Falls back to OTHER. */
    public static ExpenseCategory guess(String description) {
        if (description == null) {
            return OTHER;
        }
        String text = description.toLowerCase(Locale.ROOT);
        for (ExpenseCategory category : values()) {
            for (String keyword : category.keywords) {
                if (text.contains(keyword)) {
                    return category;
                }
            }
        }
        return OTHER;
    }
}
