# MC Stonks
A small for-fun project that simulates a "stock market", used in one of my Minecraft servers.

## Description
This program acts as a simulator for "stock" prices. Firstly, a "stock" is mostly a mathematical "model" (basically just a fancy self-updating equation). Each type of stock has a different equation and unique mechanics surrounding said equation. All of these equations rely heavily on random components.

Upon every run of this program, the prices (and related values) are first read from a file, advanced one iteration, and finally saved back to the same file. This way, by running the program many times over, you can simulate price movement.

This program is only designed to update files with new price data, not enable an actual stock market. The way you use the data is up to you. Specifically, you can check out my [SignStonks](https://github.com/Tardnicus/SignStonks) project, which uses the data from this program to do exactly what I mentioned, within a Minecraft server.

Currently, the program supports 3 types of stocks: "baby", "risky", and "meme". As the names might suggest, the stocks go from least "risky" to most "risky". By "risk", I mean in terms of trying to make "money" off of it by buying and selling shares at certain prices.

One important detail is that each stock type supports bankruptcy. This means that eventually, every stock will "go bankrupt" in the sense that its price freaks out and then crashes down to the minimum value of $0.25. The intent with this feature is to make shares that were bought prior to the bankruptcy worthless (unable to sell).

## Installation
To "install" this program, just download the built `.jar` from the [Releases Page](https://github.com/Tardnicus/mc-stonks/releases) or build from source.

When using the binary, all you need is Java 8. All other libraries are precompiled in the `.jar` file. When building from source, you need to download the dependencies (but Gradle can do that for you). 

Other Java versions may work, but this was tested mainly on Java 8.

## Usage
This is a command line application. To use it, use the JRE in a shell:

```shell script
java -jar mc-stonks.jar <path_to_stocks_file | path_to_folder> [-q | --quiet] [-h | --help ]
```

In plain english, run `java -jar mc-stonks.jar`, and provide it with one argument that is either the path to the file you want to create/update, or the parent folder where you want the file to be. Additionally, you may provide optional parameters `-h` and `-q`.

If used as part of a "stock market", it is suggested to run this to updates the prices once every hour or so.

### Options
The following options are available:

| Option | Description |
|--------|-------------|
| `-h` or `--help`     | Displays the help screen
| `-q` or `--quiet`    | Suppresses non-error output

## Building from Source
The project uses Gradle, and there's a custom task for building a fat `.jar` (has all the dependencies included in the file):

```shell script
# UNIX based OSs
./gradlew jar

# Windows
gradlew.bat jar
```

The built `.jar` will be in `build/libs`.
