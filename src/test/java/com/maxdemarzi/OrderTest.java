package com.maxdemarzi;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.ArrayList;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static junit.framework.TestCase.assertEquals;

public class OrderTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Rule
    public final Neo4jRule neo4j = new Neo4jRule()
            .withFixture(MODEL_STATEMENT)
            .withProcedure(Order.class);

    @Test
    public void testOrderTasks() throws Exception {
        HTTP.Response response = HTTP.POST(neo4j.httpURI().resolve("/db/data/transaction/commit").toString(), QUERY1);
        int count = response.get("results").get(0).get("data").size();
        assertEquals(3, count);
        Map task3 = mapper.convertValue(response.get("results").get(0).get("data").get(0).get("row").get(0), Map.class);
        assertEquals("Provider Two", task3.get("provider"));
        assertEquals("t3", task3.get("id"));
        Map task2 = mapper.convertValue(response.get("results").get(0).get("data").get(1).get("row").get(0), Map.class);
        assertEquals("Provider Two", task3.get("provider"));
        assertEquals("t2", task2.get("id"));
        Map task1 = mapper.convertValue(response.get("results").get(0).get("data").get(2).get("row").get(0), Map.class);
        assertEquals("Provider One", task1.get("provider"));
        assertEquals("t1", task1.get("id"));
        ArrayList<Map<String, Object>> dependencies = ((ArrayList)task1.get("dependencies"));
        assertEquals(2, dependencies.size());
        assertEquals(new ArrayList<String>() {{add("e2");}}, dependencies.get(0).get("missing"));
        assertEquals(new ArrayList<String>() {{add("e4");}}, dependencies.get(1).get("remove"));
    }

    private static final Map QUERY1 =
            singletonMap("statements", singletonList(singletonMap("statement",
                    "CALL com.maxdemarzi.order.tasks('o1') yield value return value")));

    private static final String MODEL_STATEMENT =
        "CREATE (o:Order {id:'o1'})" +
        "CREATE (p1:Provider {name:'Provider One'})" +
        "CREATE (p2:Provider {name:'Provider Two'})" +
        "CREATE (w1:Work {id:'w1'})" +
        "CREATE (w2:Work {id:'w2'})" +
        "CREATE (t1:Task {id:'t1', requires:'(e1 & e2) | (e3 & !e4)'})" +
        "CREATE (t2:Task {id:'t2'})" +
        "CREATE (t3:Task {id:'t3'})" +
        "CREATE (e1:Event {id:'e1'})" +
        "CREATE (e4:Event {id:'e4'})" +
        "CREATE (e2:Event {id:'e2'})" +
        "CREATE (e2neg:Event {id:'-e2'})" +
        "CREATE (o)-[:HAS_WORK]->(w1)" +
        "CREATE (o)-[:HAS_WORK]->(w2)" +
        "CREATE (p1)-[:PERFORMS]->(w1)" +
        "CREATE (p2)-[:PERFORMS]->(w2)" +
        "CREATE (w1)-[:HAS_TASK]->(t1)" +
        "CREATE (w2)-[:HAS_TASK]->(t2)" +
        "CREATE (w2)-[:HAS_TASK]->(t3)" +
        "CREATE (e1)-[:FIRST]->(o)" +
        "CREATE (e4)-[:PREVIOUS]->(e1)"+
        "CREATE (e2)-[:PREVIOUS]->(e4)"+
        "CREATE (e2neg)-[:PREVIOUS]->(e2)";

}
