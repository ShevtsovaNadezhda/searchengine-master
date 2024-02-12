package searchengine.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Embeddable
@Getter
@Setter
public class SiteLemmaKey {
    @Column(name = "site_id")
    private int siteId;
    @Column(name = "lemma_id")
    private int lemmaId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SiteLemmaKey that = (SiteLemmaKey) o;
        return siteId == that.siteId && lemmaId == that.lemmaId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId, lemmaId);
    }
}
