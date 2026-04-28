package ntnu.no.backend.shared;

import java.util.Set;

/**
 * Predefined list of blocked words for display names and notebook entries.
 * Checked case-insensitively with substring matching.
 *
 * @author norakirknes
 * @version 0.1
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
   * Checks if the input contains any blocked word (case-insensitive, substring match).
   *
   * @param input the text to check
   * @return true if a blocked word is found
   */
  public static boolean containsBlockedWord(String input) {
    if (input == null) return false;
    String lower = input.toLowerCase();
    return BLOCKED.stream().anyMatch(lower::contains);
  }
}
