package com.javabanker.exercises.ex02;

/** Value of the compacted topic bank.customers.v1 (key = accountId). */
public record Customer(String accountId, String name, String segment, String country) {
}
