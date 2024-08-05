package com.example.convoiturage_javafx;

import com.example.convoiturage_javafx.model.Reservation;
import com.example.convoiturage_javafx.model.Trajet;
import com.example.convoiturage_javafx.model.Utilisateur;
import com.example.convoiturage_javafx.Repository.ReservationRepository;
import com.example.convoiturage_javafx.Repository.TrajetRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.List;

public class UtilisateurController extends BaseController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private ComboBox<Trajet> comboTrajet;
    @FXML
    private DatePicker dateReservationPicker;
    @FXML
    private Button btnValiderReservation;
    @FXML
    private Button btnAnnuler;
    @FXML
    private Label reservationStatusLabel;
    @FXML
    private TableView<Reservation> tableReservations;
    @FXML
    private TableColumn<Reservation, String> colTrajet;
    @FXML
    private TableColumn<Reservation, String> colDateReservation;
    @FXML
    private TableColumn<Reservation, String> colStatus;

    private Utilisateur utilisateurConnecte;
    private ReservationRepository reservationRepository = new ReservationRepository();
    private TrajetRepository trajetRepository = new TrajetRepository();

    public void setUtilisateurConnecte(Utilisateur utilisateur) {
        this.utilisateurConnecte = utilisateur;
        updateWelcomeMessage();
        updateReservationStatus(); // Assurez-vous de mettre à jour le statut de réservation à la connexion
        loadReservations(); // Charge les réservations du passager
    }

    @FXML
    public void initialize() {
        List<Trajet> trajets = trajetRepository.findAll();
        comboTrajet.setItems(FXCollections.observableArrayList(trajets));

        comboTrajet.setCellFactory((comboBox) -> new ListCell<Trajet>() {
            @Override
            protected void updateItem(Trajet item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : item.getVilleDepart() + " -> " + item.getVilleArrivee());
            }
        });

        comboTrajet.setConverter(new StringConverter<Trajet>() {
            @Override
            public String toString(Trajet trajet) {
                return trajet == null ? null : trajet.getVilleDepart() + " -> " + trajet.getVilleArrivee();
            }

            @Override
            public Trajet fromString(String trajetId) {
                return null;
            }
        });

        btnValiderReservation.setOnAction(e -> validerReservation());
        btnAnnuler.setOnAction(e -> annulerReservation());

        // Mise à jour du statut de réservation
        updateReservationStatus();
    }

    private void updateWelcomeMessage() {
        if (utilisateurConnecte != null) {
            String prenom = utilisateurConnecte.getPrenom(); // Assurez-vous que getPrenom() existe
            String nom = utilisateurConnecte.getNom(); // Assurez-vous que getNom() existe
            welcomeLabel.setText("Bienvenue " + prenom + " " + nom + "!");
        }
    }

    @FXML
    private void validerReservation() {
        if (utilisateurConnecte != null) {
            // Vérifiez si tous les champs sont remplis
            if (comboTrajet.getValue() == null || dateReservationPicker.getValue() == null) {
                showAlert(Alert.AlertType.ERROR, "Champs manquants", "Veuillez remplir tous les champs avant de valider.");
                return;
            }

            Reservation reservation = new Reservation();
            reservation.setPassager(utilisateurConnecte);
            reservation.setTrajet(comboTrajet.getValue());
            reservation.setDateReservation(dateReservationPicker.getValue().atStartOfDay());

            reservationRepository.save(reservation);
            showAlert(Alert.AlertType.INFORMATION, "Réservation réussie", "La réservation a été ajoutée avec succès.");

            sendReservationEmail(reservation);

            // Réinitialiser les champs
            annulerReservation();
            // Mettre à jour le statut de réservation
            updateReservationStatus();
            loadReservations(); // Recharger les réservations après ajout
        }
    }

    @FXML
    private void annulerReservation() {
        comboTrajet.setValue(null);
        dateReservationPicker.setValue(null);
        updateReservationStatus();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void sendReservationEmail(Reservation reservation) {
        // Configuration de l'envoi d'email (inchangé)
    }

    private void updateReservationStatus() {
        if (utilisateurConnecte != null) {
            List<Reservation> reservations = reservationRepository.findByPassager(utilisateurConnecte);
            if (reservations.isEmpty()) {
                reservationStatusLabel.setText("Vous n'avez pas encore de réservation.");
            } else {
                Reservation latestReservation = reservations.stream()
                        .max((r1, r2) -> r1.getDateReservation().compareTo(r2.getDateReservation()))
                        .orElse(null);

                LocalDateTime now = LocalDateTime.now();
                if (latestReservation != null && latestReservation.getDateReservation().toLocalDate().isEqual(now.toLocalDate())) {
                    reservationStatusLabel.setText("Vous avez une réservation en cours pour le trajet\n" +
                            latestReservation.getTrajet().getVilleDepart() + " -> " +
                            latestReservation.getTrajet().getVilleArrivee() + " le " +
                            latestReservation.getDateReservation().toLocalDate() + " à " +
                            latestReservation.getDateReservation().toLocalTime() + ".");
                } else if (latestReservation != null && latestReservation.getDateReservation().isBefore(now)) {
                    reservationStatusLabel.setText("Vous avez une réservation passée pour le trajet\n" +
                            latestReservation.getTrajet().getVilleDepart() + " -> " +
                            latestReservation.getTrajet().getVilleArrivee() + " le " +
                            latestReservation.getDateReservation().toLocalDate() + " à " +
                            latestReservation.getDateReservation().toLocalTime() + ".");
                } else {
                    reservationStatusLabel.setText("Vous avez une réservation future pour le trajet\n" +
                            latestReservation.getTrajet().getVilleDepart() + " -> " +
                            latestReservation.getTrajet().getVilleArrivee() + " le " +
                            latestReservation.getDateReservation().toLocalDate() + " à " +
                            latestReservation.getDateReservation().toLocalTime() + ".");
                }
            }
        }
    }

    private void loadReservations() {
        if (utilisateurConnecte != null) {
            List<Reservation> reservations = reservationRepository.findByPassager(utilisateurConnecte);

            colTrajet.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTrajet().getVilleDepart() + " -> " + cellData.getValue().getTrajet().getVilleArrivee()));
            colDateReservation.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateReservation().toLocalDate().toString()));
            colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDateReservation().isBefore(LocalDateTime.now()) ? "Passée" : "Future"));

            tableReservations.setItems(FXCollections.observableArrayList(reservations));
        }
    }
}
