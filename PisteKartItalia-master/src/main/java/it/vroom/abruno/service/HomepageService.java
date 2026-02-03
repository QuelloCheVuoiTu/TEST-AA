package it.vroom.abruno.service;

import it.vroom.abruno.classi.Homepage;
import it.vroom.abruno.classi.Event;
import it.vroom.abruno.repository.HomepageRepository;
import it.vroom.abruno.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize; // Import necessario per la sicurezza
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HomepageService {

    @Autowired
    private HomepageRepository homepageRepository;

    @Autowired
    private EventRepository eventRepository;

    // =========================================================================
    // METODI DI LETTURA (PUBBLICI - Accessibili a tutti)
    // =========================================================================

    /**
     * Ottiene la lista degli eventi promossi dalla homepage (lettura pubblica).
     */
    public List<Event> getPromotedEvents() {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage == null || homepage.getPromotedEvents() == null) return new ArrayList<>();
        List<Event> events = new ArrayList<>();
        for (Integer id : homepage.getPromotedEvents()) {
            // Assicurati che l'EventRepository abbia un metodo findById(int id)
            Event e = eventRepository.findById(id);
            if (e != null) events.add(e);
        }
        return events;
    }

    /**
     * Ottiene la lista dei link video promossi dalla homepage (lettura pubblica).
     */
    public List<String> getPromotedVideos() {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage == null || homepage.getPromotedVideos() == null) return new ArrayList<>();
        return homepage.getPromotedVideos();
    }

    // =========================================================================
    // METODI DI MODIFICA (PROTETTI - Accessibili solo a ROLE_ADMIN)
    // =========================================================================

    /**
     * Aggiunge un Evento promosso. Accesso riservato a ROLE_ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void addPromotedEvent(int eventId) {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage == null) homepage = new Homepage();
        if (homepage.getPromotedEvents() == null) homepage.setPromotedEvents(new ArrayList<>());
        if (!homepage.getPromotedEvents().contains(eventId)) homepage.getPromotedEvents().add(eventId);
        homepageRepository.saveHomepage(homepage);
    }

    /**
     * Rimuove un Evento promosso. Accesso riservato a ROLE_ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void removePromotedEvent(int eventId) {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage != null && homepage.getPromotedEvents() != null) {
            homepage.getPromotedEvents().removeIf(id -> id.equals(eventId)); // Usato .equals() per integrità Integer
            homepageRepository.saveHomepage(homepage);
        }
    }

    /**
     * Sostituisce l'intera lista di Eventi promossi. Accesso riservato a ROLE_ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void replacePromotedEvents(List<Integer> newEvents) {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage == null) homepage = new Homepage();
        homepage.setPromotedEvents(newEvents != null ? newEvents : new ArrayList<>());
        homepageRepository.saveHomepage(homepage);
    }

    /**
     * Aggiunge un Video promosso. Accesso riservato a ROLE_ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void addPromotedVideo(String link) {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage == null) homepage = new Homepage();
        if (homepage.getPromotedVideos() == null) homepage.setPromotedVideos(new ArrayList<>());
        if (!homepage.getPromotedVideos().contains(link)) homepage.getPromotedVideos().add(link);
        homepageRepository.saveHomepage(homepage);
    }

    /**
     * Rimuove un Video promosso. Accesso riservato a ROLE_ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void removePromotedVideo(String link) {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage != null && homepage.getPromotedVideos() != null) {
            homepage.getPromotedVideos().removeIf(l -> l.equals(link));
            homepageRepository.saveHomepage(homepage);
        }
    }

    /**
     * Sostituisce l'intera lista di Video promossi. Accesso riservato a ROLE_ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void replacePromotedVideos(List<String> newVideos) {
        Homepage homepage = homepageRepository.getHomepage();
        if (homepage == null) homepage = new Homepage();
        homepage.setPromotedVideos(newVideos != null ? newVideos : new ArrayList<>());
        homepageRepository.saveHomepage(homepage);
    }
}