package ca.utoronto.utm.mcs;

import java.io.IOException;

import java.io.OutputStream;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.rmi.registry.RegistryHandler;
import java.security.Policy.Parameters;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.jar.Attributes.Name;

import org.json.*;
import org.neo4j.driver.*;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.types.Node;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class computeBaconPath implements HttpHandler {

	private Driver driver;

	public computeBaconPath(String uri, String user, String password) {

		driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));

	}

	public void handle(HttpExchange exchange) throws IOException {
		try {
			if (exchange.getRequestMethod().equals("GET")) {
				handleGet(exchange);
			} else if (exchange.getRequestMethod().equals("PUT")) {
				handle(exchange);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//actor return 0
	//movie return 1
	public Integer determineIDtype(String id){
		Integer i = 0;
		if(id.charAt(0)=='t'){
			i = 0;
		} else {
			i = 1;
		}
		return i;
	}

	public JSONObject[] getJSONbyNodelst(List<org.neo4j.driver.types.Node> nodes, Integer length, HttpExchange exchange)
			throws IOException, JSONException {
		JSONObject[] baconPath = new JSONObject[length + 1];
		//Since the first node that will be return will always be 0, so default to actor 
		Integer previous_type = 0;
		String previous_id = "";
		try (Session session = driver.session()){
			for(int i=0; i <= length; i++){
				Query get_id = new Query("MATCH (s) WHERE ID(s)='"+ nodes.get(i).get("id") + "' RETURN s.id as id");
				Result res = session.run(get_id);
				Record num_record = res.single();
				Value temp2 = num_record.get("id");
				//String node_id = temp2.asString();
				// JSONObject node = new JSONObject();
				// if(previous_id.isEmpty()){
				// 	previous_id = node_id;
				// 	previous_type = determineIDtype(node_id);
				// } else {
				// 	node.put("id", node_id);
				// 	if(determineIDtype(node_id) == 0){
				// 		node.put("actorId", node_id);
				// 		node.put("movieId", previous_id);
				// 		previous_type = 0;
				// 	} else {
				// 		node.put("actorId", previous_id);
				// 		node.put("movieId", node_id);
				// 		previous_type = 1;
				// 	}
				// 	previous_id = node_id;
				// 	baconPath[i] = node;
				// }
			}
		} catch (DatabaseException e) {
			exchange.sendResponseHeaders(404, -1);
		}
		
		//MATCH (s) WHERE ID(s)=92025 return s.id as id 

		return baconPath;
	}

	public void handleGet(HttpExchange exchange) throws IOException, JSONException {
		String body = Utils.convert(exchange.getRequestBody());
		JSONObject converted = new JSONObject(body);
		String actorID = "";

		try (Session session = driver.session()) {
			if (converted.has("actorId")) {
				actorID = converted.getString("actorId");
			} else {
				exchange.sendResponseHeaders(400, -1);
			}
			JSONObject response = new JSONObject();
			if (actorID == App.BACONID) {

				response.put("baconNumber", 0);
				response.put("baconPath", new JSONArray());
				int byteSize = response.toString().getBytes().length;
				exchange.sendResponseHeaders(200, byteSize);
				OutputStream os = exchange.getResponseBody();
				os.write(response.toString().getBytes());
				os.close();
			} else {
				try {
					Query get_num = new Query("MATCH (a:actor {id:'" + actorID + "'}), (kb:actor {id:'" + App.BACONID
							+ "'}), p=shortestPath((a)" + "-[*]-(kb)) RETURN length(p) as short");
					Result res = session.run(get_num);
					Record num_record = res.single();
					Value temp2 = num_record.get("short");
					Integer temp3 = temp2.asInt();
					Integer result = temp3 / 2;
					String baconNum = Integer.toString(result);
					response.put("baconNumber", baconNum);

					Query get_path = new Query("MATCH (a:actor {id:'" + actorID + "'}), (kb:actor {id:'" + App.BACONID
							+ "'}), p=shortestPath((a)" + "-[*]-(kb)) RETURN p as path");
					Result res2 = session.run(get_path);
					Record path_record = res2.single();
					Value paths = path_record.get("path");
					List<org.neo4j.driver.types.Node> nodes = new ArrayList<org.neo4j.driver.types.Node>();
					org.neo4j.driver.types.Path p = paths.asPath();
					Iterator<org.neo4j.driver.types.Node> it = p.nodes().iterator();
					Integer i=0;
					while(it.hasNext()){
						org.neo4j.driver.types.Node rel = it.next();
						nodes.add(rel);
					}
					response.put("nodes", nodes.get(0).get("id"));
					response.put("baconPath", getJSONbyNodelst(nodes, nodes.size()-1, exchange));

					int byteSize = response.toString().getBytes().length;
					exchange.sendResponseHeaders(200, byteSize);
					OutputStream os = exchange.getResponseBody();
					os.write(response.toString().getBytes());
					os.close();
				}catch (DatabaseException e) {
					response.put("baconNumber", "0");
				}
			}
		}catch (NoSuchRecordException ex) {
			exchange.sendResponseHeaders(404, -1);
		}
		
	}
}
