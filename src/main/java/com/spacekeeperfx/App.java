package com.spacekeeperfx;

import com.spacekeeperfx.config.AppConfig;
import com.spacekeeperfx.persistence.Database;
import com.spacekeeperfx.persistence.dao.RecordDao;
import com.spacekeeperfx.persistence.dao.SubspaceDao;
import com.spacekeeperfx.persistence.dao.VaultDao;
import com.spacekeeperfx.repository.RecordRepository;
import com.spacekeeperfx.repository.SubspaceRepository;
import com.spacekeeperfx.repository.VaultRepository;
import com.spacekeeperfx.service.ImageService;
import com.spacekeeperfx.service.WindowResizer;
import com.spacekeeperfx.ui.MainController;
import com.spacekeeperfx.ui.TitleBar;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        stage.initStyle(StageStyle.UNDECORATED);
        // 1) Load config instance
        AppConfig config = AppConfig.load();

        // 2) Initialize DB (single-db fallback; you later open a vault .db from UI)
        Path dbPath = config.getDbPath();
        Database db = new Database(dbPath);
        db.init(); // runs migrations

        // 3) Wire DAOs and Repos
        VaultRepository vaultRepo       = new VaultRepository(new VaultDao(db));
        SubspaceRepository subspaceRepo = new SubspaceRepository(new SubspaceDao(db));
        RecordRepository recordRepo     = new RecordRepository(new RecordDao(db));

        // 4) Services
        ImageService imageService = new ImageService(config.getPhotosDir());

        // 5) Load UI
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main_layout.fxml"));
        Parent fxmlRoot  = loader.load();
        MainController controller = loader.getController();

        // 6) Inject repos & services
        controller.setRepositories(vaultRepo, subspaceRepo, recordRepo);
        controller.setImageService(imageService);

        if (!(fxmlRoot instanceof javafx.scene.layout.Region)) {
            throw new IllegalStateException("main_layout.fxml root must be a Region");
        }
        javafx.scene.layout.Region appContent = (javafx.scene.layout.Region) fxmlRoot;

        // tiny 16px icon for the bar
        Image smallIcon = new Image(
                getClass().getResourceAsStream("/icons/app.png")
        );

        TitleBar titleBar = new TitleBar(stage, "SpaceKeeperFX", smallIcon);
        titleBar.getStylesheets().add(
                getClass().getResource("/css/app.css").toExternalForm()
        );

        // Outer container (lets us style rounded corners, shadow, etc.)
        javafx.scene.layout.VBox windowRoot = new javafx.scene.layout.VBox(titleBar, appContent);
        windowRoot.getStyleClass().add("window-root");

        // 4) Scene + theme + resizer
        Scene scene = new Scene(windowRoot, 1200, 800);
        config.applyTheme(scene); // keep your theming

        // Edge resize for undecorated window
        new WindowResizer(stage, windowRoot).setBorderWidth(6);

        loadStageIcons(stage);

        stage.setTitle("SpaceKeeperFX");
        stage.setScene(scene);
        stage.show();
    }

    private void loadStageIcons(Stage stage) {
        String[] names = {"app.png"};
        List<Image> icons = new ArrayList<>();
        for (String n : names) {
            URL url = App.class.getResource("/icons/" + n);
            if (url != null) {
                icons.add(new Image(url.toExternalForm()));
            } else {
                System.err.println("WARN: icon not found: /icons/" + n);
            }
        }
        if (!icons.isEmpty()) {
            stage.getIcons().setAll(icons);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
