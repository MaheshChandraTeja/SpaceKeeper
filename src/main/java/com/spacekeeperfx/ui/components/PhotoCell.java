package com.spacekeeperfx.ui.components;

import com.spacekeeperfx.model.Record;
import com.spacekeeperfx.service.ImageService;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class PhotoCell extends TableCell<Record, String> {

    private final ImageService imageService;
    private final String columnId;
    private final BiConsumer<Record, String> saver;   // persist callback

    private final StackPane root = new StackPane();
    private final ImageView imageView = new ImageView();
    private final Button addBtn = new Button("+");

    public PhotoCell(ImageService imageService,
                     String columnId,
                     BiConsumer<Record, String> saver) {
        this.imageService = imageService;
        this.columnId = columnId;
        this.saver = saver;

        // image view
        imageView.setFitWidth(64);
        imageView.setFitHeight(64);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("photo-thumb");

        // add button (when no image yet)
        addBtn.getStyleClass().add("photo-add");
        addBtn.setOnAction(e -> chooseAndSave());

        root.getChildren().addAll(imageView, addBtn);
        StackPane.setAlignment(addBtn, Pos.CENTER);

        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setAlignment(Pos.CENTER);
        getStyleClass().add("photo-cell");
    }

    @Override
    protected void updateItem(String relPath, boolean empty) {
        super.updateItem(relPath, empty);
        if (empty) {
            setGraphic(null);
            return;
        }

        if (relPath == null || relPath.isBlank()) {
            imageView.setImage(null);
            addBtn.setVisible(true);
        } else {
            try {
                Path abs = imageService.resolve(relPath);       // <— absolute path for ImageView
                Image img = new Image(abs.toUri().toString(), 64, 64, true, true);
                imageView.setImage(img);
                addBtn.setVisible(false);
            } catch (Exception ex) {
                imageView.setImage(null);
                addBtn.setVisible(true);
            }
        }
        setGraphic(root);
    }

    private void chooseAndSave() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp"));
        File f = fc.showOpenDialog(getScene().getWindow());
        if (f == null) return;

        try {
            // copy into the vault’s photos folder -> return RELATIVE path
            String relative = imageService.importPhoto(f);
            Record r = getTableRow() == null ? null : getTableRow().getItem();
            if (r != null) {
                saver.accept(r, relative);          // persist + update model (done by controller)
                getTableView().refresh();
            }
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Failed to import image:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}
