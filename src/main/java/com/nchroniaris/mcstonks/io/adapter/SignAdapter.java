package com.nchroniaris.mcstonks.io.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.nchroniaris.mcstonks.model.Sign;

import java.io.IOException;

/**
 * Custom Gson adapter in order to serialize signs to their usual numerical representation (+1 and -1, rather than {@code POSITIVE} and {@code NEGATIVE})
 */
public class SignAdapter extends TypeAdapter<Sign> {

    @Override
    public Sign read(JsonReader reader) throws IOException {

        // Peek at the next token and determine if it's null before proceeding
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }

        // Read the next integer and convert it to a sign. Values outside {-1, 1} will create an error here.
        return Sign.fromValue(reader.nextInt());

    }

    @Override
    public void write(JsonWriter writer, Sign sign) throws IOException {

        // If the sign value is null, then write a JSON null and return.
        if (sign == null) {
            writer.nullValue();
            return;
        }

        // Write the *value* (-1 or 1) of the sign to the JSON string.
        writer.value(sign.value());

    }

}
