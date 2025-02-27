package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "indexes")
@NoArgsConstructor
@Getter
@Setter
public class IndexModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name="page_id", nullable = false)
    private PageModel page;

    @ManyToOne
    @JoinColumn(name="lemma_id", nullable = false)
    private LemmaModel lemma;

    @Column(nullable = false)
    private float ranks;
}
