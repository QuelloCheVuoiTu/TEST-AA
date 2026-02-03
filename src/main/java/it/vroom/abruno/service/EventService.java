package it.vroom.abruno.service;

import it.vroom.abruno.classi.Event;
import it.vroom.abruno.repository.EventRepository;
import it.vroom.abruno.service.MultipartInputStreamFileResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpMethod;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TrackService trackService;

    // DEFINIZIONE URL INTERNO DOCKER (vroom-images è il nome del container sulla porta 8181)
    private final String INTERNAL_IMAGE_SERVICE_URL = "http://vroom-images:8181/api/images";

    // =========================================================================
    // METODI DI SOLA LETTURA (Pubblici - Invariati)
    // =========================================================================

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Event getEventByMongoId(String mongoId) {
        return eventRepository.findByMongoId(mongoId);
    }

    public Event getEventById(int id) {
        return eventRepository.findById(id);
    }

    public List<Event> getEventsByIdOrganizzatore(int idOrganizzatore) {
        return eventRepository.findByIdOrganizzatore(idOrganizzatore);
    }

    public List<Event> getEventsByIdPista(int idPista) {
        return eventRepository.findByIdPista(idPista);
    }

    public List<Event> searchByTitolo(String titolo) {
        List<Event> allEvents = eventRepository.findAll();
        return allEvents.stream()
                .filter(event -> event.getTitolo().toLowerCase().contains(titolo.toLowerCase()))
                .toList();
    }

    public List<Event> filterByDataMinima(LocalDateTime minData) {
        List<Event> allEvents = eventRepository.findAll();
        return allEvents.stream()
                .filter(event -> event.getData() != null && event.getData().isAfter(minData))
                .toList();
    }

    // =========================================================================
    // METODI DI SCRITTURA (PROTETTI CON PROPAGAZIONE SICUREZZA)
    // =========================================================================

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#event.idPista))")
    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    // Crea evento con immagine (Corretto per Docker)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#event.idPista))")
    public Event createEventWithImage(Event event, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(imageFile.getInputStream(), imageFile.getOriginalFilename()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Remote-User", username);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Chiamata al container vroom-images invece di localhost
            ResponseEntity<String> response = restTemplate.postForEntity(
                    INTERNAL_IMAGE_SERVICE_URL + "/upload", requestEntity, String.class
            );

            // Salviamo il path relativo restituito dal servizio immagini
            event.setFoto(response.getBody());
        }
        return eventRepository.save(event);
    }

    // Aggiorna evento (Corretto per Docker)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#updatedEvent.idPista))")
    public Event updateEvent(Event updatedEvent, MultipartFile newImageFile) throws IOException {
        Event existingEvent = eventRepository.findById(updatedEvent.getId());

        if (existingEvent == null) {
            throw new RuntimeException("Evento non trovato");
        }

        // LOGICA DI SICUREZZA PER IMMUTABILITÀ DI idPista
        if (existingEvent.getIdPista() != updatedEvent.getIdPista()) {
            boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));

            if (!isAdmin) {
                throw new AccessDeniedException("Solo gli amministratori possono modificare l'ID della pista associata all'evento.");
            }
        }

        String oldFoto = existingEvent.getFoto();
        updatedEvent.setMongoId(existingEvent.getMongoId());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        if (newImageFile != null && !newImageFile.isEmpty()) {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new MultipartInputStreamFileResource(newImageFile.getInputStream(), newImageFile.getOriginalFilename()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Remote-User", username);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Upload al nuovo indirizzo Docker
            ResponseEntity<String> response = restTemplate.postForEntity(
                    INTERNAL_IMAGE_SERVICE_URL + "/upload", requestEntity, String.class
            );

            updatedEvent.setFoto(response.getBody());

            // Elimina la vecchia immagine se esisteva (Operazione DELETE Protetta)
            if (oldFoto != null && !oldFoto.isEmpty()) {
                try {
                    String oldFilename = extractFilenameFromUrl(oldFoto);
                    if (oldFilename != null && !oldFilename.isEmpty()) {
                        HttpHeaders deleteHeaders = new HttpHeaders();
                        deleteHeaders.set("Remote-User", username);
                        HttpEntity<String> deleteEntity = new HttpEntity<>(deleteHeaders);

                        restTemplate.exchange(
                                INTERNAL_IMAGE_SERVICE_URL + "/" + oldFilename,
                                HttpMethod.DELETE,
                                deleteEntity,
                                Void.class
                        );
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            updatedEvent.setFoto(oldFoto);
        }

        return eventRepository.update(updatedEvent);
    }

    // Elimina evento (Corretto per Docker)
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteEvent(String mongoId) {
        Event event = eventRepository.findByMongoId(mongoId);
        if (event != null && event.getFoto() != null && !event.getFoto().isEmpty()) {
            try {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                String fotoUrl = event.getFoto();
                String filename = fotoUrl.substring(fotoUrl.lastIndexOf('/') + 1);

                // URL Docker per la cancellazione
                String deleteUrl = INTERNAL_IMAGE_SERVICE_URL + "/" + filename;

                HttpHeaders deleteHeaders = new HttpHeaders();
                deleteHeaders.set("Remote-User", username);
                HttpEntity<String> deleteEntity = new HttpEntity<>(deleteHeaders);

                restTemplate.exchange(deleteUrl, HttpMethod.DELETE, deleteEntity, Void.class);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return eventRepository.delete(mongoId);
    }

    private String extractFilenameFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        String path = url.split("\\?")[0];
        return path.substring(path.lastIndexOf('/') + 1);
    }
}