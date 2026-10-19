package com.changeimpact.model;

/**
 * Type of dependency edge between nodes in the graph.
 */
public enum EdgeType {
    DEPENDS_ON,
    CALLS,
    EXERCISES,
    CONTAINS
}
