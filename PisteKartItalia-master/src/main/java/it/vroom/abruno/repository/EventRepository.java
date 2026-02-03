package it.vroom.abruno.repository;

import it.vroom.abruno.classi.Event;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

@Repository
public class EventRepository {

    private final MongoCollection<Document> collection;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    public EventRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("karting_db");
        this.collection = database.getCollection("eventi");
    }

    // Metodo per trovare il massimo ID presente nella collezione
    public int findMaxId() {
        Document maxIdDoc = collection.find()
                .sort(Sorts.descending("id"))
                .projection(Projections.include("id"))
                .first();

        return maxIdDoc != null ? maxIdDoc.getInteger("id", 0) : 0;
    }

    public List<Event> findAll() {
        List<Event> events = new ArrayList<>();
        for (Document doc : collection.find()) {
            events.add(documentToEvent(doc));
        }
        return events;
    }

    public Event findByMongoId(String mongoId) {
        Document doc = collection.find(eq("_id", new ObjectId(mongoId))).first();
        return doc != null ? documentToEvent(doc) : null;
    }

    public Event findById(int id) {
        Document doc = collection.find(eq("id", id)).first();
        return doc != null ? documentToEvent(doc) : null;
    }

    public List<Event> findByIdOrganizzatore(int idOrganizzatore) {
        List<Event> events = new ArrayList<>();
        for (Document doc : collection.find(eq("idOrganizzatore", idOrganizzatore))) {
            events.add(documentToEvent(doc));
        }
        return events;
    }

    public List<Event> findByIdPista(int idPista) {
        List<Event> events = new ArrayList<>();
        for (Document doc : collection.find(eq("idPista", idPista))) {
            events.add(documentToEvent(doc));
        }
        return events;
    }

    public Event save(Event event) {
        // Se l'ID non è stato impostato (0), genera il prossimo ID progressivo
        if (event.getId() == 0) {
            int maxId = findMaxId();
            event.setId(maxId + 1);
        }

        Document doc = eventToDocument(event);
        collection.insertOne(doc);
        event.setMongoId(doc.getObjectId("_id").toString());
        return event;
    }

    public Event update(Event event) {
        Document doc = eventToDocument(event);
        collection.replaceOne(eq("_id", new ObjectId(event.getMongoId())), doc);
        return event;
    }

    public boolean delete(String mongoId) {
        return collection.deleteOne(eq("_id", new ObjectId(mongoId))).getDeletedCount() > 0;
    }

    // Conversione Document -> Event
    private Event documentToEvent(Document doc) {
        Event event = new Event();
        event.setMongoId(doc.getObjectId("_id").toString());
        event.setId(doc.getInteger("id"));
        event.setIdOrganizzatore(doc.getInteger("idOrganizzatore"));
        event.setIdPista(doc.getInteger("idPista"));
        event.setTitolo(doc.getString("titolo"));
        event.setDescrizione(doc.getString("descrizione"));
        event.setLinkEsterno(doc.getString("linkEsterno"));
        event.setFoto(doc.getString("foto"));
        event.setNote(doc.getString("note"));
        String dataStr = doc.getString("data");
        if (dataStr != null) {
            event.setData(LocalDateTime.parse(dataStr, DATE_FORMATTER));
        }
        return event;
    }

    // Conversione Event -> Document
    private Document eventToDocument(Event event) {
        Document doc = new Document();
        if (event.getMongoId() != null) {
            doc.put("_id", new ObjectId(event.getMongoId()));
        }
        doc.put("id", event.getId());
        doc.put("idOrganizzatore", event.getIdOrganizzatore());
        doc.put("idPista", event.getIdPista());
        doc.put("titolo", event.getTitolo());
        doc.put("descrizione", event.getDescrizione());
        doc.put("linkEsterno", event.getLinkEsterno());
        doc.put("foto", event.getFoto());
        doc.put("note", event.getNote());
        if (event.getData() != null) {
            doc.put("data", event.getData().format(DATE_FORMATTER));
        }
        return doc;
    }
}