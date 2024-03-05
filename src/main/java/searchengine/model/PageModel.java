package searchengine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.controllers.ApiController;
import searchengine.repositories.PageRepo;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Objects;

@Entity
@Table(name = "page", indexes = @Index(name = "path_index", columnList = "path"))
@NoArgsConstructor
@Getter
@Setter
public class PageModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @JsonIgnore
    @ManyToOne //(cascade = CascadeType.ALL)
    @JoinColumn(name="site_id", nullable = false, updatable = false)
    private SiteModel site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String path;

    @Column(columnDefinition = "INT", nullable = false)
    private int code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    public HashSet<PageModel> getChildren() throws IOException {
        String url = site.getUrl() + path;
        String regexLink = "(http|ftp|https):\\/\\/([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:\\/~+#-]*[\\w@?^=%&\\/~+#-])";
        HashSet<PageModel> pages = new HashSet<>();
        HashSet<String> links = new HashSet<>();

        URL urlToTest = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlToTest.openConnection();
        int statusCode = 0;
        try {
            connection.connect();
            statusCode = connection.getResponseCode();
            connection.disconnect();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        setCode(statusCode);

        if (statusCode == 200) {
            Document doc = null;
            try {
                doc = Jsoup.connect(url).timeout(60000).get();
                setContent(doc.toString());
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
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

    public boolean isFail(String link) {
        link = link.toLowerCase();
        return link.endsWith(".pdf") ||
                link.endsWith(".doc") ||
                link.endsWith(".jpg") ||
                link.endsWith(".png") ||
                link.endsWith(".jpeg");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageModel pageModel = (PageModel) o;
        return Objects.equals(path, pageModel.path);
    }

    @Override
    public int hashCode() {
        return 37 * path.hashCode();
    }
}
