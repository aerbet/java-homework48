package kg.attractor.java.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import kg.attractor.java.model.Candidate;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileUtil {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path PATH = Paths.get("data", "candidates.json");

  public static List<Candidate> readCandidates() {
    if (!Files.exists(PATH)) return new ArrayList<>();

    try (Reader reader = Files.newBufferedReader(PATH)) {
      List<Candidate> candidates = GSON.fromJson(reader, new TypeToken<List<Candidate>>() {}.getType());

      boolean needSave = false;

      for (Candidate c : candidates) {
        if (c.getId() == null || c.getId().isEmpty()) {
          c.setId(UUID.randomUUID().toString());
          needSave = true;
        }
      }

      if (needSave) {
        saveCandidates(candidates);
      }

      return candidates;
    } catch (IOException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public static void saveCandidates(List<Candidate> candidates) {
    try (Writer writer = Files.newBufferedWriter(PATH)) {
      GSON.toJson(candidates, writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
