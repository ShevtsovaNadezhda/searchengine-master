package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.IndexModel;

public interface IndexRepo extends CrudRepository<IndexModel, Integer> {
}
