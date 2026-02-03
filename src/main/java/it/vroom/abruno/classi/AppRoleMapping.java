package it.vroom.abruno.classi;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Entità che mappa un utente ai ruoli di alto livello (ADMIN, TRACK_MANAGER) nell'applicazione.
 */
@Document(collection = "appRoleMapping")
@CompoundIndex(def = "{'username': 1, 'appRole': 1}", unique = true)
public class AppRoleMapping {

    @Id
    private String id;

    @Indexed
    private String username; // L'email/username fornito da Authelia (Remote-User)

    private String appRole;  // Il ruolo dell'applicazione (es. "ADMIN", "TRACK_MANAGER", "USER")

    // Costruttori, Getter e Setter...
    public AppRoleMapping() {}

    public AppRoleMapping(String username, String appRole) {
        this.username = username;
        this.appRole = appRole;
    }

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAppRole() { return appRole; }
    public void setAppRole(String appRole) { this.appRole = appRole; }
}