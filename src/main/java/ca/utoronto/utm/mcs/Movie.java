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

public class Movie implements HttpHandler {

	private Driver driver;

	public Movie(String uri, String user, String password){

		driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

	}


	public void handle(HttpExchange exchange) throws IOException{

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
		String movieId = "";

		try(Session session=driver.session()){

			if (converted.has("name")) {
				name = converted.getString("name");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}
			if (converted.has("movieId")) {
				movieId = converted.getString("movieId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}

			Query checkSame = new Query("MATCH (m:movie {id:'" + movieId + "'}) "
					+ "RETURN m");
			Result same = session.run(checkSame);
			boolean hasSame = same.hasNext();
			if (!hasSame) {
				Query query = new Query("CREATE(n:movie{Name:'"+ name + "',id:'" + movieId + "'})");
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
		String movieID = "";
		try (Session session=driver.session()) {
			if (converted.has("movieId")) {
				movieID = converted.getString("movieId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}
			Query q = new Query("MATCH (m:movie{id:'" + movieID + "'}) "
					+ "RETURN m.Name as name");
			Result r = session.run(q);
			try {


				Record currMovie = r.single();
				JSONObject response = new JSONObject();
				response.put("name", currMovie.get("name"));
				response.put("id", movieID);

				Query actorList = new Query("MATCH (a:actor)-[r:ACTED_IN]->"
	                	+"(m:movie {id:'"+ movieID + "'}) RETURN a.id AS id");
				Result act = session.run(actorList);
				List<Record> allActors = act.list();
				String[] actors = new String[allActors.size()];
				
				for (int i=0; i<allActors.size(); i++) {
					Record actorRecord = allActors.get(i);
					actors[i] = actorRecord.get("id").asString();
				}
				response.put("actors", new JSONArray(actors));
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