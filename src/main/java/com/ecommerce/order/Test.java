package com.ecommerce.order;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Test {

    public static void main(String[] args) {
        String str = "Rohit Negi has nine years of expirence";
        Map<String, Long> result = countVowelsWithStream(str);
        result.forEach((word, count) ->
                System.out.println(word + " → " + count + " vowels"));
    }

    private static Map<String, Long> countVowelsWithStream(String sentence) {
        String vowels = "aeiouAEIOU";

        return Arrays.stream(sentence.split("\\s+"))
                .collect(Collectors.toMap(
                        word -> word,
                        word -> word.chars()
                                .filter(ch -> vowels.indexOf(ch) != -1)
                                .count()
                ));
    }
}
