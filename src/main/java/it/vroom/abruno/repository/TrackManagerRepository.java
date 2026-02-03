package it.vroom.abruno.repository;

import it.vroom.abruno.classi.TrackManagerMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

/**
 * Repository per la gestione della mappatura tra TRACK_MANAGER e Pista.
 */
public interface TrackManagerRepository extends MongoRepository<TrackManagerMapping, String> {

    /**
     * Trova tutte le mappature per un dato username (Remote-User di Authelia).
     * @param username L'username del manager (solitamente l'email).
     * @return Lista di mappature associate.
     */
    List<TrackManagerMapping> findByUsername(String username);
}