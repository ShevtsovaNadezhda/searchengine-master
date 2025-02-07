package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.LemmaModel;

public interface LemmaRepo extends CrudRepository<LemmaModel, Integer> {
}
