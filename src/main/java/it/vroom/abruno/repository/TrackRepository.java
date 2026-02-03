package it.vroom.abruno.repository;

import it.vroom.abruno.classi.Track;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

@Repository
public class TrackRepository {

    private final MongoCollection<Document> collection;

    @Autowired
    public TrackRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("karting_db");
        this.collection = database.getCollection("kartodromi");
    }

    // Trova tutti i track
    public List<Track> findAll() {
        List<Track> tracks = new ArrayList<>();
        for (Document doc : collection.find()) {
            tracks.add(documentToTrack(doc));
        }
        return tracks;
    }

    // Trova per MongoDB ID
    public Track findByMongoId(String mongoId) {
        Document doc = collection.find(eq("_id", new ObjectId(mongoId))).first();
        return doc != null ? documentToTrack(doc) : null;
    }

    // Trova per ID progressivo
    public Track findById(int id) {
        Document doc = collection.find(eq("id", id)).first();
        return doc != null ? documentToTrack(doc) : null;
    }

    // Trova per regione
    public List<Track> findByRegione(String regione) {
        List<Track> tracks = new ArrayList<>();
        for (Document doc : collection.find(eq("regione", regione))) {
            tracks.add(documentToTrack(doc));
        }
        return tracks;
    }

    // Trova per grado
    public List<Track> findByGrado(String grado) {
        List<Track> tracks = new ArrayList<>();
        for (Document doc : collection.find(eq("grado", grado))) {
            tracks.add(documentToTrack(doc));
        }
        return tracks;
    }

    // Trova per città
    public List<Track> findByCitta(String citta) {
        List<Track> tracks = new ArrayList<>();
        for (Document doc : collection.find(eq("citta", citta))) {
            tracks.add(documentToTrack(doc));
        }
        return tracks;
    }

    // Salva nuovo track con id progressivo
    public Track save(Track track) {
        // Trova il massimo id attuale
        Document last = collection.find().sort(new Document("id", -1)).first();
        int nextId = (last != null && last.get("id") != null) ? last.getInteger("id") + 1 : 1;
        track.setId(nextId);

        Document doc = trackToDocument(track);
        collection.insertOne(doc);

        // Recupera l'ID generato da MongoDB
        track.setMongoId(doc.getObjectId("_id").toString());
        return track;
    }

    // Aggiorna track per ID progressivo (MODIFICATO)
    public Track update(Track track) {
        // Usa l'ID progressivo invece del mongoId
        Document doc = trackToDocument(track);
        collection.replaceOne(eq("id", track.getId()), doc);
        return track;
    }

    // Aggiornamento parziale per ID progressivo (NUOVO METODO)
    public Track updatePartial(Track track) {
        Document updateDoc = new Document();

        if (track.getNome() != null) updateDoc.put("nome", track.getNome());
        if (track.getCitta() != null) updateDoc.put("citta", track.getCitta());
        if (track.getRegione() != null) updateDoc.put("regione", track.getRegione());
        if (track.getGrado() != null) updateDoc.put("grado", track.getGrado());
        if (track.getLunghezza() != 0) updateDoc.put("lunghezza", track.getLunghezza());
        if (track.getCapienza() != 0) updateDoc.put("capienza", track.getCapienza());
        if (track.getNote() != null) updateDoc.put("note", track.getNote());
        if (track.getLatitudine() != 0.0) updateDoc.put("latitudine", track.getLatitudine());
        if (track.getLongitudine() != 0.0) updateDoc.put("longitudine", track.getLongitudine());

        if (!updateDoc.isEmpty()) {
            collection.updateOne(eq("id", track.getId()), new Document("$set", updateDoc));
        }

        return findById(track.getId());
    }

    // Elimina track
    public boolean delete(String mongoId) {
        return collection.deleteOne(eq("_id", new ObjectId(mongoId))).getDeletedCount() > 0;
    }

    // Conversione Document -> Track
    private Track documentToTrack(Document doc) {
        Track track = new Track();
        track.setMongoId(doc.getObjectId("_id").toString());
        track.setId(doc.getInteger("id"));
        track.setNome(doc.getString("nome"));
        track.setCitta(doc.getString("citta"));
        track.setRegione(doc.getString("regione"));
        track.setGrado(doc.getString("grado"));
        track.setLunghezza(doc.getInteger("lunghezza"));
        track.setCapienza(doc.getInteger("capienza"));
        track.setLatitudine(doc.getDouble("latitudine"));
        track.setLongitudine(doc.getDouble("longitudine"));
        track.setNote(doc.getString("note"));
        return track;
    }

    // Conversione Track -> Document
    private Document trackToDocument(Track track) {
        Document doc = new Document();
        if (track.getMongoId() != null) {
            doc.put("_id", new ObjectId(track.getMongoId()));
        }
        doc.put("id", track.getId());
        doc.put("nome", track.getNome());
        doc.put("citta", track.getCitta());
        doc.put("regione", track.getRegione());
        doc.put("grado", track.getGrado());
        doc.put("lunghezza", track.getLunghezza());
        doc.put("capienza", track.getCapienza());
        doc.put("latitudine", track.getLatitudine());
        doc.put("longitudine", track.getLongitudine());
        doc.put("note", track.getNote());
        return doc;
    }
}