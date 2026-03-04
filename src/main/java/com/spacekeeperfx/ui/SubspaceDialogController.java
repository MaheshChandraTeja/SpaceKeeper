package com.spacekeeperfx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

/** Minimal "New Subspace" dialog (name + Photograph toggle). */
public final class SubspaceDialogController {

    public record Result(String name, boolean enablePhotograph) {}

    private SubspaceDialogController() {}

    public static Optional<Result> createNew(Window owner) {
        Stage stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("New Subspace");

        TextField name = new TextField();
        name.setPromptText("Subspace name");
        CheckBox photo = new CheckBox("Enable Photograph column");
        photo.setSelected(true);

        Button ok = new Button("Create");
        ok.getStyleClass().add("primary");
        ok.setDefaultButton(true);
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("ghost");
        cancel.setCancelButton(true);

        ok.setOnAction(e -> stage.close());
        cancel.setOnAction(e -> {
            name.setUserData("CANCEL");
            stage.close();
        });

        GridPane gp = new GridPane();
        gp.setPadding(new Insets(12));
        gp.setHgap(10);
        gp.setVgap(10);
        gp.addRow(0, new Label("Name"), name);
        gp.addRow(1, new Label("Options"), photo);

        HBox buttons = new HBox(8, ok, cancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        gp.add(buttons, 1, 2);

        stage.setScene(new Scene(gp));
        stage.showAndWait();

        if ("CANCEL".equals(name.getUserData())) return Optional.empty();
        String n = name.getText() == null ? "" : name.getText().trim();
        if (n.isBlank()) return Optional.empty();
        return Optional.of(new Result(n, photo.isSelected()));
    }
}
