package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Parameter;
import java.security.Policy.Parameters;
import java.util.concurrent.Exchanger;
import java.util.jar.Attributes.Name;

import org.json.*;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.NoSuchRecordException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Relationship implements HttpHandler {

	private Driver driver;

	public Relationship(String uri, String user, String password){

		driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

	}


	public void handle(HttpExchange exchange) throws IOException {

		try {
			if (exchange.getRequestMethod().equals("GET")) {
				handleGet(exchange);
			} else if (exchange.getRequestMethod().equals("PUT")) {
				handlePut(exchange);
			}
		} catch (IOException e) {
			exchange.sendResponseHeaders(500, -1);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

    
    public void handleGet(HttpExchange exchange) throws IOException, JSONException {
		String body = Utils.convert(exchange.getRequestBody());
		JSONObject converted = new JSONObject(body);
		String actorID = "";
		String movieID = "";
		boolean hasRelationship = false;
		
		try (Session session=driver.session()) {
			if (converted.has("actorId")) {
				actorID = converted.getString("actorId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}
			if (converted.has("movieId")) {
				movieID = converted.getString("movieId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}
			
			Query q = new Query("MATCH (n:actor{id:'" + actorID + "'}) "
					+ "RETURN n.Name as name");
			Result r = session.run(q);
			try {


				Record actor = r.single();
				JSONObject response = new JSONObject();

				Query hasActor = new Query("MATCH (a:actor {id:'"+ actorID + "'}) "
						+"RETURN a");
				Query hasMovie = new Query("MATCH (m:movie {id:'"+ movieID + "'}) "
						+"RETURN m");
				Result hasMovs = session.run(hasMovie);
				Result hasActs = session.run(hasActor);
				if (hasMovs.hasNext() && hasActs.hasNext()) {
					response.put("actorId", actorID);
					response.put("movieId", movieID);
					Query relCheck = new Query("MATCH (a:actor {id:'" + actorID + "'})-[r:ACTED_IN]->"
							+ "(m:movie {id:'" + movieID + "'}) RETURN r");
					Result hasRel = session.run(relCheck);
					if (hasRel.hasNext()) {
						hasRelationship = true;
					}
					response.put("hasRelationship", hasRelationship);
					int byteSize = response.toString().getBytes().length;
					exchange.sendResponseHeaders(200, byteSize);
					OutputStream os = exchange.getResponseBody();

					os.write(response.toString().getBytes());
					os.close();
				} else {
					exchange.sendResponseHeaders(404, -1);
				}
				
			} catch (NoSuchRecordException e) {
				exchange.sendResponseHeaders(403, -1);
			}
		}


    }
    
    public void handlePut(HttpExchange exchange) throws IOException, JSONException{
		String body = Utils.convert(exchange.getRequestBody());
		JSONObject converted = new JSONObject(body);
		String movieID = "";
		String actorID = "";

		try(Session session=driver.session()){

			if (converted.has("movieId")) {
				movieID = converted.getString("movieId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}
			if (converted.has("actorId")) {
				actorID = converted.getString("actorId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}

			Query hasActor = new Query("MATCH (a:actor {id:'"+ actorID + "'}) "
					+"RETURN a");
			Query hasMovie = new Query("MATCH (m:movie {id:'"+ movieID + "'}) "
					+"RETURN m");
			Result hasMovs = session.run(hasMovie);
			Result hasActs = session.run(hasActor);
			Boolean actsMovsExist = hasMovs.hasNext() && hasActs.hasNext();

			Query checkSame = new Query("MATCH (a:actor {id:'" + actorID + "'})-[r:ACTED_IN]->(m:movie {id:'" + movieID + "'}) "
					+ "RETURN r");
			Result same = session.run(checkSame);
			Boolean hasSame = same.hasNext();
			if (hasSame) {
				exchange.sendResponseHeaders(400, -1);
			} else {
				if (actsMovsExist) {
					Query query = new Query("MATCH (a:actor),(m:movie) WHERE a.id = '" + actorID + "' AND m.id = '" + movieID + "' "
							+ "CREATE (a)-[r:ACTED_IN]->(m) RETURN r");
					Result result = session.run(query);
				}
				else {
					exchange.sendResponseHeaders(404, -1);
				}
			}
		} catch (Exception e) {
			exchange.sendResponseHeaders(404, -1);
		}
		exchange.sendResponseHeaders(200, -1);

    }
}