package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

/**
 * Created by ton on 17.07.15.
 */
public class MessageTime {
    public static final int SIZE = 8;

    private final long timestamp;

    public MessageTime(ByteBuffer buffer) {
        this.timestamp = buffer.getLong();
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("TIME: %s", timestamp);
    }
}
