package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.PageModel;
import searchengine.model.SiteModel;

public interface PageRepo extends CrudRepository<PageModel, Integer> {
}
