package com.alexcova;

import com.google.cloud.datastore.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class DatastoreControllerImpl implements DatastoreController {

    private Datastore datastore;
    private final List<String> namespaces;
    private final List<Key> kinds;

    private Consumer<List<String>> namespacesListener;
    private Consumer<List<Key>> kindsListener;

    public DatastoreControllerImpl() {
        this.namespaces = new ArrayList<>();
        this.kinds = new ArrayList<>();
    }

    public DatastoreControllerImpl setNamespacesListener(Consumer<List<String>> namespacesListener) {
        this.namespacesListener = namespacesListener;
        return this;
    }

    public DatastoreControllerImpl setKindsListener(Consumer<List<Key>> kindsListener) {
        this.kindsListener = kindsListener;
        return this;
    }

    public void connect(@NotNull Configuration configuration) {
        datastore = DatastoreOptions.newBuilder()
                .setProjectId(configuration.projectId())
                .setHost(configuration.host())
                .build()
                .getService();

        load();
    }

    public List<String> getNamespaces() {
        return namespaces;
    }

    public List<Key> getKinds() {
        return kinds;
    }

    @Override
    public void load() {
        loadNamespaces();
        loadKinds(null);
    }

    public void loadNamespaces() {

        namespaces.clear();

        QueryResults<Key> nameSpaceResults = datastore.run(Query.newKeyQueryBuilder()
                .setKind("__namespace__")
                .build());

        if (!nameSpaceResults.hasNext()) {
            return;
        }

        while (nameSpaceResults.hasNext()) {
            namespaces.add(nameSpaceResults.next().getName());
        }

        namespaces.add(0, "[Default]");

        if (namespacesListener != null) namespacesListener.accept(namespaces);
    }

    @Override
    public void loadKinds(@Nullable String namespace) {
        System.out.println("Loafing kinds...");
        kinds.clear();

        var query =
                Query.newKeyQueryBuilder()
                        .setKind("__kind__");

        if (namespace != null) query.setNamespace(namespace);

        QueryResults<Key> results = datastore.run(query.build());

        if (!results.hasNext()) {
            return;
        }

        while (results.hasNext()) {
            kinds.add(results.next());
        }

        if (kindsListener != null) kindsListener.accept(kinds);
    }

    @Override
    public @NotNull Map<String, ValueType> getProperties(@NotNull Key kind) {

        QueryResults<Entity> results = datastore.run(Query.newEntityQueryBuilder()
                .setKind("__property__")
                .setNamespace(kind.getNamespace())
                .setFilter(StructuredQuery.PropertyFilter.hasAncestor(kind))
                .build());

        Map<String, ValueType> representationsMap = new HashMap<>();

        while (results.hasNext()) {
            Entity result = results.next();
            String propertyName = result.getKey().getName();

            if (propertyName.contains(".")) {
                propertyName = propertyName.substring(0, propertyName.indexOf(".")) + "."
                        + propertyName.substring(propertyName.lastIndexOf(".") + 1);
            }

            List<StringValue> representations = result.getList("property_representation");


            for (StringValue value : representations) {
                representationsMap.put(propertyName, value.getType());
            }
        }

        return representationsMap;
    }

    @Override
    public void delete(Key key) {
        datastore.delete(key);
    }

    @Override
    public QueryResults<?> runQuery(String gql, String nameSpace, String kind) {
        var query = Query.newGqlQueryBuilder(gql);

        if (nameSpace != null) {
            query.setNamespace(nameSpace);
        }

        return datastore.run(query.build());
    }

    @Override
    public Entity getEntity(String id, String namespace, String kind) {
        KeyFactory keyFactory = datastore.newKeyFactory()
                .setProjectId(datastore.getOptions().getProjectId())
                .setKind(kind);

        if (namespace != null) keyFactory.setNamespace(namespace);

        Key key;

        if (id.matches("[\\d+]")) {
            key = keyFactory.newKey(Long.parseLong(id));
        } else {
            key = keyFactory.newKey(id);
        }

        return datastore.get(key);
    }

    @Override
    public @Nullable Key getKind(int index) {
        if (index > kinds.size()) return null;
        if (kinds.isEmpty()) return null;
        return kinds.get(index);
    }

    @Override
    public boolean disconnected() {
        return datastore == null;
    }
}
