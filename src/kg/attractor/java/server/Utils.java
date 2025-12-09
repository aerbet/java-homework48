package kg.attractor.java.server;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
  private Utils() {

  }

  public static Map<String, String> parseUrlEncoded(String raw, String delimiter) {
    String[] parts = raw.split(delimiter);
    Stream<Map.Entry<String, String>> stream = Arrays.stream(parts)
            .map(Utils::decode)
            .filter(Optional::isPresent)
            .map(Optional::get);

    return stream.collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
    ));
  }

  private static Optional<Map.Entry<String, String>> decode(String kv) {
    if (!kv.contains("=")) {
      return Optional.empty();
    }

    String[] parts = kv.split("=");
    if (parts.length != 2) {
      return Optional.empty();
    }

    Charset utf8 = StandardCharsets.UTF_8;
    String key = URLDecoder.decode(parts[0].trim(), utf8);
    String value = URLDecoder.decode(parts[1].trim(), utf8);

    return Optional.of(Map.entry(key, value));
  }
}
