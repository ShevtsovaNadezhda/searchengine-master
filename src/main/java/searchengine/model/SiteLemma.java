package searchengine.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "site_lemma")
@NoArgsConstructor
@Getter
@Setter
public class SiteLemma {

    @EmbeddedId
    private SiteLemmaKey siteLemmaKey;
    @Column(name = "site_id", insertable = false, updatable = false)
    private int siteId;

    @Column(name = "lemma_id", insertable = false, updatable = false)
    private int lemmaId;

    @Column(columnDefinition = "INT", nullable = false)
    private int frequency;

}
