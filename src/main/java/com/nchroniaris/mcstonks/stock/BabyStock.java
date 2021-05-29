package com.nchroniaris.mcstonks.stock;

import com.nchroniaris.mcstonks.model.Sign;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * This is a concrete implementation of a Stock, named BabyStock to represent the overall lack in volatility with respect to its stock price.
 */
public class BabyStock extends Stock {

    private static final float DEFAULT_PRICE = 50.0f;
    private static final List<Sign> DEFAULT_SIGN_VECTOR = Arrays.asList(Sign.POSITIVE, Sign.POSITIVE);

    // Maximum failure level that this stock can attain. After reaching this value, the NEXT cycle it will be considered bankrupt. This is essentially a threshold value.
    private static final int MAX_FAILURE_LEVEL = 5;

    // Indicates the stock's failure level. 0 means that it's not failing (but could be bankrupt), and a value from 1-5 means that it is in the process of failing.
    private int failureLevel;

    /**
     * Default constructor that returns a default instance, with <b>initial</b> values.
     * This should only be used when you need a new instance of a stock, not loading one already defined as a file.
     */
    public BabyStock() {

        // Use parameterized constructor, and input default values. We set bankrupt to false and failureLevel to 0 because we assume that it is NOT failing, to begin with.
        this(BabyStock.DEFAULT_PRICE, BabyStock.DEFAULT_SIGN_VECTOR, false, null, 0);

    }

    /**
     * Constructs a BabyStock with the given values.
     *
     * @param price        Initial price of the stock
     * @param signVector   A (length 2) List of {@link Sign}s that indicate the initial value of the signs
     * @param bankrupt     Whether the stock is currently bankrupt
     * @param failureLevel The failure level of this stock. Ranges from 0 to MAX_FAILURE_LEVEL
     * @param stockUUID    The UUID of the stock. <b>This can be null, which will cause the stock to generate a random one.</b>
     */
    public BabyStock(float price, List<Sign> signVector, boolean bankrupt, UUID stockUUID, int failureLevel) {

        // This initializes the three common properties. Remember that this constructor is called when stocks are in the middle of their overall lifetime (as opposed to setting up new ones /w default values), which can include bankruptcy.
        super(price, signVector, bankrupt, stockUUID);

        // We only use 2 signs out of the sign vector, so we make sure that this is true. If the caller provides a shorter or longer one than this would male no sense.
        if (signVector.size() != BabyStock.DEFAULT_SIGN_VECTOR.size())
            throw new IllegalArgumentException(String.format("The baby stock must have exactly %d elements in its sign vector!", BabyStock.DEFAULT_SIGN_VECTOR.size()));

        // failureLevel cannot be less than 0 or more than MAX_FAILURE_LEVEL + 1. The +1 allows room for detecting when the stock has finished its failure cycle.
        if (failureLevel < 0 || failureLevel > BabyStock.MAX_FAILURE_LEVEL + 1)
            throw new IllegalArgumentException(String.format("failureLevel must be a value from 0-%d!", BabyStock.MAX_FAILURE_LEVEL + 1));

        // Illegal state checking
        if (failureLevel != 0 && bankrupt)
            throw new IllegalStateException(String.format("The stock cannot be failing and bankrupt at the same time! Please double check the values: (failureLevel: %d)", failureLevel));

        this.failureLevel = failureLevel;

    }

    @Override
    public void resetValues() {

        this.price = BabyStock.DEFAULT_PRICE;

        // Clear array and repopulate with default values. We do this because the signVector is declared final.
        this.signVector.clear();
        this.signVector.addAll(BabyStock.DEFAULT_SIGN_VECTOR);

        // We reset its bankruptcy state and set the failure level to 0 (not failing)
        this.bankrupt = false;
        this.failureLevel = 0;

    }

    @Override
    public float advance() {

        // This method is filled with hardcoded values, but I feel like in this situation it is well worth it for readability's sake. Also, there would be little point in parameterizing/abstracting these values since the relevant info is only used once and only in here. Additionally, because the mechanics are so intricate and specific, there would also be a lack of utility for the user of this class to tweak the values of this (although it could be argued that some [not all] of the values could be useful to parametrize).

        /* Full Equation

            new value =

                max (0.25,

                     old value
                   + ( 80% to have the same sign ) * ( uniformly choose in [1.5, 3) )
                   + ( 80% to have the same sign ) * ( uniformly choose in [1.5, 2) )
                   + ( 5% chance to be included ) * ( 50% to be negative sign ) * ( uniformly choose in [5, 15) )

                )
         */

        // -- Bankruptcy Mechanics -- //

        // If the stock has exhausted its failing cycle (done exactly MAX_FAILURE_LEVEL number of abnormal cycles), then we must declare the stock bankrupt.
        if (this.failureLevel > BabyStock.MAX_FAILURE_LEVEL) {

            // We must set failure level to 0 as the stock is considered bankrupt, NOT failing (failure comes before bankruptcy)
            this.bankrupt = true;
            this.failureLevel = 0;

            // The stock is bankrupt so there is no reason to update anything for this cycle, which is why we return.
            this.price = Stock.MINIMUM_PRICE;
            return this.price;

        }

        // If the stock is bankrupt, this means that we have already gone through one cycle where it was declared as such, so we reset the values and return (effectively making the cycle right after bankruptcy be a default one)
        if (this.isBankrupt()) {

            this.resetValues();
            return this.price;

        }


        // -- Equation Modifications -- //


        // For all signs in the sign vector, flip the sign with a 20% chance.
        // Recall that the sign vector only includes the first two signs, the third one has special behaviour
        for (int i = 0; i < this.signVector.size(); i++)
            if (this.random.nextFloat() <= 0.20f)
                this.signVector.set(i, this.signVector.get(i).negative());

        // newPrice += (80% chance to have the same sign) (+/-) * uniform random in [1.5, 3)
        this.price += this.signVector.get(0).value() * this.uniformRandom(1.5f, 3.0f);

        // newPrice += (80% chance to have the same sign) (+/-) * uniform random in [1.5, 2)
        this.price += this.signVector.get(1).value() * this.uniformRandom(1.5f, 2.0f);

        // 5% chance for the following modifications to be included
        if (this.random.nextFloat() <= 0.05f) {

            // newPrice += (50% chance to be pos/neg) (+/-) * uniform random in [5, 15)
            Sign sign = (this.random.nextFloat() <= 0.5f) ? Sign.POSITIVE : Sign.NEGATIVE;
            this.price += sign.value() * this.uniformRandom(5.0f, 15.0f);

        }

        // Force a minimum bound
        this.price = Math.max(this.price, Stock.MINIMUM_PRICE);


        // -- Stock Failure Mechanics -- //


        // If failureLevel is anything other than 0, then it gets an extra multiplicative modifier on the changes just applied. In other words, For every failureLevel increase, there is an increase by 0.04. The `1 + ...` part is to ensure it's a multiplicative **increase**.
        this.price *= (1 + this.failureLevel * 0.04f);

        // If the stock is actively failing (any number other than zero) increment the failure level for the next run. This must be done before the initial failureLevel from 0 to 1 or else it will double trigger. This could be combined with the conditional below but for the sake of readability, I won't.
        if (this.failureLevel > 0)
            failureLevel++;

        // Only if the failure level is 0 do we consider failing the stock (with 1/500 chance). Failing a stock just means that we set the level to 1. We use a double for accurate precision.
        if (this.failureLevel == 0 && this.random.nextDouble() <= 0.002d)
            this.failureLevel++;

        return this.price;

    }

}
