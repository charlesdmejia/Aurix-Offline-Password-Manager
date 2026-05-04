package gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import javax.crypto.SecretKey;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.beans.binding.Bindings;
import javafx.util.Duration;
import core.BreachChecker;
import core.AccessLimiter;
import core.KeyDerivation;
import core.VaultSearch;
import core.SecurityInfo;
import core.VaultRepo;
import model.PasswordEntry;
import model.Vault;
import model.VaultMeta;

public class MainApp extends Application {

    // ─── Colors
    private static final Color BG_DARK      = Color.web("#0a0b0f");
    private static final Color BG_CARD      = Color.web("#111926");
    private static final Color BG_INPUT     = Color.web("#1e2430");
    private static final Color ACCENT       = Color.web("#63b5ed");
    private static final Color ACCENT2      = Color.web("#9ae4b4");
    private static final Color DANGER       = Color.web("#fc8191");
    private static final Color WARNING      = Color.web("#f6ad55");
    private static final Color TEXT_PRIMARY = Color.web("#edf2f7");
    private static final Color TEXT_MUTED   = Color.web("#718096");
    private static final Color BORDER_COLOR = Color.web("#2d3748");

    // ─── Fonts
    private static final Font FONT_TITLE = Font.font("Consolas", FontWeight.BOLD,   26);
    private static final Font FONT_LABEL = Font.font("Consolas", FontWeight.BOLD,   12);
    private static final Font FONT_BODY  = Font.font("Consolas", FontWeight.NORMAL, 13);
    private static final Font FONT_SMALL = Font.font("Consolas", FontWeight.NORMAL, 11);

    // ─── Core fields
    private final AccessLimiter bruteForce    = new AccessLimiter();
    private final VaultSearch        searchManager = new VaultSearch();
    private VaultRepo storageManager;
    private Vault          vault;
    private SecretKey      aesKey;
    private VaultMeta      vaultMeta;

    // ─── GUI state
    private ObservableList<PasswordEntry> vaultData         = FXCollections.observableArrayList();
    private TableView<PasswordEntry>      table;
    private StackPane                     mainPane;
    private Scene                         scene;
    private Label                         countLabel;
    private String                        recoveryKey       = null;
    private int                           clipboardClearSec = 0;

    // ─────────────────────────────────────────────
    //  ENTRY POINT
    // ─────────────────────────────────────────────
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        buildTable();
        mainPane = new StackPane();
        scene = new Scene(mainPane, 1200, 800);

        var cssUrl = getClass().getResource("styles.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

        primaryStage.setTitle("AURIX");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMaximized(true);
        showLogin();
    }

    // ─────────────────────────────────────────────
    //  BUILD TABLE
    // ─────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void buildTable() {
        table = new TableView<>(vaultData);
        table.setStyle(
            "-fx-background-color: " + toHex(BG_DARK) + "; " +
            "-fx-control-inner-background: " + toHex(BG_DARK) + "; " +
            "-fx-table-cell-border-color: transparent; " +
            "-fx-table-header-border-color: transparent; " +
            "-fx-background-insets: 0; " +
            "-fx-padding: 0;"
        );
        table.setPlaceholder(new Label(""));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<PasswordEntry, String> colSite = new TableColumn<>("Website");
        colSite.setCellValueFactory(new PropertyValueFactory<>("website"));

