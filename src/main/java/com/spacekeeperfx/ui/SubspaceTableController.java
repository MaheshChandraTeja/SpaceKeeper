package com.spacekeeperfx.ui;

import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.ColumnType;
import com.spacekeeperfx.model.Record;
import com.spacekeeperfx.repository.RecordRepository;
import com.spacekeeperfx.service.ImageService;
import com.spacekeeperfx.ui.components.InlineEditTextCell;
import com.spacekeeperfx.ui.components.PhotoCell;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Comparator;

public class SubspaceTableController {

    @FXML private Label subspaceNameLabel;
    @FXML private TableView<Record> table;

    private ObservableList<Record> backingItems;

    // persistence
    private RecordRepository recordRepo;
    private ImageService imageService;
    private String currentSubspaceId;

    private java.util.function.Consumer<String> notifier = s -> {};
    public void setNotifier(java.util.function.Consumer<String> n) { this.notifier = (n == null ? s->{} : n); }

    @FXML
    private void initialize() {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPlaceholder(new Label("No records yet"));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setPlaceholder(new Label("No records yet"));
        table.setRowFactory(tv -> { var r = new TableRow<Record>(); r.setPrefHeight(72); return r; });

        table.setRowFactory(tv -> {
            TableRow<Record> r = new TableRow<>();
            r.setPrefHeight(72);
            return r;
        });
    }

    /* ==== Wiring from MainController ==== */
    public void setRecordRepository(RecordRepository repo) { this.recordRepo = repo; }
    public void setImageService(ImageService imageService) { this.imageService = imageService; }
    public void setCurrentSubspace(String subspaceId, String name) {
        this.currentSubspaceId = subspaceId;
        setSubspaceName(name);
    }

    public void setSubspaceName(String name) { subspaceNameLabel.setText(name); }

    public void setRecords(ObservableList<Record> items) {
        this.backingItems = items;
        table.setItems(items);
    }

    /** Build columns according to ColumnDefs (order respected). */
    // --- build table columns from ColumnDefs (unique by id, order respected)
    public void rebuildColumns(List<ColumnDef> defs) {
        Objects.requireNonNull(defs, "defs");
        table.getColumns().clear();

        // De-dup by column id while preserving first occurrence (LinkedHashMap keeps order)
        var byId = new java.util.LinkedHashMap<String, ColumnDef>();
        for (ColumnDef d : defs) {
            if (d.isEnabled() && !byId.containsKey(d.getId())) {
                byId.put(d.getId(), d);
            }
        }
        var unique = new java.util.ArrayList<>(byId.values());

        for (ColumnDef def : unique) {
            switch (def.getType()) {
                case TEXT   -> table.getColumns().add(buildTextColumn(def));
                case NUMBER -> table.getColumns().add(buildNumberColumn(def));
                case PHOTO  -> table.getColumns().add(buildPhotoColumn(def));
            }
        }
    }

    // --- persistText: store NULL for blank strings so "unique" columns don't clash on ""
    private void persistText(String recordId, String columnId, String value) {
        try {
            String v = (value == null || value.trim().isEmpty()) ? null : value.trim();
            recordRepo.setText(recordId, columnId, v);
            notifier.accept("Saved.");
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to save text:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void persistPhoto(String recordId, String columnId, String relativeOrAbsolutePath) {
        try {
            recordRepo.setPhotoPath(recordId, columnId, relativeOrAbsolutePath);
            notifier.accept("Saved.");
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to save photo:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private TableColumn<Record, String> buildTextColumn(ColumnDef def) {
        TableColumn<Record, String> col = new TableColumn<>(def.getName());
        col.setEditable(true);
        col.setPrefWidth(220);

        // read value
        col.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getText(def.getId()).orElse("")
                )
        );

        // inline edit
        col.setCellFactory(tc -> new InlineEditTextCell());
        col.setOnEditCommit(evt -> {
            Record r = evt.getRowValue();
            String newVal = evt.getNewValue();
            r.setText(def.getId(), newVal);                 // update model
            persistText(r.getId(), def.getId(), newVal);    // persist to DB
            table.refresh();
        });

        return col;
    }

    // --- dedicated PHOTO builder (uses your PhotoCell)
    private TableColumn<Record, String> buildPhotoColumn(ColumnDef def) {
        TableColumn<Record, String> col = new TableColumn<>(def.getName());
        col.setEditable(true);
        col.setStyle("-fx-alignment: CENTER;");
        col.setPrefWidth(130);
        col.setMinWidth(110);
        col.setMaxWidth(170);

        // getPhotoPath(...) -> Optional<String>
        col.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getPhotoPath(def.getId()).orElse(null)
                )
        );

        col.setCellFactory(tc -> new PhotoCell(imageService, def.getId(), (rec, relPath) -> {
            // If your model stores String paths:
            rec.setPhotoPath(def.getId(), Path.of(relPath));

            // If your model stores Path objects instead, use this line:
            // rec.setPhotoPath(def.getId(), relPath == null ? null : java.nio.file.Path.of(relPath));

            recordRepo.setPhotoPath(rec.getId(), def.getId(), relPath);
            notifier.accept("Saved.");
        }));

        return col;
    }

