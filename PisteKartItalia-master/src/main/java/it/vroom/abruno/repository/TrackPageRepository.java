package it.vroom.abruno.repository;

import it.vroom.abruno.classi.TrackPage;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

@Repository
public class TrackPageRepository {

    private final MongoCollection<Document> collection;

    @Autowired
    public TrackPageRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("karting_db");
        this.collection = database.getCollection("track_pages");
    }

    // Trova tutti i track pages
    public List<TrackPage> findAll() {
        List<TrackPage> trackPages = new ArrayList<>();
        for (Document doc : collection.find()) {
            trackPages.add(documentToTrackPage(doc));
        }
        return trackPages;
    }

    // Trova per MongoDB ID
    public TrackPage findByMongoId(String mongoId) {
        Document doc = collection.find(eq("_id", new ObjectId(mongoId))).first();
        return doc != null ? documentToTrackPage(doc) : null;
    }

    // Trova per idTrack
    public TrackPage findByIdTrack(int idTrack) {
        Document doc = collection.find(eq("idTrack", idTrack)).first();
        return doc != null ? documentToTrackPage(doc) : null;
    }

    // Salva nuovo track page
    public TrackPage save(TrackPage trackPage) {
        Document doc = trackPageToDocument(trackPage);
        collection.insertOne(doc);

        // Recupera l'ID generato da MongoDB
        trackPage.setMongoId(doc.getObjectId("_id").toString());
        return trackPage;
    }

    // Aggiorna track page per idTrack (MODIFICATO)
    public TrackPage update(TrackPage trackPage) {
        // Usa idTrack invece del mongoId
        Document doc = trackPageToDocument(trackPage);
        collection.replaceOne(eq("idTrack", trackPage.getIdTrack()), doc);
        return trackPage;
    }

    // Aggiorna solo i campi non nulli presenti nell'oggetto passato, lasciando invariati gli altri campi del documento. Usa
    public TrackPage updatePartial(TrackPage trackPage) {
        Document updateDoc = new Document();

        if (trackPage.getDescrizione() != null) updateDoc.put("descrizione", trackPage.getDescrizione());
        if (trackPage.getCurve() != null) updateDoc.put("curve", trackPage.getCurve());
        if (trackPage.getBestLap() != null) updateDoc.put("bestLap", trackPage.getBestLap());
        if (trackPage.getVideoCopertina() != null) updateDoc.put("videoCopertina", trackPage.getVideoCopertina());
        if (trackPage.getMappa() != null) updateDoc.put("mappa", trackPage.getMappa());

        // Per le liste, aggiorna solo se non nulle
        if (trackPage.getComunicazioni() != null) updateDoc.put("comunicazioni", trackPage.getComunicazioni());
        if (trackPage.getGalleriaVideo() != null) updateDoc.put("galleriaVideo", trackPage.getGalleriaVideo());
        if (trackPage.getGalleriaFoto() != null) updateDoc.put("galleriaFoto", trackPage.getGalleriaFoto());

        if (!updateDoc.isEmpty()) {
            collection.updateOne(eq("idTrack", trackPage.getIdTrack()), new Document("$set", updateDoc));
        }

        return findByIdTrack(trackPage.getIdTrack());
    }

    // Elimina track page
    public boolean delete(String mongoId) {
        return collection.deleteOne(eq("_id", new ObjectId(mongoId))).getDeletedCount() > 0;
    }

    // Elimina per idTrack
    public boolean deleteByIdTrack(int idTrack) {
        return collection.deleteOne(eq("idTrack", idTrack)).getDeletedCount() > 0;
    }

    // Conversione Document -> TrackPage (MODIFICATO - gestione liste nulle)
    private TrackPage documentToTrackPage(Document doc) {
        TrackPage trackPage = new TrackPage();
        trackPage.setMongoId(doc.getObjectId("_id").toString());
        trackPage.setIdTrack(doc.getInteger("idTrack"));
        trackPage.setCurve(doc.getInteger("curve"));
        trackPage.setBestLap(doc.getInteger("bestLap"));
        trackPage.setDescrizione(doc.getString("descrizione"));

        // GESTIONE LISTE - Se sono null, inizializza liste vuote
        List<String> comunicazioni = doc.getList("comunicazioni", String.class);
        trackPage.setComunicazioni(comunicazioni != null ? comunicazioni : new ArrayList<>());

        List<String> galleriaVideo = doc.getList("galleriaVideo", String.class);
        trackPage.setGalleriaVideo(galleriaVideo != null ? galleriaVideo : new ArrayList<>());

        List<String> galleriaFoto = doc.getList("galleriaFoto", String.class);
        trackPage.setGalleriaFoto(galleriaFoto != null ? galleriaFoto : new ArrayList<>());

        trackPage.setVideoCopertina(doc.getString("videoCopertina"));
        trackPage.setMappa(doc.getString("mappa"));
        return trackPage;
    }

    // Conversione TrackPage -> Document
    private Document trackPageToDocument(TrackPage trackPage) {
        Document doc = new Document();
        if (trackPage.getMongoId() != null) {
            doc.put("_id", new ObjectId(trackPage.getMongoId()));
        }
        doc.put("idTrack", trackPage.getIdTrack());
        doc.put("curve", trackPage.getCurve());
        doc.put("bestLap", trackPage.getBestLap());
        doc.put("descrizione", trackPage.getDescrizione());
        doc.put("comunicazioni", trackPage.getComunicazioni());
        doc.put("galleriaVideo", trackPage.getGalleriaVideo());
        doc.put("galleriaFoto", trackPage.getGalleriaFoto());
        doc.put("videoCopertina", trackPage.getVideoCopertina());
        doc.put("mappa", trackPage.getMappa());
        return doc;
    }
}