package com.alexcova.swing;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Value;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TableModel extends DefaultTableModel {

    private List<Entity> entityList = new ArrayList<>();
    private final JTable table;

    public TableModel(JTable table) {
        this.table = table;
    }

    public final void drop() {
        entityList.clear();
        setDataVector(new Object[][]{}, new String[]{});
    }

    public TableModel setData(List<Entity> entityList) {
        this.entityList = entityList;
        return this;
    }

    public TableModel setAll(List<Entity> entityList) {
        this.entityList = entityList;
        return this;
    }

    public boolean isEmpty() {
        return entityList.isEmpty();
    }

    public Entity get(int index) {
        return entityList.get(index);
    }

    public void add(Entity entity) {
        entityList.add(entity);
    }

    public void render() {
        List<String> columns = new ArrayList<>();

        List<Object[]> values = new ArrayList<>();

        boolean nameId = false;

        for (Entity next : entityList) {

            if (columns.size() < next.getProperties().keySet().size()) {
                columns = new ArrayList<>(next.getProperties().keySet());
                columns.add(0, "Name/ID");
            }

            ArrayList<Object> objects = next.getProperties().values()
                    .stream()
                    .map(Value::get)
                    .collect(Collectors.toCollection(ArrayList::new));

            if (next.getKey().getName() != null) {
                objects.add(0, next.getKey().getName());
                nameId = true;
            } else {
                objects.add(0, next.getKey().getId());
            }

            values.add(objects.toArray());
        }

        setDataVector(values.toArray(Object[][]::new), columns.toArray());
        processColumns(nameId);
        fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    private void processColumns(boolean id) {

        if (table.getColumnCount() < 5) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        } else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);

            if (i == 0) {
                if (id) {
                    column.setMinWidth(250);
                    column.setMaxWidth(250);
                } else {
                    column.setMinWidth(100);
                    column.setMaxWidth(100);
                }

                column.setResizable(false);
            } else {
                column.setMinWidth(90);
                column.setPreferredWidth(90);
            }
        }
    }

}
