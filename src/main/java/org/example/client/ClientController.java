package org.example.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.rmi.MagasinService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.time.LocalDate;
import java.util.List;

public class ClientController {

    // --- Champs de formulaire ---
    @FXML private ComboBox<String> referenceCombo;
    @FXML private ComboBox<String> referenceVenteCombo;
    @FXML private ComboBox<String> familleCombo;
    @FXML private TextField clientIdVenteField;
    @FXML private TextField clientIdField;
    @FXML private TextField quantiteField;
    @FXML private ComboBox<String> modePaiementCombo;
    @FXML private ComboBox<String> modePaiementFactureCombo;
    @FXML private TextField factureIdField;
    @FXML private TextField stockQuantiteField;
    @FXML private DatePicker datePicker;

    // --- Zone de résultats ---
    @FXML private TextArea resultArea;

    // --- Tables ---
    @FXML private TableView<Article> articleTable;
    @FXML private TableColumn<Article, String> refColumn;
    @FXML private TableColumn<Article, String> familleColumn;
    @FXML private TableColumn<Article, BigDecimal> prixColumn;
    @FXML private TableColumn<Article, Integer> stockColumn;

    @FXML private TableView<Facture> factureTable;
    @FXML private TableColumn<Facture, Long> factureIdColumn;
    @FXML private TableColumn<Facture, String> clientColumn;
    @FXML private TableColumn<Facture, BigDecimal> totalColumn;
    @FXML private TableColumn<Facture, LocalDate> dateColumn;
    @FXML private TableColumn<Facture, Boolean> payeeColumn;

    // --- Panneaux de navigation (sidebar) ---
    @FXML private VBox articlesPane;
    @FXML private VBox ventePane;
    @FXML private VBox facturationPane;
    @FXML private VBox statistiquesPane;

    @FXML private Button btnArticles;
    @FXML private Button btnVente;
    @FXML private Button btnFacturation;
    @FXML private Button btnStatistiques;

    private MagasinService magasinService;

    @FXML
    public void initialize() {
        try {
            magasinService = (MagasinService) Naming.lookup("rmi://localhost:1099/MagasinService");

            modePaiementCombo.setItems(FXCollections.observableArrayList("Espèces", "Carte bancaire", "Chèque"));
            modePaiementCombo.setValue("Carte bancaire");

            modePaiementFactureCombo.setItems(FXCollections.observableArrayList("Espèces", "Carte bancaire", "Chèque"));
            modePaiementFactureCombo.setValue("Carte bancaire");

            setupTableColumns();

            List<Article> articles = magasinService.getTousLesArticles();
            articleTable.setItems(FXCollections.observableArrayList(articles));

            List<String> references = articles.stream()
                    .map(Article::getReference)
                    .sorted()
                    .toList();
            referenceCombo.setItems(FXCollections.observableArrayList(references));
            referenceVenteCombo.setItems(FXCollections.observableArrayList(references));

            List<String> familles = articles.stream()
                    .map(Article::getFamille)
                    .distinct()
                    .sorted()
                    .toList();
            familleCombo.setItems(FXCollections.observableArrayList(familles));

            resultArea.setText("Connexion au serveur magasin réussie!");

        } catch (Exception e) {
            resultArea.setText("Erreur de connexion au serveur: " + e.getMessage());
        }
    }

    // =========================================================
    //  Navigation sidebar
    // =========================================================

    @FXML private void showArticles()     { navigateTo(articlesPane,     btnArticles); }
    @FXML private void showVente()        { navigateTo(ventePane,         btnVente); }
    @FXML private void showFacturation()  { navigateTo(facturationPane,   btnFacturation); }
    @FXML private void showStatistiques() { navigateTo(statistiquesPane,  btnStatistiques); }

    private void navigateTo(VBox targetPane, Button activeBtn) {
        VBox[]    panes   = { articlesPane, ventePane, facturationPane, statistiquesPane };
        Button[]  buttons = { btnArticles,  btnVente,  btnFacturation,  btnStatistiques };

        for (VBox p : panes) {
            p.setVisible(false);
            p.setManaged(false);
        }
        for (Button b : buttons) {
            b.getStyleClass().remove("nav-button-active");
        }

        targetPane.setVisible(true);
        targetPane.setManaged(true);
        if (!activeBtn.getStyleClass().contains("nav-button-active")) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }

