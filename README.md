# Order Workflow
Order Workflow Stored Procedures

This project requires Neo4j 3.2.x

Instructions
------------ 

This project uses maven, to build a jar-file with the procedure in this
project, simply package the project with maven:

    mvn clean package

This will produce a jar-file, `target/workflow-1.0-SNAPSHOT.jar`,
that can be copied to the `plugin` directory of your Neo4j instance.

    cp target/workflow-1.0-SNAPSHOT.jar neo4j-enterprise-3.2.3/plugins/.


Edit your Neo4j/conf/neo4j.conf file by adding this line:

    dbms.security.procedures.unrestricted=com.maxdemarzi.*    

Restart your Neo4j Server.

Create the Schema by running this stored procedure:

    CALL com.maxdemarzi.schema.generate
    
Create some test data:

    CREATE (o:Order {id:'o1'})
    CREATE (p1:Provider {name:'Provider One'})
    CREATE (p2:Provider {name:'Provider Two'})
    CREATE (w1:Work {id:'w1'})
    CREATE (w2:Work {id:'w2'})
    CREATE (t1:Task {id:'t1', requires:"(e1 & e2) | (e3 & !e4)"})
    CREATE (t2:Task {id:'t2'})
    CREATE (t3:Task {id:'t3'})
    CREATE (e1:Event {id:'e1'})
    CREATE (e4:Event {id:'e4'})
    
    CREATE (o)-[:HAS_WORK]->(w1)
    CREATE (o)-[:HAS_WORK]->(w2)
    CREATE (p1)-[:PERFORMS]->(w1)
    CREATE (p2)-[:PERFORMS]->(w2)
    CREATE (w1)-[:HAS_TASK]->(t1)
    CREATE (w2)-[:HAS_TASK]->(t2)
    CREATE (w2)-[:HAS_TASK]->(t3)
    CREATE (e1)-[:FIRST]->(o)
    CREATE (e4)-[:PREVIOUS]->(e1)
    
See which tasks can be completed and their event dependencies:

    CALL com.maxdemarzi.order.tasks('o1') yield value return value