package bluesky.restapi.models;



import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightWeightAccount {

    private String responsibleId;
    private String externalId;
    private String ownershipMode;
    private Node node;
    private String nodeStatus;
    private String orderId;
    private String settlementId;
    private String startDate;
    private String endDate;


    public String getResponsibleId()
    {
        return responsibleId;
    }

    public LightWeightAccount setResponsibleId(String responsibleId)
    {
        this.responsibleId = responsibleId;
        return this;
    }

    public String getExternalId()
    {
        return externalId;
    }

    public LightWeightAccount setExternalId(String externalId)
    {
        this.externalId = externalId;
        return this;
    }

    public String getOwnershipMode()
    {
        return ownershipMode;
    }

    public LightWeightAccount setOwnershipMode(String ownershipMode)
    {
        this.ownershipMode = ownershipMode;
        return this;
    }

    public Node getNode ()
    {
        return node;
    }

    public LightWeightAccount setNode (Node node)
    {
        this.node = node;
        return this;
    }

    public String getNodeStatus ()
    {
        return nodeStatus;
    }

    public LightWeightAccount setNodeStatus (String nodeStatus)
    {
        this.nodeStatus = nodeStatus;
        return this;
    }

    public String getOrderId()
    {
        return orderId;
    }

    public LightWeightAccount setOrderId(String orderId)
    {
        this.orderId = orderId;
        return this;
    }

    public String getSettlementId()
    {
        return settlementId;
    }

    public LightWeightAccount setSettlementId(String settlementId)
    {
        this.settlementId = settlementId;
        return this;
    }

    public String getStartDate ()
    {
        return startDate;
    }

    public LightWeightAccount setStartDate (String startDate)
    {
        this.startDate = startDate;
        return this;
    }

    public String getEndDate ()
    {
        return endDate;
    }

    public LightWeightAccount setEndDate (String endDate)
    {
        this.endDate = endDate;
        return this;
    }


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Node
    {
        private HashMap<String,Object> attributes;
        private String eventType;
        private String id;
        private String nodeType;
        private String nodeTypeVersion;
        private String schemaType;


        public HashMap<String,Object> getAttributes() {
            return attributes;
        }

        public Node setAttributes(HashMap<String,Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public String getEventType ()
        {
            return eventType;
        }

        public Node setEventType (String eventType)
        {
            this.eventType = eventType;
            return this;
        }

        public String getId ()
        {
            return id;
        }

        public Node setId (String id)
        {
            this.id = id;
            return this;
        }

        public String getNodeType ()
        {
            return nodeType;
        }

        public Node setNodeType (String nodeType)
        {
            this.nodeType = nodeType;
            return this;
        }

        public String getNodeTypeVersion ()
        {
            return nodeTypeVersion;
        }

        public Node setNodeTypeVersion (String nodeTypeVersion)
        {
            this.nodeTypeVersion = nodeTypeVersion;
            return this;
        }

        public String getSchemaType ()
        {
            return schemaType;
        }

        public Node setSchemaType (String schemaType)
        {
            this.schemaType = schemaType;
            return this;
        }

    }
}
