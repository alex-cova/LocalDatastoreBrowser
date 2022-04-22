package com.alexcova.swing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

import java.io.IOException;

public class EntitySerializer {

    private static final EntitySerializer INSTANCE = new EntitySerializer();

    private final ObjectMapper mapper;

    private EntitySerializer() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Value.class, new JsonSerializer<>() {

            @Override
            public void serialize(Value value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

                if (value.get() == null) return;
                if (value.getType() == ValueType.NULL) return;
                if (value.getType() == ValueType.KEY) return;

                serializers.defaultSerializeValue(value.get(), gen);
            }
        });
        mapper.registerModule(module);
    }

    public static String serialize(FullEntity<?> entity) {
        try {
            return INSTANCE.mapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String serializeBeauty(FullEntity<?> entity) {
        try {
            return INSTANCE.mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
