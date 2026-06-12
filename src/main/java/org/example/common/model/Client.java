package org.example.common.model;

import java.io.Serializable;

public class Client implements Serializable {
    private int id;
    private String nomFamille;
    private String prenom;

    public Client() {}

    public Client(int id, String nomFamille, String prenom) {
        this.id = id;
        this.nomFamille = nomFamille;
        this.prenom = prenom;
    }

    public int  getId()        { return id; }
    public void setId(int id)  { this.id = id; }

    public String getNomFamille()               { return nomFamille; }
    public void   setNomFamille(String nomFamille) { this.nomFamille = nomFamille; }

    public String getPrenom()              { return prenom; }
    public void   setPrenom(String prenom) { this.prenom = prenom; }

    public String getNomComplet() { return nomFamille + " " + prenom; }

    @Override
    public String toString() { return getNomComplet(); }
}
