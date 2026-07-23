package cz.nablizku.app;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ScamAnalyzer {
    private ScamAnalyzer() {}

    public static int score(String message) {
        String text = message == null ? "" : message.toLowerCase(Locale.forLanguageTag("cs-CZ"));
        int score = 0;
        score += containsAny(text, Arrays.asList(
            "pošli", "pošlete", "převod", "účet", "bitcoin", "peníze", "platbu"
        )) ? 2 : 0;
        score += containsAny(text, Arrays.asList(
            "heslo", "pin", "ověřovací kód", "bankovnictví", "číslo karty"
        )) ? 3 : 0;
        score += containsAny(text, Arrays.asList(
            "ihned", "spěchá", "okamžitě", "poslední šance", "zablokován"
        )) ? 2 : 0;
        score += containsAny(text, Arrays.asList(
            "http://", "https://", "klikněte", "klikni na odkaz"
        )) ? 2 : 0;
        score += containsAny(text, Arrays.asList(
            "nové číslo", "rozbil se mi telefon", "mami", "tati"
        )) ? 3 : 0;
        score += containsAny(text, Arrays.asList(
            "vyhráli jste", "výhra", "sleva", "nabídka", "zdarma", "akce končí"
        )) ? 2 : 0;
        return score;
    }

    public static String label(String message) {
        int score = score(message);
        if (score >= 4) return "Pravděpodobný podvod";
        if (score >= 2) return "Podezřelý spam";
        return "Bez zjevných rizik";
    }

    private static boolean containsAny(String text, List<String> words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }
}
