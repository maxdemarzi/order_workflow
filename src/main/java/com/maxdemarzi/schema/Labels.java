package com.maxdemarzi.schema;

import org.neo4j.graphdb.Label;

public enum Labels implements Label {
    Provider,
    Order,
    Event,
    Work,
    Task
}
