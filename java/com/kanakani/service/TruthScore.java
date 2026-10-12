package com.kanakani.service;

/**
 * A person's Truth Score: how many of their settled rumours came true.
 *
 * The number people see is the plain percentage. The ranking uses the Wilson lower bound,
 * which asks "how good are they, at the very least, given how few rumours we have seen?".
 * Without it, someone with 3 lucky guesses would outrank someone with 40 careful ones.
 */
public record TruthScore(int settled, int hits) {

    public static final int MIN_FOR_BOARD = 3;
    public static final int MIN_FOR_ORACLE = 5;
    public static final double ORACLE_RATE = 0.85;
    private static final double Z = 1.96; // 95% confidence

    public TruthScore {
        if (settled < 0 || hits < 0 || hits > settled) {
            throw new IllegalArgumentException("hits must be between 0 and settled");
        }
    }

    public double rate() {
        return settled == 0 ? 0.0 : (double) hits / settled;
    }

    public int percent() {
        return (int) Math.round(rate() * 100);
    }

    public boolean onBoard() {
        return settled >= MIN_FOR_BOARD;
    }

    public boolean isOracle() {
        return settled >= MIN_FOR_ORACLE && rate() >= ORACLE_RATE;
    }

    /** Lower end of the 95% confidence interval for the true rate. Used to rank people. */
    public double wilsonLowerBound() {
        if (settled == 0) {
            return 0.0;
        }
        double n = settled;
        double p = rate();
        double z2 = Z * Z;
        double centre = p + z2 / (2 * n);
        double margin = Z * Math.sqrt((p * (1 - p) + z2 / (4 * n)) / n);
        return (centre - margin) / (1 + z2 / n);
    }
}
