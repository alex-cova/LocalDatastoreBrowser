package com.alexcova;

import com.alexcova.swing.*;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.cloud.datastore.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class Workbench extends javax.swing.JFrame {

    private Datastore datastore;
    private List<String> namespaces = Collections.emptyList();

    private final TableModel tableModel;
    private final List<Key> kinds = new ArrayList<>();

    /**
     * Creates new form Workbench
     */
    public Workbench() {
        initComponents();

        tableModel = new TableModel(table);

        setLocationRelativeTo(null);
        setSize(820, 500);

        cbNameSpace.setModel(new DefaultComboBoxModel<>());
        cbKind.setModel(new DefaultComboBoxModel<>());

        cbNameSpace.addActionListener(e -> {
            if (namespaces.isEmpty()) return;
            loadKinds();
        });

        cbKind.addActionListener(e -> {
            if (cbKind.getItemCount() <= 0) return;
            taQuery.setText("SELECT * FROM " + cbKind.getSelectedItem());
            loadItems();
        });

        bRun.setText("Reload Kinds");
        bRun.setBackground(Color.DARK_GRAY);
        bRun.setForeground(Color.WHITE);
        bRun.setPreferredSize(new Dimension(120, 27));

        tabPane.addChangeListener(e -> {
            bRun.setText(tabPane.getSelectedIndex() == 0 ? "Reload Kinds" : "Run Query");
            bRun.setBackground(tabPane.getSelectedIndex() == 0 ? Color.DARK_GRAY : GoogleColors.GREEN);
        });

        bRun.addActionListener(e -> {
            if (tabPane.getSelectedIndex() == 1) {
                loadItems();
            } else {
                loadNameSpaces();
                loadKinds();
            }
        });

        bFetch.addActionListener(e -> loadItems());

        RendererUtil.installRenderer(table, new EntitiesRenderer());
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        table.setModel(tableModel);
        tableModel.drop();

        bDelete.setText("Delete");
        bDelete.setBackground(GoogleColors.RED);
        bDelete.setForeground(Color.WHITE);

        bDelete.addActionListener(e -> {
            if (tableModel.isEmpty()) return;

            deleteSelected();
        });

        bFetch.setBackground(GoogleColors.BLUE);
        bFetch.setForeground(Color.WHITE);

        SwingUtilities.invokeLater(this::askConfig);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    var selected = tableModel.getSelectedEntity();

                    if (selected == null) return;

                    EntityDialog.show(Workbench.this, selected);
                }
            }
        });

    }

    private void deleteSelected() {

        if (table.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Select an entity");
            return;
        }

        Entity entity = tableModel.get(table.getSelectedRow());

        datastore.delete(entity.getKey());
        loadItems();

        JOptionPane.showMessageDialog(this, "Deleted");
    }

    private void loadItems() {

        if (datastore == null) {
            askConfig();
            return;
        }

        if (cbKind.getSelectedItem() == null) {
            return;
        }

        String nameSpace = getNameSpace();

        QueryResults<?> results;

        final long init = System.currentTimeMillis();

        if (!tfKey.getText().isBlank() && tabPane.getSelectedIndex() == 0) {

            var factory = datastore.newKeyFactory()
                    .setKind(cbKind.getSelectedItem().toString())
                    .setNamespace(nameSpace)
                    .setProjectId(datastore.getOptions().getProjectId());

            Key key;

            key = factory.newKey(tfKey.getText());

            @Nullable Entity entity = datastore.get(key);

            if (entity == null) {
                tableModel.drop();
                statsLabel.setText("Entity with key '%s' not found".formatted(tfKey.getText()));
                return;
            }

            tableModel.setAll(List.of(entity));
            tableModel.render();

            return;
        } else {
            var gql = taQuery.getText();

            if (tabPane.getSelectedIndex() == 0) {
                gql = "SELECT * FROM " + cbKind.getSelectedItem();
            }

            var query = Query.newGqlQueryBuilder(gql);

            if (nameSpace != null) {
                query.setNamespace(nameSpace);
            }

            try {
                results = datastore.run(query.build());
            } catch (DatastoreException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage());
                return;
            }
        }

        statsLabel.setText("Fetch time: " + (System.currentTimeMillis() - init) + " Milliseconds");

        if (!results.hasNext()) {
            tableModel.drop();
            statsLabel.setText("No results");
            return;
        }

        namespaces = new ArrayList<>();
        tableModel.drop();

        while (results.hasNext()) {

            Entity next = (Entity) results.next();

            tableModel.add(next);
        }

        if (!tableModel.isEmpty()) {
            Entity entity = tableModel.get(0);

            if (entity.getKey().getKind() != cbKind.getSelectedItem()) {
                cbKind.setSelectedItem(entity.getKey().getKind());
            }
        }

        tableModel.render();
        printProperties(kinds.get(cbKind.getSelectedIndex()));
    }


    private void connect() {
        datastore = DatastoreOptions.newBuilder()
                .setProjectId(projectField.getText())
                .setHost(hostField.getText())
                .build()
                .getService();

        loadNameSpaces();
        loadItems();
    }

    private void loadNameSpaces() {

        if (datastore == null) {
            askConfig();
            return;
        }

        Query<Key> query =
                Query.newKeyQueryBuilder()
                        .setKind("__namespace__")
                        .build();

        QueryResults<Key> results = datastore.run(query);

        if (!results.hasNext()) {
            System.out.println("No content.");
        }

        namespaces = new ArrayList<>();
        while (results.hasNext()) {
            namespaces.add(results.next().getName());
        }

        namespaces.add("[Default]");

        cbNameSpace.setModel(new DefaultComboBoxModel<>(namespaces.toArray(String[]::new)));
        if (cbNameSpace.getItemCount() > 0) {
            cbNameSpace.setSelectedIndex(0);
        }
    }

    record Layout(Workbench workbench) implements LayoutManager {

        @Override
        public void addLayoutComponent(String s, Component component) {

        }

        @Override
        public void removeLayoutComponent(Component component) {

        }

        @Contract(value = "_ -> new", pure = true)
        @Override
        public @NotNull Dimension preferredLayoutSize(Container container) {
            return new Dimension(700, 500);
        }

        @Contract(value = "_ -> new", pure = true)
        @Override
        public @NotNull Dimension minimumLayoutSize(Container container) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(@NotNull Container container) {

            for (Component component : container.getComponents()) {
                if (component == workbench.toolbar) {
                    component.setBounds(0, 0, container.getWidth(), 40);
                }

                if (component == workbench.tabPane) {
                    component.setBounds(0, 40, container.getWidth(), 150);
                }

                if (component == workbench.scrollPaneTable) {
                    component.setBounds(0, 190, container.getWidth(), container.getHeight() - 220);
                }

                if (component == workbench.footer) {
                    component.setBounds(0, container.getHeight() - 40, container.getWidth(), 40);
                }
            }

        }
    }

    public String getNameSpace() {
        if (cbNameSpace.getSelectedIndex() == -1 || cbNameSpace.getSelectedItem() == null) {
            return null;
        }

        String s = cbNameSpace.getSelectedItem().toString();

        if (s.equals("[Default]")) {
            return null;
        }

        return s;
    }

    private void loadKinds() {

        kinds.clear();

        if (datastore == null) {
            askConfig();
            return;
        }

        if (namespaces.isEmpty()) return;
        if (cbNameSpace.getSelectedIndex() == -1) return;

        var query =
                Query.newKeyQueryBuilder()
                        .setKind("__kind__");

        String nameSpace = getNameSpace();

        if (nameSpace != null) query.setNamespace(nameSpace);

        QueryResults<Key> results = datastore.run(query.build());

        if (!results.hasNext()) {
            System.out.println("No content.");
        }

        List<String> result = new ArrayList<>();

        while (results.hasNext()) {
            Key next = results.next();
            kinds.add(next);
            result.add(next.getName());
        }

        cbKind.setModel(new DefaultComboBoxModel<>(result.toArray(String[]::new)));
    }

    public void printProperties(@NotNull Key key) {

        propertiesPane.removeAll();

        Query<Entity> query =
                Query.newEntityQueryBuilder()
                        .setKind("__property__")
                        .setNamespace(key.getNamespace())
                        .setFilter(StructuredQuery.PropertyFilter.hasAncestor(key))
                        .build();

        QueryResults<Entity> results = datastore.run(query);
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

        propertiesPane.removeAll();

        Object[][] vector = new Object[representationsMap.size()][2];

        int i = 0;

        for (Map.Entry<String, ValueType> entry : representationsMap.entrySet()) {
            vector[i] = new Object[]{entry.getKey(), entry.getValue()};
            i++;
        }

        JTable table = new JTable();
        table.setModel(new DefaultTableModel(vector, new String[]{"Name", "Type"}));
        propertiesPane.add(new JScrollPane(table));
    }


    private void initComponents() {

        setTitle("Local DataStore Browser");

        toolbar = new javax.swing.JPanel();
        bRun = new javax.swing.JButton();
        cbNameSpace = new javax.swing.JComboBox<>();
        cbKind = new javax.swing.JComboBox<>();
        tabPane = new javax.swing.JTabbedPane();
        JPanel operationsPane = new JPanel();
        tfKey = new javax.swing.JTextField();
        bFetch = new javax.swing.JButton();
        bDelete = new javax.swing.JButton();
        JScrollPane scrollPaneQuery = new JScrollPane();
        taQuery = new javax.swing.JTextArea();
        scrollPaneTable = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        footer = new JPanel();
        statsLabel = new JLabel(";)");
        statsLabel.setPreferredSize(new Dimension(250, 40));
        propertiesPane = new JPanel();
        propertiesPane.setLayout(new FlowLayout(FlowLayout.CENTER));

        footer.add(statsLabel);
        footer.setLayout(new FlowLayout(FlowLayout.LEFT));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        toolbar.setBackground(new java.awt.Color(66, 133, 244));
        toolbar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        bRun.setBackground(new java.awt.Color(234, 67, 53));
        bRun.setForeground(Color.WHITE);
        bRun.setText("Run");
        toolbar.add(bRun);
        var separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(15, 35));
        toolbar.add(separator);

        cbNameSpace.setModel(new javax.swing.DefaultComboBoxModel<>());
        cbNameSpace.setPreferredSize(new java.awt.Dimension(120, 27));
        var lnamespace = new JLabel("Namespace");
        lnamespace.setForeground(Color.WHITE);
        toolbar.add(lnamespace);
        toolbar.add(cbNameSpace);

        separator = new JSeparator(JSeparator.VERTICAL);
        separator.setPreferredSize(new Dimension(15, 35));
        toolbar.add(separator);

        cbKind.setModel(new javax.swing.DefaultComboBoxModel<>());
        cbKind.setPreferredSize(new java.awt.Dimension(120, 27));
        var lkind = new JLabel("Kind");
        lkind.setForeground(Color.WHITE);
        toolbar.add(lkind);
        toolbar.add(cbKind);

        tfKey.setPreferredSize(new java.awt.Dimension(200, 26));
        operationsPane.add(new JLabel("Name/ID"));
        operationsPane.add(tfKey);

        bFetch.setText("fetch");
        operationsPane.add(bFetch);

        bDelete.setText("Delete Selected");
        operationsPane.add(bDelete);

        var copy = new JButton("Copy ID");
        copy.setForeground(Color.DARK_GRAY);
        copy.addActionListener(e -> {

            if (tableModel.isEmpty()) return;
            if (table.getSelectedRow() == -1) return;

            Entity entity = tableModel.get(table.getSelectedRow());

            String nameId = "";

            if (entity.getKey().getName() != null) {
                nameId += entity.getKey().getName();
            } else {
                nameId += entity.getKey().getId();
            }

            StringSelection stringSelection = new StringSelection(nameId);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);

            JOptionPane.showMessageDialog(this, "Name/ID copied to clipboard");
        });
        operationsPane.add(copy);

        tabPane.addTab("Operation", operationsPane);

        taQuery.setColumns(20);
        taQuery.setRows(5);
        scrollPaneQuery.setViewportView(taQuery);

        tabPane.addTab("Query", scrollPaneQuery);
        table.setModel(new DefaultTableModel());
        table.setRowHeight(35);

        table.setSelectionBackground(new Color(27, 166, 214));
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JScrollPane scrollProperties = new JScrollPane();
        scrollProperties.setViewportView(propertiesPane);
        tabPane.add("Properties", scrollProperties);

        scrollPaneTable.setViewportView(table);

        var bConfig = new JButton("Configure");
        bConfig.setPreferredSize(new Dimension(100, 27));
        bConfig.addActionListener(e -> askConfig());

        toolbar.add(bConfig);
        scrollPaneTable.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        add(toolbar);
        add(tabPane);
        add(scrollPaneTable);
        add(footer);
        setLayout(new Layout(this));

        projectField = new JTextField();
        hostField = new JTextField("http://localhost:8081");

        hostField.addMouseListener(new SelectAllEvent());
        projectField.addMouseListener(new SelectAllEvent());

        pack();
    }

    public void askConfig() {

        int result = JOptionPane.showOptionDialog(this, new Object[]{"Please specify your Project ID", projectField, hostField},
                "Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
        if (result == JOptionPane.OK_OPTION) {
            connect();
        }
    }

    public static void main(String[] args) {

        FlatLightLaf.setup();

        java.awt.EventQueue.invokeLater(() -> new Workbench().setVisible(true));
    }


    private JPanel propertiesPane;
    private JTextField projectField;
    private JTextField hostField;
    private JLabel statsLabel;
    private JPanel footer;
    private javax.swing.JButton bDelete;
    private javax.swing.JButton bFetch;
    private javax.swing.JComboBox<String> cbKind;
    private javax.swing.JComboBox<String> cbNameSpace;
    private javax.swing.JButton bRun;
    private javax.swing.JPanel toolbar;
    private javax.swing.JScrollPane scrollPaneTable;
    private javax.swing.JTable table;
    private javax.swing.JTextArea taQuery;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTextField tfKey;
}
