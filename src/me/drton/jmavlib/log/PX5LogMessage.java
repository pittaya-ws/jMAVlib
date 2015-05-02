package me.drton.jmavlib.log;

import java.util.List;

/**
 * User: ton Date: 03.06.13 Time: 16:18
 */
public class PX5LogMessage {
    public final PX5LogMessageDescription description;
    private final int id;
    private final List<Object> data;

    public PX5LogMessage(PX5LogMessageDescription description, int id, List<Object> data) {
        this.description = description;
        this.id = id;
        this.data = data;
    }

    public Object get(int idx) {
        return data.get(idx);
    }

    public long getLong(int idx) {
        return (Long) data.get(idx);
    }

    public Object get(String field) {
        Integer idx = description.fieldsMap.get(field);
        return idx == null ? null : data.get(idx);
    }

    @Override
    public String toString() {
        return String.format("PX5LogMessage: type=%s, id=%s, name=%s, data=%s", description.type, id, description.name, data);
    }
}
