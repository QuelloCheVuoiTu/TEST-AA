package it.vroom.abruno.classi;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entità che mappa un utente (TRACK_MANAGER) all'ID della Pista che può modificare.
 * La collection sarà 'track_manager_mapping'.
 */
@Document(collection = "track_manager_mapping")
@CompoundIndex(def = "{'username': 1, 'idTrack': 1}", unique = true) // Assicura che un utente non possa gestire due volte la stessa pista
public class TrackManagerMapping {

    @Id
    private String id;

    @Indexed // Indicizzazione per ricerca rapida per username
    private String username; // L'email/username fornito da Authelia (Remote-User)

    private int idTrack;     // L'ID progressivo della pista

    // Costruttore
    public TrackManagerMapping() {}

    public TrackManagerMapping(String username, int idTrack) {
        this.username = username;
        this.idTrack = idTrack;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getIdTrack() {
        return idTrack;
    }

    public void setIdTrack(int idTrack) {
        this.idTrack = idTrack;
    }

    @Override
    public String toString() {
        return "TrackManagerMapping{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", idTrack=" + idTrack +
                '}';
    }
}