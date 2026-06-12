package org.example.common.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Facture implements Serializable {

    public static final String SEPARATOR = "========================================";
    public static final String LINE_SEP  = "----------------------------------------";

    private Long id;
    private String clientId;
    private BigDecimal totalFacture;
    private List<LigneFacture> lignes;
    private String modePaiement;
    private LocalDate dateFacturation;
    private boolean payee;

    public Facture() {
        this.lignes = new ArrayList<>();
        this.dateFacturation = LocalDate.now();
        this.payee = false;
    }

    public Facture(String clientId, String modePaiement) {
        this();
        this.clientId = clientId;
        this.modePaiement = modePaiement;
    }

    public void ajouterLigne(LigneFacture ligne) {
        this.lignes.add(ligne);
        calculerTotal();
    }

    private void calculerTotal() {
        this.totalFacture = lignes.stream()
                .map(LigneFacture::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public BigDecimal getTotalFacture() {
        return totalFacture;
    }

    public void setTotalFacture(BigDecimal totalFacture) {
        this.totalFacture = totalFacture;
    }

    public List<LigneFacture> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneFacture> lignes) {
        this.lignes = lignes;
        calculerTotal();
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public LocalDate getDateFacturation() {
        return dateFacturation;
    }

    public void setDateFacturation(LocalDate dateFacturation) {
        this.dateFacturation = dateFacturation;
    }

    public boolean isPayee() {
        return payee;
    }

    public void setPayee(boolean payee) {
        this.payee = payee;
    }

    public String toTicket() {
        StringBuilder sb = new StringBuilder();
        sb.append("FACTURE #").append(id).append("\n");
        sb.append("Date : ").append(dateFacturation).append("\n");
        sb.append("Client : ").append(clientId).append("\n");
        sb.append("Mode : ").append(modePaiement).append("\n");
        sb.append("Statut : ").append(payee ? "PAYEE" : "EN_ATTENTE").append("\n");
        sb.append(LINE_SEP).append("\n");
        for (LigneFacture ligne : lignes) {
            sb.append(String.format(Locale.US, "%-10s | x%-3d | %8.2f€ | %8.2f€\n",
                    ligne.getReferenceArticle(),
                    ligne.getQuantite(),
                    ligne.getPrixUnitaire(),
                    ligne.getSousTotal()));
        }
        sb.append("TOTAL : ").append(String.format(Locale.US, "%.2f€", totalFacture)).append("\n");
        sb.append(LINE_SEP).append("\n");
        sb.append(SEPARATOR).append("\n\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Facture{" +
                "id=" + id +
                ", clientId='" + clientId + '\'' +
                ", totalFacture=" + totalFacture +
                ", lignes=" + lignes +
                ", modePaiement='" + modePaiement + '\'' +
                ", dateFacturation=" + dateFacturation +
                ", payee=" + payee +
                '}';
    }
}