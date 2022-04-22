package com.alexcova;

import com.alexcova.swing.*;
import com.formdev.flatlaf.FlatLightLaf;
import com.google.cloud.datastore.*;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Workbench extends javax.swing.JFrame {

    private Datastore datastore;
    private List<String> namespaces = Collections.emptyList();

    private final TableModel tableModel;

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
            bRun.setBackground(tabPane.getSelectedIndex() == 0 ? Color.DARK_GRAY : new Color(7, 135, 7));
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
        bDelete.setBackground(Color.RED);
        bDelete.setForeground(Color.WHITE);

        bDelete.addActionListener(e -> {
            if (tableModel.isEmpty()) return;

            deleteSelected();
        });

        bFetch.setBackground(Color.decode("#0282B7"));
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
    }


    private void connect() {
        datastore = DatastoreOptions.newBuilder()
                .setProjectId(projectField.getText())
                .setHost(hostField.getText())
                .build()
                .getService();

        loadNameSpaces();
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

        @Override
        public Dimension preferredLayoutSize(Container container) {
            return new Dimension(700, 500);
        }

        @Override
        public Dimension minimumLayoutSize(Container container) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(Container container) {

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
            result.add(results.next().getName());
        }

        cbKind.setModel(new DefaultComboBoxModel<>(result.toArray(String[]::new)));
    }


    private void initComponents() {

        setTitle("Google DataStore Entity Browser by Alejandro Covarrubias");

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
        statsLabel = new JLabel("Hi");
        statsLabel.setPreferredSize(new Dimension(250, 40));

        footer.add(statsLabel);
        footer.setLayout(new FlowLayout(FlowLayout.LEFT));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        toolbar.setBackground(new java.awt.Color(2, 130, 183));
        toolbar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        bRun.setBackground(new java.awt.Color(255, 51, 0));
        bRun.setForeground(Color.WHITE);
        bRun.setText("Run this shit");
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

            String myString = "";

            if (entity.getKey().getName() != null) {
                myString += entity.getKey().getName();
            } else {
                myString += entity.getKey().getId();
            }

            StringSelection stringSelection = new StringSelection(myString);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);

            JOptionPane.showMessageDialog(this, "id copiado");
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
