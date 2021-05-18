package com.nchroniaris.mcstonks.stock;

import com.nchroniaris.mcstonks.model.Sign;

import java.util.Arrays;
import java.util.List;

public class RiskyStock extends Stock {

    private static final float DEFAULT_PRICE = 100.0f;
    private static final List<Sign> DEFAULT_SIGN_VECTOR = Arrays.asList(Sign.POSITIVE, Sign.POSITIVE, Sign.POSITIVE);

    // Maximum failure level that this stock can attain. After reaching this value, the NEXT cycle it will be considered bankrupt. This is essentially a threshold value.
    private static final int MAX_FAILURE_LEVEL = 5;

    // Indicates the stock's failure level. 0 means that it's not failing (but could be bankrupt), and a value from 1-5 means that it is in the process of failing.
    private int failureLevel;

    /**
     * Default constructor that returns a default instance, with <b>initial</b> values.
     * This should only be used when you need a new instance of a stock, not loading one already defined as a file.
     */
    public RiskyStock() {

        // Use parameterized constructor, and input default values. We set bankrupt to false and failureLevel to 0 because we assume that it is NOT failing, to begin with.
        this(RiskyStock.DEFAULT_PRICE, RiskyStock.DEFAULT_SIGN_VECTOR, false, 0);

    }

    /**
     * Constructs a RiskyStock with the given values.
     *
     * @param price        Initial price of the stock
     * @param signVector   A (length 3) List of {@link Sign}s that indicate the initial value of the signs
     * @param bankrupt     Whether the stock is currently bankrupt
     * @param failureLevel The failure level of this stock. Ranges from 0 to MAX_FAILURE_LEVEL
     */
    public RiskyStock(float price, List<Sign> signVector, boolean bankrupt, int failureLevel) {

        // This initializes the three common properties. Remember that this constructor is called when stocks are in the middle of their overall lifetime (as opposed to setting up new ones /w default values), which can include bankruptcy.
        super(price, signVector, bankrupt);

        // We only use 3 signs out of the sign vector, so we make sure that this is true. If the caller provides a shorter or longer one than this would male no sense.
        if (signVector.size() != RiskyStock.DEFAULT_SIGN_VECTOR.size())
            throw new IllegalArgumentException(String.format("The risky stock must have exactly %d elements in its sign vector!", RiskyStock.DEFAULT_SIGN_VECTOR.size()));

        // failureLevel cannot be less than 0 or more than MAX_FAILURE_LEVEL + 1. The +1 allows room for detecting when the stock has finished its failure cycle.
        if (failureLevel < 0 || failureLevel > RiskyStock.MAX_FAILURE_LEVEL + 1)
            throw new IllegalArgumentException(String.format("failureLevel must be a value from 0-%d!", RiskyStock.MAX_FAILURE_LEVEL + 1));

        // Illegal state checking
        if (failureLevel != 0 && bankrupt)
            throw new IllegalStateException(String.format("The stock cannot be failing and bankrupt at the same time! Please double check the values: (failureLevel: %d)", failureLevel));

        this.failureLevel = failureLevel;

    }

    @Override
    public void resetValues() {

        this.price = RiskyStock.DEFAULT_PRICE;

        // Clear array and repopulate with default values. We do this because the signVector is declared final.
        this.signVector.clear();
        this.signVector.addAll(RiskyStock.DEFAULT_SIGN_VECTOR);

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
                   + ( 66% chance to be included ) * ( 60% to have the same sign ) * ( uniformly choose in [8, 10) )
                   + ( 66% chance to be included ) * ( 60% to have the same sign ) * ( uniformly choose in [5, 7) )
                   + ( 66% chance to be included ) * ( 60% to have the same sign ) * ( uniformly choose in [5, 10) )
                   + ( 10% chance to be included ) * ( 50% to be negative sign ) * ( uniformly choose in [25, 75) )

                )
         */


        // -- Bankruptcy Mechanics -- //


