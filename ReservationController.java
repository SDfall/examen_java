package com.example.convoiturage_javafx;

import com.example.convoiturage_javafx.model.Reservation;
import com.example.convoiturage_javafx.model.Trajet;
import com.example.convoiturage_javafx.model.Utilisateur;
import com.example.convoiturage_javafx.Repository.ReservationRepository;
import com.example.convoiturage_javafx.Repository.TrajetRepository;
import com.example.convoiturage_javafx.Repository.UtilisateurRepository;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ReservationController {

    @FXML
    private ComboBox<Utilisateur> comboPassager;
    @FXML
    private ComboBox<Trajet> comboTrajet;
    @FXML
    private DatePicker dateReservationPicker;
    @FXML
    private TableView<Reservation> tableReservations;
    @FXML
    private TableColumn<Reservation, String> colPassager;
    @FXML
    private TableColumn<Reservation, String> colTrajet;
    @FXML
    private TableColumn<Reservation, LocalDateTime> colDateReservation;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnUpdate;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnClear;
    @FXML
    private ToggleGroup filterGroup;
    @FXML
    private RadioButton rbAll;
    @FXML
    private RadioButton rbInProgress;
    @FXML
    private RadioButton rbPast;
    @FXML
    private RadioButton rbFuture;

    private ReservationRepository reservationRepository = new ReservationRepository();
    private UtilisateurRepository utilisateurRepository = new UtilisateurRepository();
    private TrajetRepository trajetRepository = new TrajetRepository();
    private ObservableList<Reservation> reservations;

    @FXML
    public void initialize() {
        List<Utilisateur> passagers = utilisateurRepository.findAll().stream()
                .filter(utilisateur -> "Passager".equals(utilisateur.getProfil()))
                .collect(Collectors.toList());
        comboPassager.setItems(FXCollections.observableArrayList(passagers));

        List<Trajet> trajets = trajetRepository.findAll();
        comboTrajet.setItems(FXCollections.observableArrayList(trajets));

        comboPassager.setCellFactory((comboBox) -> new ListCell<Utilisateur>() {
            @Override
            protected void updateItem(Utilisateur item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : item.getPrenom() + " " + item.getNom());
            }
        });

        comboTrajet.setCellFactory((comboBox) -> new ListCell<Trajet>() {
            @Override
            protected void updateItem(Trajet item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? null : item.getVilleDepart() + " -> " + item.getVilleArrivee());
            }
        });

        comboPassager.setConverter(new StringConverter<Utilisateur>() {
            @Override
            public String toString(Utilisateur utilisateur) {
                return utilisateur == null ? null : utilisateur.getPrenom() + " " + utilisateur.getNom();
            }

            @Override
            public Utilisateur fromString(String userId) {
                return null;
            }
        });

        comboTrajet.setConverter(new StringConverter<Trajet>() {
            @Override
            public String toString(Trajet trajet) {
                return trajet == null ? null : trajet.getVilleDepart() + " ==> "+ " " + trajet.getVilleArrivee();
            }

            @Override
            public Trajet fromString(String trajetId) {
                return null;
            }
        });

        colPassager.setCellValueFactory(cellData -> {
            Utilisateur passager = cellData.getValue().getPassager();
            return new ReadOnlyStringWrapper(passager == null ? "" : passager.getPrenom() + " " + passager.getNom());
        });

        colTrajet.setCellValueFactory(cellData -> {
            Trajet trajet = cellData.getValue().getTrajet();
            return new ReadOnlyStringWrapper(trajet == null ? "" : trajet.getVilleDepart() + " -> " + trajet.getVilleArrivee());
        });

        colDateReservation.setCellValueFactory(new PropertyValueFactory<>("dateReservation"));

        rbAll.setOnAction(e -> loadReservations("all"));
        rbInProgress.setOnAction(e -> loadReservations("inProgress"));
        rbPast.setOnAction(e -> loadReservations("past"));
        rbFuture.setOnAction(e -> loadReservations("future"));

        loadReservations("all");

        tableReservations.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                Reservation reservation = newSelection;
                comboPassager.setValue(reservation.getPassager());
                comboTrajet.setValue(reservation.getTrajet());
                LocalDateTime dateReservation = reservation.getDateReservation();
                dateReservationPicker.setValue(dateReservation == null ? null : dateReservation.toLocalDate());
            }
        });

        btnSave.setOnAction(e -> saveReservation());
        btnUpdate.setOnAction(e -> updateReservation());
        btnDelete.setOnAction(e -> deleteReservation());
        btnClear.setOnAction(e -> clearForm());
    }

    private void loadReservations(String filter) {
        List<Reservation> allReservations = reservationRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDate nowDate = now.toLocalDate();
        List<Reservation> filteredReservations;

        switch (filter) {
            case "inProgress":
                filteredReservations = allReservations.stream()
                        .filter(r -> r.getDateReservation().toLocalDate().isBefore(nowDate)
                                && r.getTrajet().getDateFin().isAfter(nowDate))
                        .collect(Collectors.toList());
                break;
            case "past":
                filteredReservations = allReservations.stream()
                        .filter(r -> r.getTrajet().getDateFin().isBefore(nowDate))
                        .collect(Collectors.toList());
                break;
            case "future":
                filteredReservations = allReservations.stream()
                        .filter(r -> r.getDateReservation().toLocalDate().isAfter(nowDate))
                        .collect(Collectors.toList());
                break;
            default:
                filteredReservations = allReservations;
                break;
        }

        reservations = FXCollections.observableArrayList(filteredReservations);
        tableReservations.setItems(reservations);
    }

    private void saveReservation() {
        Reservation reservation = new Reservation();
        reservation.setPassager(comboPassager.getValue());
        reservation.setTrajet(comboTrajet.getValue());
        if (dateReservationPicker.getValue() != null) {
            reservation.setDateReservation(dateReservationPicker.getValue().atStartOfDay());
        }
        reservationRepository.save(reservation);
        sendReservationEmail(reservation);
        loadReservations(getCurrentFilter());
        clearForm();
    }

    private void updateReservation() {
        Reservation reservation = tableReservations.getSelectionModel().getSelectedItem();
        if (reservation != null) {
            reservation.setPassager(comboPassager.getValue());
            reservation.setTrajet(comboTrajet.getValue());
            if (dateReservationPicker.getValue() != null) {
                reservation.setDateReservation(dateReservationPicker.getValue().atStartOfDay());
            }
            reservationRepository.update(reservation);
            loadReservations(getCurrentFilter());
            clearForm();
        }
    }

    private void deleteReservation() {
        Reservation reservation = tableReservations.getSelectionModel().getSelectedItem();
        if (reservation != null) {
            reservationRepository.delete(reservation);
            loadReservations(getCurrentFilter());
            clearForm();
        }
    }

    private void clearForm() {
        comboPassager.setValue(null);
        comboTrajet.setValue(null);
        dateReservationPicker.setValue(null);
    }

    private String getCurrentFilter() {
        if (rbInProgress.isSelected()) {
            return "inProgress";
        } else if (rbPast.isSelected()) {
            return "past";
        } else if (rbFuture.isSelected()) {
            return "future";
        } else {
            return "all";
        }
    }

    private void sendReservationEmail(Reservation reservation) {
        String to = reservation.getPassager().getEmail();
        String from = "seydoulefa1021@gmail.com";  // Remplacez par votre email d'expéditeur
        String host = "sandbox.smtp.mailtrap.io";
        String username = "2a675a740f619e";
        String password = "0380dea0bf2864";

        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        properties.setProperty("mail.smtp.port", "2525");
        properties.setProperty("mail.smtp.auth", "true");
        properties.setProperty("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject("Confirmation de réservation");
            message.setText("Votre réservation a été confirmée pour le trajet " +
                    reservation.getTrajet().getVilleDepart() + " -> " +
                    reservation.getTrajet().getVilleArrivee() + " le " +
                    reservation.getDateReservation().toLocalDate() + ".");

            Transport.send(message);
            System.out.println("Email envoyé avec succès.");
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
