package org.example.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.example.common.model.Article;
import org.example.common.model.Client;
import org.example.common.model.Facture;
import org.example.common.model.Famille;
import org.example.common.model.LigneFacture;
import org.example.common.rmi.MagasinService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClientController {

    // --- Champs de formulaire ---
    @FXML private ComboBox<Article>  referenceCombo;
    @FXML private ComboBox<Article>  referenceVenteCombo;
    @FXML private ComboBox<Famille>  familleCombo;
    @FXML private TextField nomClientField;
    @FXML private TextField prenomClientField;
    @FXML private TextField searchNomField;
    @FXML private TextField searchPrenomField;
    @FXML private TextField quantiteField;
    @FXML private ComboBox<String> modePaiementCombo;
    @FXML private TextField factureIdField;
    @FXML private TextField stockQuantiteField;
    @FXML private DatePicker datePicker;
    @FXML private Label totalPanierLabel;

    // --- Zone de résultats ---
    @FXML private TextArea resultArea;

    // --- Table Articles ---
    @FXML private TableView<Article> articleTable;
    @FXML private TableColumn<Article, String>     refColumn;
    @FXML private TableColumn<Article, String>     nomColumn;
    @FXML private TableColumn<Article, String>     familleColumn;
    @FXML private TableColumn<Article, BigDecimal> prixColumn;
    @FXML private TableColumn<Article, Integer>    stockColumn;

    // --- Table Panier ---
    @FXML private TableView<LigneFacture> panierTable;
    @FXML private TableColumn<LigneFacture, String>     panierRefColumn;
    @FXML private TableColumn<LigneFacture, String>     panierNomColumn;
    @FXML private TableColumn<LigneFacture, Integer>    panierQteColumn;
    @FXML private TableColumn<LigneFacture, BigDecimal> panierPrixColumn;
    @FXML private TableColumn<LigneFacture, BigDecimal> panierSousTotalColumn;

    // --- Table Factures ---
    @FXML private TableView<Facture> factureTable;
    @FXML private TableColumn<Facture, Long>       factureIdColumn;
    @FXML private TableColumn<Facture, String>     clientColumn;
    @FXML private TableColumn<Facture, BigDecimal> totalColumn;
    @FXML private TableColumn<Facture, LocalDate>  dateColumn;
    @FXML private TableColumn<Facture, String>     modePaiementColumn;
    @FXML private TableColumn<Facture, Boolean>    payeeColumn;

    // --- Panneaux de navigation ---
    @FXML private VBox articlesPane;
    @FXML private VBox ventePane;
    @FXML private VBox facturationPane;
    @FXML private VBox statistiquesPane;

    @FXML private Button btnArticles;
    @FXML private Button btnVente;
    @FXML private Button btnFacturation;
    @FXML private Button btnStatistiques;

    private MagasinService magasinService;
    private final ObservableList<LigneFacture> panier = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            magasinService = (MagasinService) Naming.lookup("rmi://localhost:1099/MagasinService");

            modePaiementCombo.setItems(FXCollections.observableArrayList("Espèces", "Carte bancaire", "Chèque"));
            modePaiementCombo.setValue("Carte bancaire");

            setupTableColumns();

            List<Article> articles = magasinService.getTousLesArticles();
            articleTable.setItems(FXCollections.observableArrayList(articles));

            StringConverter<Article> articleConverter = new StringConverter<>() {
                public String toString(Article a) {
                    return a == null ? "" : a.getReference() + " - " + a.getNom();
                }
                public Article fromString(String s) { return null; }
            };

            List<Article> articlesTries = articles.stream()
                    .sorted((a, b) -> a.getReference().compareTo(b.getReference()))
                    .toList();

            referenceCombo.setItems(FXCollections.observableArrayList(articlesTries));
            referenceCombo.setConverter(articleConverter);

            referenceVenteCombo.setItems(FXCollections.observableArrayList(articlesTries));
            referenceVenteCombo.setConverter(articleConverter);

            List<Famille> familles = magasinService.getToutesLesFamilles();
            StringConverter<Famille> familleConverter = new StringConverter<>() {
                public String toString(Famille f) { return f == null ? "" : f.getNom(); }
                public Famille fromString(String s) { return null; }
            };
            familleCombo.setItems(FXCollections.observableArrayList(familles));
            familleCombo.setConverter(familleConverter);

            resultArea.setText("Connexion au serveur magasin réussie!");

        } catch (Exception e) {
            resultArea.setText("Erreur de connexion au serveur: " + e.getMessage());
        }
    }

    // =========================================================
    //  Navigation sidebar
    // =========================================================

    @FXML private void showArticles() {
        navigateTo(articlesPane, btnArticles);
        voirTousLesArticles();
    }

    @FXML private void showVente()        { navigateTo(ventePane,        btnVente); }
    @FXML private void showStatistiques() { navigateTo(statistiquesPane, btnStatistiques); }

    @FXML private void showFacturation() {
        navigateTo(facturationPane, btnFacturation);
        chargerToutesLesFactures();
    }

    private void navigateTo(VBox targetPane, Button activeBtn) {
        VBox[]   panes   = { articlesPane, ventePane, facturationPane, statistiquesPane };
        Button[] buttons = { btnArticles,  btnVente,  btnFacturation,  btnStatistiques };

        for (VBox p : panes)     { p.setVisible(false); p.setManaged(false); }
        for (Button b : buttons)   b.getStyleClass().remove("nav-button-active");

        targetPane.setVisible(true);
        targetPane.setManaged(true);
        if (!activeBtn.getStyleClass().contains("nav-button-active"))
            activeBtn.getStyleClass().add("nav-button-active");
    }

    // =========================================================
    //  Colonnes de tables
    // =========================================================

    private void setupTableColumns() {
        refColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        familleColumn.setCellValueFactory(new PropertyValueFactory<>("famille"));
        prixColumn.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("quantiteEnStock"));

        panierRefColumn.setCellValueFactory(new PropertyValueFactory<>("referenceArticle"));
        panierNomColumn.setCellValueFactory(new PropertyValueFactory<>("nomArticle"));
        panierQteColumn.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        panierPrixColumn.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        panierSousTotalColumn.setCellValueFactory(new PropertyValueFactory<>("sousTotal"));
        panierTable.setItems(panier);
        panierTable.setPlaceholder(new Label("Panier vide"));

        factureIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        clientColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getNomComplet()));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalFacture"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFacturation"));
        modePaiementColumn.setCellValueFactory(new PropertyValueFactory<>("modePaiement"));
        payeeColumn.setCellValueFactory(new PropertyValueFactory<>("payee"));
        payeeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                setText(empty || val == null ? null : val ? "Oui" : "Non");
            }
        });
        factureTable.setPlaceholder(new Label("Aucune facture disponible"));

        // Clic sur une facture → afficher son détail dans la console
        factureTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, ancien, selectionne) -> {
                if (selectionne != null) {
                    afficherTicketCaisse(selectionne);
                    javafx.application.Platform.runLater(() ->
                            factureTable.getSelectionModel().clearSelection());
                }
            }
        );
    }

    private boolean checkConnection() {
        if (magasinService == null) {
            resultArea.setText("Non connecté au serveur magasin.");
            return false;
        }
        return true;
    }

    @FXML
    private void chargerToutesLesFactures() {
        if (!checkConnection()) return;
        try {
            List<Facture> factures = magasinService.getToutesLesFactures();
            factureTable.setItems(FXCollections.observableArrayList(factures));
        } catch (Exception e) {
            resultArea.setText("Erreur lors du chargement des factures: " + e.getMessage());
        }
    }

    private void afficherDetailFacture(Facture facture) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== FACTURE N° ").append(facture.getId()).append(" ===\n");
        sb.append("Client : ").append(facture.getNomComplet()).append("\n");
        sb.append("Date : ").append(facture.getDateFacturation()).append("\n");
        sb.append("Mode de paiement : ").append(facture.getModePaiement()).append("\n");
        sb.append("Payée : ").append(facture.isPayee() ? "Oui" : "Non").append("\n");
        sb.append("---\n");
        facture.getLignes().forEach(ligne ->
            sb.append("• ").append(ligne.getNomArticle())
              .append(" x").append(ligne.getQuantite())
              .append(" à ").append(ligne.getPrixUnitaire()).append("€")
              .append(" = ").append(ligne.getSousTotal()).append("€\n")
        );
        sb.append("---\nTOTAL : ").append(facture.getTotalFacture()).append("€");
        resultArea.setText(sb.toString());
    }

    // =========================================================
    //  Actions — Articles
    // =========================================================

    @FXML
    private void voirTousLesArticles() {
        if (!checkConnection()) return;
        try {
            List<Article> articles = magasinService.getTousLesArticles();
            articleTable.setItems(FXCollections.observableArrayList(articles));
            referenceCombo.setValue(null);
            familleCombo.setValue(null);
        } catch (Exception e) {
            resultArea.setText("Erreur lors du chargement des articles: " + e.getMessage());
        }
    }

    @FXML
    private void consulterArticle() {
        if (!checkConnection()) return;
        try {
            Article selected = referenceCombo.getValue();
            if (selected == null) {
                resultArea.setText("Veuillez sélectionner un article");
                return;
            }

            Article article = magasinService.consulterStockArticle(selected.getReference());
            if (article != null) {
                articleTable.setItems(FXCollections.observableArrayList(article));
                resultArea.setText("Article : " + article.getReference() + " - " + article.getNom()
                        + "\nStock : " + article.getQuantiteEnStock());
            } else {
                articleTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucun article trouvé : " + selected.getReference());
            }

        } catch (Exception e) {
            resultArea.setText("Erreur lors de la consultation: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherParFamille() {
        if (!checkConnection()) return;
        try {
            Famille selected = familleCombo.getValue();
            if (selected == null) {
                resultArea.setText("Veuillez sélectionner une famille");
                return;
            }

            List<String> references = magasinService.rechercherArticleParFamille(selected.getNom());
            if (!references.isEmpty()) {
                ObservableList<Article> articles = FXCollections.observableArrayList();
                StringBuilder sb = new StringBuilder("Famille '" + selected.getNom() + "' :\n");
                for (String ref : references) {
                    Article article = magasinService.consulterStockArticle(ref);
                    if (article != null) {
                        articles.add(article);
                        sb.append("• ").append(ref).append(" - ").append(article.getNom()).append("\n");
                    }
                }
                articleTable.setItems(articles);
                resultArea.setText(sb.toString());
            } else {
                articleTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucun article en stock dans la famille : " + selected.getNom());
            }

        } catch (Exception e) {
            resultArea.setText("Erreur lors de la recherche: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterStock() {
        try {
            Article selected    = referenceCombo.getValue();
            String  quantiteStr = stockQuantiteField.getText().trim();

            if (selected == null || quantiteStr.isEmpty()) {
                resultArea.setText("Veuillez sélectionner un article et saisir la quantité");
                return;
            }

            int quantite = Integer.parseInt(quantiteStr);
            boolean success = magasinService.ajouterStock(selected.getReference(), quantite);

            if (success) {
                resultArea.setText("Stock ajouté : " + selected.getNom() + " +  " + quantite);
                consulterArticle();
            } else {
                resultArea.setText("Échec de l'ajout de stock.");
            }

        } catch (NumberFormatException e) {
            resultArea.setText("Quantité invalide.");
        } catch (Exception e) {
            resultArea.setText("Erreur lors de l'ajout de stock: " + e.getMessage());
        }
    }

    // =========================================================
    //  Actions — Vente (panier)
    // =========================================================

    @FXML
    private void ajouterAuPanier() {
        Article selected    = referenceVenteCombo.getValue();
        String  quantiteStr = quantiteField.getText().trim();

        if (selected == null) {
            resultArea.setText("Veuillez sélectionner un article");
            return;
        }
        if (quantiteStr.isEmpty()) {
            resultArea.setText("Veuillez saisir une quantité");
            return;
        }

        try {
            int quantite = Integer.parseInt(quantiteStr);
            if (quantite <= 0) { resultArea.setText("La quantité doit être positive."); return; }

            // Si l'article est déjà dans le panier, on cumule la quantité
            for (LigneFacture ligne : panier) {
                if (ligne.getReferenceArticle().equals(selected.getReference())) {
                    ligne.setQuantite(ligne.getQuantite() + quantite);
                    panierTable.refresh();
                    mettreAJourTotalPanier();
                    quantiteField.clear();
                    return;
                }
            }

            panier.add(new LigneFacture(selected.getReference(), selected.getNom(), quantite, selected.getPrixUnitaire()));
            mettreAJourTotalPanier();
            referenceVenteCombo.setValue(null);
            quantiteField.clear();

        } catch (NumberFormatException e) {
            resultArea.setText("Quantité invalide.");
        }
    }

    @FXML
    private void viderPanier() {
        panier.clear();
        mettreAJourTotalPanier();
    }

    @FXML
    private void passerEnCaisse() {
        if (!checkConnection()) return;
        if (panier.isEmpty()) {
            resultArea.setText("Le panier est vide.");
            return;
        }

        String nom      = nomClientField.getText().trim();
        String prenom   = prenomClientField.getText().trim();
        String modePaiement = modePaiementCombo.getValue();

        if (nom.isEmpty() || prenom.isEmpty()) {
            resultArea.setText("Veuillez saisir le nom et le prénom du client.");
            return;
        }
        try {
            Map<String, Integer> panierMap = new LinkedHashMap<>();
            for (LigneFacture ligne : panier) {
                panierMap.put(ligne.getReferenceArticle(), ligne.getQuantite());
            }

            Facture facture = magasinService.passerEnCaisse(nom, prenom, panierMap, modePaiement);

            if (facture != null) {
                resultArea.setText("Vente enregistrée — Facture N° " + facture.getId());
                afficherTicketCaisse(facture);
                viderPanier();
                nomClientField.clear();
                prenomClientField.clear();
            } else {
                resultArea.setText("Échec : stock insuffisant pour un ou plusieurs articles.");
            }

        } catch (Exception e) {
            resultArea.setText("Erreur lors du passage en caisse: " + e.getMessage());
        }
    }

    private void afficherTicketCaisse(Facture facture) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Ticket de caisse");
        modal.setResizable(false);

        VBox root = new VBox(6);
        root.setPadding(new Insets(24, 32, 24, 32));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #fffdf4;");
        root.setPrefWidth(360);

        String sep  = "- - - - - - - - - - - - - - - - - -";
        String sep2 = "= = = = = = = = = = = = = = = = = =";
        Font mono      = Font.font("Courier New", 13);
        Font monoBold  = Font.font("Courier New", FontWeight.BOLD, 14);
        Font monoTitle = Font.font("Courier New", FontWeight.BOLD, 16);

        root.getChildren().addAll(
            ticket(sep2, mono),
            ticketCentre("BRICO-MERLIN", monoTitle),
            ticketCentre("Poste vendeur", mono),
            ticket(sep2, mono)
        );

        String horodatage = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm"));
        root.getChildren().addAll(
            ticket("", mono),
            ticket("Facture N° " + facture.getId(), monoBold),
            ticket("Date    : " + horodatage, mono),
            ticket("Client  : " + facture.getNomComplet(), mono),
            ticket(sep, mono)
        );

        for (LigneFacture ligne : facture.getLignes()) {
            String nom = ligne.getNomArticle().length() > 18
                    ? ligne.getNomArticle().substring(0, 18)
                    : ligne.getNomArticle();
            String ligneStr = String.format("%-18s x%-2d  %6.2f EUR",
                    nom, ligne.getQuantite(), ligne.getSousTotal());
            root.getChildren().add(ticket(ligneStr, mono));
        }

        root.getChildren().addAll(
            ticket(sep, mono),
            ticket(String.format("%-22s %6.2f EUR", "TOTAL :", facture.getTotalFacture()), monoBold),
            ticket(sep, mono),
            ticket("Mode : " + facture.getModePaiement(), mono),
            ticket("Statut : PAYÉE ✓", mono),
            ticket("", mono),
            ticket(sep2, mono),
            ticketCentre("Merci de votre visite !", mono),
            ticketCentre("À bientôt chez BRICO-MERLIN", mono),
            ticket(sep2, mono)
        );

        Button btnFermer = new Button("Fermer");
        btnFermer.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 24;");
        btnFermer.setOnAction(e -> modal.close());
        VBox.setMargin(btnFermer, new Insets(12, 0, 0, 0));
        root.getChildren().add(btnFermer);

        modal.setScene(new Scene(root));
        modal.showAndWait();
    }

    private Label ticket(String texte, Font font) {
        Label lbl = new Label(texte);
        lbl.setFont(font);
        lbl.setStyle("-fx-text-fill: #2c2c2c;");
        return lbl;
    }

    private Label ticketCentre(String texte, Font font) {
        Label lbl = ticket(texte, font);
        lbl.setTextAlignment(TextAlignment.CENTER);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);
        return lbl;
    }

    private void mettreAJourTotalPanier() {
        BigDecimal total = panier.stream()
                .map(LigneFacture::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalPanierLabel.setText(String.format("Total : %.2f €", total));
    }

    // =========================================================
    //  Actions — Facturation
    // =========================================================

    @FXML
    private void consulterFacture() {
        if (!checkConnection()) return;
        try {
            String factureIdStr = factureIdField.getText().trim();
            if (factureIdStr.isEmpty()) {
                resultArea.setText("Veuillez saisir un numéro de facture");
                return;
            }

            Long factureId = Long.parseLong(factureIdStr);
            Facture facture = magasinService.consulterFacture(factureId);

            if (facture != null) {
                factureTable.setItems(FXCollections.observableArrayList(facture));
                afficherDetailFacture(facture);
            } else {
                factureTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucune facture trouvée avec le numéro : " + factureId);
            }

        } catch (NumberFormatException e) {
            resultArea.setText("Numéro de facture invalide.");
        } catch (Exception e) {
            resultArea.setText("Erreur lors de la consultation : " + e.getMessage());
        }
    }

    @FXML
    private void voirFacturesClient() {
        try {
            String nom    = searchNomField.getText().trim();
            String prenom = searchPrenomField.getText().trim();
            if (nom.isEmpty() || prenom.isEmpty()) {
                resultArea.setText("Veuillez saisir le nom et le prénom du client.");
                return;
            }

            List<Facture> factures = magasinService.getFacturesClient(nom, prenom);
            if (!factures.isEmpty()) {
                factureTable.setItems(FXCollections.observableArrayList(factures));

                BigDecimal totalDu = factures.stream()
                    .filter(f -> !f.isPayee())
                    .map(Facture::getTotalFacture)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                resultArea.setText("Client " + nom + " " + prenom + " — " + factures.size()
                        + " facture(s) — Total dû : " + totalDu + "€");
            } else {
                factureTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucune facture pour le client : " + nom + " " + prenom);
            }

        } catch (Exception e) {
            resultArea.setText("Erreur : " + e.getMessage());
        }
    }

    // =========================================================
    //  Actions — Statistiques
    // =========================================================

    @FXML
    private void calculerChiffreAffaires() {
        try {
            LocalDate date = datePicker.getValue();
            if (date == null) {
                resultArea.setText("Veuillez sélectionner une date");
                return;
            }

            BigDecimal chiffre = magasinService.calculerChiffreAffaires(date);
            resultArea.setText("Chiffre d'affaires du " + date + " : " + chiffre + "€");

        } catch (Exception e) {
            resultArea.setText("Erreur : " + e.getMessage());
        }
    }
}
