package it.vroom.abruno.pistekartitalia;

import it.vroom.abruno.classi.Event;
import it.vroom.abruno.classi.Track;
import it.vroom.abruno.classi.TrackPage;
import it.vroom.abruno.service.EventService;
import it.vroom.abruno.service.HomepageService;
import it.vroom.abruno.service.TrackPageService;
import it.vroom.abruno.service.TrackService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

// Import per Spring Security
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

// import necessari per invio immagini (mantenuti anche se non utilizzati direttamente qui)
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Arrays;

@Controller
public class PageController {

    private final TrackService trackService;
    private final EventService eventService;
    private final TrackPageService trackPageService;
    private final HomepageService homepageService;

    public PageController(TrackService trackService, EventService eventService,
                          TrackPageService trackPageService, HomepageService homepageService) {
        this.trackService = trackService;
        this.eventService = eventService;
        this.trackPageService = trackPageService;
        this.homepageService = homepageService;
    }

    //------------------------------------------------------------------------------------------------------
    // PAGINE VISITABILI (ACCESSO PUBBLICO)

    @GetMapping("/home")
    public String home(Model model) {
        System.out.println("=== DEBUG COMPLETO HOME ===");

        model.addAttribute("tracks", trackService.getAllTracks());

        List<Event> promotedEvents = homepageService.getPromotedEvents();

        List<Event> allEvents = eventService.getAllEvents();

        if (promotedEvents.isEmpty()) {
            if (!allEvents.isEmpty()) {
                promotedEvents = allEvents.stream()
                        .limit(2)
                        .collect(Collectors.toList());
            }
        }

        List<Track> allTracks = trackService.getAllTracks();

        Map<Integer, String> trackNames = allTracks.stream()
                .collect(Collectors.toMap(
                        Track::getId,
                        Track::getNome,
                        (existing, replacement) -> existing
                ));

        List<Map<String, Object>> simpleEvents = promotedEvents.stream()
                .map(event -> {
                    Map<String, Object> simpleEvent = new HashMap<>();
                    simpleEvent.put("id", event.getId());
                    simpleEvent.put("titolo", event.getTitolo());
                    simpleEvent.put("descrizione", event.getDescrizione());
                    simpleEvent.put("foto", event.getFoto());
                    simpleEvent.put("linkEsterno", event.getLinkEsterno());
                    simpleEvent.put("data", event.getData() != null ? event.getData().toString() : "N/D");
                    simpleEvent.put("idOrganizzatore", event.getIdOrganizzatore());
                    simpleEvent.put("idPista", event.getIdPista());
                    simpleEvent.put("trackName", trackNames.getOrDefault(event.getIdPista(), "Pista sconosciuta"));
                    return simpleEvent;
                })
                .collect(Collectors.toList());

        model.addAttribute("promotedEvents", simpleEvents);
        model.addAttribute("promotedVideos", homepageService.getPromotedVideos());

        System.out.println("=== FINE DEBUG HOME ===");
        return "home";
    }

    // PAGINA PISTA
    @GetMapping("/pista/{id}")
    public String mostraPista(@PathVariable int id, Model model) {
        Track pista = trackService.getTrackById(id);
        if (pista == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista non trovata");
        }
        model.addAttribute("track", pista);
        model.addAttribute("events", eventService.getEventsByIdPista(id));
        model.addAttribute("trackPage", trackPageService.getTrackPageByIdTrack(id));
        return "pista";
    }

    // Pagina informativa
    @GetMapping("/info")
    public String info() {
        return "info";
    }

