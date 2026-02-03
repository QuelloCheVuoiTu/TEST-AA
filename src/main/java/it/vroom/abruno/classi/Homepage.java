package it.vroom.abruno.classi;

import java.util.List;

public class Homepage {
    private String id; // MongoDB ObjectId
    private List<Integer> promotedEvents; // id progressivi degli eventi
    private List<String> promotedVideos;  // link video

    // Getter e Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<Integer> getPromotedEvents() { return promotedEvents; }
    public void setPromotedEvents(List<Integer> promotedEvents) { this.promotedEvents = promotedEvents; }
    public List<String> getPromotedVideos() { return promotedVideos; }
    public void setPromotedVideos(List<String> promotedVideos) { this.promotedVideos = promotedVideos; }
}
