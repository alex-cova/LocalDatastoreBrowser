package com.sicar;

import com.google.cloud.datastore.*;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Workbench extends javax.swing.JFrame {

    private Datastore datastore;
    private List<String> namespaces = Collections.emptyList();
    private final List<Entity> entities = new ArrayList<>();

    /**
     * Creates new form Workbench
     */
    public Workbench() {
        initComponents();

        setLocationRelativeTo(null);
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cbNameSpace.setModel(new DefaultComboBoxModel<>());
        cbKind.setModel(new DefaultComboBoxModel<>());

        connect();


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
        bRun.setContentAreaFilled(false);
        bRun.setOpaque(true);
        bRun.setBackground(Color.DARK_GRAY);
        bRun.setForeground(Color.WHITE);

        tabPane.addChangeListener(e -> {
            bRun.setText(tabPane.getSelectedIndex() == 0 ? "Reload Kinds" : "Run this shit");
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

        bFetch.addActionListener(e -> {
            loadItems();
        });

        installRenderer(table, new Renderer());
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());

        bDelete.setText("Destroy");
        bDelete.setBackground(Color.RED);
        bDelete.setForeground(Color.WHITE);

        bDelete.addActionListener(e -> {
            if (entities.isEmpty()) return;

            deleteSelected();
        });

        bFetch.setContentAreaFilled(false);
        bFetch.setOpaque(true);
        bFetch.setBackground(Color.decode("#0282B7"));
        bFetch.setForeground(Color.WHITE);

        bDelete.setContentAreaFilled(false);
        bDelete.setOpaque(true);

        System.out.println("Ready");

        SwingUtilities.invokeLater(() -> {
            loadNameSpaces();
            loadKinds();
        });
    }

    private void deleteSelected() {

        if (table.getSelectedRow() == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una entidad");
            return;
        }

        Entity entity = entities.get(table.getSelectedRow());

        datastore.delete(entity.getKey());
        loadItems();

        JOptionPane.showMessageDialog(this, "Deleted");
    }

    static Color SELECTION_COLOR = new Color(27, 166, 214);
    static Color SECOND_COLOR = new Color(0, 150, 201).brighter();
    static Color ZEBRA_COLOR = new Color(240, 240, 240);

    static final Color BORDE_COLOR = new Color(230, 230, 230);

    static final MatteBorder MATTE_BORDE_DERECHO = new MatteBorder(0, 0, 1, 1, BORDE_COLOR);
    static final MatteBorder MATTE_BORDE_IZQUIERDO = new MatteBorder(0, 0, 1, 0, BORDE_COLOR);

    static final EmptyBorder EMPTY_BORDER_DERECHO = new EmptyBorder(5, 5, 0, 5);
    static final EmptyBorder EMPTY_BORDER_IZQUIERDO = new EmptyBorder(5, 5, 0, 5);

    static final CompoundBorder BORDER_DERECHO = new CompoundBorder(MATTE_BORDE_DERECHO, EMPTY_BORDER_DERECHO);
    static final CompoundBorder BORDER_IZQUIERDO = new CompoundBorder(MATTE_BORDE_IZQUIERDO, EMPTY_BORDER_IZQUIERDO);

    static class HeaderRenderer extends JLabel implements TableCellRenderer {

        public HeaderRenderer() {
            super.setMinimumSize(new Dimension(20, 30));
            super.setPreferredSize(new Dimension(super.getWidth(), 30));
            super.setOpaque(true);
            super.setHorizontalAlignment(SwingConstants.CENTER);
            super.setFont(new Font("Arial", Font.PLAIN, 14));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            setBackground(Color.DARK_GRAY);
            setForeground(Color.WHITE);
            setText(value.toString());

            return this;
        }
    }

    static class Renderer extends JLabel implements TableCellRenderer {

        public Renderer() {
            setOpaque(true);
            setFont(new Font("Arial", Font.PLAIN, 13));
            setMinimumSize(new Dimension(150, 35));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (table.getRowHeight() < 35) {
                table.setRowHeight(35);
            }

            if (isSelected) {
                setBackground(SELECTION_COLOR);
                setForeground(Color.WHITE);
            } else {
                setBackground(row % 2 == 0 ? Color.WHITE : ZEBRA_COLOR);
                setForeground(Color.DARK_GRAY);
            }
            setBorder((column < table.getColumnCount() - 1) ? BORDER_DERECHO : BORDER_IZQUIERDO);

            if (value == null) {
                setText("");
            } else {
                if (value instanceof FullEntity<?> entity) {
                    var t = entity.getProperties()
                            .entrySet()
                            .stream()
                            .filter(a -> a.getValue().get() != null)
                            .map(a -> a.getKey() + "=" + a.getValue().get())
                            .collect(Collectors.joining(", "));

                    setText(t);
                } else if (value instanceof List<?> list) {
                    var t = list.stream()
                            .map(a -> {
                                if (a instanceof Value vl) {
                                    return vl.get().toString();
                                }

                                return a.toString();
                            }).collect(Collectors.joining(", ", "[", "]"));

                    setText(t);

                } else {
                    setText(value.toString());
                }
            }

            applyBounds(this, table, column, row);

            return this;
        }

        public final void applyBounds(JComponent component, JTable table, int column, int row) {

            try {
                TableColumn tc = table.getColumnModel().getColumn(column);


                if (tc != null) {
                    component.setPreferredSize(new Dimension(Math.max(150, tc.getPreferredWidth()), table.getRowHeight(row)));
                    component.setMinimumSize(new Dimension(Math.max(150, tc.getMinWidth()), table.getRowHeight(row)));
                    component.setMaximumSize(new Dimension(Math.max(150, tc.getMaxWidth()), table.getRowHeight(row)));
                } else {
                    System.out.println("Columna null: " + column);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void installRenderer(JTable table, TableCellRenderer render) {
        table.setDefaultRenderer(Number.class, render);
        table.setDefaultRenderer(Boolean.class, render);

        table.setDefaultRenderer(Component.class, render);

        table.setDefaultRenderer(Double.class, render);
        table.setDefaultRenderer(Date.class, render);

        table.setDefaultRenderer(Float.class, render);

        table.setDefaultRenderer(Integer.class, render);
        table.setDefaultRenderer(Icon.class, render);
        table.setDefaultRenderer(ImageIcon.class, render);

        table.setDefaultRenderer(JComponent.class, render);

        table.setDefaultRenderer(LocalDate.class, render);
        table.setDefaultRenderer(LocalDateTime.class, render);
        table.setDefaultRenderer(LocalTime.class, render);
        table.setDefaultRenderer(Long.class, render);

        table.setDefaultRenderer(Object.class, render);

        table.setDefaultRenderer(String.class, render);
    }

    private void loadItems() {

        if (cbKind.getSelectedItem() == null) {
            return;
        }

        String nameSpace = getNameSpace();

        QueryResults<?> results;

        if (!tfKey.getText().isBlank() && tabPane.getSelectedIndex() == 0) {

            var factory = datastore.newKeyFactory()
                    .setKind(cbKind.getSelectedItem().toString())
                    .setNamespace(nameSpace)
                    .setProjectId(datastore.getOptions().getProjectId());

            Key key;

            if (cbName.isSelected()) {
                key = factory.newKey(tfKey.getText());
            } else {
                key = factory.newKey(Long.parseLong(tfKey.getText()));
            }

            Entity entity = datastore.get(key);

            entities.clear();
            entities.add(entity);
            render();

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

        System.out.println(results.getCursorAfter().toUrlSafe());


        if (!results.hasNext()) {
            table.setModel(new DefaultTableModel());
            return;
        }

        namespaces = new ArrayList<>();
        entities.clear();

        while (results.hasNext()) {

            Entity next = (Entity) results.next();

            entities.add(next);

        }

        render();
    }

    private void render() {

        List<String> columns = new ArrayList<>();

        List<Object[]> values = new ArrayList<>();

        for (Entity next : entities) {

            if (columns.size() < next.getProperties().keySet().size()) {
                columns = new ArrayList<>(next.getProperties().keySet());

                if (next.getKey().getName() != null) {
                    columns.add(0, "UUID");
                } else {
                    columns.add(0, "ID");
                }
            }

            ArrayList<Object> objects = next.getProperties().values()
                    .stream()
                    .map(Value::get)
                    .collect(Collectors.toCollection(ArrayList::new));

            if (next.getKey().getName() != null) {
                objects.add(0, next.getKey().getName());
            } else {
                objects.add(0, next.getKey().getId());
            }

            values.add(objects.toArray());
        }

        if (columns.size() < 5) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        } else {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }

        var model = new DefaultTableModel(values.toArray(Object[][]::new), columns.toArray()) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table.setModel(model);

        if (entities.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No se encontraron datos");
        }
    }

    private void connect() {
        datastore = DatastoreOptions.newBuilder()
                .setProjectId("sicar-x")
                .setHost("http://localhost:8081")
                .build()
                .getService();
    }

    private void loadNameSpaces() {

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
        operationsPane = new javax.swing.JPanel();
        tfKey = new javax.swing.JTextField();
        bFetch = new javax.swing.JButton();
        bDelete = new javax.swing.JButton();
        scrollPaneQuery = new javax.swing.JScrollPane();
        taQuery = new javax.swing.JTextArea();
        scrollPaneTable = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();

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
        operationsPane.add(cbName);
        operationsPane.add(tfKey);

        bFetch.setText("fetch");
        operationsPane.add(bFetch);

        bDelete.setText("Delete Selected");
        operationsPane.add(bDelete);

        var copy = new JButton("Copy ID");
        copy.setContentAreaFilled(false);
        copy.setForeground(Color.DARK_GRAY);
        copy.setOpaque(true);
        copy.addActionListener(e -> {

            if (entities.isEmpty()) return;
            if (table.getSelectedRow() == -1) return;

            Entity entity = entities.get(table.getSelectedRow());

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

        table.setSelectionBackground(SELECTION_COLOR);
        table.setSelectionForeground(Color.WHITE);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);

        scrollPaneTable.setViewportView(table);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(toolbar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(tabPane, javax.swing.GroupLayout.DEFAULT_SIZE, 571, Short.MAX_VALUE)
                                        .addComponent(scrollPaneTable))
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(toolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(tabPane, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(scrollPaneTable)
                                .addContainerGap())
        );

        var dynamo = new JButton("DS Documentation");
        dynamo.addActionListener(e -> {
            try {
                Desktop.getDesktop()
                        .browse(new URI("https://aws.amazon.com/dynamodb/"));
            } catch (IOException | URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        toolbar.add(dynamo);
        scrollPaneTable.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneTable.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        pack();
    }


    public static void main(String args[]) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        UIManager.setLookAndFeel(NimbusLookAndFeel.class.getName());

        UIManager.getLookAndFeelDefaults().put("Panel.background", Color.WHITE);
        UIManager.getLookAndFeelDefaults().put("TabbedPane.background", Color.WHITE);

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Workbench().setVisible(true);
            }
        });
    }

    private javax.swing.JButton bDelete;
    private javax.swing.JButton bFetch;
    private javax.swing.JComboBox<String> cbKind;
    private javax.swing.JComboBox<String> cbNameSpace;
    private javax.swing.JButton bRun;
    private javax.swing.JPanel toolbar;
    private javax.swing.JPanel operationsPane;
    private javax.swing.JScrollPane scrollPaneQuery;
    private javax.swing.JScrollPane scrollPaneTable;
    private javax.swing.JTable table;
    private javax.swing.JTextArea taQuery;
    private javax.swing.JTabbedPane tabPane;
    private javax.swing.JTextField tfKey;
    private JCheckBox cbName = new JCheckBox("Name", true);
}
