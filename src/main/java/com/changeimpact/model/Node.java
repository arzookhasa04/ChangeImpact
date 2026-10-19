package com.changeimpact.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Node in the dependency graph representing a module, component/class, function, or test.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Node {
    private final String id;
    private final String name;
    private final NodeType type;
    private final String filePath;
    private final Map<String, Object> metadata;

    @JsonCreator
    public Node(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("type") NodeType type,
            @JsonProperty("filePath") String filePath,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.id = Objects.requireNonNull(id, "Node id cannot be null").trim();
        this.name = name != null ? name.trim() : this.id;
        this.type = type != null ? type : NodeType.COMPONENT;
        this.filePath = filePath;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public Node(String id, String name, NodeType type, String filePath) {
        this(id, name, type, filePath, Collections.emptyMap());
    }

    public Node(String id, NodeType type) {
        this(id, id, type, null, Collections.emptyMap());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public NodeType getType() {
        return type;
    }

    public String getFilePath() {
        return filePath;
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public Object getMetadataValue(String key) {
        return metadata.get(key);
    }

    public Node withMetadata(String key, Object value) {
        Map<String, Object> newMeta = new HashMap<>(this.metadata);
        newMeta.put(key, value);
        return new Node(this.id, this.name, this.type, this.filePath, newMeta);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                (filePath != null ? ", filePath='" + filePath + '\'' : "") +
                '}';
    }
}
