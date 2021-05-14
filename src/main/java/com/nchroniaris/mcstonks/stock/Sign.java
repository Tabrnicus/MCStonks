package com.nchroniaris.mcstonks.stock;

/**
 * This mostly exists so that I can "enforce" an int to be a sign (+/-) such that you can do something like {@code mySign.value() * 10} and get either +10 or -10. This class is by all accounts, immutable.
 */
public enum Sign {

    POSITIVE(1),
    NEGATIVE(-1);

    private final int sign;

    Sign(int sign) {

        this.sign = sign;

    }

    /**
     * Returns an enum member that matches the sign value provided
     *
     * @param value integer value of the sign. Must be either {@code 1} or {@code -1}.
     * @return {@code POSITIVE} or {@code NEGATIVE}, based on the sign
     */
    public static Sign fromValue(int value) {

        for (Sign sign : Sign.values())
            if (value == sign.value())
                return sign;

        throw new IllegalArgumentException(String.format("'%d' is not a valid sign! It must be either 1 or -1.", value));

    }

    /**
     * Gets the value (-1 or 1) for the current enum member.
     *
     * @return -1 or +1
     */
    public int value() {

        return this.sign;

    }

    /**
     * Returns the opposite sign as an enum member.
     *
     * @return {@code POSITIVE} or {@code NEGATIVE}, whatever is opposite from the current one.
     */
    public Sign negative() {

        return Sign.fromValue(this.value() * -1);

    }

    @Override
    public String toString() {

        return String.valueOf(this.value());

    }

}
