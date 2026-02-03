package it.vroom.abruno.service;

import it.vroom.abruno.classi.Track;
import it.vroom.abruno.classi.TrackPage;
import it.vroom.abruno.classi.TrackManagerMapping;
import it.vroom.abruno.repository.TrackRepository;
import it.vroom.abruno.repository.TrackManagerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrackService {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private TrackPageService trackPageService;

    // NUOVA INIEZIONE PER LA LOGICA DINAMICA
    @Autowired
    private TrackManagerRepository trackManagerRepository;

    // =========================================================================
    // LOGICA DINAMICA DI AUTORIZZAZIONE
    // =========================================================================

    /**
     * Verifica se l'utente autenticato (Remote-User/Email) è autorizzato a modificare la pista data.
     * Questa logica legge le mappature dal database (TrackManagerMapping).
     * @param trackId L'ID della pista da verificare.
     * @return true se l'utente è mappato a quella pista, false altrimenti.
     */
    public boolean userCanEditTrack(int trackId) {
        // 1. Ottieni l'username/email autenticato da Authelia
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 2. Interroga il database di mappatura
        List<TrackManagerMapping> mappings = trackManagerRepository.findByUsername(username);

        // 3. Verifica se l'ID della pista richiesta è presente tra le piste gestite
        boolean canEdit = mappings.stream()
                .anyMatch(mapping -> mapping.getIdTrack() == trackId);

        // Opzionale: Log per debug
        System.out.println("Verifica permessi pista " + trackId + " per utente " + username + ": " + (canEdit ? "AUTORIZZATO" : "NEGATO"));

        return canEdit;
    }

    // =========================================================================
    // METODI CRUD E RICERCA (CON CONTROLLI DI SICUREZZA)
    // =========================================================================

    // Ottieni tutti i track (Pubblico)
    public List<Track> getAllTracks() {
        return trackRepository.findAll();
    }

    // Ottieni track per MongoDB ID (Pubblico)
    public Track getTrackByMongoId(String mongoId) {
        return trackRepository.findByMongoId(mongoId);
    }

    // Ottieni track per ID progressivo (Pubblico)
    public Track getTrackById(int id) {
        return trackRepository.findById(id);
    }

    // Ottieni track per regione (Pubblico)
    public List<Track> getTracksByRegione(String regione) {
        return trackRepository.findByRegione(regione);
    }

    // Ottieni track per grado (Pubblico)
    public List<Track> getTracksByGrado(String grado) {
        return trackRepository.findByGrado(grado);
    }

    // Ottieni track per città (Pubblico)
    public List<Track> getTracksByCitta(String citta) {
        return trackRepository.findByCitta(citta);
    }

    // Crea nuovo track (Protetta: richiede ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    public Track createTrack(Track track) {
        Track savedTrack = trackRepository.save(track);

        // Crea automaticamente un TrackPage associato usando l'ID progressivo
        TrackPage trackPage = new TrackPage(savedTrack.getId());
        trackPageService.createTrackPage(trackPage);

        return savedTrack;
    }

    // Aggiorna track (Protetta: richiede ADMIN O (TRACK_MANAGER che gestisce la pista))
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TRACK_MANAGER') and @trackService.userCanEditTrack(#track.id))")
    public Track updateTrack(Track track) {
        return trackRepository.updatePartial(track);
    }

    // Elimina track (Protetta: richiede ROLE_ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteTrack(String mongoId) {
        Track track = trackRepository.findByMongoId(mongoId);
        if (track != null) {
            // Elimina il TrackPage associato usando l'ID progressivo
            trackPageService.deleteTrackPageByIdTrack(track.getId());
        }
        // Poi elimina il Track
        return trackRepository.delete(mongoId);
    }

    // Cerca track per nome (Pubblico)
    public List<Track> searchByNome(String nome) {
        List<Track> allTracks = trackRepository.findAll();
        return allTracks.stream()
                .filter(track -> track.getNome().toLowerCase().contains(nome.toLowerCase()))
                .toList();
    }

    // Filtra per lunghezza minima (Pubblico)
    public List<Track> filterByLunghezzaMinima(int minLunghezza) {
        List<Track> allTracks = trackRepository.findAll();
        return allTracks.stream()
                .filter(track -> track.getLunghezza() >= minLunghezza)
                .toList();
    }
}