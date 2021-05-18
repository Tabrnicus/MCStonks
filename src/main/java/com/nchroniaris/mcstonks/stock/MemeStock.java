package com.nchroniaris.mcstonks.stock;

import com.nchroniaris.mcstonks.model.Sign;

import java.util.Arrays;
import java.util.List;

public class MemeStock extends Stock {

    // Even though the meme stock has four terms, the latter two never change so there is no point to storing them. Therefore the sign vector defines the signs of the first two terms ONLY.
    private static final float DEFAULT_PRICE = 1.0f;
    private static final List<Sign> DEFAULT_SIGN_VECTOR = Arrays.asList(Sign.POSITIVE, Sign.POSITIVE);

    // Maximum number of strikes that this stock can attain. After reaching this value, the stock changes it behaviour (see advance()). This is essentially a threshold value.
    private static final int MAX_STRIKES = 3;

    // This variable indicates whether the stock is currently failing (the period right before bankruptcy).
    private boolean failing;

    // Indicates the stock's number of strikes. The value will range from 0 to MAX_STRIKES.
    private int strikes;

    /**
     * Default constructor that returns a default instance, with <b>initial</b> values.
     * This should only be used when you need a new instance of a stock, not loading one already defined as a file.
     */
    public MemeStock() {

        // Use parameterized constructor, and input default values. We set bankrupt/failing to false and strikes to 0 because we assume that it is NOT bankrupt and has no strikes to begin with.
        this(MemeStock.DEFAULT_PRICE, MemeStock.DEFAULT_SIGN_VECTOR, false, false, 0);

    }

    /**
     * Constructs a MemeStock with the given values.
     *
     * @param price      Initial price of the stock
     * @param signVector A (length 2) List of {@link Sign}s that indicate the initial value of the signs
     * @param bankrupt   Whether the stock is currently bankrupt. The stock cannot be bankrupt if it is failing.
     * @param failing    Whether the stock is currently failing. The stock cannot be failing if it is bankrupt.
     * @param strikes    The number of current strikes. Ranges from 0 to MAX_STRIKES
     */
    public MemeStock(float price, List<Sign> signVector, boolean bankrupt, boolean failing, int strikes) {

        // This initializes the three common properties. Remember that this constructor is called when stocks are in the middle of their overall lifetime (as opposed to setting up new ones /w default values), which can include bankruptcy.
        super(price, signVector, bankrupt);

        // We only use 2 signs (see note in static declaration) out of the sign vector, so we make sure that this is true. If the caller provides a shorter or longer one than this would male no sense.
        if (signVector.size() != MemeStock.DEFAULT_SIGN_VECTOR.size())
            throw new IllegalArgumentException(String.format("The meme stock must have exactly %d elements in its sign vector!", MemeStock.DEFAULT_SIGN_VECTOR.size()));

        // The stock cannot be failing and bankrupt at the same time (all other combinations are valid)
        if (failing && bankrupt)
            throw new IllegalStateException("The stock cannot be failing and bankrupt at the same time! Please double check the values.");

        this.failing = failing;

        // strikes cannot be less than 0 or more than MAX_STRIKES.
        if (strikes < 0 || strikes > MemeStock.MAX_STRIKES)
            throw new IllegalArgumentException(String.format("failureLevel must be a value from 0-%d!", MemeStock.MAX_STRIKES));

        this.strikes = strikes;

    }

    @Override
    public void resetValues() {

        this.price = MemeStock.DEFAULT_PRICE;

        // Clear array and repopulate with default values. We do this because the signVector is declared final.
        this.signVector.clear();
        this.signVector.addAll(MemeStock.DEFAULT_SIGN_VECTOR);

        // By default, the stock is not failing nor bankrupt, and has zero strikes.
        this.bankrupt = false;
        this.failing = false;
        this.strikes = 0;

    }

    @Override
    public float advance() {

        // This method is filled with hardcoded values, but I feel like in this situation it is well worth it for readability's sake. Also, there would be little point in parameterizing/abstracting these values since the relevant info is only used once and only in here. Additionally, because the mechanics are so intricate and specific, there would also be a lack of utility for the user of this class to tweak the values of this (although it could be argued that some [not all] of the values could be useful to parametrize).

        /* Full Equation

            new value =

                max (0.25,

                     old value
                   + ( uniformly choose in [1, 20) )
                   + ( uniformly choose in [1, 20) )
                   - ( uniformly choose in [1, 10) )
                   - ( uniformly choose in [1, 10) )

                )
         */


        // -- Stock Failure/Bankruptcy Mechanics -- //


        // If the stock is bankrupt, this means that we have already gone through one cycle where it was declared as such, so we reset the values and return (effectively making the cycle right after bankruptcy be a default one)
        if (this.isBankrupt()) {

            this.resetValues();
            return this.price;

        }

        // If the stock was marked as failing, that means that it has already gone through a cycle of failure, so then we must declare the stock bankrupt.
        if (this.failing) {

            // We must set failure level to 0 as the stock is considered bankrupt, NOT failing (failure comes before bankruptcy)
            this.failing = false;
            this.bankrupt = true;

            this.strikes = 0;

            // The stock is bankrupt so there is no reason to update anything for this cycle, which is why we return.
            return Stock.MINIMUM_PRICE;

        }

        // Start to fail the stock once we accrue the max number of strikes
        if (this.strikes == MemeStock.MAX_STRIKES) {

            // Set all the signs to negative (recall: last two terms are always negative)
            for (int i = 0; i < this.signVector.size(); i++)
                this.signVector.set(i, Sign.NEGATIVE);

            // The stock will begin to fail (signified by the all negative signs)
            this.failing = true;

        }


        // -- Equation Modifications -- //


        // For the former two terms, which are usually positive but sometimes negative
        for (Sign sign : this.signVector) {

            // newPrice += uniform random in [1, 20)
            this.price += sign.value() * this.uniformRandom(1.0f, 20.0f);

        }

        // For the latter two terms, which are always negative (`i` is not used here)
        for (int i = 0; i < 2; i++) {

            // newPrice -= (<-- note the negative sign) uniform random in [1, 10)
            this.price -= this.uniformRandom(1.0f, 10.0f);

        }

        // Force a minimum bound
        this.price = Math.max(this.price, Stock.MINIMUM_PRICE);


        // -- Strike Mechanics -- //


        // With a probability of price / 100000 (=== price / 1000 PERCENT), the stock accrues one strike.
        // This is skipped (via short circuit) if the stock is currently failing (i.e. no point to compute this value)
        if (!this.failing && this.random.nextDouble() <= (double) this.price / 100000)
            this.strikes++;

        return this.price;

    }


}
