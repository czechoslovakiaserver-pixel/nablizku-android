package cz.nablizku.app;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ScamAnalyzer {
    private ScamAnalyzer() {}

    public static int score(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.forLanguageTag("cs-CZ"));
        int score = 0;
        score += containsAny(text, Arrays.asList("pošli", "pošlete", "převod", "účet", "bitcoin", "peníze")) ? 2 : 0;
        score += containsAny(text, Arrays.asList("heslo", "pin", "ověřovací kód", "bankovnictví")) ? 3 : 0;
        score += containsAny(text, Arrays.asList("ihned", "spěchá", "okamžitě", "poslední šance", "zablokován")) ? 2 : 0;
        score += containsAny(text, Arrays.asList("http://", "https://", "klikněte", "klikni na odkaz")) ? 2 : 0;
        score += containsAny(text, Arrays.asList("nové číslo", "rozbil se mi telefon", "mami", "tati")) ? 3 : 0;
        return score;
    }

    public static String label(String message) {
        int score = score(message);
        return score >= 4 ? "Pravděpodobný podvod" : score >= 2 ? "Podezřelá zpráva" : "Bez zjevných rizik";
    }

    private static boolean containsAny(String text, List<String> words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }
}
