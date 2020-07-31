package se.hornta.messenger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class Translation {
  private final File file;
  private final String language;
  private final Map<Enum, String> translations = new HashMap<>();

  Translation(File file, String language) {
    this.file = file;
    this.language = language;
  }

  public String getLanguage() {
    return language;
  }

  public void load(JavaPlugin plugin, Map<Enum, String> identifiers) {
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

    Set<String> validationErrors = new HashSet<>();

    Set<String> yamlKeys = yaml.getKeys(true);
    for(String key : yamlKeys) {
      if(yaml.isConfigurationSection(key)) {
        continue;
      }

      if (!yaml.isString(key)) {
        validationErrors.add("Expected \"" + key + "\" in \"" + file.getName() + "\" to be of type string.");
      }
    }

    try {
      yaml.save(file);
    } catch (IOException ex) {
      plugin.getLogger().log(Level.SEVERE, "Failed to save to " + file.getName(), ex);
    }

    if (!validationErrors.isEmpty()) {
      plugin.getLogger().log(Level.SEVERE, "*** " + file.getName() + " has bad values ***");
      for(String string : validationErrors) {
        plugin.getLogger().log(Level.SEVERE, "*** " + string + " ***");
      }
      throw new RuntimeException("translation has some validation errors");
    }

    for(Map.Entry<Enum, String> entry : identifiers.entrySet()) {
      if(!yaml.contains(entry.getValue())) {
        continue;
      }
      translations.put(entry.getKey(), yaml.getString(entry.getValue()));
    }
  }

  public String getTranslatedString(Enum key) {
    return translations.get(key);
  }

  public boolean hasIdentifier(Enum id) {
    return translations.containsKey(id);
  }
}