    // Redirect root a home
    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    // PAGINA DI AMMINISTRAZIONE (Protetta da Spring Security - hasRole('ADMIN'))
    @GetMapping("/admin")
    public String admin(Model model, Authentication authentication) {
        System.out.println("=== INIZIO ADMIN ===");

        model.addAttribute("currentUser", authentication.getName());

        try {
            List<Track> tracks = new ArrayList<>();
            List<Event> events = new ArrayList<>();
            List<Event> promotedEvents = new ArrayList<>();
            List<String> promotedVideos = new ArrayList<>();
            List<Integer> promotedEventIds = new ArrayList<>();
            Map<Integer, String> trackNames = new HashMap<>();

            try {
                tracks = trackService.getAllTracks();
                if (tracks != null) {
                    for (Track track : tracks) {
                        if (track != null) {
                            trackNames.put(track.getId(), track.getNome());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading tracks: " + e.getMessage());
            }

            events = eventService.getAllEvents();
            promotedEvents = homepageService.getPromotedEvents();
            promotedEventIds = promotedEvents.stream().map(Event::getId).collect(Collectors.toList());
            promotedVideos = homepageService.getPromotedVideos();

            model.addAttribute("tracks", tracks != null ? tracks : new ArrayList<>());
            model.addAttribute("events", events != null ? events : new ArrayList<>());
            model.addAttribute("promotedEvents", promotedEvents != null ? promotedEvents : new ArrayList<>());
            model.addAttribute("promotedVideos", promotedVideos != null ? promotedVideos : new ArrayList<>());
            model.addAttribute("promotedEventIds", promotedEventIds != null ? promotedEventIds : new ArrayList<>());
            model.addAttribute("trackNames", trackNames);

            System.out.println("=== FINE ADMIN - Ritorno template ===");
            return "admin";

        } catch (Exception e) {
            System.err.println("FATAL ERROR in admin: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore caricamento dati admin", e);
        }
    }

    // EDIT PISTA (Protetta da hasAnyRole('ADMIN', 'TRACK_MANAGER'))
    // AGGIUNTO @PreAuthorize per proteggere la visualizzazione del form
    @GetMapping("/pista/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#id))")
    public String editPista(@PathVariable int id, Model model) {
        try {
            Track pista = trackService.getTrackById(id);
            if (pista == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista non trovata");
            }
            TrackPage trackPage = trackPageService.getTrackPageByIdTrack(id);
            if (trackPage == null) {
                trackPage = new TrackPage();
            }
            model.addAttribute("track", pista);
            model.addAttribute("trackPage", trackPage);
            return "edit-pista";
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a visualizzare il form di modifica.", ex);
        }
    }

    // UPDATE PISTA (Protetta da hasAnyRole('ADMIN', 'TRACK_MANAGER'))
    // AGGIUNTO @PreAuthorize per proteggere l'invio del form
    @PostMapping("/pista/{id}/edit")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#id))")
    public String updatePista(@PathVariable int id,
                              @ModelAttribute Track trackForm,
                              @ModelAttribute TrackPage trackPageForm) {
        try {
            System.out.println("=== UPDATE PISTA (Protetta da PreAuthorize) ===");

            Track existingTrack = trackService.getTrackById(id);
            if (existingTrack == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pista non trovata");
            }

            TrackPage existingTrackPage = trackPageService.getTrackPageByIdTrack(id);

            // 1. Aggiorna i campi del DB con i valori del form
            updateTrackFromForm(existingTrack, trackForm);

            // 2. Aggiorna TrackPage (creala se non esiste)
            if (existingTrackPage == null) {
                System.out.println("Creazione nuova TrackPage per ID: " + id);
                existingTrackPage = new TrackPage(id);
                updateTrackPageFromForm(existingTrackPage, trackPageForm);
                // La sicurezza è nel Service
                trackPageService.createTrackPage(existingTrackPage);
            } else {
                System.out.println("Aggiornamento TrackPage esistente");
                updateTrackPageFromForm(existingTrackPage, trackPageForm);
                // La sicurezza è nel Service
                trackPageService.updateTrackPage(existingTrackPage);
            }

            // 3. Salva le modifiche - USA UPDATE PARZIALE
            // La sicurezza è nel Service
            trackService.updateTrack(existingTrack);

            System.out.println("=== UPDATE COMPLETATO ===");
            return "redirect:/pista/" + id;

        } catch (AccessDeniedException ex) {
            // Cattura l'eccezione lanciata da @PreAuthorize nei Service
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a modificare questa pista.", ex);
        } catch (Exception e) {
            System.err.println("ERRORE in updatePista: " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore nell'aggiornamento: " + e.getMessage());
        }
    }

    // PAGINE DI TEST
    @GetMapping("/test/trackpage-images")
    public String showTestTrackPageImagesPage(Model model) {
        return "test-trackpage-images";
    }

    @GetMapping("/test/event-with-image")
    public String showTestEventWithImagePage(Model model) {
        return "test-event-image";
    }

    @PostMapping("/test/event-with-image")
    // AGGIORNATO: Protezione per il test di creazione evento con immagine
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idPista))")
    public String handleTestEventWithImage(
            @RequestParam String titolo,
            @RequestParam String descrizione,
            @RequestParam String data,
            @RequestParam int idOrganizzatore,
            @RequestParam int idPista,
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            Model model) throws IOException {

        try {
            Event event = new Event();
            event.setTitolo(titolo);
            event.setDescrizione(descrizione);
            event.setData(java.time.LocalDateTime.parse(data));
            event.setIdOrganizzatore(idOrganizzatore);
            event.setIdPista(idPista);

            Event createdEvent = eventService.createEventWithImage(event, imageFile);
            model.addAttribute("createdEvent", createdEvent);
            return "test-event-image";
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a creare eventi per questa pista.", ex);
        }
    }

    @GetMapping("/event/view/{id}")
    public String viewEventWithImage(@PathVariable int id, Model model) {
        Event event = eventService.getEventById(id);
        model.addAttribute("event", event);
        return "view-event-image";
    }

    //------------------------------------------------------------------------------------------------------
    // HELPER METHODS
    private void updateTrackFromForm(Track existingTrack, Track trackForm) {
        if (trackForm.getNome() != null) existingTrack.setNome(trackForm.getNome());
        if (trackForm.getCitta() != null) existingTrack.setCitta(trackForm.getCitta());
        if (trackForm.getRegione() != null) existingTrack.setRegione(trackForm.getRegione());
        if (trackForm.getGrado() != null) existingTrack.setGrado(trackForm.getGrado());
        if (trackForm.getLunghezza() != 0) existingTrack.setLunghezza(trackForm.getLunghezza());
        if (trackForm.getCapienza() != 0) existingTrack.setCapienza(trackForm.getCapienza());
        if (trackForm.getNote() != null) existingTrack.setNote(trackForm.getNote());
        if (trackForm.getLatitudine() != 0.0) existingTrack.setLatitudine(trackForm.getLatitudine());
        if (trackForm.getLongitudine() != 0.0) existingTrack.setLongitudine(trackForm.getLongitudine());
    }

    private void updateTrackPageFromForm(TrackPage existingTrackPage, TrackPage trackPageForm) {
        if (trackPageForm.getDescrizione() != null) existingTrackPage.setDescrizione(trackPageForm.getDescrizione());
        if (trackPageForm.getCurve() != null) existingTrackPage.setCurve(trackPageForm.getCurve());
        if (trackPageForm.getBestLap() != null) existingTrackPage.setBestLap(trackPageForm.getBestLap());
        if (trackPageForm.getVideoCopertina() != null) existingTrackPage.setVideoCopertina(trackPageForm.getVideoCopertina());
        // La mappa e la galleria sono gestite tramite API/POST separati, non dal form principale
    }

    //------------------------------------------------------------------------------------------------------
    // API REST HOMEPAGE
    // Le protezioni esistenti (@PreAuthorize("hasRole('ADMIN')")) sono corrette.
    @GetMapping("/api/homepage/promoted-events")
    @ResponseBody
    public List<Event> getPromotedEventsApi() {
        return homepageService.getPromotedEvents();
    }

    @PostMapping("/api/homepage/promoted-events")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void replacePromotedEventsApi(@RequestBody List<Integer> newEvents) {
        homepageService.replacePromotedEvents(newEvents);
    }

    @PutMapping("/api/homepage/promoted-events/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void addPromotedEventApi(@PathVariable int eventId) {
        homepageService.addPromotedEvent(eventId);
    }

    @DeleteMapping("/api/homepage/promoted-events/{eventId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void removePromotedEventApi(@PathVariable int eventId) {
        homepageService.removePromotedEvent(eventId);
    }

    @GetMapping("/api/homepage/promoted-videos")
    @ResponseBody
    public List<String> getPromotedVideosApi() {
        return homepageService.getPromotedVideos();
    }

    @PostMapping("/api/homepage/promoted-videos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void replacePromotedVideosApi(@RequestBody List<String> newVideos) {
        homepageService.replacePromotedVideos(newVideos);
    }

    @PutMapping("/api/homepage/promoted-videos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void addPromotedVideoApi(@RequestBody String link) {
        homepageService.addPromotedVideo(link);
    }

    @DeleteMapping("/api/homepage/promoted-videos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void removePromotedVideoApi(@RequestBody String link) {
        homepageService.removePromotedVideo(link);
    }

    @PostMapping("/api/homepage/videos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void addVideoSimpleApi(@RequestParam String link) {
        homepageService.addPromotedVideo(link);
    }

    @DeleteMapping("/api/homepage/videos")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public void removeVideoSimpleApi(@RequestParam String link) {
        homepageService.removePromotedVideo(link);
    }

    //------------------------------------------------------------------------------------------------------
    // API REST TRACK
    @GetMapping("/api/tracks")
    @ResponseBody
    public List<Track> getAllTracksApi() {
        return trackService.getAllTracks();
    }

    @GetMapping("/api/tracks/{id}")
    @ResponseBody
    public Track getTrackByIdApi(@PathVariable int id) {
        return trackService.getTrackById(id);
    }

    @GetMapping("/api/tracks/regione/{regione}")
    @ResponseBody
    public List<Track> getTracksByRegioneApi(@PathVariable String regione) {
        return trackService.getTracksByRegione(regione);
    }

    @PostMapping("/api/tracks")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public Track createTrackApi(@RequestBody Track track) {
        return trackService.createTrack(track);
    }

    @PutMapping("/api/tracks")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#track.id))")
    @ResponseBody
    public Track updateTrackApi(@RequestBody Track track) {
        try {
            return trackService.updateTrack(track);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad aggiornare la pista.", ex);
        }
    }

    @DeleteMapping("/api/tracks/{mongoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public boolean deleteTrackApi(@PathVariable String mongoId) {
        return trackService.deleteTrack(mongoId);
    }

    //------------------------------------------------------------------------------------------------------
    // API REST TRACKPAGE
    // Le protezioni sono già corrette e complete qui.

    @GetMapping("/api/trackpages")
    @ResponseBody
    public List<TrackPage> getAllTrackPagesApi() {
        return trackPageService.getAllTrackPages();
    }

    @GetMapping("/api/trackpages/track/{idTrack}")
    @ResponseBody
    public TrackPage getTrackPageByIdTrackApi(@PathVariable int idTrack) {
        TrackPage result = trackPageService.getTrackPageByIdTrack(idTrack);
        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovato per idTrack: " + idTrack);
        }
        return result;
    }

    @PostMapping("/api/trackpages/with-images")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public TrackPage createTrackPageWithImagesApi(
            @RequestPart("trackPage") TrackPage trackPage,
            @RequestPart(value = "galleriaFoto", required = false) MultipartFile[] galleriaFoto,
            @RequestPart(value = "mappa", required = false) MultipartFile mappa) throws IOException {

        try {
            List<MultipartFile> fotoList = galleriaFoto != null ? Arrays.asList(galleriaFoto) : null;
            // Nota: il Service usa createTrackPageWithImages che è protetto da ADMIN.
            return trackPageService.createTrackPageWithImages(trackPage, fotoList, mappa);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a creare TrackPage.", ex);
        }
    }

    // API PER GESTIONE IMMAGINI TRACKPAGE (PER PAGINA DI TEST)
    @PostMapping("/trackpage/{idTrack}/add-foto")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage addFotoToTrackPageTest(
            @PathVariable int idTrack,
            @RequestParam("foto") MultipartFile[] fotoFiles) throws IOException {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.addMultipleFotoToGalleria(trackPage, Arrays.asList(fotoFiles));
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad aggiungere foto a questa pista.", ex);
        }
    }
    // ... (altri metodi di test e API REST TrackPage) ...