    private TableColumn<Record, BigDecimal> buildNumberColumn(ColumnDef def) {
        TableColumn<Record, BigDecimal> col = new TableColumn<>(def.getName());
        col.setEditable(true);
        col.setCellValueFactory(data -> new SimpleObjectProperty<>(
                data.getValue().getNumber(def.getId()).orElse(null)
        ));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : (item == null ? "" : item.stripTrailingZeros().toPlainString()));
            }
        });
        col.setContextMenu(makeNumberColumnContextMenu(def));
        col.setPrefWidth(140);
        col.setEditable(true);
        col.setMinWidth(120);
        col.setPrefWidth(140);
        col.setMaxWidth(280);
        return col;
    }

    private ContextMenu makeNumberColumnContextMenu(ColumnDef def) {
        MenuItem setValue = new MenuItem("Set Value…");
        setValue.setOnAction(e -> {
            Record r = table.getSelectionModel().getSelectedItem();
            if (r == null) return;
            TextInputDialog dlg = new TextInputDialog(r.getNumber(def.getId()).map(BigDecimal::toPlainString).orElse(""));
            dlg.setHeaderText(null);
            dlg.setContentText("Enter number:");
            Optional<String> result = dlg.showAndWait();
            result.ifPresent(str -> {
                try {
                    BigDecimal bd = str == null || str.trim().isEmpty() ? null : new BigDecimal(str.trim());
                    r.setNumber(def.getId(), bd);
                    persistNumber(r.getId(), def.getId(), bd);
                    table.refresh();
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "Invalid number.", ButtonType.OK).showAndWait();
                }
            });
        });
        return new ContextMenu(setValue);
    }

    // --- Row operations (now persisted) ---

    public void addBlankRecord() {
        if (backingItems == null || recordRepo == null || currentSubspaceId == null) return;
        Record r = recordRepo.createBlank(currentSubspaceId);
        backingItems.addFirst(r);
        table.getSelectionModel().select(r);
        table.scrollTo(r);
        notifier.accept("Row added.");
    }

    public void deleteSelectedRecords() {
        if (backingItems == null || recordRepo == null || currentSubspaceId == null) return;
        var sel = table.getSelectionModel().getSelectedItems();
        if (sel == null || sel.isEmpty()) return;

        if (new Alert(Alert.AlertType.CONFIRMATION, "Delete selected records?", ButtonType.OK, ButtonType.CANCEL)
                .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        var ids = sel.stream().map(Record::getId).toList();
        boolean ok = recordRepo.deleteByIds(currentSubspaceId, ids);
        if (ok) {
            backingItems.removeAll(sel);
            notifier.accept("Deleted " + ids.size() + " row(s).");
        }
    }

    private void persistNumber(String recordId, String columnId, BigDecimal value) {
        try {
            recordRepo.setNumber(recordId, columnId, value);
            notifier.accept("Saved.");
        } catch (RuntimeException ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to save number:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}
