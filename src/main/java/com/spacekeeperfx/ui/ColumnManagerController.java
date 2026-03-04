package com.spacekeeperfx.ui;

import com.spacekeeperfx.model.ColumnDef;
import com.spacekeeperfx.model.ColumnType;
import com.spacekeeperfx.model.Subspace;
import com.spacekeeperfx.repository.SubspaceRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Window;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class ColumnManagerController {

    @FXML private ListView<ColumnDef> list;

    /** In-dialog working copy. */
    private final ObservableList<ColumnDef> workingColumns = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        if (list != null) {
            list.setItems(workingColumns);
            list.setCellFactory(lv -> new ListCell<>() {
                @Override protected void updateItem(ColumnDef item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        String enabled = item.isEnabled() ? "" : "  (disabled)";
                        setText("%d. %s — %s%s".formatted(
                                item.getDisplayOrder(),
                                item.getName(),
                                item.getType().name(),
                                enabled
                        ));
                    }
                }
            });
            // quick rename on double click
            list.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    ColumnDef sel = list.getSelectionModel().getSelectedItem();
                    if (sel != null) {
                        TextInputDialog d = new TextInputDialog(sel.getName());
                        d.setHeaderText(null);
                        d.setContentText("Column name:");
                        d.showAndWait().ifPresent(newName -> {
                            sel.setName(newName == null ? "" : newName.trim());
                            list.refresh();
                        });
                    }
                }
            });
        }
    }

    /* ===== Handlers used by your FXML ===== */

    @FXML private void onAddText(ActionEvent e)  { addNew(ColumnType.TEXT,  "Text"); }
    @FXML private void onAddNum(ActionEvent e)   { addNew(ColumnType.NUMBER,"Number"); }
    @FXML private void onAddPhoto(ActionEvent e) { addNew(ColumnType.PHOTO, "Photo"); }

    @FXML private void onRemove(ActionEvent e) {
        int idx = list.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        workingColumns.remove(idx);
        normalizeOrder();
        list.getSelectionModel().clearAndSelect(Math.min(idx, workingColumns.size() - 1));
    }

    @FXML private void onToggleEnable(ActionEvent e) {
        ColumnDef sel = list.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        sel.setEnabled(!sel.isEnabled());
        list.refresh();
    }

    @FXML private void onUp(ActionEvent e) {
        int i = list.getSelectionModel().getSelectedIndex();
        if (i <= 0) return;
        Collections.swap(workingColumns, i, i - 1);
        normalizeOrder();
        list.getSelectionModel().clearAndSelect(i - 1);
        list.scrollTo(i - 1);
    }

    @FXML private void onDown(ActionEvent e) {
        int i = list.getSelectionModel().getSelectedIndex();
        if (i < 0 || i >= workingColumns.size() - 1) return;
        Collections.swap(workingColumns, i, i + 1);
        normalizeOrder();
        list.getSelectionModel().clearAndSelect(i + 1);
        list.scrollTo(i + 1);
    }

    /* ===== Dialog open helpers ===== */

    /** Open for in-memory list editing; returns edited list on OK. */
    public static Optional<List<ColumnDef>> open(Window owner, List<ColumnDef> current) {
        try {
            FXMLLoader loader = new FXMLLoader(ColumnManagerController.class.getResource("/fxml/column_manager_dialog.fxml"));
            Object root = loader.load();

            ColumnManagerController controller = loader.getController();
            controller.seed(current != null ? current : List.of());

            Dialog<List<ColumnDef>> dlg = new Dialog<>();
            dlg.setTitle("Manage Columns");
            dlg.initModality(Modality.WINDOW_MODAL);
            if (owner != null) dlg.initOwner(owner);

            DialogPane pane = new DialogPane();
            pane.setContent((Parent) root);
            pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dlg.setDialogPane(pane);

            dlg.setResultConverter(bt -> bt == ButtonType.OK ? controller.collect() : null);
            return dlg.showAndWait();

        } catch (IOException ex) {
            throw new RuntimeException("Failed to open Column Manager dialog", ex);
        }
    }

    /** Open bound to a subspace; persists via repository if user presses OK. */
    public static Optional<List<ColumnDef>> open(Window owner, SubspaceRepository subspaceRepo, Subspace subspace) {
        if (subspaceRepo == null || subspace == null) return Optional.empty();

        List<ColumnDef> working = new ArrayList<>(subspace.getColumns());
        Optional<List<ColumnDef>> result = open(owner, working);

        result.ifPresent(updated -> {
            // attach subspace id & order
            for (int i = 0; i < updated.size(); i++) {
                ColumnDef d = updated.get(i);
                d.setSubspaceId(subspace.getId());
                d.setDisplayOrder(i);
            }
            subspaceRepo.replaceColumns(subspace.getId(), updated);
            subspace.setColumns(updated);     // Subspace should expose setColumns(List<ColumnDef>)
            subspace.sortByDisplayOrder();
        });

        return result;
    }

    /* ===== Internal helpers ===== */

    private void seed(List<ColumnDef> defs) {
        workingColumns.setAll(defs);
        normalizeOrder();
        if (list != null) list.refresh();
    }

    private List<ColumnDef> collect() {
        return new ArrayList<>(workingColumns);
    }

    private void addNew(ColumnType type, String base) {
        String name = suggestUnique(base);
        ColumnDef def = new ColumnDef(
                java.util.UUID.randomUUID().toString(),
                "",                   // subspaceId filled on save
                name,
                type,
                true,                 // enabled
                false,                // required
                false,                // unique
                workingColumns.size() // displayOrder
        );
        workingColumns.add(def);
        normalizeOrder();
        if (list != null) {
            list.getSelectionModel().clearAndSelect(workingColumns.size() - 1);
            list.scrollTo(workingColumns.size() - 1);
        }
    }

    private String suggestUnique(String base) {
        Set<String> names = new HashSet<>();
        for (ColumnDef d : workingColumns) names.add(d.getName());
        if (!names.contains(base)) return base;
        for (int i = 2; i < 9999; i++) {
            String cand = base + " " + i;
            if (!names.contains(cand)) return cand;
        }
        return base + " X";
    }

    private void normalizeOrder() {
        IntStream.range(0, workingColumns.size())
                .forEach(i -> workingColumns.get(i).setDisplayOrder(i));
        if (list != null) list.refresh();
    }
}
