package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Parameter;
import java.security.Policy.Parameters;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.jar.Attributes.Name;

import org.json.*;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.NoSuchRecordException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Actor implements HttpHandler {

    private Driver driver;

    public Actor(String uri, String user, String password){

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


    public void handlePut(HttpExchange exchange) throws IOException, JSONException{
        String body = Utils.convert(exchange.getRequestBody());
        JSONObject converted = new JSONObject(body);
        String name = "";
        String actorID = "";
        
        try(Session session=driver.session()){

            if (converted.has("name")) {
                name = converted.getString("name");
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
            Query checkSame = new Query("MATCH (a:actor {id:'" + actorID + "'}) "
					+ "RETURN a");
			Result same = session.run(checkSame);
			boolean hasSame = same.hasNext();
			if (!hasSame) {
                if (name.equals("Kevin Bacon")) {
                    App.BACONID = actorID;
                }
				Query query = new Query("CREATE(n:actor{Name:'"+ name + "',id:'" + actorID + "'})");
				Result result = session.run(query);
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}
        } catch (Exception e) {
            exchange.sendResponseHeaders(404, -1);
        }
        exchange.sendResponseHeaders(200, -1);

    }

    public void handleGet(HttpExchange exchange) throws IOException, JSONException {
		String body = Utils.convert(exchange.getRequestBody());
		JSONObject converted = new JSONObject(body);
		String actorID = "";
		try (Session session=driver.session()) {
			if (converted.has("actorId")) {
				actorID = converted.getString("actorId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}
			Query q = new Query("MATCH (n:actor{id:'" + actorID + "'}) "
					+ "RETURN n.Name AS name");
			Result r = session.run(q);
			try {


				Record actor = r.single();
				JSONObject response = new JSONObject();
				response.put("name", actor.get("name"));
				response.put("id", actorID);

				Query movs = new Query("MATCH (:actor {id:'"+ actorID + "'})"
						+"-[:ACTED_IN]->(m:movie) "
						+"RETURN m.id AS id");
				Result rmovs = session.run(movs);
				List<Record> allMovies = rmovs.list();
				String[] movies = new String[allMovies.size()];
				for (int i=0; i<allMovies.size(); i++) {
					Record movieRecord = allMovies.get(i);
					movies[i] = movieRecord.get("id").asString();
				}
				response.put("movies", new JSONArray(movies));
				int byteSize = response.toString().getBytes().length;
				exchange.sendResponseHeaders(200, byteSize);
				OutputStream os = exchange.getResponseBody();

				os.write(response.toString().getBytes());
				os.close();

			} catch (NoSuchRecordException e) {
				exchange.sendResponseHeaders(404, -1);
			}
		}


	}

    

}