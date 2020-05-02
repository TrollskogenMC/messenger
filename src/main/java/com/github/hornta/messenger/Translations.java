package com.github.hornta.messenger;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Translations {
  private static final Pattern ymlResources = Pattern.compile(".+\\.yml$");
  private final Pattern translationResource;
  private final Map<String, File> languageFiles;
  private final File translationsDirectory;
  private final JavaPlugin plugin;
  private final MessageManager messageManager;

  public Translations(JavaPlugin plugin, MessageManager messageManager) {
    this.plugin = plugin;
    this.messageManager = messageManager;
    translationResource = Pattern.compile("^translations/" + plugin.getDescription().getName() + "/.+\\.yml$");
    languageFiles = new HashMap<>();
    translationsDirectory = new File(plugin.getDataFolder() + File.separator + "translations");
    saveDefaults();
    readLanguageFiles(translationsDirectory);
  }

  private Map<String, InputStream> getResourceTranslationStreams() {
    Map<String, InputStream> streams = new HashMap<>();

    Reflections reflections = new Reflections(null, new ResourcesScanner());
    Set<String> resourceList = reflections.getResources(ymlResources);

    for(String resource : resourceList) {
      String filename = resource.lastIndexOf('/') == -1 ? resource : resource.substring(resource.lastIndexOf('/') + 1);
      String path = reflections.getStore().get(ResourcesScanner.class, filename).toArray(new String[0])[0];
      if(translationResource.matcher(path).matches()) {
        streams.put(path, getClass().getClassLoader().getResourceAsStream(path));
      }
    }

    return streams;
  }

  public Translation createTranslation(String language) {
    if(!languageFiles.containsKey(language)) {
      plugin.getLogger().log(Level.SEVERE, "Couldn't find translation `" + language + "`");
      return null;
    }

    Translation translation = new Translation(languageFiles.get(language), language);
    translation.load(plugin, messageManager.getIdentifiers());
    return translation;
  }

  private void readLanguageFiles(File translationsDirectory) {
    File[] files = translationsDirectory.listFiles();

    if(files == null) {
      return;
    }

    for (File file : files) {
      if (!file.isFile()) {
        readLanguageFiles(file);
        continue;
      }

      if(!file.getName().endsWith(".yml")) {
        continue;
      }

      languageFiles.put(getFilenameWithoutExtension(file), file);
    }
  }

  public boolean saveDefaults() {
    translationsDirectory.mkdirs();
    for(Map.Entry<String, InputStream> entry : getResourceTranslationStreams().entrySet()) {
      try {
        File destination = new File(plugin.getDataFolder(), entry.getKey().replaceAll(plugin.getDescription().getName() + "/", ""));
        boolean result = saveTranslationResource(entry, destination);
        if (!result) {
          return false;
        }
      } catch (IllegalArgumentException e) {
      }
    }

    return true;
  }

  private boolean saveTranslationResource(Map.Entry<String, InputStream> resource, File dest) {
    if(dest.exists()) {
      plugin.getLogger().log(Level.INFO, "Found existing translation file in " + dest.getName() + ".");

      YamlConfiguration destYaml = YamlConfiguration.loadConfiguration(dest);
      YamlConfiguration resourceYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(resource.getValue(), StandardCharsets.UTF_8));

      // delete keys not found in resource translation
      for(String key : destYaml.getKeys(true)) {
        if(!resourceYaml.contains(key)) {
          destYaml.set(key, null);
          plugin.getLogger().log(Level.INFO, "Deleted unused key `" + key + "` from `" + dest.getName() + "`.");
        }
      }

      // add keys from resource not found in destination
      for(String key : resourceYaml.getKeys(true)) {
        if(!destYaml.contains(key)) {
          destYaml.set(key, resourceYaml.get(key));
          plugin.getLogger().log(Level.INFO, "Added missing key `" + key + "` to `" + dest.getName() + "`.");
        }
      }

      try {
        destYaml.save(dest);
      } catch (IOException ex) {
        plugin.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
        return false;
      }
    } else {
      try {
        saveResource(resource.getKey(), resource.getKey().replaceAll(plugin.getDescription().getName() + "/", ""));
      } catch (IllegalArgumentException ex) {
        plugin.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
      }
      plugin.getLogger().log(Level.INFO, "Saving new translation file to `" + dest.getName() + "`");
    }

    return true;
  }

  public void saveResource(String resourcePath, String outPath) {
    if (resourcePath == null || resourcePath.equals("")) {
      throw new IllegalArgumentException("ResourcePath cannot be null or empty");
    }

    resourcePath = resourcePath.replace('\\', '/');
    InputStream in = plugin.getResource(resourcePath);
    if (in == null) {
      throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
    }

    File outFile = new File(plugin.getDataFolder(), outPath);
    int lastIndex = outPath.lastIndexOf('/');
    File outDir = new File(plugin.getDataFolder(), outPath.substring(0, Math.max(lastIndex, 0)));

    if (!outDir.exists()) {
      outDir.mkdirs();
    }

    try {
      if (!outFile.exists()) {
        OutputStream out = new FileOutputStream(outFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        out.close();
        in.close();
      } else {
        plugin.getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
      }
    } catch (IOException ex) {
      plugin.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
    }
  }

  private static String getFilenameWithoutExtension(File file) {
    String filename = file.getName();
    int lastDotIndex = filename.lastIndexOf('.');

    if(lastDotIndex == -1) {
      return filename;
    }

    return filename.substring(0, lastDotIndex);
  }
}
