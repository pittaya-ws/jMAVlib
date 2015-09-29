package me.drton.jmavlib.log.ulog;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * User: ton Date: 03.06.13 Time: 14:35
 */
public class MessageFormat {
    static class FieldFormat {
        public final String name;
        public final String type;
        public final int size;

        public FieldFormat(String formatStr) {
            String[] p = formatStr.split(" ");
            name = p[1];
            if (p[0].contains("[")) {
                // Array
                String[] q = p[0].split("\\[");
                type = q[0];
                size = Integer.parseInt(q[1].split("\\]")[0]);
            } else {
                type = p[0];
                size = -1;
            }
        }

        public String getFullTypeString() {
            String size_str = (size >= 0) ? ("[" + size + "]") : "";
            return type + size_str;

        }

        boolean isArray() {
            return size >= 0 && !"char".equals(type);
        }

        public String toString() {
            return String.format("%s %s", getFullTypeString(), name);
        }
    }

    private static Charset charset = Charset.forName("latin1");

    public final int msgID;
    public final int size;
    public final String name;
    public final FieldFormat[] fields;
    public final Map<String, Integer> fieldsMap = new HashMap<String, Integer>();

    private static String getString(ByteBuffer buffer, int len) {
        byte[] strBuf = new byte[len];
        buffer.get(strBuf);
        String[] p = new String(strBuf, charset).split("\0");
        return p.length > 0 ? p[0] : "";
    }

    public MessageFormat(ByteBuffer buffer) {
        msgID = buffer.get() & 0xFF;
        size = buffer.get() & 0xFF;
        int format_len = buffer.get() & 0xFF;
        String[] descr_str = getString(buffer, format_len).split(":");
        name = descr_str[0];
        if (descr_str.length > 1) {
            String[] fields_descrs_str = descr_str[1].split(";");
            fields = new FieldFormat[fields_descrs_str.length];
            for (int i = 0; i < fields_descrs_str.length; i++) {
                String field_format_str = fields_descrs_str[i];
                fields[i] = new FieldFormat(field_format_str);
                fieldsMap.put(fields[i].name, i);
            }
        } else {
            fields = new FieldFormat[0];
        }
    }

    static Object getValue(ByteBuffer buffer, String type) {
        Object v;
        if (type.equals("float")) {
            v = buffer.getFloat();
        } else if (type.equals("double")) {
            v = buffer.getDouble();
        } else if (type.equals("int8_t") || type.equals("bool")) {
            v = (int) buffer.get();
        } else if (type.equals("uint8_t")) {
            v = buffer.get() & 0xFF;
        } else if (type.equals("int16_t")) {
            v = (int) buffer.getShort();
        } else if (type.equals("uint16_t")) {
            v = buffer.getShort() & 0xFFFF;
        } else if (type.equals("int32_t")) {
            v = buffer.getInt();
        } else if (type.equals("uint32_t")) {
            v = buffer.getInt() & 0xFFFFFFFFl;
        } else if (type.equals("int64_t")) {
            v = buffer.getLong();
        } else if (type.equals("uint64_t")) {
            v = buffer.getLong();
        } else if (type.equals("char")) {
            v = buffer.get();
        } else {
            throw new RuntimeException("Unsupported type: " + type);
        }
        return v;
    }

    public List<Object> parseBody(ByteBuffer buffer) {
        List<Object> data = new ArrayList<Object>(fields.length);
        for (FieldFormat field : fields) {
            Object obj;
            if (field.size >= 0) {
                if (field.type.equals("char")) {
                    byte[] stringBytes = new byte[field.size];
                    buffer.get(stringBytes);
                    String s = new String(stringBytes);
                    obj = s.substring(0, s.indexOf('\0'));
                } else {
                    Object[] arr = new Object[field.size];
                    for (int i = 0; i < field.size; i++) {
                        arr[i] = getValue(buffer, field.type);
                    }
                    obj = arr;
                }
            } else {
                obj = getValue(buffer, field.type);
            }
            data.add(obj);
        }
        return data;
    }

    public List<String> getFields() {
        List<String> field_names = new ArrayList<String>(fields.length);
        for (FieldFormat field : fields) {
            field_names.add(field.name);
        }
        return field_names;
    }

    @Override
    public String toString() {
        return String.format("FORMAT: type=%s, size=%s, name=%s, fields=%s",
                msgID, size, name, Arrays.asList(fields));
    }
}
