package it.vroom.abruno.repository;

import it.vroom.abruno.classi.AppRoleMapping;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

// Estendendo MongoRepository, Spring userà la configurazione di application.properties
public interface AppRoleRepository extends MongoRepository<AppRoleMapping, String> {

    // Cerca automaticamente documenti dove il campo 'username' corrisponde al parametro
    List<AppRoleMapping> findByUsername(String username);
}