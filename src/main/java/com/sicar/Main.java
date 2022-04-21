package com.sicar;

import com.google.cloud.datastore.*;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        Datastore datastore = DatastoreOptions.newBuilder()
                .setProjectId("sicar-x")
                .setNamespace("1")
                .setHost("http://localhost:8081")
                .build()
                .getService();

        KeyFactory keyFactory = datastore.newKeyFactory().setKind("__namespace__");
        Query<Key> query =
                Query.newKeyQueryBuilder()
                        .setKind("__namespace__")
                        .build();

        QueryResults<Key> results = datastore.run(query);

        if (!results.hasNext()) {
            System.out.println("No content.");
        }


        List<String> namespaces = new ArrayList<>();
        while (results.hasNext()) {
            namespaces.add(results.next().getName());
        }

        System.out.println("Name spaces found: " + namespaces.size());

        for (String namespace : namespaces) {
            System.out.println(namespace);
        }

        Entity priceList = datastore.get(datastore.newKeyFactory()
                .setKind("priceList")
                .newKey("b41fe473-24ae-4737-804a-e1de45929a70"));

        System.out.println(priceList);

    }
}
