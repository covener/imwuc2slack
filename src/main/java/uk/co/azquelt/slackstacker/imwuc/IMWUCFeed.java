
package uk.co.azquelt.slackstacker.imwuc;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import uk.co.azquelt.slackstacker.SlackStacker;

public class IMWUCFeed {
    // XXX: externalize to config file
    private static String FEED_URL = "https://community.ibm.com/community/user/imwuc/rssgenerator?UserKey=156c5d6d-54bb-43f1-895f-51e7ea457ddf";
    private static int MAX_POSTS = 10;

    public static List<IMWUCEntry> getEntries(Date since) {

        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = null;
        boolean ok = false;
        try {
            feed = input.build(new XmlReader(new URL(FEED_URL)));
            ok = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!ok) return null;

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
            rv = rv.subList(0, MAX_POSTS-1);
        }
        return rv;
    }

	public static void postEntries(List<IMWUCEntry> newEntries, String webhookUrl) throws IOException {
		if (newEntries.size() == 0) {
			return; //Nothing to post!
        }
        for (IMWUCEntry ent : newEntries) {
            SlackStacker.post(IMWUCMessageBuilder.buildMessage(ent), webhookUrl);
        }
    }

    public static void main(String[] args) {

        Calendar recent = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        recent.add(Calendar.DATE, -7);

        List<IMWUCEntry> results = getEntries(recent.getTime());
        for (IMWUCEntry e: results) { 
            System.out.println(IMWUCMessageBuilder.buildMessage(e).text);
        }
    }
}