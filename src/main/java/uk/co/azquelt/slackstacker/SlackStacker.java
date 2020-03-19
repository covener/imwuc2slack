package uk.co.azquelt.slackstacker;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.cxf.transport.common.gzip.GZIPFeature;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import uk.co.azquelt.slackstacker.imwuc.IMWUCEntry;
import uk.co.azquelt.slackstacker.imwuc.IMWUCFeed;
import uk.co.azquelt.slackstacker.slack.SlackMessage;

public class SlackStacker {
	
	private static ObjectMapper stateMapper;
	
	private static Client client = ClientBuilder.newBuilder()
			.register(JacksonJsonProvider.class) // Allow us to serialise JSON <-> POJO
			.register(GZIPFeature.class) // Allow us to understand GZIP compressed pages
			.build();

	private static void saveState(State newState, String stateFileName) throws JsonGenerationException, JsonMappingException, IOException {
		File stateFile = new File(stateFileName);
		stateMapper.writerWithDefaultPrettyPrinter().forType(State.class).writeValue(stateFile, newState);
	}
	
	private static State createDefaultState(Calendar now) {
		State newState = new State();
		newState.lastUpdated = now;
		return newState;
	}

	private static State createNewState(Calendar now) {
		State newState = new State();
		newState.lastUpdated = now;
		return newState;
	}

	public static void post(SlackMessage message, String webhookUrl) throws IOException { 
		WebTarget target = client.target(webhookUrl);
		Invocation.Builder builder = target.request();
	    Response resp = builder.post(Entity.entity(message, MediaType.APPLICATION_JSON_TYPE));
		if (resp.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
			throw new IOException("Error posting messages to slack: " + resp.getStatusInfo().getReasonPhrase());
		}
	}

	private static State loadState(String stateFileName) throws JsonProcessingException, IOException, InvalidArgumentException {
		if (stateFileName == null) {
			throw new InvalidArgumentException("State file location is not set in config file");
		}
		
		File stateFile = new File(stateFileName);
		
		State state = null;
		
		if (stateFile.exists()) {
			state = stateMapper.readerFor(State.class).readValue(stateFile);
		}
		
		return state;
	}
	
	private static Config loadConfig(File configFile) throws JsonProcessingException, IOException, InvalidArgumentException {
		if (configFile == null) {
			throw new InvalidArgumentException("Config file is not set");
		}
		
		if (!configFile.exists()) {
			throw new InvalidArgumentException("Config file [" + configFile + "] does not exist");
		}
		
		Config config = stateMapper.readerFor(Config.class).readValue(configFile);
		
		return config;
	}
	
	public static void main(String[] args) throws IOException {
		
		try {
			stateMapper = new ObjectMapper();
			
			CommandLine arguments = CommandLine.processArgs(args);
			
			Config config = loadConfig(arguments.getConfigFile());
			
			State oldState = loadState(config.stateFile);
			Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			
			if (oldState != null) {
				
				if (oldState.backoffUntil != null && now.before(oldState.backoffUntil)) {
					// We've been asked by the StackExchange API to back off, don't run
					return;
				}
				
				State newState = createNewState(now);
				saveState(newState, config.stateFile);

				List<IMWUCEntry> imwuc_results = IMWUCFeed.getEntries(oldState.lastUpdated.getTime());
				if (imwuc_results != null) { 
					IMWUCFeed.postEntries(imwuc_results, config.slackWebhookUrl);
				}
			} else {
				System.out.println("No pre-existing state, setting up default state file");
				State newState = createDefaultState(now);
				saveState(newState, config.stateFile);
			}
		} catch (InvalidArgumentException e) {
			System.err.println(e.getMessage());
		}
		
	}

}
