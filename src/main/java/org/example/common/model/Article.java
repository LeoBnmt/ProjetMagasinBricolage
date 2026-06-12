package org.example.common.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Article implements Serializable {
    private String reference;
    private String nom;
    private String famille;
    private BigDecimal prixUnitaire;
    private int quantiteEnStock;

    public Article() {}

    public Article(String reference, String nom, String famille, BigDecimal prixUnitaire, int quantiteEnStock) {
        this.reference = reference;
        this.nom = nom;
        this.famille = famille;
        this.prixUnitaire = prixUnitaire;
        this.quantiteEnStock = quantiteEnStock;
    }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getFamille() { return famille; }
    public void setFamille(String famille) { this.famille = famille; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public int getQuantiteEnStock() { return quantiteEnStock; }
    public void setQuantiteEnStock(int quantiteEnStock) { this.quantiteEnStock = quantiteEnStock; }

    @Override
    public String toString() {
        return reference + " - " + nom;
    }
}