
package uk.co.azquelt.slackstacker.imwuc;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import uk.co.azquelt.slackstacker.CommandLine;
import uk.co.azquelt.slackstacker.Config;
import uk.co.azquelt.slackstacker.IMWUC;
import uk.co.azquelt.slackstacker.InvalidArgumentException;

public class IMWUCFeed {
    private static int MAX_POSTS = 10;

    public static List<IMWUCEntry> getEntries(Date since, Config c) {

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        boolean ok = false;
        try {
            feed = input.build(new XmlReader(new URL(c.feedURL)));
            ok = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!ok)
            return null;

        List<IMWUCEntry> rv = new ArrayList<IMWUCEntry>();

        for (SyndEntry ent : feed.getEntries()) {
            String title = ent.getTitle();

            // axe replies
            if (title.startsWith("RE:")) {
                continue;
            }
            if (ent.getPublishedDate().compareTo(since) <= 0) {
                continue;
            }
            rv.add(new IMWUCEntry(title, ent.getLink(), ent.getPublishedDate()));
        }
        if (rv.size() > MAX_POSTS) {
            rv = rv.subList(0, MAX_POSTS - 1);
        }
        return rv;
    }

    public static void postEntries(List<IMWUCEntry> newEntries, Config c) throws IOException {
        if (newEntries.size() == 0) {
            return; // Nothing to post!
        }
        for (IMWUCEntry ent : newEntries) {
            IMWUC.post(IMWUCMessageBuilder.buildMessage(ent, c), c.slackWebhookUrl);
        }
    }

    public static void main(String[] args) throws InvalidArgumentException, JsonProcessingException, IOException {

        Calendar recent = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        recent.add(Calendar.DATE, -7);

        CommandLine arguments = CommandLine.processArgs(args);
			
        Config config = IMWUC.loadConfig(arguments.getConfigFile());

        List<IMWUCEntry> results = getEntries(recent.getTime(), config);
        for (IMWUCEntry e: results) { 
            System.out.println(IMWUCMessageBuilder.buildMessage(e, null).text);
        }
    }
}