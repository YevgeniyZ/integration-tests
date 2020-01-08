package bluesky.restapi.models;

import java.util.ArrayList;

public class NodeType {

    private ArrayList<String> guidingAttributes = new ArrayList<String>();
    private String id;
    private String jsonSchema;
    private String processor;
    private Synonyms SynonymsObject;
    private String version;

    // Getter Methods
    public ArrayList<String> getGuidingAttributes() {
        return guidingAttributes;
    }

    public String getId() {
        return id;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public String getProcessor() {
        return processor;
    }

    public Synonyms getSynonyms() {
        return SynonymsObject;
    }

    public String getVersion() {
        return version;
    }

    // Setter Methods

    public NodeType setGuidingAttributes(ArrayList<String> guidingAttributes) {
        this.guidingAttributes = guidingAttributes;
        return this;
    }

    public NodeType setId(String id) {
        this.id = id;
        return this;
    }

    public NodeType setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
        return this;
    }

    public NodeType setProcessor(String processor) {
        this.processor = processor;
        return this;
    }

    public NodeType setSynonyms(Synonyms synonymsObject) {
        this.SynonymsObject = synonymsObject;
        return this;
    }

    public NodeType setVersion(String version) {
        this.version = version;
        return this;
    }

    public static class Synonyms {

        private String additionalProp1;
        private String additionalProp2;
        private String additionalProp3;


        // Getter Methods
        public String getAdditionalProp1() {
            return additionalProp1;
        }

        public String getAdditionalProp2() {
            return additionalProp2;
        }

        public String getAdditionalProp3() {
            return additionalProp3;
        }

        // Setter Methods
        public Synonyms setAdditionalProp1(String additionalProp1) {
            this.additionalProp1 = additionalProp1;
            return this;
        }

        public Synonyms setAdditionalProp2(String additionalProp2) {
            this.additionalProp2 = additionalProp2;
            return this;
        }

        public Synonyms setAdditionalProp3(String additionalProp3) {
            this.additionalProp3 = additionalProp3;
            return this;
        }
    }
}
