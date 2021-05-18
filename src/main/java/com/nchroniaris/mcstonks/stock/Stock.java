package com.nchroniaris.mcstonks.stock;

import com.nchroniaris.mcstonks.model.Sign;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a common "stock" that can have its price advanced
 */
public abstract class Stock {

    // Small data class that holds information about the state of a stock in one particular instance of time. Used for bulk returns, so that the caller can know when the stock has gone bankrupt, for example.
    public static final class State {

        public final float price;
        public final boolean bankrupt;

        public State(float price, boolean bankrupt) {
            this.price = price;
            this.bankrupt = bankrupt;
        }

    }

    // Minimum price a stock can go at any point. I chose 0.25f as its binary representation does not have any precision loss, which makes comparisons a little more deterministic.
    protected static final float MINIMUM_PRICE = 0.25f;

    // Variables common to all stocks. Note that the size and contents of signVector can change from stock to stock, but the abstract part of this class is not concerned with those details.
    protected float price;
    protected final List<Sign> signVector;

    // Used for bankruptcy mechanics. The implementing class may or may not use this field to declare itself bankrupt (under its own conditions). In other words, this is a semi-optional field (a stock may choose to never go bankrupt, for example)
    protected boolean bankrupt;

    // Random number generator. All stocks have some random component, so it's efficient to store it in a field. Excluded from serialization.
    protected transient final Random random;

    // Constructor for all the common properties of a stock
    public Stock(float price, List<Sign> signVector, boolean bankrupt) {

        // We double check the price to make sure it's not below our bound
        if (price < Stock.MINIMUM_PRICE)
            throw new IllegalArgumentException(String.format("The price must be greater than or equal to the minimum bound of $%.2f", Stock.MINIMUM_PRICE));

        // Set the price and clone the sign vector list (shallow copy is fine because the elements are enums)
        this.price = price;
        this.signVector = new ArrayList<>(signVector);

        this.bankrupt = bankrupt;

        // Initialize (and seed) random number generator. Since every instance of a stock will have its own instance of a RNG, they will never influence each other.
        this.random = new Random();

    }

    /**
     * Generates a random floating number in between the two ranges. i.e. the result is in [min, max)
     *
     * @param min minimum bound. Inclusive
     * @param max maximum bound. Exclusive
     * @return the generated value
     */
    protected float uniformRandom(float min, float max) {

        // Idiom for scaling up numbers generated in [0.0, 1.0) to a max and minimum
        return this.random.nextFloat() * (max - min) + min;

    }

    /**
     * Reset the stock to default values, overwriting whatever is in the class.
     * The implementing class must define what it means to "reset" the stock.
     */
    public abstract void resetValues();

    /**
     * Advances the stock price based on the currently stored values
     *
     * @return The calculated price, for convenience
     */
    public abstract float advance();

    /**
     * Advances the stock price {@code numberOfTimes} times based on the currently stored values
     *
     * @param numberOfTimes The number of times to advance the price.
     * @return The effective state of the stock (up to price and bankruptcy), stored in a list for convenience. The length of the list will be exactly equal to {@code numberOfTimes}.
     */
    public final List<State> advance(int numberOfTimes) {

        // Argument checking
        if (numberOfTimes < 1)
            throw new IllegalArgumentException("You can't advance the price less than 1 time!");

        List<State> states = new ArrayList<>();

        // this.advance should store its updated values back to itself, so a subsequent call should work without doing anything special
        for (int i = 0; i < numberOfTimes; i++) {
            this.advance();
            states.add(new State(this.getPrice(), this.isBankrupt()));
        }

        // The current price should be stored to the instance variable
        return states;

    }

    /**
     * Get the current price of the stock, without changing anything.
     *
     * @return The current price, as a {@code float}
     */
    public final float getPrice() {

        return price;

    }

    /**
     * Queries the stock if it is bankrupt or not. The caller may use this information as it wishes.
     *
     * @return A boolean value indicating if the stock is bankrupt or not
     */
    public final boolean isBankrupt() {

        return bankrupt;

    }
}
