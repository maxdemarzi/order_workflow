package com.maxdemarzi;

import com.maxdemarzi.quine.BooleanExpression;
import com.maxdemarzi.results.MapResult;
import com.maxdemarzi.schema.Labels;
import com.maxdemarzi.schema.RelationshipTypes;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

public class Order {
    // This field declares that we need a GraphDatabaseService
    // as context when any procedure in this class is invoked
    @Context
    public GraphDatabaseService db;
    // This gives us a log instance that outputs messages to the
    // standard log, normally found under `data/log/console.log`
    @Context
    public Log log;


    @Procedure(name = "com.maxdemarzi.order.tasks", mode = Mode.WRITE)
    @Description("CALL com.maxdemarzi.order.tasks(orderId) - get tasks for orders")
    public Stream<MapResult> orderTasks(@Name("orderId") String orderId) throws IOException {
        ArrayList<Map<String, Object>> tasks = new ArrayList<>();

        // We start by finding the order
        Node order = db.findNode(Labels.Order, "id", orderId);
        if (order != null) {
            // Create a traversal description that finds all events
            TraversalDescription eventTraversal = db.traversalDescription()
                    .depthFirst().expand(PathExpanders
                            .forTypesAndDirections(
                                    RelationshipTypes.FIRST, Direction.INCOMING,
                                    RelationshipTypes.PREVIOUS, Direction.INCOMING)
                    ).evaluator(Evaluators.excludeStartPosition());

            // Gather all of its event ids in to a Set
            Set<String> eventsIds = new HashSet<>();
            for (Path path : eventTraversal.traverse(order)) {
                String eventId = (String) path.endNode().getProperty("id");
                // If this is a negative event, remove existing event id
                if (eventId.charAt(0) == '-') {
                    eventsIds.remove(eventId.substring(1, eventId.length()));
                } else {
                    eventsIds.add(eventId);
                }
            }

            // Find the work required to complete the order
            for (Relationship r1 : order.getRelationships(Direction.OUTGOING, RelationshipTypes.HAS_WORK)) {
                Node work = r1.getEndNode();
                // Figure out who will perform this work
                Node provider = work.getSingleRelationship(RelationshipTypes.PERFORMS, Direction.INCOMING).getStartNode();
                String providerName = (String) provider.getProperty("name");

                // For each work item, find the associated tasks
                for (Relationship r2 : work.getRelationships(Direction.OUTGOING, RelationshipTypes.HAS_TASK)) {
                    Node task = r2.getEndNode();
                    Map<String, Object> properties = task.getAllProperties();
                    properties.put("provider", providerName);

                    // If the task has requirements see what is left to be done
                    String requires = (String) task.getProperty("requires", null);
                    if (requires != null) {
                        // Have we already calculated dependencies?
                        String[] paths = (String[])task.getProperty("dependencies", null);
                        if (paths == null) {
                            // Calculate the dependencies and save them, so we only ever do this once.
                            BooleanExpression boEx = new BooleanExpression(requires);
                            boEx.doTabulationMethod();
                            boEx.doQuineMcCluskey();
                            boEx.doPetricksMethod();
                            paths = boEx.getPathExpressions().toArray(new String[]{});
                            task.setProperty("dependencies", paths);
                        }

                        // Check our dependencies against the events of the order
                        ArrayList<HashMap<String, Object>> dependencies = new ArrayList<>();
                        for (String path : paths) {
                            String[] ids = path.split("[!&]");
                            char[] rels = path.replaceAll("[^&^!]", "").toCharArray();
                            Set<String> missing = new HashSet<>();
                            Set<String> remove = new HashSet<>();

                            // Check the first required event in the path
                            if (!eventsIds.contains(ids[0])) {
                                missing.add(ids[0]);
                            }

                            // Check the rest of the events
                            if (ids.length > 1) {
                                for (int i = 0; i < rels.length; i++) {
                                    if (rels[i] == '&') {
                                        if (!eventsIds.contains(ids[1 + i])) {
                                            missing.add(ids[1 + i]);
                                        }
                                    } else {
                                        if (eventsIds.contains(ids[1 + i])) {
                                            remove.add(ids[1 + i]);
                                        }
                                    }
                                }
                            }
                            // Add the dependencies
                            HashMap<String, Object> dependency = new HashMap<>();
                            dependency.put("missing", missing);
                            dependency.put("remove", remove);
                            dependencies.add(dependency);
                        }
                        properties.put("dependencies", dependencies);
                    }
                    tasks.add(properties);
                }

            }
        }
        return tasks.stream().map(MapResult::new);
    }

}
