package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.services.Lemmatizator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path"))
@NoArgsConstructor
@Getter
@Setter
public class PageModel implements Comparable<PageModel>{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false) // updatable = false)
    private SiteModel site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(columnDefinition = "INT", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "page", fetch = FetchType.LAZY)
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

    public HashMap<String, Integer> pageLemmatization () throws IOException {
        return Lemmatizator.getInstance().lemmatization(content);
    }

    public String getTitlePage() {
        return content.substring(content.indexOf("<title>") + 7, content.indexOf("</title>"));
    }

    public String getSnippetPage(Set<String> queryLemmaSet) {
        String snippet = "";
        String contentText = Jsoup.parse(content).text();
        HashMap<String, Integer> lemmaContent = null;
        try {
            lemmaContent = Lemmatizator.getInstance().lemmatization4Snippet(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Integer> wordIndexes = new ArrayList<>();
        for (String queryLemma : queryLemmaSet) {
            for(String word : lemmaContent.keySet()) {
                if(word.equals(queryLemma)) {
                    wordIndexes.add(lemmaContent.get(word));
                }
            }
        }
        Collections.sort(wordIndexes);
        for (Integer integer : wordIndexes) {
            if(integer == -1) {
                snippet = "Слово не нашлось";
                continue;
            }
            if (integer >= 50 && (integer + 200) < contentText.length()) {
                snippet = snippet + " ... " + contentText.substring(integer - 50, integer + 200);
            } else if (integer < 50 && (integer + 200) < contentText.length()) {
                snippet = snippet + " ... " + contentText.substring(0, integer + 200);
            } else if (integer >= 50 && (integer + 200) >= contentText.length()) {
                snippet = snippet + " ... " + contentText.substring(integer - 50, contentText.length() - 1);
            } else {
                snippet = contentText;
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
