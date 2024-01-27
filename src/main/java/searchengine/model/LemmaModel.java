package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemma")
@NoArgsConstructor
@Getter
@Setter
public class LemmaModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToMany(mappedBy = "lemmas")
    private List<SiteModel> sites = new ArrayList<>();

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String lemma;

    @Column(columnDefinition = "INT", nullable = false)
    private int frequency;
}
