package searchengine.services.implementation;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Lemmatizator {
    private final LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};

    public static Lemmatizator getInstance() throws IOException {
        LuceneMorphology morphology = new RussianLuceneMorphology();
        return new Lemmatizator(morphology);
    }

    private Lemmatizator(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    public HashMap<String, Integer> lemmatization(String text) {
        HashMap<String, Integer> lemmas = new HashMap<>();

        if (!text.isEmpty()) {
            for (String word : transformText2Array(text)) {
                String normalWord = checkWord(word);

                if (normalWord == null) {
                    continue;
                }

                if (lemmas.containsKey(normalWord)) {
                    lemmas.replace(normalWord, lemmas.get(normalWord) + 1);
                } else {
                    lemmas.put(normalWord, 1);
                }
            }
        }
        return lemmas;
    }


    public HashMap<String, int[]> lemmatization4Snippet(String text) {
        HashMap<String, int[]> lemmas4Snippet = new HashMap<>();

        if (!text.isEmpty()) {
            for (String word : transformText2Array(text)) {
                String normalWord = checkWord(word);

                if (normalWord == null) {
                    continue;
                }

                int[] wordInfo = new int[2];
                wordInfo[0] = text.toLowerCase().indexOf(word); //индекс слова в первоначальном тексте страницы
                wordInfo[1] = word.length(); //длина слова в первоначальном тексте
                lemmas4Snippet.put(normalWord, wordInfo);
            }
        }
        return lemmas4Snippet;
    }

    private String[] transformText2Array(String text) {
        return text.toLowerCase()
                .replaceAll("([^а-я\\s])", "")
                .trim()
                .split("\\s+");
    }

    private String checkWord(String word) {
        if (word.isBlank()) {
            return null;
        }

        List<String> wordBaseForms = luceneMorphology.getMorphInfo(word);
        if (anyWordBaseBelongToParticle(wordBaseForms)) {
            return null;
        }

        List<String> normalForms = luceneMorphology.getNormalForms(word);
        if (normalForms.isEmpty()) {
            return null;
        }

        return normalForms.get(0);
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
