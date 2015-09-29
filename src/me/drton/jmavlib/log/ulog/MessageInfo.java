package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;

/**
 * Created by ton on 29.09.15.
 */
public class MessageInfo {
    public final String valueType;
    public final String key;
    public final Object value;

    public MessageInfo(ByteBuffer buffer) {
        int keyLen = buffer.get() & 0xFF;
        String[] descr_str = MessageFormat.getString(buffer, keyLen).split(" ");
        valueType = descr_str[0];
        key = descr_str[1];
        value = MessageFormat.getValue(buffer, valueType);
    }

    @Override
    public String toString() {
        return String.format("INFO: key=%s, value_type=%s, value=%s", key, valueType, value);
    }
}
