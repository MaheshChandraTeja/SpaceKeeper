package com.spacekeeperfx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class TitleBar extends HBox {
    private final Stage stage;
    private double dragOffsetX, dragOffsetY;

    private boolean maximized = false;
    private Rectangle2D savedBounds = null;

    public TitleBar(Stage stage, String title, Image smallIcon16) {
        this.stage = stage;

        getStyleClass().add("sk-titlebar");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(8, 10, 8, 10));
        setSpacing(8);

        ImageView iv = new ImageView(smallIcon16);
        iv.setFitWidth(16); iv.setFitHeight(16);
        iv.setSmooth(false); // keep pixel-sharp at 16px

        Label lbl = new Label(title);
        lbl.getStyleClass().add("titlebar-title");

        Region spacer = new Region();
        spacer.setMinWidth(Region.USE_COMPUTED_SIZE);
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnMin = sysBtn("—");       // minimize
        btnMin.setOnAction(e -> stage.setIconified(true));

        Button btnMax = sysBtn("▢");       // maximize / restore
        btnMax.setOnAction(e -> toggleMaximize());

        Button btnClose = sysBtn("✕");     // close
        btnClose.getStyleClass().add("close");
        btnClose.setOnAction(e -> stage.close());

        getChildren().addAll(iv, lbl, spacer, btnMin, btnMax, btnClose);

        // drag window by titlebar
        setOnMousePressed(this::rememberOffset);
        setOnMouseDragged(this::dragWindow);
        // double-click to toggle maximize
        setOnMouseClicked(e -> {
            if (e.getButton()==MouseButton.PRIMARY && e.getClickCount()==2) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
    }

    private Screen currentScreen() {
        var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        return screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
    }

    private void toggleMaximize() {
        if (!maximized) {
            // save current size/pos
            savedBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            // fill visual bounds (excludes taskbar)
            Rectangle2D vb = currentScreen().getVisualBounds();
            stage.setX(vb.getMinX());
            stage.setY(vb.getMinY());
            stage.setWidth(vb.getWidth());
            stage.setHeight(vb.getHeight());
            maximized = true;
        } else {
            if (savedBounds != null) {
                stage.setX(savedBounds.getMinX());
                stage.setY(savedBounds.getMinY());
                stage.setWidth(savedBounds.getWidth());
                stage.setHeight(savedBounds.getHeight());
            }
            maximized = false;
        }
    }

    private Button sysBtn(String glyph) {
        Button b = new Button(glyph);
        b.getStyleClass().add("titlebar-btn");
        b.setMinSize(34, 28);
        b.setPrefSize(34, 28);

        // NEW: ensure glyphs render and remove focus ring
        b.setMnemonicParsing(false);
        b.setFocusTraversable(false);
        b.setStyle("-fx-font-family: 'Segoe UI Symbol','Segoe UI',system-ui,sans-serif; -fx-font-size: 12px;");

        return b;
    }

    private void rememberOffset(MouseEvent e) {
        if (stage.isMaximized()) return;
        dragOffsetX = e.getScreenX() - stage.getX();
        dragOffsetY = e.getScreenY() - stage.getY();
    }
    private void dragWindow(MouseEvent e) {
        if (stage.isMaximized()) return;
        stage.setX(e.getScreenX() - dragOffsetX);
        stage.setY(e.getScreenY() - dragOffsetY);
    }
}
