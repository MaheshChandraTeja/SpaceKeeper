package com.spacekeeperfx.ui;

import com.spacekeeperfx.model.Subspace;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Collapsible left column listing Subspaces in the current Vault.
 * Adds a per-item context menu: Open, Rename…, Delete…
 */
public class SidebarController {

    @FXML private VBox root;
    @FXML private ListView<Subspace> subspaceList;
    @FXML private Button collapseBtn;

    private Consumer<Subspace> onSubspaceSelected = s -> {};
    private Consumer<Subspace> onContextRename = s -> {};
    private Consumer<Subspace> onContextDelete = s -> {};

    @FXML
    private void initialize() {
        subspaceList.setCellFactory(v -> new ListCell<>() {
            private ContextMenu menu;

            @Override protected void updateItem(Subspace item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                    return;
                }
                setText(item.getName());
                if (menu == null) menu = buildMenu();
                setContextMenu(menu);
            }

            private ContextMenu buildMenu() {
                MenuItem open = new MenuItem("Open");
                open.setOnAction(e -> {
                    Subspace s = getItem();
                    if (s != null) {
                        subspaceList.getSelectionModel().select(s);
                        subspaceList.scrollTo(s);
                        onSubspaceSelected.accept(s);
                    }
                });

                MenuItem rename = new MenuItem("Rename…");
                rename.setOnAction(e -> {
                    Subspace s = getItem();
                    if (s != null) onContextRename.accept(s);
                });

                MenuItem delete = new MenuItem("Delete…");
                delete.setOnAction(e -> {
                    Subspace s = getItem();
                    if (s != null) onContextDelete.accept(s);
                });

                ContextMenu cm = new ContextMenu(open, new SeparatorMenuItem(), rename, delete);
                return cm;
            }
        });

        subspaceList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) onSubspaceSelected.accept(newV);
        });

        collapseBtn.setOnAction(e -> toggleCollapsed());
    }

    public void setSubspaces(ObservableList<Subspace> data) {
        subspaceList.setItems(data);
    }

    public void selectFirst() {
        if (!subspaceList.getItems().isEmpty()) {
            subspaceList.getSelectionModel().select(0);
        }
    }

    /** Select a subspace by ID if present. */
    public void selectById(String subspaceId) {
        if (subspaceId == null) return;
        var items = subspaceList.getItems();
        for (int i = 0; i < items.size(); i++) {
            if (subspaceId.equals(items.get(i).getId())) {
                subspaceList.getSelectionModel().select(i);
                subspaceList.scrollTo(i);
                break;
            }
        }
    }

    public void setOnSubspaceSelected(Consumer<Subspace> consumer) {
        this.onSubspaceSelected = consumer == null ? s -> {} : consumer;
    }

    /** Called by MainController to handle context Rename… */
    public void setOnContextRename(Consumer<Subspace> consumer) {
        this.onContextRename = consumer == null ? s -> {} : consumer;
    }

    /** Called by MainController to handle context Delete… */
    public void setOnContextDelete(Consumer<Subspace> consumer) {
        this.onContextDelete = consumer == null ? s -> {} : consumer;
    }

    private void toggleCollapsed() {
        boolean collapsed = root.getStyleClass().contains("collapsed");
        if (collapsed) root.getStyleClass().remove("collapsed");
        else root.getStyleClass().add("collapsed");
    }

    public void select(com.spacekeeperfx.model.Subspace target) {
        if (target == null || subspaceList.getItems() == null) {
            selectFirst();
            return;
        }
        var items = subspaceList.getItems();
        int idx = -1;
        for (int i = 0; i < items.size(); i++) {
            if (java.util.Objects.equals(items.get(i).getId(), target.getId())) {
                idx = i;
                break;
            }
        }
        if (idx >= 0) {
            subspaceList.getSelectionModel().clearAndSelect(idx);
            subspaceList.scrollTo(idx);
        } else {
            selectFirst();
        }
    }
    public Subspace getSelected() {
        return (subspaceList == null) ? null
                : subspaceList.getSelectionModel().getSelectedItem();
    }

    // (optional) handy helper if you only need the id
    public String getSelectedId() {
        Subspace s = getSelected();
        return s != null ? s.getId() : null;
    }
}
