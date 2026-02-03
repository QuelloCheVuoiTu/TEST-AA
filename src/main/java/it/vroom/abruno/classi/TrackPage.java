package it.vroom.abruno.classi;

import java.util.List;
import java.util.ArrayList;

public class TrackPage {
    private String mongoId; // MongoDB ObjectId
    private int idTrack; // Foreign key to Track
    private Integer curve;
    private Integer bestLap; // in seconds          //da parsare in millisecondi
    private String descrizione;
    private List<String> comunicazioni;
    private List<String> galleriaVideo;
    private List<String> galleriaFoto;
    private String videoCopertina;
    private String mappa;

    // Costruttore vuoto
    public TrackPage() {
        this.comunicazioni = new ArrayList<>();
        this.galleriaVideo = new ArrayList<>();
        this.galleriaFoto = new ArrayList<>();
    }

    // Costruttore con idTrack
    public TrackPage(int idTrack) {
        this();
        this.idTrack = idTrack;
    }

    // GETTER e SETTER
    public String getMongoId() {
        return mongoId;
    }

    public void setMongoId(String mongoId) {
        this.mongoId = mongoId;
    }

    public int getIdTrack() {
        return idTrack;
    }

    public void setIdTrack(int idTrack) {
        this.idTrack = idTrack;
    }

    public Integer getCurve() {
        return curve;
    }

    public void setCurve(Integer curve) {
        this.curve = curve;
    }

    public Integer getBestLap() {
        return bestLap;
    }

    public void setBestLap(Integer bestLap) {
        this.bestLap = bestLap;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public List<String> getComunicazioni() {
        return comunicazioni;
    }

    public void setComunicazioni(List<String> comunicazioni) {
        this.comunicazioni = comunicazioni;
    }

    public List<String> getGalleriaVideo() {
        return galleriaVideo;
    }

    public void setGalleriaVideo(List<String> galleriaVideo) {
        this.galleriaVideo = galleriaVideo;
    }

    public List<String> getGalleriaFoto() {
        return galleriaFoto;
    }

    public void setGalleriaFoto(List<String> galleriaFoto) {
        this.galleriaFoto = galleriaFoto;
    }

    public String getVideoCopertina() {
        return videoCopertina;
    }

    public void setVideoCopertina(String videoCopertina) {
        this.videoCopertina = videoCopertina;
    }

    public String getMappa() {
        return mappa;
    }

    public void setMappa(String mappa) {
        this.mappa = mappa;
    }

    @Override
    public String toString() {
        return "TrackPage{" +
                "mongoId='" + mongoId + '\'' +
                ", idTrack='" + idTrack + '\'' +
                ", curve=" + curve +
                ", bestLap=" + bestLap +
                ", descrizione='" + descrizione + '\'' +
                ", comunicazioni=" + comunicazioni +
                ", galleriaVideo=" + galleriaVideo +
                ", galleriaFoto=" + galleriaFoto +
                ", videoCopertina='" + videoCopertina + '\'' +
                ", mappa='" + mappa + '\'' +
                '}';
    }
}