        // If the stock has exhausted its failing cycle (done exactly MAX_FAILURE_LEVEL number of abnormal cycles), then we must declare the stock bankrupt.
        if (this.failureLevel > RiskyStock.MAX_FAILURE_LEVEL) {

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


        // The first three terms of this equation are identical up to the uniform random number, so we just precompute them and use them if necessary (some or all may not get used, depending on if the term is included or not). This is technically wasting computation time but in this case it is negligible.
        float[] uniformResults = {
                this.uniformRandom(8.0f, 10.0f),
                this.uniformRandom(5.0f, 7.0f),
                this.uniformRandom(5.0f, 10.0f)
        };

        // Extra check juuuuust in case something goes wrong
        if (this.signVector.size() != uniformResults.length)
            throw new IllegalStateException("The length of signVector and uniformResults are different: This should NOT happen!");

        // Does one run PER term (at time of writing there are 3 nearly identical terms)
        for (int i = 0; i < uniformResults.length; i++) {

            // 66.66% chance for the term to be included in the final calculation
            if (this.random.nextFloat() <= (2.0f / 3.0f)) {

                // Flip the corresponding sign with a 40% chance (=== 60% chance to have the same sign)
                // Notice how the sign will only potentially flip only when this term is included. This behaviour is intentional.
                if (this.random.nextFloat() <= 0.40f)
                    this.signVector.set(i, this.signVector.get(i).negative());

                // newPrice += (60% to have the same sign) (+/-) * uniform random in [x, y)
                // -- where x and y are the bounds in the uniformRandom() call in the corresponding element in uniformResults.
                this.price += this.signVector.get(i).value() * uniformResults[i];

            }

        }

        // The sequence [1/8, 1/6, 1/4, 1/2, 1/1] is not easily representable by a function involving failureLevel, so I opted to use an array
        final float[] MODIFIED_PROBABILITIES = {
                1.0f / 8.0f,
                1.0f / 6.0f,
                1.0f / 4.0f,
                1.0f / 2.0f,
                1.0f,
        };

        float spikeTermProbability;

        // The probability of including the spike term is 10% when not failing, and some element of the above array when failing. We do a `-1` at the end there to ensure we are not subscripting past the array.
        // Also note how the else clause short circuits the value of failureLevel such that it will never be 0 in the else clause, which is another reason for the `-1`.
        if (this.failureLevel == 0)
            spikeTermProbability = 0.10f;
        else
            spikeTermProbability = MODIFIED_PROBABILITIES[this.failureLevel - 1];

        // some % chance (see above) for the following modifications to be included
        if (this.random.nextFloat() <= spikeTermProbability) {

            // newPrice += (50% chance to be pos/neg) (+/-) * uniform random in [25, 75)
            Sign sign = (this.random.nextFloat() <= 0.5f) ? Sign.POSITIVE : Sign.NEGATIVE;
            this.price += sign.value() * this.uniformRandom(25.0f, 75.0f);

        }

        // Force a minimum bound
        this.price = Math.max(this.price, Stock.MINIMUM_PRICE);


        // -- Stock Failure Mechanics -- //


        // If failureLevel is anything other than 0, then it gets an extra multiplicative modifier on the changes just applied. In other words, For every failureLevel increase, there is an increase by 0.10. The `1 + ...` part is to ensure it's a multiplicative **increase**.
        this.price *= (1 + this.failureLevel * 0.10f);

        // If the stock is actively failing (any number other than zero) increment the failure level for the next run. This must be done before the initial failureLevel from 0 to 1 or else it will double trigger. This could be combined with the conditional below but for the sake of readability, I won't.
        if (this.failureLevel > 0)
            failureLevel++;

        // Only if the failure level is 0 do we consider failing the stock (with 1/250 chance). Failing a stock just means that we set the level to 1. We use a double for accurate precision.
        if (this.failureLevel == 0 && this.random.nextDouble() <= 0.004d)
            this.failureLevel++;

        return this.price;

    }

}
