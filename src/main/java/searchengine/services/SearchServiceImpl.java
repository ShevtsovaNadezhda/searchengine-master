package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchDataItem;
import searchengine.dto.search.SearchResponse;
import searchengine.model.LemmaModel;
import searchengine.model.PageModel;
import searchengine.repositories.IndexRepo;
import searchengine.repositories.LemmaRepo;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final LemmaRepo lemmaRepo;
    private final IndexRepo indexRepo;

    @Override
    public SearchResponse searching(String query, String site, Integer offset, Integer limit) {
        List<SearchDataItem> dataList = new ArrayList<>();
        SearchResponse searchResponse = new SearchResponse();
        if (query.isEmpty()) {
            searchResponse.setResult(false);
            searchResponse.setError("Задан пустой поисковый запрос");
        } else {
            HashMap<String, Integer> queryLemmasMap = null;
            try {
                queryLemmasMap = Lemmatizator.getInstance().lemmatization(query);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //TreeMap (Integer - это siteId)
            TreeMap<Integer, List<LemmaModel>> queryLemmaTree = new TreeMap<>();

            Iterable<LemmaModel> lemmaRepoIterable = lemmaRepo.findAll();
            queryLemmasMap.keySet().forEach(queryLemma -> {
                for (LemmaModel lemma : lemmaRepoIterable) {
                    if ((site == null || site.equals(lemma.getSite().getUrl()))
                            && queryLemma.equals(lemma.getLemma())
                            && lemma.getFrequency() <= 50) {
                        if (queryLemmaTree.containsKey(lemma.getSite().getId())) {
                            queryLemmaTree.get(lemma.getSite().getId()).add(lemma);
                        } else {
                            List<LemmaModel> queryLemmas = new ArrayList<>();
                            queryLemmas.add(lemma);
                            queryLemmaTree.put(lemma.getSite().getId(), queryLemmas);
                        }
                    }
                }
            });

            Iterator<Map.Entry<Integer, List<LemmaModel>>> iterator = queryLemmaTree.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, List<LemmaModel>> entry = iterator.next();
                if (entry.getValue().size() != queryLemmasMap.keySet().size()) {
                    iterator.remove();
                }
            }

            TreeMap<PageModel, List<Float>> resultPageTree = new TreeMap<>();

            queryLemmaTree.keySet().forEach(siteId -> {
                Collections.sort(queryLemmaTree.get(siteId));
                for (LemmaModel lemma : queryLemmaTree.get(siteId)) {
                    lemma.getIndexes().forEach(indexModel -> {
                        if (resultPageTree.containsKey(indexModel.getPage())) {
                            resultPageTree.get(indexModel.getPage()).add(indexModel.getRanks());
                        } else {
                            List<Float> ranks = new ArrayList<>();
                            ranks.add(indexModel.getRanks());
                            resultPageTree.put(indexModel.getPage(), ranks);
                        }
                    });
                }
            });

            TreeMap<PageModel, Float> result = new TreeMap<>();

            Iterator<Map.Entry<PageModel, List<Float>>> resultIterator = resultPageTree.entrySet().iterator();
            while (resultIterator.hasNext()) {
                Map.Entry<PageModel, List<Float>> resultEntry = resultIterator.next();
                if (resultEntry.getValue().size() != queryLemmasMap.keySet().size()) {
                    resultIterator.remove();
                } else {
                    float sum = 0F;
                    for (Float rank : resultPageTree.get(resultEntry.getKey())) {
                        sum = sum + rank;
                    }
                    result.put(resultEntry.getKey(), sum);
                }
            }

            float maxRank = result.values().stream().max(Float::compare).get();

            for (PageModel page : result.keySet()) {
                SearchDataItem searchDataItem = new SearchDataItem();
                searchDataItem.setSite(page.getSite().getUrl());
                searchDataItem.setSiteName(page.getSite().getName());
                searchDataItem.setUri(page.getPath());
                searchDataItem.setTitle(page.getTitlePage());
                searchDataItem.setSnippet(page.getSnippetPage(queryLemmasMap.keySet()));
                searchDataItem.setRelevance(result.get(page) / maxRank);
                dataList.add(searchDataItem);
            }

            Collections.sort(dataList, Collections.reverseOrder());
            searchResponse.setDate(dataList);
            searchResponse.setCount(dataList.size());
        }
        return searchResponse;
    }

}
