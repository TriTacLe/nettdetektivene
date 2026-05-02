package ntnu.no.backend.shared;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Predefined list of blocked words for display names and notebook entries.
 * Checked case-insensitively with word-boundary matching to avoid false
 * positives
 *
 * @author norakirknes
 * @version 0.2
 * @since 0.1
 */
public final class CensoredWords {
  private CensoredWords() {}

  public static final Set<String> BLOCKED = Set.of(
      "faen", "helvete", "jævla", "jævel", "dritt", "drittunge",
      "hore", "hora", "fitte", "fitta", "kuk", "kukk", "pikk",
      "rass", "ræva", "ræv", "hestkuk", "idiot", "sex",
      "satan", "forbanna", "forpulte", "knull", "knulle", "knulla", "pule", "puling",
      "bitch", "slut", "whore",
      "fuck", "shit", "ass", "damn", "bastard",
      "dick", "cock", "cunt", "pussy"
  );

  /**
   * Regex that matches any word in {@link #BLOCKED} using
   * word boundaries ({@code \b}) to avoid false positives on substrings
   */
  private static final Pattern BLOCKED_PATTERN = Pattern.compile(
      BLOCKED.stream()
          .map(Pattern::quote)
          .collect(Collectors.joining("|", "\\b(", ")\\b")),
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
  );

  /**
   * Checks if the input contains any blocked word (case-insensitive, word-boundary match).
   *
   * @param input the text to check
   * @return true if a blocked word is found
   */
  public static boolean containsBlockedWord(String input) {
    if (input == null) return false;
    return BLOCKED_PATTERN.matcher(input).find();
  }
}
