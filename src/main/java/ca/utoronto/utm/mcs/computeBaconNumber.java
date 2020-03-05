package ca.utoronto.utm.mcs;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Parameter;
import java.security.Policy.Parameters;
import java.util.concurrent.Exchanger;
import java.util.jar.Attributes.Name;

import org.json.*;
import org.neo4j.driver.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.exceptions.DatabaseException;

public class computeBaconNumber implements HttpHandler {

	private Driver driver;

	public computeBaconNumber(String uri, String user, String password){

		driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

	}


	public void handle(HttpExchange exchange) throws IOException {

		try {
			if (exchange.getRequestMethod().equals("GET")) {
				handleGet(exchange);
			} else if (exchange.getRequestMethod().equals("PUT")) {
				handle(exchange);
			}
		} catch (IOException e) {
			exchange.sendResponseHeaders(500, -1);
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	public void handleGet(HttpExchange exchange) throws IOException, JSONException{
		String body = Utils.convert(exchange.getRequestBody());
		JSONObject converted = new JSONObject(body);
		String actorID = "";
		int result = 0;
		JSONObject response = new JSONObject();

		try(Session session=driver.session()){

			if (converted.has("actorId")) {
				actorID = converted.getString("actorId");
			}
			else {
				exchange.sendResponseHeaders(400, -1);
			}

			if (App.BACONID == ""){
				exchange.sendResponseHeaders(400, -1);
			}
			else {

				if (actorID == App.BACONID) {
					response.put("baconNumber", "0");
				}
				else {
					try {
						Query getter = new Query("MATCH (a:actor {id:'" + actorID + "'}), (kb:actor {id:'" + App.BACONID + "'}), p=shortestPath((a)"
								+"-[*]-(kb)) RETURN length(p) as short");
						Result len = session.run(getter);
						Record temp_record = len.single();
						Value temp2 = temp_record.get("short");
						Integer temp3 = temp2.asInt();
						result = temp3 / 2;
						String baconNum = Integer.toString(result);
						response.put("baconNumber", baconNum);

					}catch (DatabaseException e) {
						response.put("baconNumber", "0");
					}
				}
				int byteSize = response.toString().getBytes().length;
				exchange.sendResponseHeaders(200, byteSize);
				OutputStream os = exchange.getResponseBody();

				os.write(response.toString().getBytes());
				os.close();
			}
		}


	}



}