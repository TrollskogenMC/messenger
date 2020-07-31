package se.hornta.messenger;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
  private static MessageManager instance;
  private static final Pattern placeholderPattern = Pattern.compile("<([a-z_]+)(?:\\|(.+))?>", Pattern.CASE_INSENSITIVE);
  private Translation translation;
  private static final Map<String, List<String>> placeholderValues = new HashMap<>();
  private static final Map<String, Enum> placeholderKeys = new HashMap<>();
  private final Map<Enum, String> identifiers;

  public MessageManager(Map<Enum, String> identifiers) {
    if(instance == null) {
      instance = this;
    }
    this.identifiers = identifiers;
  }

  public void setTranslation(Translation translation) {
    this.translation = translation;
  }

  public static MessageManager getInstance() {
    return instance;
  }

  String transformPattern(String input) {
    return StringReplacer.replace(input, placeholderPattern, (Matcher m) -> {
      String placeholder = m.group(1);

      Map<PlaceholderOption, String> options = Collections.emptyMap();

      if (m.group(2) != null) {
        options = getPlaceholderOptions(m.group(2));
      }

      String delimiter = "";
      if (placeholderValues.containsKey(placeholder)) {
        if (options.containsKey(PlaceholderOption.DELIMITER) && options.get(PlaceholderOption.DELIMITER) != null) {
          delimiter = options.get(PlaceholderOption.DELIMITER);
        }
        return String.join(delimiter, placeholderValues.get(placeholder));
      } else if(placeholderKeys.containsKey(placeholder)) {
        return getTranslation(placeholderKeys.get(placeholder));
      } else {
        return m.group();
      }
    });
  }

  public static void sendMessage(CommandSender commandSender, Enum id) {
    String message = getInstance().getTranslation(id);
    message = getInstance().transformPlaceholders(message);
    commandSender.sendMessage(message);
  }

  public String transformPlaceholders(String input) {
    String transformed = transformPattern(input);
    placeholderValues.clear();
    placeholderKeys.clear();

    return transformed;
  }

  public static void setValue(String key, Enum id) {
    placeholderKeys.put(key, id);
  }

  public static void setValue(String key, Object value) {
    if (value == null) {
      value = Collections.emptyList();
    }

    if(!(value instanceof Collection<?>)) {
      value = Collections.singletonList(value.toString());
    }

    placeholderValues.put(key.toLowerCase(Locale.ENGLISH), (List<String>) value);
  }

  public static void broadcast(Enum id) {
    String message = getInstance().getTranslation(id);
    message = getInstance().transformPlaceholders(message);
    Bukkit.broadcastMessage(message);
  }

  public static String getMessage(Enum id) {
    String message = getInstance().getTranslation(id);
    return getInstance().transformPlaceholders(message);
  }

  private static Map<PlaceholderOption, String> getPlaceholderOptions(String options) {
    options = options.trim();
    Map<PlaceholderOption, String> map = new EnumMap<>(PlaceholderOption.class);
    for (String option : options.split(",")) {
      option = option.trim();
      PlaceholderOption key;
      String value;
      if(option.contains(":")) {
        key = PlaceholderOption.fromString(option.substring(0, option.lastIndexOf(":")).trim());
        value = option.substring(option.lastIndexOf(":") + 1).trim();
      } else {
        key = PlaceholderOption.fromString(option);
        value = null;
      }

      if(key != null) {
        map.put(key, value);
      }
    }

    return map;
  }

  private String getTranslation(Enum id) {
    if(translation.hasIdentifier(id)) {
      return translation.getTranslatedString(id);
    }
    return id.name();
  }

  public Map<Enum, String> getIdentifiers() {
    return identifiers;
  }
}
