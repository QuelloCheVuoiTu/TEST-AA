// File: src/main/java/it/vroom/abruno/service/MultipartInputStreamFileResource.java
package it.vroom.abruno.service;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

public class MultipartInputStreamFileResource extends InputStreamResource {
    private final String filename;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename) {
        super(inputStream);
        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    public long contentLength() throws IOException {
        return -1; // Non conosciuto
    }
}

/**La classe `MultipartInputStreamFileResource` è necessaria perché `RestTemplate` di Spring, quando invii file tramite multipart, si aspetta una risorsa (`Resource`) che abbia anche il nome del file.
 La classe standard `InputStreamResource` non fornisce il nome del file, quindi il server riceverebbe un file senza nome.
 `MultipartInputStreamFileResource` estende `InputStreamResource` e aggiunge il nome del file, permettendo così l’upload corretto tramite multipart.
 */