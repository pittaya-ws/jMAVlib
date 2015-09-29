package me.drton.jmavlib.log.ulog;

import me.drton.jmavlib.log.BinaryLogReader;
import me.drton.jmavlib.log.FormatErrorException;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ton Date: 03.06.13 Time: 14:18
 */
public class ULogReader extends BinaryLogReader {
    static final byte MESSAGE_TYPE_FORMAT = (byte) 'F';
    static final byte MESSAGE_TYPE_DATA = (byte) 'D';

    private long dataStart = 0;
    private Map<Integer, MessageFormat> messageFormats
            = new HashMap<Integer, MessageFormat>();
    private Map<String, String> fieldsList = null;
    private long sizeUpdates = -1;
    private long sizeMicroseconds = -1;
    private long startMicroseconds = -1;
    private long utcTimeReference = -1;
    private Map<String, Object> version = new HashMap<String, Object>();
    private Map<String, Object> parameters = new HashMap<String, Object>();

    public ULogReader(String fileName) throws IOException, FormatErrorException {
        super(fileName);
        readFormats();
        updateStatistics();
    }

    @Override
    public String getFormat() {
        return "ULog";
    }

    @Override
    public long getSizeUpdates() {
        return sizeUpdates;
    }

    @Override
    public long getStartMicroseconds() {
        return startMicroseconds;
    }

    @Override
    public long getSizeMicroseconds() {
        return sizeMicroseconds;
    }

    @Override
    public long getUTCTimeReferenceMicroseconds() {
        return utcTimeReference;
    }

