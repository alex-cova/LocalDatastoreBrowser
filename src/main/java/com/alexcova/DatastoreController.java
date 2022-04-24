package com.alexcova;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.ValueType;

import java.util.Map;

public interface DatastoreController {

    void connect(Configuration configuration);

    void load();

    void loadKinds(String namespace);

    Map<String, ValueType> getProperties(Key kind);

    void delete(Key key);

    Entity getEntity(String obj, String namespace, String kind);

    boolean disconnected();

    QueryResults<?> runQuery(String gql, String nameSpace, String kind);

    Key getKind(int index);
}
