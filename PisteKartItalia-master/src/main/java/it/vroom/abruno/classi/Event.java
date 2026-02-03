package it.vroom.abruno.classi;

import java.time.LocalDateTime;

public class Event {
    private String mongoId; // MongoDB ObjectId
    private int id = 0; // Il campo "id" progressivo - inizializzato a 0
    private int idOrganizzatore; // chiave esterna per utente organizzatore
    private int idPista; // chiave esterna per pista
    private String titolo;
    private String descrizione;
    private String linkEsterno;
    private String foto; // URL foto evento
    private String note;
    private LocalDateTime data; // Data evento

    // Costruttore vuoto
    public Event() {}

    // Costruttore senza ID progressivo (verrà generato automaticamente)
    public Event(int idOrganizzatore, int idPista, String titolo, String descrizione,
                 String linkEsterno, String foto, String note, LocalDateTime data) {
        this.idOrganizzatore = idOrganizzatore;
        this.idPista = idPista;
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.linkEsterno = linkEsterno;
        this.foto = foto;
        this.note = note;
        this.data = data;
    }

    // Costruttore completo (per recupero da database)
    public Event(String mongoId, int id, int idOrganizzatore, int idPista, String titolo,
                 String descrizione, String linkEsterno, String foto, String note, LocalDateTime data) {
        this.mongoId = mongoId;
        this.id = id;
        this.idOrganizzatore = idOrganizzatore;
        this.idPista = idPista;
        this.titolo = titolo;
        this.descrizione = descrizione;
        this.linkEsterno = linkEsterno;
        this.foto = foto;
        this.note = note;
        this.data = data;
    }

    // Getter e Setter
    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdOrganizzatore() {
        return idOrganizzatore;
    }

    public void setIdOrganizzatore(int idOrganizzatore) {
        this.idOrganizzatore = idOrganizzatore;
    }

    public int getIdPista() {
        return idPista;
    }

    public void setIdPista(int idPista) {
        this.idPista = idPista;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getLinkEsterno() {
        return linkEsterno;
    }

    public void setLinkEsterno(String linkEsterno) {
        this.linkEsterno = linkEsterno;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Event{" +
                "mongoId='" + mongoId + '\'' +
                ", id=" + id +
                ", idOrganizzatore=" + idOrganizzatore +
                ", idPista=" + idPista +
                ", titolo='" + titolo + '\'' +
                ", descrizione='" + descrizione + '\'' +
                ", linkEsterno='" + linkEsterno + '\'' +
                ", foto='" + foto + '\'' +
                ", note='" + note + '\'' +
                ", data=" + data +
                '}';
    }
}