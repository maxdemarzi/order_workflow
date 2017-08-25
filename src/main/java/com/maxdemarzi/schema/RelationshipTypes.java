package com.maxdemarzi.schema;

import org.neo4j.graphdb.RelationshipType;

public enum RelationshipTypes implements RelationshipType {
    HAS_WORK,
    PERFORMS,
    FIRST,
    PREVIOUS,
    HAS_TASK
}
