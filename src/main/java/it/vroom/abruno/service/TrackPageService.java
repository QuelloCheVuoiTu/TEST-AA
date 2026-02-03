package it.vroom.abruno.service;

import it.vroom.abruno.classi.TrackPage;
import it.vroom.abruno.repository.TrackPageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;
import java.util.List;

@Service
public class TrackPageService {

    private final RestTemplate restTemplate;
    private final TrackPageRepository trackPageRepository;
    private final TrackService trackService;

    // URL interno del servizio immagini nel network Docker
    private final String INTERNAL_IMAGE_SERVICE_URL = "http://vroom-images:8181/api/images";

    @Autowired
    public TrackPageService(
            RestTemplate restTemplate,
            TrackPageRepository trackPageRepository,
            @Lazy TrackService trackService) {
        this.restTemplate = restTemplate;
        this.trackPageRepository = trackPageRepository;
        this.trackService = trackService;
    }

    // =========================================================================
    // METODI DI SOLA LETTURA (Invariati)
    // =========================================================================

    public List<TrackPage> getAllTrackPages() {
        return trackPageRepository.findAll();
    }

    public TrackPage getTrackPageByMongoId(String mongoId) {
        return trackPageRepository.findByMongoId(mongoId);
    }

    public TrackPage getTrackPageByIdTrack(int idTrack) {
        return trackPageRepository.findByIdTrack(idTrack);
    }

    // =========================================================================
    // METODI CRUD E DI MODIFICA (PROTETTI)
    // =========================================================================

    @PreAuthorize("hasRole('ADMIN')")
    public TrackPage createTrackPage(TrackPage trackPage) {
        return trackPageRepository.save(trackPage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public TrackPage createTrackPageWithImages(TrackPage trackPage, List<MultipartFile> galleriaFoto,
                                               MultipartFile mappaImage) throws IOException {
        if (galleriaFoto != null && !galleriaFoto.isEmpty()) {
            for (MultipartFile imageFile : galleriaFoto) {
                if (imageFile != null && !imageFile.isEmpty()) {
                    String imageUrl = uploadImageToServer(imageFile);
                    if (!trackPage.getGalleriaFoto().contains(imageUrl)) {
                        trackPage.getGalleriaFoto().add(imageUrl);
                    }
                }
            }
        }

        if (mappaImage != null && !mappaImage.isEmpty()) {
            String mappaUrl = uploadImageToServer(mappaImage);
            trackPage.setMappa(mappaUrl);
        }

        return trackPageRepository.save(trackPage);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#trackPage.idTrack))")
    public TrackPage updateTrackPage(TrackPage trackPage) {
        return trackPageRepository.updatePartial(trackPage);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#trackPage.idTrack))")
    public TrackPage updateTrackPageWithImages(TrackPage trackPage, List<MultipartFile> nuoveFoto,
                                               MultipartFile nuovaMappa) throws IOException {
        if (nuoveFoto != null && !nuoveFoto.isEmpty()) {
            for (MultipartFile imageFile : nuoveFoto) {
                if (imageFile != null && !imageFile.isEmpty()) {
                    String imageUrl = uploadImageToServer(imageFile);
                    if (!trackPage.getGalleriaFoto().contains(imageUrl)) {
                        trackPage.getGalleriaFoto().add(imageUrl);
                    }
                }
            }
        }

        if (nuovaMappa != null && !nuovaMappa.isEmpty()) {
            String mappaUrl = uploadImageToServer(nuovaMappa);
            trackPage.setMappa(mappaUrl);
        }

        return trackPageRepository.updatePartial(trackPage);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteTrackPage(String mongoId) {
        return trackPageRepository.delete(mongoId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteTrackPageByIdTrack(int idTrack) {
        return trackPageRepository.deleteByIdTrack(idTrack);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#trackPage.idTrack))")
    public void addFotoToGalleria(TrackPage trackPage, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = uploadImageToServer(imageFile);
            if (!trackPage.getGalleriaFoto().contains(imageUrl)) {
                trackPage.getGalleriaFoto().add(imageUrl);
                trackPageRepository.update(trackPage);
            }
        }
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#trackPage.idTrack))")
    public void addMultipleFotoToGalleria(TrackPage trackPage, List<MultipartFile> imageFiles) throws IOException {
        if (imageFiles != null && !imageFiles.isEmpty()) {
            for (MultipartFile imageFile : imageFiles) {
                if (imageFile != null && !imageFile.isEmpty()) {
                    String imageUrl = uploadImageToServer(imageFile);
                    if (!trackPage.getGalleriaFoto().contains(imageUrl)) {
                        trackPage.getGalleriaFoto().add(imageUrl);
                    }
                }
            }
            trackPageRepository.update(trackPage);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#trackPage.idTrack))")
    public void setMappaImage(TrackPage trackPage, MultipartFile imageFile) throws IOException {
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = uploadImageToServer(imageFile);
            trackPage.setMappa(imageUrl);
            trackPageRepository.update(trackPage);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#trackPage.idTrack))")
    public void removeFotoFromGalleria(TrackPage trackPage, String imageUrl) {
        if (trackPage.getGalleriaFoto() != null) {
            trackPage.getGalleriaFoto().remove(imageUrl);
            trackPageRepository.update(trackPage);
        }
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#trackPage.idTrack))")
    public void removeMappa(TrackPage trackPage) {
        trackPage.setMappa(null);
        trackPageRepository.update(trackPage);
    }

    // =========================================================================
    // METODO PRIVATO (ADATTATO PER DOCKER)
    // =========================================================================

    private String uploadImageToServer(MultipartFile imageFile) throws IOException {
        // 1. RECUPERA L'USERNAME DALL'AUTENTICAZIONE
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(imageFile.getInputStream(), imageFile.getOriginalFilename()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 2. AGGIUNGI L'HEADER REMOTE-USER PER IL SERVIZIO IMMAGINI
        headers.set("Remote-User", username);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 3. CHIAMATA AL CONTAINER vroom-images (Porta 8181)
        ResponseEntity<String> response = restTemplate.postForEntity(
                INTERNAL_IMAGE_SERVICE_URL + "/upload",
                requestEntity,
                String.class
        );

        // 4. RESTITUIAMO SOLO IL PATH RELATIVO (Es: /api/images/uuid.jpg)
        // Evitiamo di salvare "http://vroom-images:8181" nel DB perché il browser non saprebbe risolverlo
        return response.getBody();
    }
}