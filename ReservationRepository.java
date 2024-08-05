package com.example.convoiturage_javafx.Repository;

import com.example.convoiturage_javafx.model.Reservation;
import com.example.convoiturage_javafx.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class ReservationRepository extends GenericRepository<Reservation> {
    public ReservationRepository() {
        super(Reservation.class);
    }

    public List<Reservation> findByPassager(Utilisateur passager) {
        EntityManager em = JPAUtil.getEntityManager();
        String query = "SELECT r FROM Reservation r WHERE r.passager = :passager";
        TypedQuery<Reservation> typedQuery = em.createQuery(query, Reservation.class);
        typedQuery.setParameter("passager", passager);
        return typedQuery.getResultList();
    }

    public List<Reservation> findByConducteur(Utilisateur conducteur) {
        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Reservation> query = em.createQuery("SELECT r FROM Reservation r WHERE r.trajet.conducteur = :conducteur", Reservation.class);
        query.setParameter("conducteur", conducteur);
        return query.getResultList();
    }


}
