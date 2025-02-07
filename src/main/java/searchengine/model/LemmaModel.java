package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "lemma")
@NoArgsConstructor
@Getter
@Setter
public class LemmaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name="site_id", nullable = false, updatable = false)
    private SiteModel site;

    /*@ManyToMany(mappedBy = "lemmas")
    private List<SiteModel> sites = new ArrayList<>();*/

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(columnDefinition = "INT", nullable = false)
    private int frequency;

    @OneToMany (cascade = CascadeType.REMOVE, mappedBy="lemma", fetch=FetchType.LAZY)
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
}
