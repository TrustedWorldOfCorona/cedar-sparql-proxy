package com.github.trustedworldofcorona.cedar.proxy;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriterFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.util.Repositories;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class ProxyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }

    @RestController
    static class WebController {
        @Value("${cedar.base}")
        String base;
        @Value("${cedar.token}")
        String token;

        static final HttpClient client = HttpClient.newHttpClient();
        static final ObjectMapper mapper = new ObjectMapper();

        /*
        curl http://localhost:8080/resource\?id=https://repo.metadatacenter.org/template-instances/2b5fbe93-5439-44af-9c20-b77c6336a1e5&q=SELECT%20\?s%20\?p%20\?o%20%7B%20%3Fs%20%3Fp%20%3Fo%20%7D
         */
        @GetMapping("/resource")
        String query(@RequestParam String id, @RequestParam String q) {
            var repository = new SailRepository(new MemoryStore());
            consumeResource(id, repository);

            var factory = new SPARQLResultsJSONWriterFactory();
            var out = new ByteArrayOutputStream();
            var writer = factory.getWriter(out);
            Repositories.tupleQueryNoTransaction(repository, q, writer);

            return out.toString();
        }

        /*
        folder with single instance:
        curl http://localhost:8080/folder\?id=https://repo.metadatacenter.org/folders/b06136a8-d4fb-4ad6-b939-ed1b37ea829d&q=SELECT%20\?s%20\?p%20\?o%20%7B%20%3Fs%20%3Fp%20%3Fo%20%7D
        parent folder with subfolders:
        curl http://localhost:8080/folder\?id=https://repo.metadatacenter.org/folders/cc9fbb7e-d15f-48db-a123-d20635877a0f&q=SELECT%20\?s%20\?p%20\?o%20%7B%20%3Fs%20%3Fp%20%3Fo%20%7D
         */
        @GetMapping("/folder")
        String queryFolder(@RequestParam String id, @RequestParam String q) {
            var repository = new SailRepository(new MemoryStore());
            consumeFolder(id, repository);

            var factory = new SPARQLResultsJSONWriterFactory();
            var out = new ByteArrayOutputStream();
            var writer = factory.getWriter(out);
            Repositories.tupleQueryNoTransaction(repository, q, writer);

            return out.toString();
        }

        void consumeFolder(String id, Repository repository) {
            final InputStream body;
            try {
                var req = HttpRequest.newBuilder()
                        .header("Authorization", token)
                        .header("Accept", "application/json")
                        .uri(URI.create(base + "/folders/"+URLEncoder.encode(id, StandardCharsets.UTF_8)+"/contents?resource_types=folder,instance"))
                        .build();
                var resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                body = resp.body();
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }

            final FolderResponse response;
            try {
                response = mapper.readValue(body, FolderResponse.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            for (var res : response.resources) {
                if ("folder".equals(res.resourceType)) {
                    consumeFolder(res.id, repository);
                } else if ("instance".equals(res.resourceType)) {
                    consumeResource(res.id, repository);
                }
            }
        }

        void consumeResource(String id, Repository repository) {
            final InputStream body;
            try {
                var req = HttpRequest.newBuilder()
                        .uri(URI.create(id))
                        .header("Authorization", token)
                        .header("Accept", "application/json")
                        .build();
                var response = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                body = response.body();
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException(e);
            }

            Repositories.consume(repository, conn -> {
                try {
                    conn.add(body, RDFFormat.JSONLD);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    @JsonIncludeProperties("resources")
    static class FolderResponse {
        @JsonProperty
        ArrayList<FolderResource> resources;
    }
    @JsonIncludeProperties({ "resourceType", "@id" })
    static class FolderResource {
        @JsonProperty
        String resourceType;
        @JsonProperty("@id")
        String id;
    }
}
