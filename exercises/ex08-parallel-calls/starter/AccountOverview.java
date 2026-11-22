package com.javabanker.exercises.ex08;

import java.math.BigDecimal;
import java.util.List;

public record AccountOverview(String customerName, BigDecimal balance, List<String> cards) {
}
