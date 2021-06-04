package com.nchroniaris.mcstonks.core;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static final String USAGE_STRING = "java -jar mc-stonks.jar [path_to_stocks_file | path_to_folder] [-v | --verbose] [-h | --help]";

    public static void main(String[] args) {

        // Separate combined arguments to make the following more deterministic
        List<String> allArguments = Main.convertArguments(args);

        // This will hold all the positional arguments after the while loop
        List<String> positionalArguments = new ArrayList<>();

        MCStonks.Options options = new MCStonks.Options();

        // This while loop will eventually exhaust (by way of "popping" elements) all the non-positional arguments (and their pairings), and upon exiting the loop the positionalArguments list will only consist of positional arguments.
        while (!allArguments.isEmpty()) {

            String argument = allArguments.remove(0);

            // While the arguments list still has something, "pop" the head element and analyze it, popping more elements if necessary.
            switch (argument) {

                // Print help and usage information and quit
                case "-h":
                case "--help":
                    Main.printHelp();
                    return;

                case "-v":
                case "--verbose":
                    options.verbose = true;
                    break;

                // TODO: 2021-05-20 add more arguments

                // If we are here, that means the element we are analyzing doesn't match a dashed argument, which by definition means it's a positional argument.
                default:

                    // Extra check for extraneous dashed options (to avoid things like 'java -jar mc-stonks.jar -j' interpreting '-j' as a filename)
                    if (argument.startsWith("-")) {
                        System.err.printf("ERROR: Unrecognized option '%s'%n", argument);
                        Main.printUsage();
                        return;
                    }

                    // Otherwise, add the argument to the positional args list
                    positionalArguments.add(argument);

            }

        }

        // Extract first (and only) positional argument
        try {

            // Remove head and set appropriate path from string.
            options.pathToStocksFile = Paths.get(positionalArguments.remove(0));

        } catch (IndexOutOfBoundsException e) {

            // If there is no first positional argument we will set the path to null to indicate no user input.
            options.pathToStocksFile = null;

        } catch (InvalidPathException e) {

            // InvalidPathException will be raised if applicable and should be descriptive enough on its own, but we print the usage string to supplement it.
            Main.printUsage();
            throw e;

        }

        // Run program with options that we just set
        MCStonks mcStonks = new MCStonks(options);
        mcStonks.run();

    }

    /**
     * Creates a new list of String arguments, with single combination arguments, like "-xyz" into full arguments, like "-x", "-y", and "-z" while preserving order.
     *
     * @param programArgs A primitive String array that represents the current argument list
     * @return A {@code List<String>} that expands combination arguments into multiple entries, but is otherwise the same.
     */
    private static List<String> convertArguments(String[] programArgs) {

        List<String> convertedArgs = new ArrayList<>();

        // Iterate through all the arguments given by the OS
        for (String arg : programArgs) {

            // Trim the argument to allow things like `... " -h" ...` to get through (i.e. quoted at the shell)
            arg = arg.trim();

            // If the argument is a single-dash argument (the character class is for safety), with 1 or more arguments (like "-scd" for example)
            if (arg.matches("^-[A-Za-z0-9]+$")) {

                // For every letter (index 1 and onward, basically excluding the dash), add that as a new entry in the new list. This essentially splits arguments like "-scd" multiple ones: "-s", "-c", and "-d".
                for (int i = 1; i < arg.length(); i++)
                    convertedArgs.add("-" + arg.charAt(i));

            } else {

                // If it's not a single argument in that format just add the argument verbatim. This case will execute for double-dashed arguments, like "--help"
                convertedArgs.add(arg);

            }

        }

        return convertedArgs;

    }

    /**
     * Prints out the full usage and help information. This includes the usage text, link to the GitHub repo, options summary, and additional information.
     */
    private static void printHelp() {

        System.out.printf("Usage: %s%n%n", Main.USAGE_STRING);
        System.out.println("mc-stonks is a small, for-fun program that generates fake \"stock\" data.");
        System.out.println("See https://github.com/Tardnicus/mc-stonks for more detailed information.");
        System.out.println();

        System.out.println("Argument Summary:");

        System.out.println("\tpath_to_stocks_file, path_to_folder");
        System.out.println("\t\tPath to the file (or parent folder) where the main program data is stored.");
        System.out.println("\t\tThe file need not exist and can be named pretty much anything -- although a .json extension is recommended for clarity.");
        System.out.println();

        System.out.println("Option Summary:");

        System.out.println("\t-h, --help");
        System.out.println("\t\tShows this help menu");
        System.out.println();

        System.out.println("\t-q, --quiet");
        System.out.println("\t\tSuppresses all regular, non-error output");
        System.out.println();

        // TODO: 2021-05-20 -a --add, -r --remove, -s --set, -l --list, -o --output
        //       (new options for when StockCollection is removed)

    }

    /**
     * Prints out the usage information to stdout. Meant to be called when an incorrect parameter has been read. This is the brief version, as the full help text is in printHelp().
     */
    private static void printUsage() {

        System.out.printf("Usage: %s%n%n", Main.USAGE_STRING);
        System.out.println("Try 'java -jar mc-stonks.jar -h' for more information.");
        System.out.println();

    }

}
