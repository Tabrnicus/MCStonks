package com.nchroniaris.mcstonks.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import com.nchroniaris.mcstonks.io.adapter.SignAdapter;
import com.nchroniaris.mcstonks.model.Sign;
import com.nchroniaris.mcstonks.stock.BabyStock;
import com.nchroniaris.mcstonks.stock.MemeStock;
import com.nchroniaris.mcstonks.stock.RiskyStock;
import com.nchroniaris.mcstonks.stock.Stock;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is the main way to read/write data from the main stocks file. This file stores the values of each stock, used to advance the price further.
 */
public class StocksFile {

    // Default filename, used in the case that a folder (not file) is provided
    private static final String DEFAULT_FILENAME = "stonks.json";

    // Represents the Type of a List<Stock> using the static getParameterized() method. This is to get around the fact that you cannot represent a generic type in Java.
    private static final Type TYPE_LIST_STOCK = TypeToken.getParameterized(List.class, Stock.class).getType();

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
                .registerTypeAdapterFactory(this.createStockTypeAdapterFactory())
                .create();

        // If the given path is a directory, convert the File object such that it points to the default filename, in that given directory
        if (this.stocksFile.isDirectory())
            this.stocksFile = new File(this.stocksFile, StocksFile.DEFAULT_FILENAME);

        try {

            // Creates a new file, if it returns false the file already exists and nothing else happens, but if it returns true, that means the file previously did NOT exist, and it has now been created. We do this to not only create a file if it doesn't exist, but also to double check that it is a proper file.
            if (this.stocksFile.createNewFile()) {

                // Write default values to the file
                System.err.printf("WARNING: Stocks file (%s) not found, creating...%n", this.stocksFile.getPath());
                this.writeStocks(StocksFile.createDefaultStockList());

            }

        } catch (IOException e) {

            e.printStackTrace();
            throw new IllegalArgumentException(String.format("The given path (%s) is not a valid file nor a valid directory!", pathName));

        }

    }

    /**
     * Constructs a {@code List<Stock>} that represents the default collection of stocks when no data is otherwise present
     *
     * @return Default collection of stocks, in a {@code List<Stock>}.
     */
    private static List<Stock> createDefaultStockList() {

        List<Stock> stockList = new ArrayList<>();

        stockList.add(new BabyStock());
        stockList.add(new RiskyStock());
        stockList.add(new MemeStock());

        return stockList;

    }

    /**
     * Constructs a {@link RuntimeTypeAdapterFactory} for use in the method {@link GsonBuilder#registerTypeAdapterFactory(TypeAdapterFactory)}. This factory registers all the subtypes of {@link Stock} so that they can be serialized/deserialized properly.
     *
     * @return A {@link RuntimeTypeAdapterFactory} defining {@link Stock} and its subclasses.
     */
    private TypeAdapterFactory createStockTypeAdapterFactory() {

        return RuntimeTypeAdapterFactory.of(Stock.class, "stockType")
                .registerSubtype(BabyStock.class)
                .registerSubtype(RiskyStock.class)
                .registerSubtype(MemeStock.class);

    }

    /**
     * Reads the stock data from the file and returns it as a {@code List<Stock>}. This object is meant to be mutated and passed back to this class's {@link #writeStocks(List)}.
     *
     * @return A {@code List<Stock>} that has as elements, the data of all the stocks from this file.
     */
    public List<Stock> readStocks() {

        List<Stock> stockList = null;

        // Trying to open file set in the constructor as a reader
        try (BufferedReader json = new BufferedReader(new FileReader(this.stocksFile))) {

            // Ask Gson to parse the json file as a list of Stocks, using the TypeToken defined above. This will return null if it is somehow invalid.
            stockList = this.gson.fromJson(json, StocksFile.TYPE_LIST_STOCK);

        } catch (FileNotFoundException e) {

            throw new IllegalStateException(String.format("The file (%s) no longer exists! Was it deleted as the program was running?", this.stocksFile.getPath()));

        } catch (IOException e) {

            throw new IllegalStateException(String.format("There was an issue opening the file (%s). It is likely corrupted, so I would suggest you delete the file and try again.", this.stocksFile.getPath()));

        } catch (JsonParseException e) {

            throw new IllegalStateException(String.format("The file (%s) contains JSON that is not parsable by this program. It is likely corrupted, so I would suggest you delete the file and try again.", this.stocksFile.getPath()));

        }

        // If anything *else* goes wrong, execution will stop here.
        if (stockList == null)
            throw new IllegalStateException(String.format("Something else went wrong parsing the file (%s). It is likely corrupted, so I would suggest you delete the file and try again.", this.stocksFile.getPath()));

        return stockList;

    }

    /**
     * Writes the passed in {@code List<Stock>} to the stocks file, overwriting any previous values stored there.
     *
     * @param stockList A valid {@code List<Stock>}. The order that stocks occur in this list will not really matter that much because firstly the entire file is overwritten and secondly a user of this "API" should use {@link Stock#getUUID()} to identify a stock, not the order.
     */
    public void writeStocks(List<Stock> stockList) {

        // Try to open file set in the constructor as a writer
        try (BufferedWriter json = new BufferedWriter(new FileWriter(this.stocksFile))) {

            // We specify the parameterized type here because of type erasure (I think). For some reason without it Gson doesn't realize that this is a List<Stock> and never invokes the RuntimeTypeAdapterFactory we defined beforehand.
            json.write(this.gson.toJson(stockList, StocksFile.TYPE_LIST_STOCK));

        } catch (IOException e) {

            e.printStackTrace();
            System.exit(1);

        }

    }

}
