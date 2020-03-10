package uk.co.azquelt.slackstacker.imwuc;

import java.util.Date;

public class IMWUCEntry {
    private String title;
    private String link;
    private Date publishedAt;
    public IMWUCEntry(String title, String link, Date publishedAt) { 
        this.title = title;
        this.link = link;
        this.publishedAt = publishedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Date getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        this.publishedAt = publishedAt;
    }
}
