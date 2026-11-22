package com.javabanker.exercises.ex06;

import java.time.Instant;

/** State per account: when it was last active and whether the dormant alert was already sent. */
record AccountActivity(Instant lastActivity, boolean alerted) {

    AccountActivity markAlerted() {
        return new AccountActivity(lastActivity, true);
    }
}