        TableColumn<PasswordEntry, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<PasswordEntry, String> colPass = new TableColumn<>("Password");
        colPass.setCellValueFactory(new PropertyValueFactory<>("password"));
        colPass.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String pw, boolean empty) {
                super.updateItem(pw, empty);
                setText(empty || pw == null ? null : maskPassword(pw));
            }
        });

        TableColumn<PasswordEntry, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<PasswordEntry, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button copyBtn = miniBtn("Copy",   ACCENT);
            private final Button editBtn = miniBtn("Edit",   ACCENT2);
            private final Button deleteBtn = miniBtn("Delete", DANGER);
            private final HBox box = new HBox(6, copyBtn, editBtn, deleteBtn);
            {
                box.setAlignment(Pos.CENTER);
                copyBtn.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < vaultData.size())
                        copyToClipboard(vaultData.get(idx).getPassword(), "✓ Password copied!");
                });
                editBtn.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < vaultData.size()) showAddEditDialog(idx);
                });
                deleteBtn.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < vaultData.size()) {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this entry?");
                        styleAlert(alert);
                        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                            vaultData.remove(idx);
                            vault.getEntries().remove(idx);
                            saveVaultSilently();
                            refreshTable();
                        }
                    }
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) {
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                } else {
                    setGraphic(box);
                    setStyle("-fx-background-color: " + toHex(BG_DARK) +
                             "; -fx-border-color: rgba(255,255,255,0.12) transparent transparent transparent;");
                }
            }
        });

        table.getColumns().addAll(colSite, colUser, colPass, colCat, colActions);

        // ── Double-click a row to reveal its password for 10 seconds
        table.setRowFactory(tv -> {
            TableRow<PasswordEntry> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    PasswordEntry entry = row.getItem();

                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Identity Verification");
                    dialog.setHeaderText("Re-enter your master password to reveal this entry.");
                    dialog.setContentText("Master password:");
                    dialog.getDialogPane().setStyle(
                            "-fx-background-color: " + toHex(BG_CARD) + ";");
                    dialog.getDialogPane().lookupAll(".label").forEach(n ->
                            ((Label) n).setTextFill(TEXT_PRIMARY));

                    dialog.showAndWait().ifPresent(input -> {
                        try {
                            SecretKey testKey = KeyDerivation.generateKey(input, vaultMeta.getSalt());
                            new VaultRepo(testKey).loadVault();

                            int idx = vaultData.indexOf(entry);
                            if (idx < 0) return;

                            Alert reveal = new Alert(Alert.AlertType.INFORMATION);
                            reveal.setTitle("Password Revealed");
                            reveal.setHeaderText(entry.getWebsite() + " — " + entry.getUsername());
                            reveal.setContentText(
                                    "Password: " + entry.getPassword() +
                                    "\n\nThis dialog closes automatically in 10 seconds.");
                            styleAlert(reveal);
                            reveal.show();

                            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                    .setContents(new java.awt.datatransfer.StringSelection(
                                            entry.getPassword()), null);

                            if (clipboardClearSec > 0) {
                                new Timeline(new KeyFrame(Duration.seconds(clipboardClearSec), ae -> {
                                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                            .setContents(new java.awt.datatransfer.StringSelection(""), null);
                                })).play();
                            }

                            new Timeline(new KeyFrame(Duration.seconds(10), ae -> reveal.close()))
                                    .play();

                        } catch (Exception ex) {
                            showToast("❌ Incorrect password. Access denied.");
                        }
                    });
                }
            });
            return row;
        });
    }

    // ─────────────────────────────────────────────
    //  SCREEN 1 — LOGIN
    // ─────────────────────────────────────────────
    private void showLogin() {
        // ── AnchorPane so background truly fills the entire window
        AnchorPane root = new AnchorPane();

        Pane animatedBg = createAnimatedLoginBackground();
        AnchorPane.setTopAnchor(animatedBg,    0.0);
        AnchorPane.setBottomAnchor(animatedBg, 0.0);
        AnchorPane.setLeftAnchor(animatedBg,   0.0);
        AnchorPane.setRightAnchor(animatedBg,  0.0);
        root.getChildren().add(animatedBg);

        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                      "; -fx-border-color: " + toHex(BORDER_COLOR) +
                      "; -fx-border-radius: 12; -fx-background-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0.2, 0, 8);");
        card.setMaxWidth(480);
        card.setPrefWidth(480);
        card.setMaxHeight(Region.USE_PREF_SIZE); 
        card.setPrefHeight(Region.USE_COMPUTED_SIZE); 

        Label logo = new Label("AURIX");
        logo.setFont(FONT_TITLE);
        logo.setTextFill(ACCENT);

        Label sub = new Label("SECURE PASSWORD MANAGER");
        sub.setFont(FONT_SMALL);
        sub.setTextFill(TEXT_MUTED);

        Separator sep = new Separator();

        Label pwLabel         = styledLabel("MASTER PASSWORD");
        PasswordField pwField = styledPasswordField("Enter master password");
        Button loginBtn       = primaryButton("UNLOCK VAULT", ACCENT);
        Button createBtn      = ghostButton("Create New Vault →");
        Button forgotBtn      = ghostButton("Forgot master password?");
        forgotBtn.setFont(FONT_SMALL);
        Label errLabel        = new Label(" ");
        errLabel.setFont(FONT_SMALL);
        errLabel.setTextFill(DANGER);

        Timeline[] countdownHolder = { null };

        loginBtn.setOnAction(e -> {
            String entered = pwField.getText().trim();
            if (entered.isEmpty()) { errLabel.setText("⚠ Password cannot be empty."); return; }

            if (!bruteForce.attemptLogin()) {
                if (countdownHolder[0] != null) countdownHolder[0].stop();

                Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), ae -> {
                    long secs = bruteForce.getRemainingLockoutSeconds();
                    if (secs > 0) {
                        errLabel.setText("🔒 Locked — try again in " + secs + "s");
                        loginBtn.setDisable(true);
                        pwField.setDisable(true);
                    } else {
                        errLabel.setText("✓ Lockout expired. You may try again.");
                        loginBtn.setDisable(false);
                        pwField.setDisable(false);
                        if (countdownHolder[0] != null) countdownHolder[0].stop();
                    }
                }));
                countdown.setCycleCount(Timeline.INDEFINITE);
                countdown.play();
                countdownHolder[0] = countdown;

                long secs = bruteForce.getRemainingLockoutSeconds();
                errLabel.setText("🔒 Locked — try again in " + secs + "s");
                loginBtn.setDisable(true);
                pwField.setDisable(true);
                return;
            }

            loginBtn.setDisable(false);
            pwField.setDisable(false);

            try {
                storageManager = new VaultRepo(null);
                vaultMeta      = storageManager.loadOrCreateMeta();
                aesKey         = KeyDerivation.generateKey(entered, vaultMeta.getSalt());
                storageManager.setKey(aesKey);
                vault          = storageManager.loadVault();

                bruteForce.resetAttempts();
                pwField.clear();
                errLabel.setText(" ");
                vaultData.setAll(vault.getEntries());
                showVault();
                refreshTable();

            } catch (Exception ex) {
                errLabel.setText("⚠ Incorrect password or vault is corrupted.");
                pwField.clear();
            }
        });

        forgotBtn.setOnAction(e -> {
            if (recoveryKey == null) {
                showToast("⚠ No recovery key found. Create a vault first.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Vault Recovery");
            dialog.setHeaderText("Enter your 24-character recovery key:");
            dialog.setContentText("Recovery key:");
            dialog.getDialogPane().setStyle("-fx-background-color: " + toHex(BG_CARD) + ";");
            dialog.getDialogPane().lookupAll(".label").forEach(n ->
                    ((Label) n).setTextFill(TEXT_PRIMARY));
            dialog.showAndWait().ifPresent(input -> {
                if (input.trim().equals(recoveryKey)) {
                    showToast("🔑 Key verified. Reset your master password.");
                    showCreate();
                } else {
                    showToast("❌ Invalid recovery key.");
                }
            });
        });

        createBtn.setOnAction(e -> { errLabel.setText(" "); showCreate(); });

        card.getChildren().addAll(logo, sub, sep, pwLabel, pwField, loginBtn, errLabel, forgotBtn, createBtn);

        // ── StackPane centers the card; AnchorPane stretches it to fill root
        card.setMinHeight(Region.USE_PREF_SIZE);  
        card.setMaxHeight(Region.USE_PREF_SIZE);  
        StackPane cardHolder = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER); // ensure centering
        AnchorPane.setTopAnchor(cardHolder,    0.0);
        AnchorPane.setBottomAnchor(cardHolder, 0.0);
        AnchorPane.setLeftAnchor(cardHolder,   0.0);
        AnchorPane.setRightAnchor(cardHolder,  0.0);
        root.getChildren().add(cardHolder);

        mainPane.getChildren().setAll(root);
    }

    // ─────────────────────────────────────────────
    //  ANIMATED BACKGROUND
    // ─────────────────────────────────────────────
    private Pane createAnimatedLoginBackground() {
        Pane pane = new Pane();
        pane.setStyle("-fx-background-color: linear-gradient(to bottom, #0b1020, #0a0b0f);");

        int maxParticles = 150;

        for (int i = 0; i < 100; i++) {
            addParticle(pane);
        }

        pane.setOnMouseMoved(e -> {
            for (var node : pane.getChildren()) {
                if (node instanceof Circle circle) {
                    double dx = (e.getX() - circle.getLayoutX()) * 0.01;
                    double dy = (e.getY() - circle.getLayoutY()) * 0.01;
                    circle.setLayoutX(circle.getLayoutX() + dx);
                    circle.setLayoutY(circle.getLayoutY() + dy);
                }
            }
        });

        pane.setOnMouseClicked(e -> {
            if (pane.getChildren().size() < maxParticles) {
                int toAdd = Math.min(5, maxParticles - pane.getChildren().size());
                for (int i = 0; i < toAdd; i++) {
                    double offsetX = (Math.random() - 0.5) * 100;
                    double offsetY = (Math.random() - 0.5) * 100;
                    addParticleAt(pane, e.getX() + offsetX, e.getY() + offsetY);
                }
            }
        });

        pane.setOnMouseExited(e -> {
            for (var node : pane.getChildren()) {
                if (node instanceof Circle circle) {
                    circle.setFill(Color.web("#63b5ed", 0.14));
                }
            }
        });

        return pane;
    }

    private void addParticle(Pane pane) {
        double size = 3 + Math.random() * 4;
        Circle c = new Circle(size, Color.web("#63b5ed", 0.18));
        // Use 1920×1080 so particles cover any maximized screen
        c.setLayoutX(Math.random() * 1920);
        c.setLayoutY(Math.random() * 1080);
        pane.getChildren().add(c);

        double targetX = Math.random() * 1920;
        double targetY = Math.random() * 1080;
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(c.layoutXProperty(), c.getLayoutX()),
                new KeyValue(c.layoutYProperty(), c.getLayoutY())),
            new KeyFrame(Duration.seconds(12 + Math.random() * 8),
                new KeyValue(c.layoutXProperty(), targetX),
                new KeyValue(c.layoutYProperty(), targetY))
        );
        t.setCycleCount(Timeline.INDEFINITE);
        t.setAutoReverse(true);
        t.play();
    }

    private void addParticleAt(Pane pane, double x, double y) {
        double size = 3 + Math.random() * 4;
        Circle c = new Circle(size, Color.web("#63b5ed", 0.18));
        c.setLayoutX(x);
        c.setLayoutY(y);
        pane.getChildren().add(c);

        double targetX = Math.random() * 1920;
        double targetY = Math.random() * 1080;
        Timeline t = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(c.layoutXProperty(), c.getLayoutX()),
                new KeyValue(c.layoutYProperty(), c.getLayoutY())),
            new KeyFrame(Duration.seconds(12 + Math.random() * 8),
                new KeyValue(c.layoutXProperty(), targetX),
                new KeyValue(c.layoutYProperty(), targetY))
        );
        t.setCycleCount(Timeline.INDEFINITE);
        t.setAutoReverse(true);
        t.play();
    }

    // ─────────────────────────────────────────────
    //  SCREEN 2 — CREATE VAULT
    // ─────────────────────────────────────────────
    private void showCreate() {
        // ── AnchorPane so background truly fills the entire window
        AnchorPane root = new AnchorPane();

        Pane animatedBg = createAnimatedLoginBackground();
        AnchorPane.setTopAnchor(animatedBg,    0.0);
        AnchorPane.setBottomAnchor(animatedBg, 0.0);
        AnchorPane.setLeftAnchor(animatedBg,   0.0);
        AnchorPane.setRightAnchor(animatedBg,  0.0);
        root.getChildren().add(animatedBg);

        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                      "; -fx-border-color: " + toHex(BORDER_COLOR) +
                      "; -fx-border-radius: 12; -fx-background-radius: 12; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 15, 0.2, 0, 8);");
        card.setMaxWidth(480);
        card.setPrefWidth(480);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        Label title = new Label("CREATE YOUR VAULT");
        title.setFont(FONT_TITLE);
        title.setTextFill(ACCENT2);

        Label sub = new Label("Set a strong master password");
        sub.setFont(FONT_SMALL);
        sub.setTextFill(TEXT_MUTED);

        Separator sep = new Separator();

        PasswordField pw1 = styledPasswordField("New master password");
        PasswordField pw2 = styledPasswordField("Confirm master password");

        HBox strengthPanel = new HBox(10);
        strengthPanel.setAlignment(Pos.CENTER_LEFT);
        Label strengthLabel = new Label("Strength: —");
        strengthLabel.setFont(FONT_SMALL);
        strengthLabel.setTextFill(TEXT_MUTED);
        Rectangle strengthBg   = new Rectangle(150, 10);
        strengthBg.setFill(Color.web("#2c3e50"));
        Rectangle strengthFill = new Rectangle(0, 10);
        strengthFill.setFill(Color.web("#008000"));
        StackPane strengthBar  = new StackPane(strengthBg, strengthFill);
        strengthPanel.getChildren().addAll(strengthLabel, strengthBar);

        pw1.textProperty().addListener((obs, old, nw) -> {
            double percent = passwordStrength(nw) / 100.0;
            percent = Math.max(0, Math.min(1, percent));
            strengthFill.setWidth(150 * percent);
            if (percent < 0.3) {
                strengthLabel.setText("Strength: Weak");
                strengthFill.setFill(Color.web("#ff0000"));
            } else if (percent < 0.6) {
                strengthLabel.setText("Strength: Fair");
                strengthFill.setFill(Color.web("#ffa500"));
            } else {
                strengthLabel.setText("Strength: Strong");
                strengthFill.setFill(Color.web("#008000"));
            }
        });

        Button createBtn = primaryButton("CREATE VAULT", ACCENT2);
        Button backBtn   = ghostButton("← Back to Login");
        Label errLabel   = new Label(" ");
        errLabel.setFont(FONT_SMALL);
        errLabel.setTextFill(DANGER);

        createBtn.setOnAction(e -> {
            String p1 = pw1.getText().trim();
            String p2 = pw2.getText().trim();
            if (p1.isEmpty())    { errLabel.setText("⚠ Password cannot be empty.");      return; }
            if (p1.length() < 6) { errLabel.setText("⚠ Minimum 6 characters required."); return; }
            if (!p1.equals(p2))  { errLabel.setText("⚠ Passwords do not match.");         return; }

            try {
                storageManager = new VaultRepo(null);
                vaultMeta      = storageManager.loadOrCreateMeta();
                aesKey         = KeyDerivation.generateKey(p1, vaultMeta.getSalt());
                storageManager.setKey(aesKey);
                vault          = new Vault();
                storageManager.saveVault(vault);

                String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
                StringBuilder rkBuilder = new StringBuilder();
                SecureRandom sr = new SecureRandom();
                for (int i = 0; i < 24; i++) {
                    if (i > 0 && i % 6 == 0) rkBuilder.append("-");
                    rkBuilder.append(chars.charAt(sr.nextInt(chars.length())));
                }
                recoveryKey = rkBuilder.toString();

                Alert rkAlert = new Alert(Alert.AlertType.WARNING);
                rkAlert.setTitle("SAVE YOUR RECOVERY KEY");
                rkAlert.setHeaderText("Store this key somewhere safe.\nYou will NOT be shown it again.");
                TextArea rkArea = new TextArea(recoveryKey);
                rkArea.setEditable(false);
                rkArea.setWrapText(true);
                rkArea.setPrefHeight(60);
                rkArea.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
                rkArea.setStyle("-fx-background-color: " + toHex(BG_INPUT) +
                                "; -fx-text-fill: " + toHex(ACCENT2) + ";");
                rkAlert.getDialogPane().setContent(rkArea);
                styleAlert(rkAlert);
                rkAlert.initOwner(mainPane.getScene().getWindow());
                rkAlert.showAndWait();

                pw1.clear(); pw2.clear();
                errLabel.setText(" ");
                showToast("✓ Vault created! Recovery key saved.");
                showLogin();

            } catch (Exception ex) {
                errLabel.setText("⚠ Error creating vault: " + ex.getMessage());
            }
        });

        backBtn.setOnAction(e -> { errLabel.setText(" "); showLogin(); });

        Label notice = new Label(
                "⚠  Your master password is the only key to your vault. " +
                "For security reasons, it cannot be recovered by anyone — " +
                "including us. If you lose it, you will lose access to all your data.");
        notice.setWrapText(true);
        notice.setMaxWidth(400);
        notice.setFont(FONT_SMALL);
        notice.setTextFill(WARNING);
        notice.setStyle("-fx-background-color: rgba(246,173,85,0.08); " +
                        "-fx-border-color: rgba(246,173,85,0.35); " +
                        "-fx-border-radius: 6; -fx-background-radius: 6; " +
                        "-fx-padding: 10 12 10 12;");

        card.getChildren().addAll(title, sub, sep, notice,
                styledLabel("MASTER PASSWORD"), pw1, strengthPanel,
                styledLabel("CONFIRM PASSWORD"), pw2,
                createBtn, errLabel, backBtn);

        // ── StackPane centers the card; AnchorPane stretches it to fill root
        StackPane cardHolder = new StackPane(card);
        AnchorPane.setTopAnchor(cardHolder,    0.0);
        AnchorPane.setBottomAnchor(cardHolder, 0.0);
        AnchorPane.setLeftAnchor(cardHolder,   0.0);
        AnchorPane.setRightAnchor(cardHolder,  0.0);
        root.getChildren().add(cardHolder);

        mainPane.getChildren().setAll(root);
    }

    // ─────────────────────────────────────────────
    //  SCREEN 3 — VAULT
    // ─────────────────────────────────────────────
    private void showVault() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(BG_DARK) + ";");

        // ── Sidebar
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: " + toHex(BG_CARD) + "; -fx-padding: 24 16 24 16;");

        Label sideTitle = new Label("AURIX");
        sideTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 24));
        sideTitle.setTextFill(ACCENT);

        Label sideVer = new Label("v1.0 secure");
        sideVer.setFont(FONT_SMALL);
        sideVer.setTextFill(TEXT_MUTED);

        Separator sep = new Separator();

        Button nav1 = sideNavButton("🔐 My Passwords");
        Button nav2 = sideNavButton("⚙ Settings");
        Button nav3 = sideNavButton("🚪 Lock Vault");

        Label countLbl = new Label("0 entries");
        countLbl.setFont(FONT_SMALL);
        countLbl.setTextFill(TEXT_MUTED);
        this.countLabel = countLbl;

        nav2.setOnAction(e -> showSettings());
        nav3.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Lock and return to login?");
            styleAlert(alert);
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                vaultData.clear();
                vault  = null;
                aesKey = null;
                showLogin();
            }
        });

        Region spacerSide = new Region();
        VBox.setVgrow(spacerSide, Priority.ALWAYS);
        sidebar.getChildren().addAll(sideTitle, sideVer, sep, nav1, nav2, nav3, spacerSide, countLbl);

        // ── Top bar
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                        "; -fx-padding: 14 20 14 20; -fx-border-width: 0 0 1 0; -fx-border-color: " + toHex(BORDER_COLOR) + ";");

        Label vaultTitle = new Label("My Passwords");
        vaultTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        vaultTitle.setTextFill(TEXT_PRIMARY);

        TextField searchField = styledTextField("Search by website, username, or category...");
        searchField.setPrefWidth(320);

        ComboBox<String> searchFilter = new ComboBox<>();
        searchFilter.getItems().addAll("All", "Website", "Username", "Category");
        searchFilter.setValue("All");
        searchFilter.setStyle("-fx-background-color: " + toHex(BG_INPUT) +
                              "; -fx-text-fill: " + toHex(TEXT_PRIMARY) +
                              "; -fx-border-color: " + toHex(BORDER_COLOR) +
                              "; -fx-border-radius: 4;");

        searchField.textProperty().addListener((obs, old, nw) ->
                applySearchWithFilter(nw.trim(), searchFilter.getValue()));
        searchFilter.valueProperty().addListener((obs, old, nw) ->
                applySearchWithFilter(searchField.getText().trim(), nw));

        Region spacerTop = new Region();
        HBox.setHgrow(spacerTop, Priority.ALWAYS);

        Button addBtn = primaryButton("+ Add Entry", ACCENT);
        addBtn.setOnAction(e -> showAddEditDialog(-1));

        topBar.getChildren().addAll(vaultTitle, searchField, searchFilter, spacerTop, addBtn);

        // ── Table container
        Label emptyLabel = new Label("🔐\n\nNo passwords saved yet.\nClick \"+ Add Entry\" to get started.");
        emptyLabel.setStyle("-fx-text-fill: " + toHex(TEXT_MUTED) + "; -fx-alignment: center; -fx-text-alignment: center;");
        emptyLabel.setFont(Font.font("Consolas", 16));
        emptyLabel.visibleProperty().bind(Bindings.isEmpty(vaultData));
        emptyLabel.managedProperty().bind(emptyLabel.visibleProperty());

        StackPane tableContainer = new StackPane(table, emptyLabel);

        // ── Bottom generator bar
        HBox bottomBar = new HBox(10);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                           "; -fx-padding: 10 20 10 20; -fx-border-width: 1 0 0 0; -fx-border-color: " + toHex(BORDER_COLOR) + ";");

        Label genTitle = new Label("⚡ Quick Generator");
        genTitle.setFont(FONT_LABEL);
        genTitle.setTextFill(TEXT_MUTED);

        CheckBox cbSymbols = styledCheckbox("Symbols");
        CheckBox cbNumbers = styledCheckbox("Numbers");
        CheckBox cbUpper   = styledCheckbox("Uppercase");
        cbSymbols.setSelected(true);
        cbNumbers.setSelected(true);
        cbUpper.setSelected(true);

        Label lenLabel = new Label("Length: 16");
        lenLabel.setFont(FONT_SMALL);
        lenLabel.setTextFill(TEXT_MUTED);

        Slider lenSlider = new Slider(8, 32, 16);
        lenSlider.valueProperty().addListener((obs, old, nw) -> lenLabel.setText("Length: " + nw.intValue()));

        TextField genOutput = styledTextField("");
        genOutput.setEditable(false);
        genOutput.setPrefWidth(200);

        Button genBtn     = primaryButton("Generate", ACCENT);
        Button copyGenBtn = ghostButton("Copy");

        genBtn.setOnAction(e ->
                genOutput.setText(generatePassword(lenSlider.getValue(),
                        cbUpper.isSelected(), cbNumbers.isSelected(), cbSymbols.isSelected())));

        copyGenBtn.setOnAction(e -> {
            String pw = genOutput.getText();
            if (!pw.isEmpty()) copyToClipboard(pw, "✓ Generated password copied!");
        });

        bottomBar.getChildren().addAll(genTitle, cbUpper, cbNumbers, cbSymbols,
                lenLabel, lenSlider, genOutput, genBtn, copyGenBtn);

        BorderPane content = new BorderPane();
        content.setStyle("-fx-background-color: " + toHex(BG_DARK) + ";");
        content.setTop(topBar);
        content.setCenter(tableContainer);
        content.setBottom(bottomBar);

        root.setLeft(sidebar);
        root.setCenter(content);
        mainPane.getChildren().setAll(root);
    }

    // ─────────────────────────────────────────────
    //  SEARCH
    // ─────────────────────────────────────────────
    private void applySearchWithFilter(String query, String filter) {
        if (vault == null) return;
        if (query == null || query.isEmpty()) {
            vaultData.setAll(vault.getEntries());
        } else {
            List<PasswordEntry> results;
            switch (filter) {
                case "Website"  -> results = searchManager.searchByWebsite(vault, query);
                case "Username" -> results = searchManager.searchByUsername(vault, query);
                case "Category" -> results = searchManager.searchByCategory(vault, query);
                default -> {
                    results = searchManager.searchByWebsite(vault, query);
                    for (PasswordEntry e : searchManager.searchByUsername(vault, query))
                        if (!results.contains(e)) results.add(e);
                    for (PasswordEntry e : searchManager.searchByCategory(vault, query))
                        if (!results.contains(e)) results.add(e);
                }
            }
            vaultData.setAll(results);
        }
        refreshTable();
    }

    // ─────────────────────────────────────────────
    //  SCREEN 4 — SETTINGS
    // ─────────────────────────────────────────────
    private void showSettings() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + toHex(BG_DARK) + ";");

        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                        "; -fx-padding: 14 20 14 20; -fx-border-width: 0 0 1 0; -fx-border-color: " + toHex(BORDER_COLOR) + ";");
        Label title  = new Label("Settings & Security");
        title.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        title.setTextFill(TEXT_PRIMARY);
        Button backBtn = ghostButton("← Back to Vault");
        backBtn.setOnAction(e -> showVault());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(title, spacer, backBtn);

        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 20, 30, 20));
        content.setAlignment(Pos.TOP_CENTER);
        content.setPrefWidth(600);
        content.setMaxWidth(600);
        content.setStyle("-fx-background-color: transparent;");

        // ── Security Info card
        VBox secInfoCard = settingsCard("🛡 How Your Vault is Protected");
        secInfoCard.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                             "; -fx-border-color: " + toHex(BORDER_COLOR) +
                             "; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 25;");

        for (String line : SecurityInfo.getSecurityExplanation().split("\n")) {
            Label lbl = new Label(line);
            lbl.setWrapText(true);
            lbl.setMaxWidth(550);
            if (line.endsWith(":")) {
                lbl.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
                lbl.setTextFill(ACCENT);
            } else if (line.isBlank()) {
                lbl.setMinHeight(4);
            } else {
                lbl.setFont(FONT_SMALL);
                lbl.setTextFill(TEXT_MUTED);
            }
            secInfoCard.getChildren().add(lbl);
        }

        // ── Change password card
        VBox changePwCard = settingsCard("🔒 Change Master Password");
        changePwCard.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                              "; -fx-border-color: " + toHex(BORDER_COLOR) +
                              "; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 25;");

        PasswordField oldPw  = styledPasswordField("Current master password");
        PasswordField newPw  = styledPasswordField("New master password");
        PasswordField conPw  = styledPasswordField("Confirm new password");
        Button changePwBtn   = primaryButton("UPDATE PASSWORD", ACCENT);
        Label changePwErr    = new Label(" ");
        changePwErr.setFont(FONT_SMALL);
        changePwErr.setTextFill(DANGER);

        changePwBtn.setOnAction(e -> {
            String oldVal = oldPw.getText().trim();
            String newVal = newPw.getText().trim();
            String conVal = conPw.getText().trim();
            if (newVal.length() < 6)    { changePwErr.setText("⚠ Min 6 characters required.");  return; }
            if (!newVal.equals(conVal)) { changePwErr.setText("⚠ New passwords do not match."); return; }
            try {
                SecretKey testKey = KeyDerivation.generateKey(oldVal, vaultMeta.getSalt());
                VaultRepo testSM = new VaultRepo(testKey);
                testSM.loadVault();

                aesKey = KeyDerivation.generateKey(newVal, vaultMeta.getSalt());
                storageManager.setKey(aesKey);
                storageManager.saveVault(vault);

                oldPw.clear(); newPw.clear(); conPw.clear();
                changePwErr.setText(" ");
                showToast("Master password updated!");
            } catch (Exception ex) {
                changePwErr.setText("Current password is incorrect.");
            }
        });

        changePwCard.getChildren().addAll(
                styledLabel("CURRENT PASSWORD"), oldPw,
                styledLabel("NEW PASSWORD"), newPw,
                styledLabel("CONFIRM NEW PASSWORD"), conPw,
                changePwBtn, changePwErr);

        // ── Danger zone card
        VBox dangerCard = settingsCard("Proceed with Caution");
        dangerCard.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                            "; -fx-border-color: " + toHex(BORDER_COLOR) +
                            "; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 25;");

        Button clearBtn  = primaryButton("CLEAR ALL ENTRIES", DANGER);
        Button logoutBtn = primaryButton("LOCK & LOG OUT", Color.web("#506070"));
        Label dangerNote = new Label("These actions cannot be undone!");
        dangerNote.setFont(FONT_SMALL);
        dangerNote.setTextFill(DANGER);

        clearBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Delete ALL saved passwords?\nThis cannot be undone!");
            styleAlert(alert);
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                vaultData.clear();
                vault.getEntries().clear();
                saveVaultSilently();
                refreshTable();
                showToast("⚠ All entries cleared.");
            }
        });
        logoutBtn.setOnAction(e -> {
            vaultData.clear();
            vault  = null;
            aesKey = null;
            showLogin();
        });

        dangerCard.getChildren().addAll(dangerNote, clearBtn, logoutBtn);

        // ── Clipboard card
        VBox clipCard = settingsCard("Auto-Clear Clipboard");
        clipCard.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                          "; -fx-border-color: " + toHex(BORDER_COLOR) +
                          "; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 25;");

        Label clipDesc = new Label("Automatically clear the clipboard after copying a password.");
        clipDesc.setFont(FONT_SMALL);
        clipDesc.setTextFill(TEXT_MUTED);
        clipDesc.setWrapText(true);

        ComboBox<String> clipCombo = new ComboBox<>();
        clipCombo.getItems().addAll("Never", "15 seconds", "30 seconds", "1 minute", "2 minutes");
        clipCombo.setValue(clipboardClearSec == 0   ? "Never"      :
                           clipboardClearSec == 15  ? "15 seconds" :
                           clipboardClearSec == 30  ? "30 seconds" :
                           clipboardClearSec == 60  ? "1 minute"   : "2 minutes");
        clipCombo.setStyle("-fx-background-color: " + toHex(BG_INPUT) +
                           "; -fx-text-fill: " + toHex(TEXT_PRIMARY) +
                           "; -fx-border-color: " + toHex(BORDER_COLOR) +
                           "; -fx-border-radius: 4;");
        clipCombo.setPrefWidth(180);

        clipCombo.valueProperty().addListener((obs, old, nw) -> {
            clipboardClearSec = switch (nw) {
                case "15 seconds" -> 15;
                case "30 seconds" -> 30;
                case "1 minute"   -> 60;
                case "2 minutes"  -> 120;
                default           -> 0;
            };
        });

        Label clipStatus = new Label(" ");
        clipStatus.setFont(FONT_SMALL);
        clipStatus.setTextFill(ACCENT2);

        clipCard.getChildren().addAll(clipDesc, clipCombo, clipStatus);
        content.getChildren().addAll(secInfoCard, changePwCard, clipCard, dangerCard);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; " +
                        "-fx-border-color: transparent; -fx-viewport-background-color: transparent;");

        HBox centerWrapper = new HBox(scroll);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setStyle("-fx-background-color: " + toHex(BG_DARK) +
                               "; -fx-border-color: " + toHex(BORDER_COLOR) +
                               "; -fx-border-width: 1 0 1 0;");

        root.setTop(topBar);
        root.setCenter(centerWrapper);
        mainPane.getChildren().setAll(root);
    }

    // ─────────────────────────────────────────────
    //  ADD / EDIT DIALOG
    // ─────────────────────────────────────────────
    private void showAddEditDialog(int editIndex) {
        StackPane overlay = new StackPane();
        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(mainPane.widthProperty());
        bg.heightProperty().bind(mainPane.heightProperty());
        bg.setFill(Color.web("rgba(0,0,0,0.55)"));
        overlay.getChildren().add(bg);

        VBox panel = new VBox(10);
        panel.setPadding(new Insets(24, 28, 28, 28));
        panel.setMaxWidth(460);
        panel.setMaxHeight(560);
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                       "; -fx-border-color: " + toHex(BORDER_COLOR) +
                       "; -fx-border-radius: 14; -fx-background-radius: 14;");

        Label dlgTitle = new Label(editIndex < 0 ? "Add New Entry" : "Edit Entry");
        dlgTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        dlgTitle.setTextFill(ACCENT);

        TextField siteField = styledTextField("e.g. Facebook, Google...");
        TextField userField = styledTextField("email@example.com");
        TextField catField  = styledTextField("e.g. Social, Work, Banking...");

        PasswordField pf = styledPasswordField("Enter password");
        TextField     tf = styledTextField("Enter password");
        tf.setManaged(false);
        tf.setVisible(false);
        tf.textProperty().bindBidirectional(pf.textProperty());

        Label breachWarn = new Label("⚠ This is a commonly known password. Choose a different one.");
        breachWarn.setFont(FONT_SMALL);
        breachWarn.setTextFill(WARNING);
        breachWarn.setWrapText(true);
        breachWarn.setMaxWidth(400);
        breachWarn.setVisible(false);
        breachWarn.setManaged(false);

        pf.textProperty().addListener((obs, old, nw) -> {
            boolean breached = !nw.isEmpty() && BreachChecker.isBreached(nw);
            breachWarn.setVisible(breached);
            breachWarn.setManaged(breached);
        });

        if (editIndex >= 0) {
            PasswordEntry entry = vaultData.get(editIndex);
            siteField.setText(entry.getWebsite());
            userField.setText(entry.getUsername());
            pf.setText(entry.getPassword());
            catField.setText(entry.getCategory());
        }

        StackPane passStack = new StackPane(pf, tf);

        CheckBox showPw = styledCheckbox("Show password");
        showPw.setOnAction(e -> {
            boolean show = showPw.isSelected();
            tf.setVisible(show);  tf.setManaged(show);
            pf.setVisible(!show); pf.setManaged(!show);
        });

        Button quickGen = ghostButton("⚡ Auto-generate");
        quickGen.setOnAction(e -> pf.setText(generatePassword(16, true, true, true)));

        Button saveBtn   = primaryButton(editIndex < 0 ? "SAVE ENTRY" : "UPDATE ENTRY", ACCENT);
        Button cancelBtn = ghostButton("CANCEL");
        Label errLbl     = new Label(" ");
        errLbl.setFont(FONT_SMALL);
        errLbl.setTextFill(DANGER);

        cancelBtn.setOnAction(e -> mainPane.getChildren().remove(overlay));

        saveBtn.setOnAction(e -> {
            String site = siteField.getText().trim();
            String user = userField.getText().trim();
            String pw   = pf.getText().trim();
            String cat  = catField.getText().trim();
            if (cat.isEmpty()) cat = "General";

            if (site.isEmpty() || user.isEmpty() || pw.isEmpty()) {
                errLbl.setText("⚠ Website, username and password are required.");
                return;
            }

            if (BreachChecker.isBreached(pw)) {
                errLbl.setText("⚠ Cannot save a commonly known password. Please choose a different one.");
                return;
            }

            try {
                byte[] hash = KeyDerivation.hashPassword(pw, vaultMeta.getSalt());

                if (vault.getPreviousHashes().stream().anyMatch(h -> Arrays.equals(h, hash))) {
                    errLbl.setText("⚠ Cannot reuse a previously used password.");
                    return;
                }

                PasswordEntry entry = new PasswordEntry(site, user, pw, cat);

                if (editIndex < 0) {
                    vaultData.add(entry);
                    vault.getEntries().add(entry);
                    vault.addHash(hash);
                } else {
                    vaultData.set(editIndex, entry);
                    vault.getEntries().set(editIndex, entry);
                }

                storageManager.saveVault(vault);
                refreshTable();
                showToast(editIndex < 0 ? "✓ Entry added!" : "✓ Entry updated!");
                mainPane.getChildren().remove(overlay);

            } catch (Exception ex) {
                errLbl.setText("⚠ Error saving entry: " + ex.getMessage());
            }
        });

        HBox footer = new HBox(10, cancelBtn, saveBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);

        panel.getChildren().addAll(
                dlgTitle,
                styledLabel("WEBSITE / APP NAME"),  siteField,
                styledLabel("USERNAME / EMAIL"),     userField,
                styledLabel("CATEGORY"),             catField,
                styledLabel("PASSWORD"),
                passStack,
                breachWarn,
                quickGen, showPw,
                errLbl, footer);

        overlay.getChildren().add(panel);
        mainPane.getChildren().add(overlay);
    }

    // ─────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────
    private void saveVaultSilently() {
        if (storageManager == null || vault == null) return;
        try {
            storageManager.saveVault(vault);
        } catch (Exception ex) {
            System.err.println("Auto-save failed: " + ex.getMessage());
        }
    }

    private static String maskPassword(String pw) {
        int count = Math.min(pw.length(), 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) sb.append("●");
        return sb.toString();
    }

    private static int passwordStrength(String pw) {
        if (pw == null || pw.isEmpty()) return 0;
        int score = 0;
        if (pw.length() >= 8)               score += 20;
        if (pw.length() >= 12)              score += 15;
        if (pw.length() >= 16)              score += 10;
        if (pw.matches(".*[A-Z].*"))        score += 15;
        if (pw.matches(".*[0-9].*"))        score += 15;
        if (pw.matches(".*[^A-Za-z0-9].*")) score += 25;
        return Math.min(score, 100);
    }

    private static String generatePassword(double len, boolean upper, boolean numbers, boolean symbols) {
        String lower  = "abcdefghijklmnopqrstuvwxyz";
        String up     = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String digits = "0123456789";
        String sym    = "!@#$%^&*-_=+?";
        StringBuilder pool = new StringBuilder(lower);
        if (upper)   pool.append(up);
        if (numbers) pool.append(digits);
        if (symbols) pool.append(sym);
        SecureRandom rng = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (int) len; i++)
            sb.append(pool.charAt(rng.nextInt(pool.length())));
        return sb.toString();
    }

    private void refreshTable() {
        table.refresh();
        if (countLabel != null) countLabel.setText(vaultData.size() + " entries");
    }

    private void copyToClipboard(String text, String toastMsg) {
        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(text), null);
        showToast(toastMsg);
        if (clipboardClearSec > 0) {
            Timeline clearTimer = new Timeline(new KeyFrame(
                    Duration.seconds(clipboardClearSec), ae -> {
                        java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                                .setContents(new java.awt.datatransfer.StringSelection(""), null);
                        showToast("🧹 Clipboard cleared after " + clipboardClearSec + "s.");
                    }));
            clearTimer.setCycleCount(1);
            clearTimer.play();
        }
    }

    private void styleAlert(Alert alert) {

        if (mainPane != null && mainPane.getScene() != null){
            alert.initOwner(mainPane.getScene().getWindow());
        }

        alert.initStyle(javafx.stage.StageStyle.UNDECORATED);

        DialogPane dp = alert.getDialogPane();
        String card  = toHex(BG_CARD);
        String input = toHex(BG_INPUT);
        String text  = toHex(TEXT_PRIMARY);
        String acc   = toHex(ACCENT);
        String dark  = toHex(BG_DARK);

        dp.setStyle(
            "-fx-background-color: " + card + ";" +
            "-fx-border-color: #ffffff;" +
            "-fx-border-width: 1.5;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: null;"
        );

        try {
            java.io.File tmp = java.io.File.createTempFile("aurix-dialog", ".css");
            tmp.deleteOnExit();
            String css =
                ".dialog-pane { -fx-background-color: " + card + "; -fx-border-color: #ffffff; -fx-border-width: 1.5; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 0; }\n" +
                ".dialog-pane > .header-panel { -fx-background-color: " + input + "; -fx-background-radius: 0; -fx-border-color: transparent; -fx-border-width: 0; }\n" +
                ".dialog-pane > .header-panel > .label { -fx-text-fill: " + text + "; -fx-font-family: Consolas; -fx-font-size: 14px; -fx-font-weight: bold; }\n" +
                ".dialog-pane .graphic-container { -fx-background-color: " + card + "; -fx-padding: 0; -fx-border-color: transparent; -fx-border-width: 0; }\n" +
                ".dialog-pane .content { -fx-background-color: " + card + "; -fx-border-color: transparent; -fx-border-width: 0; }\n" +
                ".dialog-pane .label { -fx-text-fill: " + text + "; -fx-font-family: Consolas; -fx-font-size: 13px; }\n" +
                ".dialog-pane .button-bar { -fx-background-color: " + card + "; -fx-border-color: transparent; -fx-border-width: 0; }\n" +
                ".dialog-pane .button { -fx-background-color: " + acc + "; -fx-text-fill: " + dark + "; -fx-font-family: Consolas; -fx-font-weight: bold; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: transparent; -fx-padding: 6 16 6 16; }\n" +
                ".dialog-pane .button:hover { -fx-opacity: 0.85; }\n" +
                ".dialog-pane:header .header-panel { -fx-background-color: " + input + "; -fx-border-color: transparent; }\n" +
                ".dialog-pane:header .graphic-container { -fx-background-color: " + card + "; }\n";
            java.nio.file.Files.writeString(tmp.toPath(), css);
            dp.getStylesheets().add(tmp.toURI().toString());
        } catch (Exception e) {
            dp.lookupAll(".label").forEach(n -> n.setStyle(
                "-fx-text-fill: " + text + "; -fx-font-family: Consolas;"));
            dp.lookupAll(".button").forEach(n -> n.setStyle(
                "-fx-background-color: " + acc + "; -fx-text-fill: " + dark + ";" +
                "-fx-font-family: Consolas; -fx-font-weight: bold;" +
                "-fx-background-radius: 6; -fx-border-radius: 6;"));
        }
    }

    private void showToast(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        styleAlert(alert);
        alert.show();
    }

    // ─────────────────────────────────────────────
    //  COMPONENT HELPERS
    // ─────────────────────────────────────────────
    private Button primaryButton(String text, Color bg) {
        Button btn = new Button(text);
        btn.setFont(FONT_LABEL);
        btn.setStyle("-fx-background-color: " + toHex(bg) +
                     "; -fx-text-fill: " + toHex(BG_DARK) +
                     "; -fx-background-radius: 8; -fx-border-radius: 8;");
        btn.setPrefHeight(38);

        ScaleTransition scaleIn  = new ScaleTransition(Duration.millis(220), btn);
        scaleIn.setToX(1.08); scaleIn.setToY(1.08);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(220), btn);
        scaleOut.setToX(1.0); scaleOut.setToY(1.0);

        DropShadow glow = new DropShadow(12, bg.brighter());
        glow.setSpread(0.25);

        btn.setOnMouseEntered(e -> {
            new ParallelTransition(scaleIn).playFromStart();
            btn.setEffect(glow);
            btn.setStyle("-fx-background-color: " + toHex(bg.brighter()) +
                         "; -fx-text-fill: " + toHex(BG_DARK) +
                         "; -fx-background-radius: 8; -fx-border-radius: 8;");
        });
        btn.setOnMouseExited(e -> {
            new ParallelTransition(scaleOut).playFromStart();
            btn.setEffect(null);
            btn.setStyle("-fx-background-color: " + toHex(bg) +
                         "; -fx-text-fill: " + toHex(BG_DARK) +
                         "; -fx-background-radius: 8; -fx-border-radius: 8;");
        });

        return btn;
    }

    private Button ghostButton(String text) {
        Button btn = new Button(text);
        btn.setFont(FONT_BODY);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " +
                     toHex(TEXT_MUTED) + "; -fx-border-color: transparent;");

        ScaleTransition scaleIn  = new ScaleTransition(Duration.millis(180), btn);
        scaleIn.setToX(1.05); scaleIn.setToY(1.05);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), btn);
        scaleOut.setToX(1.0); scaleOut.setToY(1.0);
        FadeTransition fadeIn    = new FadeTransition(Duration.millis(180), btn);
        fadeIn.setToValue(0.72);
        FadeTransition fadeOut   = new FadeTransition(Duration.millis(180), btn);
        fadeOut.setToValue(1.0);

        btn.setOnMouseEntered(e -> {
            new ParallelTransition(scaleIn, fadeIn).playFromStart();
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + toHex(ACCENT) +
                         "; -fx-border-color: transparent; -fx-underline: true;");
        });
        btn.setOnMouseExited(e -> {
            new ParallelTransition(scaleOut, fadeOut).playFromStart();
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + toHex(TEXT_MUTED) +
                         "; -fx-border-color: transparent; -fx-underline: false;");
        });

        return btn;
    }

    private Button sideNavButton(String text) {
        Button btn = new Button(text);
        btn.setFont(FONT_BODY);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + toHex(TEXT_MUTED) +
                     "; -fx-border-color: transparent; -fx-alignment: center-left;");
        btn.setPrefWidth(168);
        btn.setPrefHeight(36);

        ScaleTransition scaleIn  = new ScaleTransition(Duration.millis(200), btn);
        scaleIn.setToX(1.03); scaleIn.setToY(1.03);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), btn);
        scaleOut.setToX(1.0); scaleOut.setToY(1.0);
        FadeTransition fadeIn    = new FadeTransition(Duration.millis(200), btn);
        fadeIn.setToValue(0.8);
        FadeTransition fadeOut   = new FadeTransition(Duration.millis(200), btn);
        fadeOut.setToValue(1.0);

        btn.setOnMouseEntered(e -> {
            new ParallelTransition(scaleIn, fadeIn).playFromStart();
            btn.setStyle("-fx-background-color: " + toHex(BG_INPUT) +
                         "; -fx-text-fill: " + toHex(ACCENT) +
                         "; -fx-border-color: transparent; -fx-alignment: center-left;");
        });
        btn.setOnMouseExited(e -> {
            new ParallelTransition(scaleOut, fadeOut).playFromStart();
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + toHex(TEXT_MUTED) +
                         "; -fx-border-color: transparent; -fx-alignment: center-left;");
        });

        return btn;
    }

    private Label styledLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(FONT_LABEL);
        lbl.setTextFill(TEXT_MUTED);
        return lbl;
    }

    private TextField styledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setFont(FONT_BODY);
        tf.setStyle("-fx-background-color: " + toHex(BG_INPUT) +
                    "; -fx-text-fill: " + toHex(TEXT_PRIMARY) +
                    "; -fx-border-color: " + toHex(BORDER_COLOR) +
                    "; -fx-border-radius: 4;");
        tf.setPrefHeight(38);
        return tf;
    }

    private PasswordField styledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setFont(FONT_BODY);
        pf.setStyle("-fx-background-color: " + toHex(BG_INPUT) +
                    "; -fx-text-fill: " + toHex(TEXT_PRIMARY) +
                    "; -fx-border-color: " + toHex(BORDER_COLOR) +
                    "; -fx-border-radius: 4;");
        pf.setPrefHeight(38);
        return pf;
    }

    private CheckBox styledCheckbox(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setFont(FONT_SMALL);
        cb.setTextFill(TEXT_MUTED);
        cb.setStyle("-fx-background-color: transparent;");
        return cb;
    }

    private VBox settingsCard(String headerText) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: " + toHex(BG_CARD) +
                      "; -fx-border-color: " + toHex(BORDER_COLOR) +
                      "; -fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 14 20 20 20;");
        Label header = new Label(headerText);
        header.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        header.setTextFill(TEXT_PRIMARY);
        card.getChildren().add(header);
        return card;
    }

    private Button miniBtn(String text, Color c) {
        Button btn = new Button(text);
        btn.setFont(FONT_SMALL);
        btn.setStyle("-fx-background-color: " + toHex(c) +
                     "; -fx-text-fill: " + toHex(BG_DARK) +
                     "; -fx-border-color: " + toHex(c) +
                     "; -fx-border-radius: 4; -fx-background-radius: 4;" +
                     "; -fx-font-weight: bold;");
        btn.setPrefSize(80, 26);
        return btn;
    }

    private String toHex(Color c) {
        return String.format("#%02x%02x%02x",
                (int)(c.getRed()   * 255),
                (int)(c.getGreen() * 255),
                (int)(c.getBlue()  * 255));
    }
}