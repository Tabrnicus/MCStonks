package com.nchroniaris.mcstonks.stock;

import java.util.Arrays;
import java.util.List;

/**
 * Pure data class mostly for the purposes of serialization. Since I haven't quite figured out Gson's custom serializers/deserializers, I am going to go with this solution for now, as opposed to a list of Stocks. This class will get removed in the future.
 */
public class StockCollection {

    public final BabyStock babyStock;
    public final RiskyStock riskyStock;
    public final MemeStock memeStock;

    public StockCollection(BabyStock babyStock, RiskyStock riskyStock, MemeStock memeStock) {

        if (babyStock == null || riskyStock == null || memeStock == null)
            throw new IllegalArgumentException("None of babyStock, riskyStock, or memeStock can be null!");

        this.babyStock = babyStock;
        this.riskyStock = riskyStock;
        this.memeStock = memeStock;
    }

    /**
     * Convenience method. Returns a polymorphic representation of all the stocks in the collection. Any changes made to the mutable stocks will reflect in this collection.
     *
     * @return A {@link List} of {@link Stock}s that the caller can advance.
     */
    public List<Stock> toList() {

        return Arrays.asList(this.babyStock, this.riskyStock, this.memeStock);

    }

}