    @PostMapping("/trackpage/{idTrack}/update-mappa")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage updateMappaTest(
            @PathVariable int idTrack,
            @RequestParam("mappa") MultipartFile mappaFile) throws IOException {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.setMappaImage(trackPage, mappaFile);
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad aggiornare la mappa.", ex);
        }
    }

    @DeleteMapping("/trackpage/{idTrack}/remove-foto")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage removeFotoFromGalleriaTest(
            @PathVariable int idTrack,
            @RequestParam String imageUrl) {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.removeFotoFromGalleria(trackPage, imageUrl);
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a rimuovere la foto.", ex);
        }
    }

    @DeleteMapping("/trackpage/{idTrack}/remove-mappa")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage removeMappaTest(@PathVariable int idTrack) {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.removeMappa(trackPage);
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a rimuovere la mappa.", ex);
        }
    }

    // API REST PER GESTIONE IMMAGINI TRACKPAGE (PER CHIAMATE API)
    @PostMapping("/api/trackpages/{idTrack}/add-foto")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage addFotoToTrackPageApi(
            @PathVariable int idTrack,
            @RequestParam("foto") MultipartFile[] fotoFiles) throws IOException {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.addMultipleFotoToGalleria(trackPage, Arrays.asList(fotoFiles));
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad aggiungere foto a questa pista.", ex);
        }
    }

    @PostMapping("/api/trackpages/{idTrack}/update-mappa")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage updateMappaApi(
            @PathVariable int idTrack,
            @RequestParam("mappa") MultipartFile mappaFile) throws IOException {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.setMappaImage(trackPage, mappaFile);
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad aggiornare la mappa.", ex);
        }
    }

    @DeleteMapping("/api/trackpages/{idTrack}/remove-foto")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage removeFotoFromGalleriaApi(
            @PathVariable int idTrack,
            @RequestParam String imageUrl) {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.removeFotoFromGalleria(trackPage, imageUrl);
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a rimuovere la foto.", ex);
        }
    }

    @DeleteMapping("/api/trackpages/{idTrack}/remove-mappa")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage removeMappaApi(@PathVariable int idTrack) {

        TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);
        if (trackPage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
        }

        try {
            trackPageService.removeMappa(trackPage);
            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a rimuovere la mappa.", ex);
        }
    }

    // API PER GESTIONE VIDEO TRACKPAGE (PER PAGINA DI EDIT)
    @PostMapping("/trackpage/{idTrack}/add-video")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage addVideoToTrackPageTest(
            @PathVariable int idTrack,
            @RequestBody Map<String, String> request) {

        try {
            String videoUrl = request.get("videoUrl");
            TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);

            if (trackPage == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
            }

            if (trackPage.getGalleriaVideo() == null) {
                trackPage.setGalleriaVideo(new ArrayList<>());
            }

            if (!trackPage.getGalleriaVideo().contains(videoUrl)) {
                trackPage.getGalleriaVideo().add(videoUrl);
                trackPageService.updateTrackPage(trackPage);
            }

            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad aggiungere video.", ex);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore nell'aggiunta del video: " + e.getMessage());
        }
    }

    @DeleteMapping("/trackpage/{idTrack}/remove-video")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idTrack))")
    @ResponseBody
    public TrackPage removeVideoFromGalleriaTest(
            @PathVariable int idTrack,
            @RequestBody Map<String, String> request) {

        try {
            String videoUrl = request.get("videoUrl");
            TrackPage trackPage = trackPageService.getTrackPageByIdTrack(idTrack);

            if (trackPage == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TrackPage non trovata per idTrack: " + idTrack);
            }

            trackPage.getGalleriaVideo().remove(videoUrl);
            trackPageService.updateTrackPage(trackPage);

            return trackPage;
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a rimuovere video.", ex);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore nella rimozione del video: " + e.getMessage());
        }
    }
    //------------------------------------------------------------------------------------------------------
    // API REST EVENT
    @GetMapping("/api/events")
    @ResponseBody
    public List<Event> getAllEventsApi() {
        return eventService.getAllEvents();
    }

    @GetMapping("/api/events/{id}")
    @ResponseBody
    public Event getEventByIdApi(@PathVariable int id) {
        return eventService.getEventById(id);
    }

    @GetMapping("/api/events/pista/{idPista}")
    @ResponseBody
    public List<Event> getEventsByIdPistaApi(@PathVariable int idPista) {
        return eventService.getEventsByIdPista(idPista);
    }

    @GetMapping("/api/events/organizzatore/{idOrganizzatore}")
    @ResponseBody
    public List<Event> getEventsByIdOrganizzatoreApi(@PathVariable int idOrganizzatore) {
        return eventService.getEventsByIdOrganizzatore(idOrganizzatore);
    }

    @GetMapping("/api/events/search")
    @ResponseBody
    public List<Event> searchEventsByTitoloApi(@RequestParam String titolo) {
        return eventService.searchByTitolo(titolo);
    }

    @PostMapping("/api/events")
    // AGGIORNATO: Protezione per creazione evento
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#event.idPista))")
    @ResponseBody
    public Event createEventApi(@RequestBody Event event) {
        try {
            return eventService.createEvent(event);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a creare l'evento.", ex);
        }
    }

    @PostMapping("/api/events/simple")
    // AGGIORNATO: Protezione per creazione evento semplice
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#idPista))")
    @ResponseBody
    public Event createEventSimpleApi(
            @RequestParam String titolo,
            @RequestParam String descrizione,
            @RequestParam String data,
            @RequestParam int idOrganizzatore,
            @RequestParam int idPista) {

        Event event = new Event();
        event.setTitolo(titolo);
        event.setDescrizione(descrizione);
        event.setData(java.time.LocalDateTime.parse(data));
        event.setIdOrganizzatore(idOrganizzatore);
        event.setIdPista(idPista);

        try {
            return eventService.createEvent(event);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a creare l'evento.", ex);
        }
    }

    @PostMapping("/api/events/with-image")
    // AGGIORNATO: Protezione per creazione evento con immagine
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#event.idPista))")
    @ResponseBody
    public Event createEventWithImageApi(
            @RequestPart("event") Event event,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {
        try {
            return eventService.createEventWithImage(event, imageFile);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a creare l'evento.", ex);
        }
    }

    // NUOVO METODO: AGGIUNTO UPDATE EVENT API
    @PutMapping("/api/events")
    // Protezione per l'aggiornamento evento (verifica che l'utente possa modificare la pista specificata nell'oggetto 'event')
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#event.idPista))")
    @ResponseBody
    public Event updateEventApi(
            @RequestPart("event") Event event,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) throws IOException {
        try {
            // Il Service gestisce l'aggiornamento, l'immagine e la verifica di immutabilità di idPista
            return eventService.updateEvent(event, imageFile);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato ad aggiornare l'evento.", ex);
        }
    }

    @DeleteMapping("/api/events/{mongoId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseBody
    public boolean deleteEventApi(@PathVariable String mongoId) {
        try {
            // Il Service gestisce l'eliminazione dell'immagine associata
            return eventService.deleteEvent(mongoId);
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a eliminare l'evento.", ex);
        }
    }
}