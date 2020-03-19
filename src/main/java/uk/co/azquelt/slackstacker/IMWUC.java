package uk.co.azquelt.slackstacker;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import uk.co.azquelt.slackstacker.imwuc.IMWUCEntry;
import uk.co.azquelt.slackstacker.imwuc.IMWUCFeed;
import uk.co.azquelt.slackstacker.slack.SlackMessage;

public class IMWUC {

	private static ObjectMapper stateMapper;

	private static void saveState(State newState, String stateFileName)
			throws JsonGenerationException, JsonMappingException, IOException {
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

	static {
		stateMapper = new ObjectMapper();
	}

	public static void post(SlackMessage message, String webhookUrl) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(webhookUrl);
		ObjectMapper mapper = new ObjectMapper();
		String jsonStr = mapper.writeValueAsString(message);
		StringEntity requestEntity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
		httpPost.setEntity(requestEntity);
		CloseableHttpResponse resp = httpclient.execute(httpPost);
		if (resp.getStatusLine().getStatusCode() != 200) { 
			throw new IOException("Error posting messages to slack: " + resp);
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
	
	public static Config loadConfig(File configFile) throws JsonProcessingException, IOException, InvalidArgumentException {
		if (configFile == null) {
			throw new InvalidArgumentException("Config file is not set");
		}
		
		if (!configFile.exists()) {
			throw new InvalidArgumentException("Config file [" + configFile + "] does not exist");
		}
		
		Config config = stateMapper.readerFor(Config.class).readValue(configFile);
		if (!config.stateFile.startsWith("/")) { 
			config.stateFile = System.getProperty("user.home") + "/" + config.stateFile;
		}
		
		return config;
	}
	
	public static void main(String[] args) throws IOException {
		
		try {
			
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

				List<IMWUCEntry> imwuc_results = IMWUCFeed.getEntries(oldState.lastUpdated.getTime(), config);
				if (imwuc_results != null) { 
					IMWUCFeed.postEntries(imwuc_results, config);
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
