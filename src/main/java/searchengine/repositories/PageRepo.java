package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.PageModel;

public interface PageRepo extends CrudRepository<PageModel, Integer> {
}
