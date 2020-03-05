package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import org.neo4j.driver.*;
import org.neo4j.driver.Driver;

public class App 
{
	private final static String URI = "bolt://localhost:7687";
	private final static String UNAME = "neo4j";
	private final static String PW = "1234";
	private static Driver driver;
	public static String BACONID = "nm0000102";
    static int PORT = 8080;
     public static void main(String[] args) throws IOException
     {
    	 
    	 
         HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
         driver = GraphDatabase.driver(URI, AuthTokens.basic(UNAME, PW));
         //ActorMethods actors = new ActorMethods();
         server.createContext("/api/v1/addActor", new Actor(URI, UNAME, PW));
         server.createContext("/api/v1/getActor", new Actor(URI, UNAME, PW));
        
         server.createContext("/api/v1/addMovie", new Movie(URI, UNAME, PW));
         server.createContext("/api/v1/getMovie", new Movie(URI, UNAME, PW));
         
         server.createContext("/api/v1/addRelationship", new Relationship(URI, UNAME, PW));
         server.createContext("/api/v1/hasRelationship", new Relationship(URI, UNAME, PW));
         
         server.createContext("/api/v1/computeBaconNumber", new computeBaconNumber(URI, UNAME, PW));
         server.createContext("/api/v1/computeBaconPath", new computeBaconPath(URI, UNAME, PW));
         
         server.start();
         System.out.printf("Server started on port %d...\n", PORT);
     }
}
