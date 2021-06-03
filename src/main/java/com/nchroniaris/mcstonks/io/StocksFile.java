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
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
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

    private Path stocksPath;

    private final Gson gson;

    /**
     * Upon construction, this will use the given {@link Path} to validate or create a file that contains stock data.
     * If a file is created, the file will initially be populated with default values, and is guaranteed to be as such by the time this object is created. Whether the write methods are accessed after the fact is not of importance to this class.
     *
     * @param filePath A {@link Path} pointing to either the file desired, or its parent folder (either is fine). If the file does not exist, A reference to the default file location is made (current working directory, "stonks.json")
     */
    public StocksFile(@Nullable Path filePath) {

        // If argument is null, refer to default filename in working directory. Else just use the path they provided. I'm confident that the direct assignment as opposed to copying is fine
        if (filePath == null)
            this.stocksPath = Paths.get(StocksFile.DEFAULT_FILENAME);
        else
            this.stocksPath = filePath;

        // We register an adapter for the Sign enum in order to have more readable outputs (see javadoc for Sign). This causes Gson to invoke our adapter every time it encounters a Sign object
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Sign.class, new SignAdapter())
                .registerTypeAdapterFactory(this.createStockTypeAdapterFactory())
                .create();

        // If the given path is a directory, resolve the default filename against the file path. The resulting path is such that it points to the default filename, in that given directory
        if (Files.isDirectory(this.stocksPath))
            this.stocksPath = this.stocksPath.resolve(StocksFile.DEFAULT_FILENAME);

        try {

            // Attempts to create a new file. If the call succeeds, that means the file previously did NOT exist, and it has now been created -- however if it fails (with a FileAlreadyExistsException), then the file already exists and nothing else should happen.
            // We do this to not only create a file if it doesn't exist, but also to double check that it is a proper file.
            Files.createFile(this.stocksPath);

            // Write default values to the newly created file, warn user just in case they didn't realize.
            System.err.printf("WARNING: Stocks file (%s) not found, creating...%n", this.stocksPath.toAbsolutePath());
            this.writeStocks(StocksFile.createDefaultStockList());

        } catch (FileAlreadyExistsException ignored) {

            // If the file already exists we don't have to write any default data to it, so we ignore it when this happens

        } catch (NoSuchFileException e) {

            // Also subclasses IOException but we catch it and throw a more descriptive error
            throw new IllegalArgumentException(String.format("The given path (%s) is not a valid file nor a valid directory! Please check if the directories/files exists.", filePath));

        } catch (IOException e) {

            // For any other IO exception, error out
            e.printStackTrace();
            throw new IllegalArgumentException(String.format("There was an IO error trying to process file (%s)!", filePath));

        }

    }

    /**
     * Default constructor. This is equivalent to calling {@code StocksFile(null)}. More specifically this constructs this class with the default filename and path (current working directory, "stonks.json")
     */
    public StocksFile() {

        this(null);

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
        try (BufferedReader json = new BufferedReader(new FileReader(this.stocksPath.toFile()))) {

            // Ask Gson to parse the json file as a list of Stocks, using the TypeToken defined above. This will return null if it is somehow invalid.
            stockList = this.gson.fromJson(json, StocksFile.TYPE_LIST_STOCK);

        } catch (FileNotFoundException e) {

            throw new IllegalStateException(String.format("The file (%s) no longer exists! Was it deleted as the program was running?", this.stocksPath.toAbsolutePath()));

        } catch (IOException e) {

            throw new IllegalStateException(String.format("There was an issue opening the file (%s). It is likely corrupted, so I would suggest you delete the file and try again.", this.stocksPath.toAbsolutePath()));

        } catch (JsonParseException e) {

            throw new IllegalStateException(String.format("The file (%s) contains JSON that is not parsable by this program. It is likely corrupted, so I would suggest you delete the file and try again.", this.stocksPath.toAbsolutePath()));

        }

        // If anything *else* goes wrong, execution will stop here.
        if (stockList == null)
            throw new IllegalStateException(String.format("Something else went wrong parsing the file (%s). It is likely corrupted, so I would suggest you delete the file and try again.", this.stocksPath.toAbsolutePath()));

        return stockList;

    }

    /**
     * Writes the passed in {@code List<Stock>} to the stocks file, overwriting any previous values stored there.
     *
     * @param stockList A valid {@code List<Stock>}. The order that stocks occur in this list will not really matter that much because firstly the entire file is overwritten and secondly a user of this "API" should use {@link Stock#getUUID()} to identify a stock, not the order.
     */
    public void writeStocks(List<Stock> stockList) {

        // Try to open file set in the constructor as a writer
        try (BufferedWriter json = new BufferedWriter(new FileWriter(this.stocksPath.toFile()))) {

            // We specify the parameterized type here because of type erasure (I think). For some reason without it Gson doesn't realize that this is a List<Stock> and never invokes the RuntimeTypeAdapterFactory we defined beforehand.
            json.write(this.gson.toJson(stockList, StocksFile.TYPE_LIST_STOCK));

        } catch (IOException e) {

            e.printStackTrace();
            System.exit(1);

        }

    }

}
