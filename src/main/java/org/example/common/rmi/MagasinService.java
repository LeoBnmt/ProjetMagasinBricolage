package org.example.common.rmi;

import org.example.common.model.Article;
import org.example.common.model.Facture;
import org.example.common.model.Famille;
import org.example.common.model.LigneFacture;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface MagasinService extends Remote {

    List<Article> getTousLesArticles() throws RemoteException;

    List<Famille> getToutesLesFamilles() throws RemoteException;

    Article consulterStockArticle(String reference) throws RemoteException;

    List<String> rechercherArticleParFamille(String famille) throws RemoteException;

    boolean acheterArticle(String clientId, String referenceArticle, int quantite, String modePaiement) throws RemoteException;

    boolean payerFacture(Long factureId, String modePaiement) throws RemoteException;

    Facture consulterFacture(Long factureId) throws RemoteException;

    List<Facture> getFacturesClient(String clientId) throws RemoteException;

    boolean ajouterStock(String referenceArticle, int quantite) throws RemoteException;

    BigDecimal calculerChiffreAffaires(LocalDate date) throws RemoteException;

    void recevoirMiseAJourPrix(Map<String, BigDecimal> nouveauxPrix) throws RemoteException;

    List<Facture> getToutesLesFactures() throws RemoteException;

    void viderFichierFactures() throws RemoteException;
}