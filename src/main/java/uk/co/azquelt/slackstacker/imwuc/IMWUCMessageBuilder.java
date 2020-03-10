package uk.co.azquelt.slackstacker.imwuc;

import uk.co.azquelt.slackstacker.slack.SlackMessage;

/**
 * Builds slack messages from filtered IMWUC feed entries
 */
public class IMWUCMessageBuilder {

	/**
	 * Builds a SlackMessage from a list of IMWUC feed entries
	 * 
	 * @param entry a parsed IMWUC entry
	 * @return a message which includes the given question
	 */
	public static SlackMessage buildMessage(IMWUCEntry entry) {
		StringBuilder sb = new StringBuilder();
		appendEntry(sb, entry);
		
		SlackMessage message = new SlackMessage();
		message.text = sb.toString();
		
		return message;
	}
	
	/**
	 * Format an IMWUC entry and append it to a string builder
	 * 
	 * @param sb the string builder
	 * @param entry a parsed IMWUC entry
	 */
	private static void appendEntry(StringBuilder sb, IMWUCEntry entry) {
		sb.append("IBM Middleware Community: <");
		sb.append(entry.getLink());
		sb.append("|");
		sb.append(entry.getTitle());
		sb.append(">");
		sb.append("\n");
	}
}