    @Override
    public Map<String, Object> getVersion() {
        return version;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    private void updateStatistics() throws IOException, FormatErrorException {
        position(dataStart);
        long packetsNum = 0;
        long timeStart = -1;
        long timeEnd = -1;
        while (true) {
            Object msg;
            try {
                msg = readMessage();
            } catch (EOFException e) {
                break;
            }
            packetsNum++;

            if (msg instanceof MessageData) {
                MessageData msgData = (MessageData) msg;

                if (timeStart < 0) {
                    timeStart = msgData.timestamp;
                }
                timeEnd = msgData.timestamp;

                // Version
                if ("_VERSION".equals(msgData.format.name)) {
                    String sw = (String) msgData.get("software");
                    if (sw != null) {
                        version.put("FW", sw);
                    }
                    String hw = (String) msgData.get("hardware");
                    if (hw != null) {
                        version.put("HW", hw);
                    }
                }

                // Parameters
                if ("_PARMETER".equals(msgData.format.name)) {
                    parameters.put((String) msgData.get("name"), msgData.get("value"));
                }

                if ("GPS".equals(msgData.format.name)) {
                    if (utcTimeReference < 0) {
                        try {
                            int fix = ((Number) msgData.get("Fix")).intValue();
                            long gpsT = ((Number) msgData.get("GPSTime")).longValue();
                            if (fix >= 3 && gpsT > 0) {
                                utcTimeReference = gpsT - timeEnd;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }
        startMicroseconds = timeStart;
        sizeUpdates = packetsNum;
        sizeMicroseconds = timeEnd - timeStart;
        seek(0);
    }

    @Override
    public boolean seek(long seekTime) throws IOException, FormatErrorException {
        position(dataStart);
        if (seekTime == 0) {      // Seek to start of log
            return true;
        }
        // Seek to specified timestamp without parsing all messages
        try {
            while (true) {
                fillBuffer(11);
                buffer.mark();
                int msgType = buffer.get() & 0xFF;
                if (msgType == MESSAGE_TYPE_DATA) {
                    int msgID = buffer.get() & 0xFF;
                    buffer.get();   // MultiID
                    long timestamp = buffer.getLong();
                    if (timestamp >= seekTime) {
                        // Time found
                        buffer.reset();
                        return true;
                    }
                    MessageFormat msgFormat = messageFormats.get(msgID);
                    if (msgFormat == null) {
                        buffer.reset();
                        throw new FormatErrorException("Unknown message ID: " + msgID + " at " + position());
                    }
                    fillBuffer(msgFormat.size);
                    buffer.position(buffer.position() + msgFormat.size);
                } else {
                    buffer.reset();
                    throw new FormatErrorException("Unknown message type: " + msgType + " at " + position());
                }
            }
        } catch (EOFException e) {
            return false;
        }
    }

    private void applyMsg(Map<String, Object> update, MessageData msg) {
        applyMsgAsName(update, msg, msg.format.name + "[" + msg.multiID + "]");
        if (msg.isActive) {
            applyMsgAsName(update, msg, msg.format.name);
        }
    }

    void applyMsgAsName(Map<String, Object> update, MessageData msg, String msg_name) {
        MessageFormat.FieldFormat[] fields = msg.format.fields;
        for (int i = 0; i < fields.length; i++) {
            MessageFormat.FieldFormat field = fields[i];
            if (field.isArray()) {
                for (int j = 0; j < field.size; j++) {
                    update.put(msg_name + "." + field.name + "[" + j + "]", ((Object[]) msg.get(i))[j]);
                }
            } else {
                update.put(msg_name + "." + field.name, msg.get(i));
            }
        }
    }

    @Override
    public long readUpdate(Map<String, Object> update) throws IOException, FormatErrorException {
        while (true) {
            Object msg = readMessage();
            if (msg instanceof MessageData) {
                applyMsg(update, (MessageData) msg);
                return ((MessageData) msg).timestamp;
            }
        }
    }

    @Override
    public Map<String, String> getFields() {
        return fieldsList;
    }

    private void readFormats() throws IOException, FormatErrorException {
        fieldsList = new HashMap<String, String>();
        try {
            while (true) {
                fillBuffer(4);  // Min size of FORMAT message
                while (true) {
                    buffer.mark();
                    try {
                        int msgType = buffer.get() & 0xFF;
                        if (msgType == MESSAGE_TYPE_FORMAT) {
                            // Message format
                            MessageFormat msgFormat = new MessageFormat(buffer);
                            messageFormats.put(msgFormat.msgID, msgFormat);
                            if (msgFormat.name.charAt(0) != '_') {
                                for (int i = 0; i < msgFormat.fields.length; i++) {
                                    MessageFormat.FieldFormat fieldDescr = msgFormat.fields[i];
                                    if (fieldDescr.isArray()) {
                                        for (int j = 0; j < fieldDescr.size; j++) {
                                            fieldsList.put(msgFormat.name + "." + fieldDescr.name + "[" + j + "]", fieldDescr.type);
                                        }
                                    } else {
                                        fieldsList.put(msgFormat.name + "." + fieldDescr.name, fieldDescr.type);
                                    }
                                }
                            }
                        } else {
                            // Data message
                            buffer.reset();
                            dataStart = position();
                            return;
                        }
                    } catch (BufferUnderflowException e) {
                        buffer.reset();
                        break;
                    }
                }
            }
        } catch (EOFException ignored) {
        }
    }

    /**
     * Read next message from log
     *
     * @return log message
     * @throws IOException  on IO error
     * @throws EOFException on end of stream
     */
    public Object readMessage() throws IOException, FormatErrorException {
        fillBuffer(2);
        buffer.mark();
        int msgType = buffer.get() & 0xFF;
        if (msgType == MESSAGE_TYPE_DATA) {
            int msgID = buffer.get() & 0xFF;
            MessageFormat msgFormat = messageFormats.get(msgID);
            if (msgFormat == null) {
                buffer.reset();
                throw new FormatErrorException("Unknown message ID: " + msgID + " at " + position());
            }
            fillBuffer(9 + msgFormat.size);
            return new MessageData(msgFormat, buffer);
        } else {
            buffer.reset();
            throw new FormatErrorException("Unknown message type: " + msgType + " at " + position());
        }
    }

    public static void main(String[] args) throws Exception {
        ULogReader reader = new ULogReader("test.ulg");
        long tStart = System.currentTimeMillis();
        while (true) {
//            try {
//                Object msg = reader.readMessage();
//            } catch (EOFException e) {
//                break;
//            }
            Map<String, Object> update = new HashMap<String, Object>();
            try {
                reader.readUpdate(update);
                System.out.println(update);
            } catch (EOFException e) {
                break;
            }
        }
        long tEnd = System.currentTimeMillis();
        System.out.println(tEnd - tStart);
        reader.close();
    }
}
