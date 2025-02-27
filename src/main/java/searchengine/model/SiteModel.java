package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "site")
@NoArgsConstructor
@Getter
@Setter
public class SiteModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEnum status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    @OneToMany (mappedBy="site", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch=FetchType.LAZY)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Set<PageModel> pages = new HashSet<>();

    @OneToMany (mappedBy="site", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch=FetchType.LAZY)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Set<LemmaModel> lemmas = new HashSet<>();

    public void addPageInSet(PageModel page) {
        pages.add(page);
    }

}
