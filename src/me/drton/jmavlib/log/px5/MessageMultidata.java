package me.drton.jmavlib.log.px5;

import java.nio.ByteBuffer;

/**
 * Created by ton on 17.07.15.
 */
public class MessageMultidata extends MessageData {
    private final int multiID;
    private final boolean isActive;

    public MessageMultidata(MessageFormat format, byte multiIDRaw, ByteBuffer buffer) {
        super(format, buffer);
        this.multiID = multiIDRaw & 0x7F;
        this.isActive = (multiIDRaw & 0x80) != 0;
    }

    public int getMultiID() {
        return multiID;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public String toString() {
        return String.format("MULTIDATA: msg_id=%s, multi_id=%s, name=%s, data=%s", format.msgID, multiID, format.name, data);
    }
}
