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

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="page_id", insertable = false, nullable = false, updatable = false)
    private PageModel page;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name="lemma_id", insertable = false, nullable = false, updatable = false)
    private Lemma lemma;

    @Column(nullable = false)
    private float ranks;
}
