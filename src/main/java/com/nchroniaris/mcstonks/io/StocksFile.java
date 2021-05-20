package com.nchroniaris.mcstonks.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.nchroniaris.mcstonks.io.adapter.SignAdapter;
import com.nchroniaris.mcstonks.model.Sign;
import com.nchroniaris.mcstonks.stock.BabyStock;
import com.nchroniaris.mcstonks.stock.MemeStock;
import com.nchroniaris.mcstonks.stock.RiskyStock;
import com.nchroniaris.mcstonks.stock.StockCollection;

import java.io.*;

/**
 * This class is the main way to read/write data from the main stocks file. This file stores the values of each stock, used to advance the price further.
 */
public class StocksFile {

    // Default filename, used in the case that a folder (not file) is provided
    private static final String DEFAULT_FILENAME = "stonks.json";

    private File stocksFile;

    private final Gson gson;

    /**
     * Upon construction, this will use the given pathname to validate or create a file that contains stock data.
     * If a file is created, the file will initially be populated with default values, and is guaranteed to be as such by the time this object is created. Whether the write methods are accessed after the fact is not of importance to this class.
     *
     * @param pathName A path pointing to either the file desired, or its parent folder (either is fine). If the file does not exist
     */
    public StocksFile(String pathName) {

        if (pathName == null)
            throw new IllegalArgumentException("You must provide a valid file path!");

        this.stocksFile = new File(pathName);

        // We register an adapter for the Sign enum in order to have more readable outputs (see javadoc for Sign). This causes Gson to invoke our adapter every time it encounters a Sign object
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Sign.class, new SignAdapter())
                .create();

        // If the given path is a directory, convert the File object such that it points to the default filename, in that given directory
        if (this.stocksFile.isDirectory())
            this.stocksFile = new File(this.stocksFile, StocksFile.DEFAULT_FILENAME);

        try {

            // Creates a new file, if it returns false the file already exists and nothing else happens, but if it returns true, that means the file previously did NOT exist, and it has now been created. We do this to not only create a file if it doesn't exist, but also to double check that it is a proper file.
            if (this.stocksFile.createNewFile()) {

                // Write default values to the file
                System.err.printf("WARNING: Stocks file (%s) not found, creating...%n", this.stocksFile.getPath());
                this.writeCollection(new StockCollection(new BabyStock(), new RiskyStock(), new MemeStock()));

            }

        } catch (IOException e) {

            e.printStackTrace();
            throw new IllegalArgumentException(String.format("The given path (%s) is not a valid file nor a valid directory!", pathName));

        }

    }

    /**
     * Reads the stock data from the file and returns it as a {@link StockCollection}. This object is meant to be mutated and passed back to this class's {@link #writeCollection(StockCollection)}.
     * <b>Be warned, in a future release this will likely return a {@code List<Stock>} and StockCollection will be deprecated.</b>
     *
     * @return A {@link StockCollection} that has as members, the data of all the stocks from this file.
     */
    public StockCollection readCollection() {

        StockCollection collection = null;

        // Trying to open file set in the constructor as a reader
        try (BufferedReader json = new BufferedReader(new FileReader(this.stocksFile))) {

            // Ask Gson to parse the json file as a StockCollection class. This will return null if it is somehow invalid.
            collection = this.gson.fromJson(json, StockCollection.class);

        } catch (FileNotFoundException e) {

            throw new IllegalStateException(String.format("The file (%s) no longer exists! Was it deleted as the program was running?", this.stocksFile.getPath()));

        } catch (IOException e) {

            e.printStackTrace();
            System.exit(1);

        } catch (JsonParseException e) {

            e.printStackTrace();

        }

        // If anything goes wrong with the JSON parsing, execution will stop here.
        if (collection == null)
            throw new IllegalStateException(String.format("The file (%s) contains JSON that is not parsable by this program. It is likely corrupted, so I would suggest you delete the file and try again.", this.stocksFile.getPath()));

        return collection;

    }

    /**
     * Writes the passed in {@link StockCollection} to the stocks file, overwriting any previous values stored there.
     *
     * @param collection A valid {@link StockCollection}
     */
    public void writeCollection(StockCollection collection) {

        // Try to open file set in the constructor as a writer
        try (BufferedWriter json = new BufferedWriter(new FileWriter(this.stocksFile))) {

            // Write to the file the contents of the serialized StockCollection object.
            json.write(this.gson.toJson(collection));

        } catch (IOException e) {

            e.printStackTrace();
            System.exit(1);

        }

    }

}
