CREATE DATABASE IF NOT EXISTS brico_merlin;
USE brico_merlin;

CREATE TABLE articles (
    ref VARCHAR(50) PRIMARY KEY,
    famille VARCHAR(100) NOT NULL,
    prix_unitaire DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE factures (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id VARCHAR(50) NOT NULL,
    date_facturation DATE NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    mode_paiement ENUM('carte', 'especes', 'cheque') NOT NULL,
    statut ENUM('en_attente', 'payee') DEFAULT 'en_attente'
) ENGINE=InnoDB;

CREATE TABLE lignes_facture (
    id INT AUTO_INCREMENT PRIMARY KEY,
    facture_id INT NOT NULL,
    ref_article VARCHAR(50) NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (facture_id) REFERENCES factures(id),
    FOREIGN KEY (ref_article) REFERENCES articles(ref)
) ENGINE=InnoDB;

-- Données de test
INSERT INTO articles VALUES
    ('ART001', 'Visserie',  2.50,  100),
    ('ART002', 'Visserie',  1.20,  50),
    ('ART003', 'Peinture',  15.00, 20),
    ('ART004', 'Outillage', 49.99, 10),
    ('ART005', 'Peinture',  8.90,  35),
    ('ART006', 'Outillage', 12.50, 0);
