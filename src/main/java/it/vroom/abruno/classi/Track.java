package it.vroom.abruno.classi;

public class Track {
    private String mongoId; // MongoDB ObjectId
    private int id; // Il campo "id" progressivo 0-55 del JSON
    private String nome;
    private String citta;
    private String regione;
    private String grado;
    private int lunghezza;
    private int capienza;
    private double latitudine;
    private double longitudine;
    private String note;

    // Costruttore vuoto
    public Track() {}

    // Costruttore con parametri
    public Track(int id, String nome, String citta, String regione, String grado,
                 int lunghezza, int capienza, double latitudine, double longitudine, String note) {
        this.id = id;
        this.nome = nome;
        this.citta = citta;
        this.regione = regione;
        this.grado = grado;
        this.lunghezza = lunghezza;
        this.capienza = capienza;
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.note = note;
    }

    // GETTER e SETTER
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public String getRegione() {
        return regione;
    }

    public void setRegione(String regione) {
        this.regione = regione;
    }

    public String getGrado() {
        return grado;
    }

    public void setGrado(String grado) {
        this.grado = grado;
    }

    public int getLunghezza() {
        return lunghezza;
    }

    public void setLunghezza(int lunghezza) {
        this.lunghezza = lunghezza;
    }

    public int getCapienza() {
        return capienza;
    }

    public void setCapienza(int capienza) {
        this.capienza = capienza;
    }

    public double getLatitudine() {
        return latitudine;
    }

    public void setLatitudine(double latitudine) {
        this.latitudine = latitudine;
    }

    public double getLongitudine() {
        return longitudine;
    }

    public void setLongitudine(double longitudine) {
        this.longitudine = longitudine;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "Track{" +
                "mongoId='" + mongoId + '\'' +
                ", id=" + id +
                ", nome='" + nome + '\'' +
                ", citta='" + citta + '\'' +
                ", regione='" + regione + '\'' +
                ", grado='" + grado + '\'' +
                ", lunghezza=" + lunghezza +
                ", capienza=" + capienza +
                ", latitudine=" + latitudine +
                ", longitudine=" + longitudine +
                ", note='" + note + '\'' +
                '}';
    }
}