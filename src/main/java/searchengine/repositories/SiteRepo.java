package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import searchengine.model.SiteModel;

public interface SiteRepo extends CrudRepository<SiteModel, Integer> {
}
