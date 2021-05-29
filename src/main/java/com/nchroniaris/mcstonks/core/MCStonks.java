package com.nchroniaris.mcstonks.core;

import com.nchroniaris.mcstonks.io.StocksFile;
import com.nchroniaris.mcstonks.stock.Stock;

import java.util.List;

public class MCStonks {

    /**
     * Data class representing the various parameters (CLI options) that this class can take. This is used in contrast to a parametrized constructor for the outer class -- and allows Main to fill in this object incrementally and pass it all in one go (otherwise it would have to create its own storage for the values, which would be a bit messy).
     * The instance variables are designed to be accessed directly.
     */
    public static class Options {

        public String pathToStocksFile;
        public boolean quiet;

        /**
         * Default constructor. Upon instantiation, it fills in the default values when those options are not provided.
         */
        public Options() {

            // null meaning that it has not been provided. By contrast, anything other than null, including the empty string, is a provided value.
            this.pathToStocksFile = null;
            this.quiet = false;

        }

        /**
         * Copy constructor
         *
         * @param options The options class you want to duplicate
         */
        public Options(Options options) {

            if (options == null)
                throw new IllegalArgumentException("You cannot duplicate an instance of Options from a null object.");

            this.pathToStocksFile = options.pathToStocksFile;
            this.quiet = options.quiet;

        }

    }

    private final Options options;

    /**
     * Main constructor of the program.
     *
     * @param options A {@link MCStonks.Options} class filled with some values. You may pass a default instance of this class (see {@link MCStonks.Options#Options()})
     */
    public MCStonks(Options options) {

        if (options == null)
            throw new IllegalArgumentException("The options parameter cannot be null!");

        // Copy the options object so that it will not be modified from outside the class.
        this.options = new Options(options);

    }

    /**
     * Main driver code of the program
     */
    public void run() {

        StocksFile stocksFile = new StocksFile(options.pathToStocksFile);

        // Read stock data from file and store in a list
        List<Stock> stockList = stocksFile.readStocks();

        // Advance every stock one time only.
        for (Stock stock : stockList)
            stock.advance();

        // Recall that the above mutates the objects in StockCollection so we can just pass it back to be written to the same file -- all without any extra work.
        stocksFile.writeStocks(stockList);

        if (!options.quiet)
            System.out.println("Successfully advanced all stocks by one iteration.");

    }

}
