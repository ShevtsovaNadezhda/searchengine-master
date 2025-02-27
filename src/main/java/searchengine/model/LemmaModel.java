package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;

import java.util.*;

@Entity
@Table(name = "lemma")
@NoArgsConstructor
@Getter
@Setter
public class LemmaModel implements Comparable<LemmaModel> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false)
    private SiteModel site;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(columnDefinition = "INT", nullable = false)
    private int frequency;

    @OneToMany(mappedBy = "lemma", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Set<IndexModel> indexes = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LemmaModel newLemma = (LemmaModel) o;
        return (Objects.equals(lemma, newLemma.getLemma()) && site.getId() == newLemma.getSite().getId());
    }

    @Override
    public int hashCode() {
        return 37 * lemma.hashCode();
    }

    @Override
    public int compareTo(LemmaModel lemma) {
        return Integer.compare(this.frequency, lemma.frequency);
    }
}
