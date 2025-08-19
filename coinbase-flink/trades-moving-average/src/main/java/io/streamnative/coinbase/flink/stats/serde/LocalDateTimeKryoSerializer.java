package io.streamnative.coinbase.flink.stats.serde;

import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeKryoSerializer extends Serializer<LocalDateTime> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void write(Kryo kryo, Output output, LocalDateTime object) {
        output.writeString(object.format(FORMATTER));
    }

    @Override
    public LocalDateTime read(Kryo kryo, Input input, Class<LocalDateTime> type) {
        return LocalDateTime.parse(input.readString(), FORMATTER);
    }
}
