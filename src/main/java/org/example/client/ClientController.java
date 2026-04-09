package org.example.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.rmi.MagasinService;

import java.math.BigDecimal;
import java.rmi.Naming;
import java.time.LocalDate;
import java.util.List;

public class ClientController {

    @FXML private TextField referenceField;
    @FXML private TextField referenceVenteField;
    @FXML private TextField familleField;
    @FXML private TextField clientIdField;
    @FXML private TextField quantiteField;
    @FXML private ComboBox<String> modePaiementCombo;
    @FXML private TextField factureIdField;
    @FXML private TextField stockQuantiteField;
    @FXML private DatePicker datePicker;

    @FXML private TextArea resultArea;
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

    private MagasinService magasinService;

    @FXML
    public void initialize() {
        try {
            // Connexion au service RMI
            magasinService = (MagasinService) Naming.lookup("rmi://localhost:1099/MagasinService");

            // Configuration des ComboBox
            modePaiementCombo.setItems(FXCollections.observableArrayList("Espèces", "Carte bancaire", "Chèque"));
            modePaiementCombo.setValue("Carte bancaire");

            // Configuration des colonnes de table
            setupTableColumns();

            resultArea.setText("Connexion au serveur magasin réussie!");

        } catch (Exception e) {
            resultArea.setText("Erreur de connexion au serveur: " + e.getMessage());
        }
    }

    private void setupTableColumns() {
        // Table articles
        refColumn.setCellValueFactory(new PropertyValueFactory<>("reference"));
        familleColumn.setCellValueFactory(new PropertyValueFactory<>("famille"));
        prixColumn.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("quantiteEnStock"));

        // Table factures
        factureIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        clientColumn.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("totalFacture"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateFacturation"));
        payeeColumn.setCellValueFactory(new PropertyValueFactory<>("payee"));
    }

    @FXML
    private void consulterArticle() {
        System.out.println("🖱️ CLIENT: Bouton 'Consulter Stock' cliqué!");
        try {
            String reference = referenceField.getText().trim();
            System.out.println("📝 CLIENT: Référence saisie = '" + reference + "'");
            if (reference.isEmpty()) {
                resultArea.setText("Veuillez saisir une référence d'article");
                return;
            }

            Article article = magasinService.consulterStockArticle(reference);
            if (article != null) {
                ObservableList<Article> data = FXCollections.observableArrayList(article);
                articleTable.setItems(data);
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
        try {
            String famille = familleField.getText().trim();
            if (famille.isEmpty()) {
                resultArea.setText("Veuillez saisir une famille d'articles");
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
    private void acheterArticle() {
        try {
            String clientId = clientIdField.getText().trim();
            String reference = referenceVenteField.getText().trim();
            String quantiteStr = quantiteField.getText().trim();
            String modePaiement = modePaiementCombo.getValue();

            if (clientId.isEmpty() || reference.isEmpty() || quantiteStr.isEmpty()) {
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

                // Rafraîchir l'affichage de l'article
                consulterArticle();
            } else {
                resultArea.setText("Échec de l'achat. Stock insuffisant ou article inexistant.");
            }

        } catch (NumberFormatException e) {
            resultArea.setText("Quantité invalide. Veuillez saisir un nombre entier.");
        } catch (Exception e) {
            resultArea.setText("Erreur lors de l'achat: " + e.getMessage());
        }
    }

    @FXML
    private void consulterFacture() {
        try {
            String factureIdStr = factureIdField.getText().trim();
            if (factureIdStr.isEmpty()) {
                resultArea.setText("Veuillez saisir un ID de facture");
                return;
            }

            Long factureId = Long.parseLong(factureIdStr);
            Facture facture = magasinService.consulterFacture(factureId);

            if (facture != null) {
                ObservableList<Facture> data = FXCollections.observableArrayList(facture);
                factureTable.setItems(data);

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
        try {
            String factureIdStr = factureIdField.getText().trim();
            String modePaiement = modePaiementCombo.getValue();

            if (factureIdStr.isEmpty()) {
                resultArea.setText("Veuillez saisir un ID de facture");
                return;
            }

            Long factureId = Long.parseLong(factureIdStr);
            boolean success = magasinService.payerFacture(factureId, modePaiement);

            if (success) {
                resultArea.setText("Paiement effectué avec succès!\nFacture N° " + factureId +
                                 "\nMode de paiement: " + modePaiement);

                // Rafraîchir l'affichage de la facture
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
    private void ajouterStock() {
        try {
            String reference = referenceField.getText().trim();
            String quantiteStr = stockQuantiteField.getText().trim();

            if (reference.isEmpty() || quantiteStr.isEmpty()) {
                resultArea.setText("Veuillez saisir la référence et la quantité");
                return;
            }

            int quantite = Integer.parseInt(quantiteStr);
            boolean success = magasinService.ajouterStock(reference, quantite);

            if (success) {
                resultArea.setText("Stock ajouté avec succès!\nArticle: " + reference +
                                 "\nQuantité ajoutée: " + quantite);

                // Rafraîchir l'affichage de l'article
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
                ObservableList<Facture> data = FXCollections.observableArrayList(factures);
                factureTable.setItems(data);

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
}