    // =========================================================
    //  Colonnes de tables
    // =========================================================

    private void setupTableColumns() {
        refColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
        familleColumn.setCellValueFactory(new PropertyValueFactory<>("famille"));
        prixColumn.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("quantiteEnStock"));

        factureIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        clientColumn.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalFacture"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFacturation"));
        payeeColumn.setCellValueFactory(new PropertyValueFactory<>("payee"));
    }

    private boolean checkConnection() {
        if (magasinService == null) {
            resultArea.setText("Non connecté au serveur magasin. Vérifiez que le serveur RMI est démarré sur localhost:1099.");
            return false;
        }
        return true;
    }

    // =========================================================
    //  Actions — Articles
    // =========================================================

    @FXML
    private void consulterArticle() {
        if (!checkConnection()) return;
        try {
            String reference = referenceCombo.getValue();
            if (reference == null || reference.isEmpty()) {
                resultArea.setText("Veuillez sélectionner une référence d'article");
                return;
            }

            Article article = magasinService.consulterStockArticle(reference);
            if (article != null) {
                articleTable.setItems(FXCollections.observableArrayList(article));
                resultArea.setText("Article trouvé: " + article.toString());
            } else {
                articleTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucun article trouvé avec la référence: " + reference);
            }

        } catch (Exception e) {
            resultArea.setText("Erreur lors de la consultation: " + e.getMessage());
        }
    }

    @FXML
    private void rechercherParFamille() {
        if (!checkConnection()) return;
        try {
            String famille = familleCombo.getValue();
            if (famille == null || famille.isEmpty()) {
                resultArea.setText("Veuillez sélectionner une famille d'articles");
                return;
            }

            List<String> references = magasinService.rechercherArticleParFamille(famille);
            if (!references.isEmpty()) {
                StringBuilder sb = new StringBuilder("Articles trouvés dans la famille '" + famille + "':\n");
                ObservableList<Article> articles = FXCollections.observableArrayList();

                for (String ref : references) {
                    Article article = magasinService.consulterStockArticle(ref);
                    if (article != null) {
                        articles.add(article);
                        sb.append("- ").append(ref).append("\n");
                    }
                }

                articleTable.setItems(articles);
                resultArea.setText(sb.toString());

            } else {
                articleTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucun article trouvé dans la famille: " + famille);
            }

        } catch (Exception e) {
            resultArea.setText("Erreur lors de la recherche: " + e.getMessage());
        }
    }

    @FXML
    private void ajouterStock() {
        try {
            String reference = referenceCombo.getValue();
            String quantiteStr = stockQuantiteField.getText().trim();

            if (reference == null || reference.isEmpty() || quantiteStr.isEmpty()) {
                resultArea.setText("Veuillez sélectionner la référence et saisir la quantité");
                return;
            }

            int quantite = Integer.parseInt(quantiteStr);
            boolean success = magasinService.ajouterStock(reference, quantite);

            if (success) {
                resultArea.setText("Stock ajouté avec succès!\nArticle: " + reference +
                                 "\nQuantité ajoutée: " + quantite);
                consulterArticle();
            } else {
                resultArea.setText("Échec de l'ajout de stock. Article inexistant.");
            }

        } catch (NumberFormatException e) {
            resultArea.setText("Quantité invalide.");
        } catch (Exception e) {
            resultArea.setText("Erreur lors de l'ajout de stock: " + e.getMessage());
        }
    }

    // =========================================================
    //  Actions — Vente
    // =========================================================

    @FXML
    private void acheterArticle() {
        if (!checkConnection()) return;
        try {
            String clientId = clientIdVenteField.getText().trim();
            String reference = referenceVenteCombo.getValue();
            String quantiteStr = quantiteField.getText().trim();
            String modePaiement = modePaiementCombo.getValue();

            if (clientId.isEmpty() || reference == null || reference.isEmpty() || quantiteStr.isEmpty()) {
                resultArea.setText("Veuillez remplir tous les champs obligatoires");
                return;
            }

            int quantite = Integer.parseInt(quantiteStr);
            boolean success = magasinService.acheterArticle(clientId, reference, quantite, modePaiement);

            if (success) {
                resultArea.setText("Achat réussi!\nClient: " + clientId +
                                 "\nArticle: " + reference +
                                 "\nQuantité: " + quantite +
                                 "\nMode de paiement: " + modePaiement);
            } else {
                resultArea.setText("Échec de l'achat. Stock insuffisant ou article inexistant.");
            }

        } catch (NumberFormatException e) {
            resultArea.setText("Quantité invalide. Veuillez saisir un nombre entier.");
        } catch (Exception e) {
            resultArea.setText("Erreur lors de l'achat: " + e.getMessage());
        }
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
                resultArea.setText("Veuillez saisir un ID de facture");
                return;
            }

            Long factureId = Long.parseLong(factureIdStr);
            Facture facture = magasinService.consulterFacture(factureId);

            if (facture != null) {
                factureTable.setItems(FXCollections.observableArrayList(facture));

                StringBuilder sb = new StringBuilder();
                sb.append("=== FACTURE N° ").append(facture.getId()).append(" ===\n");
                sb.append("Client: ").append(facture.getClientId()).append("\n");
                sb.append("Date: ").append(facture.getDateFacturation()).append("\n");
                sb.append("Mode de paiement: ").append(facture.getModePaiement()).append("\n");
                sb.append("Payée: ").append(facture.isPayee() ? "Oui" : "Non").append("\n\n");
                sb.append("Détail des articles:\n");

                facture.getLignes().forEach(ligne -> {
                    sb.append("- ").append(ligne.getNomArticle())
                      .append(" (").append(ligne.getReferenceArticle()).append(")")
                      .append(" x").append(ligne.getQuantite())
                      .append(" à ").append(ligne.getPrixUnitaire()).append("€")
                      .append(" = ").append(ligne.getSousTotal()).append("€\n");
                });

                sb.append("\nTOTAL: ").append(facture.getTotalFacture()).append("€");
                resultArea.setText(sb.toString());

            } else {
                factureTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucune facture trouvée avec l'ID: " + factureId);
            }

        } catch (NumberFormatException e) {
            resultArea.setText("ID de facture invalide. Veuillez saisir un nombre entier.");
        } catch (Exception e) {
            resultArea.setText("Erreur lors de la consultation de facture: " + e.getMessage());
        }
    }

    @FXML
    private void payerFacture() {
        if (!checkConnection()) return;
        try {
            String factureIdStr = factureIdField.getText().trim();
            String modePaiement = modePaiementFactureCombo.getValue();

            if (factureIdStr.isEmpty()) {
                resultArea.setText("Veuillez saisir un ID de facture");
                return;
            }

            Long factureId = Long.parseLong(factureIdStr);
            boolean success = magasinService.payerFacture(factureId, modePaiement);

            if (success) {
                resultArea.setText("Paiement effectué avec succès!\nFacture N° " + factureId +
                                 "\nMode de paiement: " + modePaiement);
                consulterFacture();
            } else {
                resultArea.setText("Échec du paiement. Facture introuvable.");
            }

        } catch (NumberFormatException e) {
            resultArea.setText("ID de facture invalide.");
        } catch (Exception e) {
            resultArea.setText("Erreur lors du paiement: " + e.getMessage());
        }
    }

    @FXML
    private void voirFacturesClient() {
        try {
            String clientId = clientIdField.getText().trim();
            if (clientId.isEmpty()) {
                resultArea.setText("Veuillez saisir un ID client");
                return;
            }

            List<Facture> factures = magasinService.getFacturesClient(clientId);
            if (!factures.isEmpty()) {
                factureTable.setItems(FXCollections.observableArrayList(factures));

                BigDecimal totalDu = factures.stream()
                    .filter(f -> !f.isPayee())
                    .map(Facture::getTotalFacture)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                resultArea.setText("Factures du client " + clientId + ":\n" +
                                 "Nombre de factures: " + factures.size() + "\n" +
                                 "Total dû: " + totalDu + "€");
            } else {
                factureTable.setItems(FXCollections.observableArrayList());
                resultArea.setText("Aucune facture trouvée pour le client: " + clientId);
            }

        } catch (Exception e) {
            resultArea.setText("Erreur lors de la consultation des factures client: " + e.getMessage());
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
            resultArea.setText("Chiffre d'affaires du " + date + ": " + chiffre + "€");

        } catch (Exception e) {
            resultArea.setText("Erreur lors du calcul du chiffre d'affaires: " + e.getMessage());
        }
    }
}