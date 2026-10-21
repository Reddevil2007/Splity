package com.splitcloud.service;

import com.splitcloud.domain.ExpenseCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseCategoryTest {

    @Test
    void guessesFromCommonWords() {
        assertEquals(ExpenseCategory.TRAVEL, ExpenseCategory.guess("Cab to airport"));
        assertEquals(ExpenseCategory.STAY, ExpenseCategory.guess("Hotel booking"));
        assertEquals(ExpenseCategory.FOOD, ExpenseCategory.guess("Beach shack dinner"));
        assertEquals(ExpenseCategory.DRINKS, ExpenseCategory.guess("Tequila"));
        assertEquals(ExpenseCategory.TRAVEL, ExpenseCategory.guess("Scooter rental"));
        assertEquals(ExpenseCategory.ACTIVITIES, ExpenseCategory.guess("Parasailing"));
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.guess("Something else"));
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.guess(null));
    }
}
