package org.example.common.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class LigneFacture implements Serializable {
    private String referenceArticle;
    private String nomArticle;
    private int quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal sousTotal;

    public LigneFacture() {}

    public LigneFacture(String referenceArticle, String nomArticle, int quantite, BigDecimal prixUnitaire) {
        this.referenceArticle = referenceArticle;
        this.nomArticle = nomArticle;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    public String getReferenceArticle() {
        return referenceArticle;
    }

    public void setReferenceArticle(String referenceArticle) {
        this.referenceArticle = referenceArticle;
    }

    public String getNomArticle() {
        return nomArticle;
    }

    public void setNomArticle(String nomArticle) {
        this.nomArticle = nomArticle;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
        this.sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        this.sousTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }

    public BigDecimal getSousTotal() {
        return sousTotal;
    }

    @Override
    public String toString() {
        return "LigneFacture{" +
                "referenceArticle='" + referenceArticle + '\'' +
                ", nomArticle='" + nomArticle + '\'' +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                ", sousTotal=" + sousTotal +
                '}';
    }
}