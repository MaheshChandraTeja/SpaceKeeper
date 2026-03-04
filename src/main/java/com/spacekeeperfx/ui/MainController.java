package com.spacekeeperfx.ui;

import com.spacekeeperfx.config.AppConfig;
import com.spacekeeperfx.model.Subspace;
import com.spacekeeperfx.model.Vault;
import com.spacekeeperfx.persistence.Database;
import com.spacekeeperfx.persistence.dao.RecordDao;
import com.spacekeeperfx.persistence.dao.SubspaceDao;
import com.spacekeeperfx.persistence.dao.VaultDao;
import com.spacekeeperfx.repository.RecordRepository;
import com.spacekeeperfx.repository.SubspaceRepository;
import com.spacekeeperfx.repository.VaultRepository;
import com.spacekeeperfx.service.ImageService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class MainController {

    /* ---------- FXML nodes ---------- */
    @FXML private BorderPane root;

    // Top bar
    @FXML private Label  vaultNameLabel;
    @FXML private Button openVaultButton;
    @FXML private Button closeVaultButton;
    @FXML private Button newVaultButton;
    @FXML private Button newSubspaceButton;
    @FXML private Button manageColumnsButton;
    @FXML private Button addRecordButton;
    @FXML private Button deleteRecordButton;

    // Bottom bar
    @FXML private Label statusLabel;

    // Included controllers (via <fx:include fx:id="sidebar" ...> and <fx:include fx:id="subspaceTable" ...>)
    @FXML private SidebarController       sidebarController;
    @FXML private SubspaceTableController subspaceTableController;

    @FXML private SplitPane mainSplit;
    @FXML private AnchorPane sidebarPane;
    @FXML private ToggleButton themeToggle;

    /* ---------- App state ---------- */
    private Database currentDb;
    private VaultRepository    vaultRepo;
    private SubspaceRepository subspaceRepo;
    private RecordRepository   recordRepo;

    private Vault currentVault;
    private ImageService imageService;

    private Button dividerToggle;
    private boolean sidebarCollapsed = false;
    private double lastDividerPos = 0.22;


    @FXML
    private void initialize() {
        // Wire top bar actions
        openVaultButton.setOnAction(e -> openVaultFlow());
        closeVaultButton.setOnAction(e -> closeVaultFlow());
        newVaultButton.setOnAction(e -> newVaultFlow());
        newSubspaceButton.setOnAction(e -> createSubspaceFlow());
        manageColumnsButton.setOnAction(e -> openColumnManager());
        addRecordButton.setOnAction(e -> subspaceTableController.addBlankRecord());
        deleteRecordButton.setOnAction(e -> subspaceTableController.deleteSelectedRecords());

        // Sidebar selection + context actions
        sidebarController.setOnSubspaceSelected(this::onSubspaceSelected);
        sidebarController.setOnContextRename(this::renameSubspace);
        sidebarController.setOnContextDelete(this::deleteSubspace);

        // Table notifies status bar
        subspaceTableController.setNotifier(this::toast);

        Platform.runLater(this::installDividerToggle);
        setupThemeToggle();
        // Start with no vault open
        setUiEnabled(false);
        vaultNameLabel.setText("No vault");
        toast("Ready.");
    }

    private static final String BASE_CSS = "/css/base.css";
    private static final String DARK_CSS = "/css/dark.css";
    /* ---------------- Vault open/close/create ---------------- */

    private void openVaultFlow() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Vault (.db)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db"));
        var startDir = AppConfig.vaultsDir();
        if (startDir != null) fc.setInitialDirectory(startDir.toFile());
        File f = fc.showOpenDialog(root.getScene().getWindow());
        if (f == null) return;

        openDb(Path.of(f.getAbsolutePath()));
    }

    private void newVaultFlow() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Create Vault");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db"));
        var startDir = AppConfig.vaultsDir();
        if (startDir != null) fc.setInitialDirectory(startDir.toFile());
        fc.setInitialFileName("MyVault.db");
        File f = fc.showSaveDialog(root.getScene().getWindow());
        if (f == null) return;

        openDb(Path.of(f.getAbsolutePath())); // Database will be created and migrated on first open
    }

    private void closeVaultFlow() {
        if (currentDb != null) {
            try { currentDb.close(); } catch (Exception ignored) {}
        }
        currentDb = null;
        currentVault = null;
        vaultRepo = null;
        subspaceRepo = null;
        recordRepo = null;

        sidebarController.setSubspaces(FXCollections.observableArrayList());
        subspaceTableController.setRecords(FXCollections.observableArrayList());
        subspaceTableController.rebuildColumns(List.of());

        setUiEnabled(false);
        vaultNameLabel.setText("No vault");
        toast("Vault closed.");
    }

    private void installDividerToggle() {
        var dividers = mainSplit.lookupAll(".split-pane-divider");
        if (dividers.isEmpty()) return;

        var divider = dividers.iterator().next();
        if (!(divider instanceof Pane pane)) return;

        // avoid duplicates
        pane.getChildren().removeIf(n -> "split-toggle".equals(n.getId()));

        dividerToggle = new Button("‹");
        dividerToggle.setId("split-toggle");
        dividerToggle.getStyleClass().add("split-toggle");
        dividerToggle.setFocusTraversable(false);
        dividerToggle.setOnAction(e -> toggleSidebar());

        // place roughly centered
        pane.heightProperty().addListener((obs, o, h) -> {
            dividerToggle.setLayoutX(3);
            dividerToggle.setLayoutY(h.doubleValue() / 2 - 14);
        });

        pane.getChildren().add(dividerToggle);
    }

    private void toggleSidebar() {
        if (!sidebarCollapsed) {
            lastDividerPos = mainSplit.getDividers().get(0).getPosition();
            SplitPane.setResizableWithParent(sidebarPane, false);
            sidebarPane.setMinWidth(0);
            sidebarPane.setPrefWidth(0);
            mainSplit.setDividerPositions(0.0);
            dividerToggle.setText("›");
        } else {
            SplitPane.setResizableWithParent(sidebarPane, true);
            mainSplit.setDividerPositions(lastDividerPos <= 0.02 ? 0.22 : lastDividerPos);
            dividerToggle.setText("‹");
        }
        sidebarCollapsed = !sidebarCollapsed;
    }

    private void openDb(Path dbPath) {
        try {
            // Close previous if any
            if (currentDb != null) {
                try { currentDb.close(); } catch (Exception ignored) {}
            }

            currentDb = Database.open(dbPath);   // your Database.open(Path) should run migrations internally
            buildRepos(currentDb);

            // Ensure a vault row exists; use filename as display if empty
            var all = vaultRepo.listAll();
            if (all.isEmpty()) {
                String name = dbPath.getFileName().toString().replace(".db", "");
                currentVault = vaultRepo.create(name);
            } else {
                currentVault = all.get(0);
            }

            vaultNameLabel.setText(currentVault.getName());

            // Load subspaces
            var subs = subspaceRepo.listByVault(currentVault.getId());
            sidebarController.setSubspaces(FXCollections.observableArrayList(subs));
            if (!subs.isEmpty()) {
                sidebarController.selectFirst();
                onSubspaceSelected(subs.get(0));
            } else {
                subspaceTableController.setRecords(FXCollections.observableArrayList());
                subspaceTableController.rebuildColumns(List.of());
            }

            setUiEnabled(true);
            toast("Opened: " + dbPath.getFileName());
        } catch (Exception ex) {
            setUiEnabled(false);
            new Alert(Alert.AlertType.ERROR, "Failed to open vault:\n" + ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void buildRepos(Database db) {
        vaultRepo    = new VaultRepository(new VaultDao(db));
        subspaceRepo = new SubspaceRepository(new SubspaceDao(db));
        recordRepo   = new RecordRepository(new RecordDao(db));

        // Image service uses the vault-specific photos directory
        imageService = new ImageService(AppConfig.photosDirFor(db));
        subspaceTableController.setRecordRepository(recordRepo);
        subspaceTableController.setImageService(imageService);
    }

    /* ---------------- Subspace actions ---------------- */

    private void onSubspaceSelected(Subspace s) {
        if (s == null) return;
        // rebuild columns based on defs
        subspaceTableController.setCurrentSubspace(s.getId(), s.getName());
        subspaceTableController.rebuildColumns(s.getColumns());

        // load records
        var recs = recordRepo.listBySubspace(s.getId());
        subspaceTableController.setRecords(FXCollections.observableArrayList(recs));
        toast("“" + s.getName() + "” loaded (" + recs.size() + " rows).");
    }

    private void createSubspaceFlow() {
        if (currentVault == null) return;

        SubspaceDialogController.createNew(root.getScene().getWindow()).ifPresent(res -> {
            Subspace created = subspaceRepo.create(currentVault.getId(), res.name(), res.enablePhotograph());

            // refresh and focus the new one
            var subs = subspaceRepo.listByVault(currentVault.getId());
            sidebarController.setSubspaces(FXCollections.observableArrayList(subs));
            subs.stream()
                    .filter(x -> x.getId().equals(created.getId()))
                    .findFirst()
                    .ifPresent(sel -> {
                        sidebarController.setOnSubspaceSelected(null);
                        sidebarController.select(sel);
                        sidebarController.setOnSubspaceSelected(this::onSubspaceSelected);
                        onSubspaceSelected(sel);
                    });

            toast("Subspace created: " + created.getName());
        });
    }

    private void renameSubspace(Subspace s) {
        if (s == null) return;
        TextInputDialog dlg = new TextInputDialog(s.getName());
        dlg.setHeaderText(null);
        dlg.setContentText("Rename subspace:");
        dlg.showAndWait().ifPresent(newName -> {
            if (newName != null && !newName.isBlank()) {
                if (subspaceRepo.rename(s.getId(), newName)) {
                    // reload list and update selection
                    var subs = subspaceRepo.listByVault(currentVault.getId());
                    sidebarController.setSubspaces(FXCollections.observableArrayList(subs));
                    subs.stream().filter(x -> x.getId().equals(s.getId())).findFirst()
                            .ifPresent(this::onSubspaceSelected);
                    toast("Renamed.");
                }
            }
        });
    }

    private void deleteSubspace(Subspace s) {
        if (s == null) return;
        if (new Alert(Alert.AlertType.CONFIRMATION,
                "Delete subspace “" + s.getName() + "” and all its rows?",
                ButtonType.OK, ButtonType.CANCEL).showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        if (subspaceRepo.delete(s.getId())) {
            var subs = subspaceRepo.listByVault(currentVault.getId());
            sidebarController.setSubspaces(FXCollections.observableArrayList(subs));
            if (!subs.isEmpty()) {
                sidebarController.selectFirst();
                onSubspaceSelected(subs.get(0));
            } else {
                subspaceTableController.setRecords(FXCollections.observableArrayList());
                subspaceTableController.rebuildColumns(List.of());
            }
            toast("Subspace deleted.");
        }
    }

    private void openColumnManager() {
        if (sidebarController.getSelected() == null) return;
        var sub = sidebarController.getSelected();
        ColumnManagerController.open(root.getScene().getWindow(), subspaceRepo, sub)
                .ifPresent(updated -> {
                    // Persist new column defs, then reload this subspace
                    subspaceRepo.replaceColumns(sub.getId(), updated);
                    Subspace fresh = subspaceRepo.getById(sub.getId());
                    onSubspaceSelected(fresh);
                    toast("Columns updated.");
                });
    }

    /* ---------------- UI helpers ---------------- */

    private void initThemeToggle() {
        // initial apply (true => dark, false => light)
        applyTheme(themeToggle.isSelected());

        themeToggle.selectedProperty().addListener((obs, wasDark, isDark) -> {
            themeToggle.setText(isDark ? "☾" : "☀︎");
            applyTheme(isDark);
            if (statusLabel != null) {
                statusLabel.setText("Theme: " + (isDark ? "Dark" : "Light"));
            }
            // optionally persist a preference here
        });
    }

    private void applyTheme(boolean dark) {
        if (root == null || root.getScene() == null) return;

        var sheets = root.getScene().getStylesheets();
        // remove any previous theme refs
        sheets.removeIf(s -> s.endsWith("base.css") || s.endsWith("dark.css"));

        // base first, then optionally dark overlay
        sheets.add(MainController.class.getResource(BASE_CSS).toExternalForm());
        if (dark) {
            sheets.add(MainController.class.getResource(DARK_CSS).toExternalForm());
        }
    }

    private void setupThemeToggle() {
        if (themeToggle == null) return;

        themeToggle.getStyleClass().add("toggle-theme");
        themeToggle.setText("🌙"); // will be restyled by CSS
        themeToggle.setSelected(true); // start in dark; flip if you want light by default
        applyTheme(themeToggle.isSelected());

        themeToggle.selectedProperty().addListener((obs, oldV, isDark) -> {
            themeToggle.setText(isDark ? "🌙" : "☀");
            applyTheme(isDark);
            status("Theme: " + (isDark ? "Dark" : "Light"));
        });
    }

    private void setUiEnabled(boolean enabled) {
        closeVaultButton.setDisable(!enabled);
        newSubspaceButton.setDisable(!enabled);
        manageColumnsButton.setDisable(!enabled);
        addRecordButton.setDisable(!enabled);
        deleteRecordButton.setDisable(!enabled);
    }

    private void toast(String msg) {
        statusLabel.setText(msg);
        FadeTransition ft = new FadeTransition(Duration.millis(250), statusLabel);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // inside MainController
    public void setRepositories(VaultRepository vaultRepo,
                                SubspaceRepository subspaceRepo,
                                RecordRepository recordRepo) {
        this.vaultRepo = vaultRepo;
        this.subspaceRepo = subspaceRepo;
        this.recordRepo = recordRepo;
    }

    public void setImageService(com.spacekeeperfx.service.ImageService imageService) {
        this.imageService = imageService;
        if (subspaceTableController != null) {
            subspaceTableController.setImageService(imageService);
        }
    }

    private void status(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }
}
