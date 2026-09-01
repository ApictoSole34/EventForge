package com.fizzycoyotestudio.eventforge.engine;

/**
 * Comparison operators used by ComparisonCondition.
 * Kept as an enum (rather than raw strings like ">" or "GT") so that
 * deserialization from JSON (Phase 3+) has a fixed, validated vocabulary.
 */
public enum Operator {
    GREATER_THAN {
        @Override
        public boolean apply(double actual, double expected) {
            return actual > expected;
        }
    },
    GREATER_THAN_OR_EQUAL {
        @Override
        public boolean apply(double actual, double expected) {
            return actual >= expected;
        }
    },
    LESS_THAN {
        @Override
        public boolean apply(double actual, double expected) {
            return actual < expected;
        }
    },
    LESS_THAN_OR_EQUAL {
        @Override
        public boolean apply(double actual, double expected) {
            return actual <= expected;
        }
    },
    EQUAL {
        @Override
        public boolean apply(double actual, double expected) {
            return Double.compare(actual, expected) == 0;
        }
    },
    NOT_EQUAL {
        @Override
        public boolean apply(double actual, double expected) {
            return Double.compare(actual, expected) != 0;
        }
    };

    public abstract boolean apply(double actual, double expected);
}
