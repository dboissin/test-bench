package dev.ceven.petapp.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "pets")
public class PetDocument {

    @Id
    private String id;
    private String name;
    private String species;

    public PetDocument() {
    }

    public PetDocument(String id, String name, String species) {
        this.id = id;
        this.name = name;
        this.species = species;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }
}
