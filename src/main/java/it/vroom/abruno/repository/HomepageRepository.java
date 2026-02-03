package it.vroom.abruno.repository;

import it.vroom.abruno.classi.Homepage;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class HomepageRepository {
    private final MongoCollection<Document> collection;

    @Autowired
    public HomepageRepository(MongoClient mongoClient) {
        MongoDatabase database = mongoClient.getDatabase("karting_db");
        this.collection = database.getCollection("homepage");
    }

    public Homepage getHomepage() {
        Document doc = collection.find().first();
        return doc != null ? documentToHomepage(doc) : null;
    }

    public void saveHomepage(Homepage homepage) {
        Document doc = collection.find().first();
        Document newDoc = homepageToDocument(homepage);
        if (doc == null) {
            collection.insertOne(newDoc);
        } else {
            collection.replaceOne(new Document("_id", doc.getObjectId("_id")), newDoc);
        }
    }

    private Homepage documentToHomepage(Document doc) {
        Homepage homepage = new Homepage();
        homepage.setId(doc.getObjectId("_id").toString());

        // Gestione sicura delle liste
        List<Integer> promotedEvents = new ArrayList<>();
        List<?> events = doc.get("promotedEvents", List.class);
        if (events != null) {
            for (Object item : events) {
                if (item instanceof Integer) {
                    promotedEvents.add((Integer) item);
                } else if (item instanceof Number) {
                    promotedEvents.add(((Number) item).intValue());
                }
            }
        }
        homepage.setPromotedEvents(promotedEvents);

        List<String> promotedVideos = new ArrayList<>();
        List<?> videos = doc.get("promotedVideos", List.class);
        if (videos != null) {
            for (Object item : videos) {
                if (item instanceof String) {
                    promotedVideos.add((String) item);
                }
            }
        }
        homepage.setPromotedVideos(promotedVideos);

        return homepage;
    }

    private Document homepageToDocument(Homepage homepage) {
        Document doc = new Document();
        if (homepage.getId() != null && !homepage.getId().isEmpty()) {
            doc.put("_id", new ObjectId(homepage.getId()));
        }
        doc.put("promotedEvents", homepage.getPromotedEvents() != null ? homepage.getPromotedEvents() : new ArrayList<>());
        doc.put("promotedVideos", homepage.getPromotedVideos() != null ? homepage.getPromotedVideos() : new ArrayList<>());
        return doc;
    }
}