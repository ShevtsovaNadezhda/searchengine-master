package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.services.implementation.Lemmatizator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path"))
@NoArgsConstructor
@Getter
@Setter
public class PageModel implements Comparable<PageModel> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(columnDefinition = "INT", nullable = false)
    private int code;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @OneToMany(mappedBy = "page", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Set<IndexModel> indexes = new HashSet<>();

    public HashSet<PageModel> getChildren() throws IOException {
        String regexLink = "(http|ftp|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])";
        HashSet<PageModel> pages = new HashSet<>();
        HashSet<String> links = new HashSet<>();

        checkStatusCode();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Document doc;
        if (getCode() == 200) {
            doc = getPageContentInDoc();
            setContent(doc.toString());

            Elements elements = doc.select("a");
            elements.forEach(e -> {
                String link = e.attr("href");
                String newLink = "";
                if (link.startsWith("/")) {
                    newLink = site.getUrl() + link;
                }
                if (newLink.startsWith(site.getUrl()) && newLink.matches(regexLink) && !isFail(newLink)) {
                    links.add(link);
                }
            });
            links.forEach(l -> {
                PageModel page = new PageModel();
                page.setPath(l);
                page.setSite(site);
                page.setContent("indexing");
                page.setCode(0);
                pages.add(page);
            });
        } else {
            setContent("Страница недоступна");
        }
        return pages;
    }

    public String getPageUrl() {
        return site.getUrl() + path;
    }

    public void checkStatusCode() throws IOException {
        URL urlToTest = new URL(getPageUrl());
        HttpURLConnection connection = (HttpURLConnection) urlToTest.openConnection();
        connection.connect();
        int statusCode = connection.getResponseCode();
        connection.disconnect();
        setCode(statusCode);
    }

    public Document getPageContentInDoc() throws IOException {
        Document doc = null;
        doc = Jsoup.connect(getPageUrl())
                .timeout(60000)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .get();
        return doc;
    }

    public String html2text(String htmlContent) {
        return Jsoup.parse(htmlContent).text();
    }

    public HashMap<String, Integer> pageLemmatization() throws IOException {
        return Lemmatizator.getInstance().lemmatization(html2text(content));
    }

    public String getTitlePage() {
        return content.substring(content.indexOf("<title>") + 7, content.indexOf("</title>"));
    }

    public String getSnippetPage(Set<String> queryLemmaSet) {
        String snippet = "";
        String contentText = html2text(content);
        HashMap<String, int[]> lemmaContent = null;
        try {
            lemmaContent = Lemmatizator.getInstance().lemmatization4Snippet(contentText);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        lemmaContent.keySet().retainAll(queryLemmaSet);
        for (String word : lemmaContent.keySet()) {
            int[] wordIndexes = lemmaContent.get(word);
            int startSnippet = wordIndexes[0] - 50;
            int endSnippet = wordIndexes[0] + 50;
            int startWord = wordIndexes[0];
            int endWord = wordIndexes[0] + wordIndexes[1];
            String wordBoltFont = "<b>".concat(contentText.substring(startWord, endWord)).concat("</b>");

            if (wordIndexes[0] == -1) {
                snippet = "Слово не нашлось";
                continue;
            }

            if (startSnippet >= 0) {
                snippet = snippet.concat("...").concat(contentText.substring(startSnippet, startWord).concat(wordBoltFont));
            } else {
                snippet = snippet.concat(contentText.substring(0, startWord).concat(wordBoltFont));
            }

            if (endSnippet < contentText.length()) {
                snippet = snippet.concat(contentText.substring(endWord, endSnippet)).concat("...");
            } else {
                snippet = snippet.concat(contentText.substring(endWord, contentText.length() - 1));
            }
        }
        return snippet;
    }


    public boolean isFail(String link) {
        link = link.toLowerCase();
        return link.endsWith(".pdf") ||
                link.endsWith(".doc") ||
                link.endsWith(".jpg") ||
                link.endsWith(".png") ||
                link.endsWith(".jpeg") ||
                link.endsWith(".sql");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageModel pageModel = (PageModel) o;
        return (Objects.equals(path, pageModel.path) && site.getId() == pageModel.getSite().getId());
    }

    @Override
    public int hashCode() {
        return 37 * path.hashCode();
    }

    @Override
    public int compareTo(PageModel page) {
        return CharSequence.compare(this.getPageUrl(), page.getPageUrl());
    }
}
