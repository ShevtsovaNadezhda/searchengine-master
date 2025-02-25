package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Lemmatizator {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    public static Lemmatizator getInstance() throws IOException {
        LuceneMorphology morphology= new RussianLuceneMorphology();
        return new Lemmatizator(morphology);
    }

    private Lemmatizator(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    public HashMap<String, Integer> lemmatization(String text) {
        String newText = html2text(text);
        HashMap<String, Integer> lemmas = new HashMap<>();

        if (!newText.isEmpty()) {
            String[] words = newText.toLowerCase()
                    .replaceAll("([^а-я\\s])", "")
                    .trim()
                    .split("\\s+");

            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }

                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }

                List<String> normalForms = luceneMorphology.getNormalForms(word);
                if (normalForms.isEmpty()) {
                    continue;
                }

                String normalWord = normalForms.get(0);

                if (lemmas.containsKey(normalWord)) {
                    lemmas.replace(normalWord, lemmas.get(normalWord) + 1);
                } else {
                    lemmas.put(normalWord, 1);
                }
            }
        }
        return lemmas;
    }


    public HashMap<String, Integer> lemmatization4Snippet (String text) {
        String newText = html2text(text);
        HashMap<String, Integer> searchMap = new HashMap<>();

        if (!newText.isEmpty()) {
            String[] words = newText.toLowerCase()
                    .replaceAll("([^а-я\\s])", "")
                    .trim()
                    .split("\\s+");

            for (String word : words) {
                if (word.isBlank()) {
                    continue;
                }

                List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
                if (anyWordBaseBelongToParticle(wordBaseForms)) {
                    continue;
                }

                List<String> normalForms = luceneMorphology.getNormalForms(word);
                if (normalForms.isEmpty()) {
                    continue;
                }

                String normalWord = normalForms.get(0);
                int wordIndex = newText.toLowerCase().indexOf(word);
                searchMap.put(normalWord, wordIndex);
            }
        }

        return searchMap;
    }

    private boolean anyWordBaseBelongToParticle(List<String> wordBaseForms) {
        return wordBaseForms.stream().anyMatch(this::hasParticleProperty);
    }

    private boolean hasParticleProperty(String wordBase) {
        for (String property : particlesNames) {
            if (wordBase.toUpperCase().contains(property)) {
                return true;
            }
        }
        return false;
    }
}
