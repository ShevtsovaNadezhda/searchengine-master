package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.search.SearchResponse;
import searchengine.exceptions.SearchException;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.repositories.LemmaRepo;
import searchengine.services.SearchService;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaRepo lemmaRepo;

    @Override
    public SearchResponse searching(String query, String site, int offset, int limit) throws SearchException {
        List<SearchDataItem> dataList = new ArrayList<>();
        SearchResponse searchResponse = new SearchResponse();

        if (query.isEmpty()) {
            throw new SearchException("Задан пустой поисковый запрос");
        } else {
            Set<String> queryLemmasSet = null; //Список лемм из поискового запроса

            try {
                queryLemmasSet = Lemmatizator.getInstance().lemmatization(query).keySet();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            TreeMap<PageModel, Float> resultPages
                    = findResultPages(findQueryLemmasInBD(queryLemmasSet, site), queryLemmasSet.size());

            if (!resultPages.isEmpty()) {
                float maxRank = resultPages.values().stream().max(Float::compare).get();
                for (PageModel page : resultPages.keySet()) {
                    SearchDataItem searchDataItem = new SearchDataItem();
                    searchDataItem.setSite(page.getSite().getUrl());
                    searchDataItem.setSiteName(page.getSite().getName());
                    searchDataItem.setUri(page.getPath());
                    searchDataItem.setTitle(page.getTitlePage());
                    searchDataItem.setSnippet(page.getSnippetPage(queryLemmasSet));
                    searchDataItem.setRelevance(resultPages.get(page) / maxRank);
                    dataList.add(searchDataItem);
                }
                Collections.sort(dataList, Collections.reverseOrder());
            }
            searchResponse.setCount(dataList.size());
            if ((offset + limit) <= dataList.size()) {
                searchResponse.setData(dataList.subList(offset, offset + limit));
            } else {
                searchResponse.setData(dataList.subList(offset, dataList.size()));
            }
        }
        return searchResponse;
    }

    private TreeMap<Integer, List<LemmaModel>> findQueryLemmasInBD(Set<String> queryLemmasSet, String site) {
        TreeMap<Integer, List<LemmaModel>> queryLemmasTree = new TreeMap<>();

        Iterable<LemmaModel> lemmaRepoIterable = lemmaRepo.findAll();

        //Находим в БД леммы, которые отвечают запросу и помещаем их в дерево,
        // где ключ - это siteId, значение - список лемм.
        queryLemmasSet.forEach(queryLemma -> {
            for (LemmaModel lemma : lemmaRepoIterable) {
                if ((site == null || site.equals(lemma.getSite().getUrl()))
                        && queryLemma.equals(lemma.getLemma())
                        && lemma.getFrequency() <= 50) {
                    if (queryLemmasTree.containsKey(lemma.getSite().getId())) {
                        queryLemmasTree.get(lemma.getSite().getId()).add(lemma);
                    } else {
                        List<LemmaModel> queryLemmasList = new ArrayList<>();
                        queryLemmasList.add(lemma);
                        queryLemmasTree.put(lemma.getSite().getId(), queryLemmasList);
                    }
                }
            }
        });

        //Удаляем из дерева ключи, где количество лемм не соответсвует количетсву лемм в запросе
        Iterator<Map.Entry<Integer, List<LemmaModel>>> iterator = queryLemmasTree.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, List<LemmaModel>> entry = iterator.next();
            if (entry.getValue().size() != queryLemmasSet.size()) {
                iterator.remove();
            }
        }
        return queryLemmasTree;
    }

    private TreeMap<PageModel, Float> findResultPages(TreeMap<Integer, List<LemmaModel>> queryLemmasTree,
                                                      int wordQueryCount) {
        //Дерево где ключ - страница, а значения - список rank, которые отвечают леммам из запроса
        TreeMap<PageModel, List<Float>> queryPagesTree = new TreeMap<>();
        TreeMap<PageModel, Float> resultPages = new TreeMap<>();

        queryLemmasTree.keySet().forEach(siteId -> {
            Collections.sort(queryLemmasTree.get(siteId));
            for (LemmaModel lemma : queryLemmasTree.get(siteId)) {
                lemma.getIndexes().forEach(indexModel -> {
                    if (queryPagesTree.containsKey(indexModel.getPage())) {
                        queryPagesTree.get(indexModel.getPage()).add(indexModel.getRanks());
                    } else {
                        List<Float> ranks = new ArrayList<>();
                        ranks.add(indexModel.getRanks());
                        queryPagesTree.put(indexModel.getPage(), ranks);
                    }
                });
            }
        });

        //Удаляем записи, где количество rank не соответсвует количеству слов из запроса, т.е. оставляем
        //страницы, на которых встречаются все слова из запроса и считаем суммарный rank для каждой страницы
        Iterator<Map.Entry<PageModel, List<Float>>> resultIterator = queryPagesTree.entrySet().iterator();
        while (resultIterator.hasNext()) {
            Map.Entry<PageModel, List<Float>> resultEntry = resultIterator.next();
            if (resultEntry.getValue().size() != wordQueryCount) {
                resultIterator.remove();
            } else {
                float sum = 0F;
                for (Float rank : queryPagesTree.get(resultEntry.getKey())) {
                    sum = sum + rank;
                }
                resultPages.put(resultEntry.getKey(), sum);
            }
        }
        return resultPages;
    }

}
