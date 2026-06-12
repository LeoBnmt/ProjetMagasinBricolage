package org.example.server.siege;

import org.example.common.model.Facture;
import org.example.common.model.LigneFacture;
import org.example.common.util.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SiegeServiceImplTest {

    private MockedStatic<DatabaseConnection> mockDC;
    private Connection mockConn;
    private PreparedStatement mockStmt;
    private ResultSet mockRs;
    private SiegeServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        mockDC = mockStatic(DatabaseConnection.class);
        DatabaseConnection mockDbInstance = mock(DatabaseConnection.class);
        mockConn = mock(Connection.class);
        mockStmt = mock(PreparedStatement.class);
        mockRs = mock(ResultSet.class);

        mockDC.when(DatabaseConnection::getInstance).thenReturn(mockDbInstance);
        when(mockDbInstance.getConnection()).thenReturn(mockConn);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenReturn(mockRs);
        when(mockStmt.executeBatch()).thenReturn(new int[]{1});

        service = new SiegeServiceImpl();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockDC.close();
        UnicastRemoteObject.unexportObject(service, true);
    }

    // ── mettreAJourPrix ──────────────────────────────────────────────────────

    @Test
    void mettreAJourPrix_utiliseLaColonneRef() throws Exception {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(mockConn.prepareStatement(sql.capture())).thenReturn(mockStmt);

        service.mettreAJourPrix(Map.of("ART001", new BigDecimal("2.50")));

        assertFalse(sql.getValue().contains("WHERE reference = ?"),
                "Bug corrigé : ne doit plus utiliser 'reference'");
        assertTrue(sql.getValue().contains("WHERE ref = ?"),
                "Doit utiliser la colonne 'ref'");
    }

    // ── calculerChiffreAffairesTotal ─────────────────────────────────────────

    @Test
    void calculerChiffreAffairesTotal_utiliseLesCorrectesColonnes() throws Exception {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(mockConn.prepareStatement(sql.capture())).thenReturn(mockStmt);

        service.calculerChiffreAffairesTotal(LocalDate.now());

        String requete = sql.getValue();
        assertFalse(requete.contains("SUM(total_facture)"),
                "Bug corrigé : ne doit plus utiliser 'total_facture'");
        assertTrue(requete.contains("SUM(total)"),
                "Doit sommer la colonne 'total'");
        assertFalse(requete.contains("payee = TRUE"),
                "Bug corrigé : ne doit plus utiliser 'payee = TRUE'");
        assertTrue(requete.contains("statut = 'payee'"),
                "Doit filtrer via la colonne 'statut'");
    }

    // ── sauvegarderFactures ──────────────────────────────────────────────────

    @Test
    void sauvegarderFactures_insertFactureUtiliseLesCorrectesColonnes() throws Exception {
        // factureExiste : getInt(1) retourne 0 par défaut → facture n'existe pas → INSERT exécuté
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(mockConn.prepareStatement(sql.capture())).thenReturn(mockStmt);

        service.sauvegarderFactures(List.of(creerFactureTest()));

        // Ordre des appels : 0=COUNT (factureExiste), 1=INSERT factures, 2=INSERT lignes_facture
        String insertFacture = sql.getAllValues().get(1);
        assertFalse(insertFacture.contains("total_facture"),
                "Bug corrigé : ne doit plus utiliser 'total_facture'");
        assertTrue(insertFacture.contains("total"),
                "Doit insérer dans la colonne 'total'");
        assertFalse(insertFacture.contains(", payee"),
                "Bug corrigé : 'payee' n'est pas une colonne, c'est une valeur de 'statut'");
        assertTrue(insertFacture.contains("statut"),
                "Doit insérer dans la colonne 'statut'");
    }

    @Test
    void sauvegarderFactures_insertLignesUtiliseLesCorrectesColonnes() throws Exception {
        // factureExiste : getInt(1) retourne 0 par défaut → INSERT exécuté
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        when(mockConn.prepareStatement(sql.capture())).thenReturn(mockStmt);

        service.sauvegarderFactures(List.of(creerFactureTest()));

        // Ordre des appels : 0=COUNT (factureExiste), 1=INSERT factures, 2=INSERT lignes_facture
        String insertLignes = sql.getAllValues().get(2);
        assertFalse(insertLignes.contains("reference_article"),
                "Bug corrigé : ne doit plus utiliser 'reference_article'");
        assertTrue(insertLignes.contains("ref_article"),
                "Doit utiliser la colonne 'ref_article'");
        assertFalse(insertLignes.contains("nom_article"),
                "Bug corrigé : 'nom_article' n'existe pas dans le schéma");
        assertFalse(insertLignes.contains("sous_total"),
                "Bug corrigé : 'sous_total' n'existe pas dans le schéma");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Facture creerFactureTest() {
        Facture facture = new Facture("CLIENT1", "Carte bancaire");
        facture.setId(1L);
        facture.setDateFacturation(LocalDate.now());
        facture.setLignes(List.of(
                new LigneFacture("ART001", "Marteau", 2, new BigDecimal("5.00"))
        ));
        return facture;
    }
}
