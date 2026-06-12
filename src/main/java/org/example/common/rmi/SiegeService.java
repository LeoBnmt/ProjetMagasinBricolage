package org.example.common.rmi;

import org.example.common.model.Article;
import org.example.common.model.Facture;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SiegeService extends Remote {

    void mettreAJourPrix(Map<String, BigDecimal> nouveauPrix) throws RemoteException;

    void sauvegarderFactures(List<Facture> factures) throws RemoteException;

    List<Article> getTousLesArticles() throws RemoteException;

    void synchroniserStock(List<Article> articles) throws RemoteException;

    BigDecimal calculerChiffreAffairesTotal(LocalDate date) throws RemoteException;
}