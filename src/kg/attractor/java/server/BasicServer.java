package kg.attractor.java.server;

import com.sun.net.httpserver.*;
import freemarker.template.*;
import kg.attractor.java.model.Candidate;
import kg.attractor.java.utils.FileUtil;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class BasicServer {
  private static final Configuration freemarker = initFreeMarker();
  private final Map<String, RouteHandler> routes = new HashMap<>();
  private final String dataDir = "data";
  private final HttpServer server;
  private final List<Candidate> candidates;

  public BasicServer(String host, int port) throws IOException {
    server = createServer(host, port);
    registerCommonHandlers();

    this.candidates = FileUtil.readCandidates();
    assignIdsIfMissing();
    FileUtil.saveCandidates(candidates);

    registerGet("/candidates", this::candidatesHandler);
    registerPost("/vote", this::voteHandler);
    registerGet("/thankyou", this::thankYouHandler);
    registerGet("/votes", this::votesHandler);
  }

  private void assignIdsIfMissing() {
    boolean changed = false;
    for (Candidate c : candidates) {
      if (c.getId() == null || c.getId().isEmpty()) {
        c.setId(UUID.randomUUID().toString());
        changed = true;
      }
    }
    if (changed) FileUtil.saveCandidates(candidates);
  }

  private void candidatesHandler(HttpExchange exchange) {
    Map<String, Object> data = new HashMap<>();
    data.put("candidates", candidates);
    renderTemplate(exchange, "candidates.ftl", data);
  }

  private void voteHandler(HttpExchange exchange) {
    String body = getRequestBody(exchange);
    Map<String, String> params = Utils.parseUrlEncoded(body, "&");

    String id = params.get("candidateId");

    Optional<Candidate> candidatesTier = candidates.stream()
            .filter(c -> c.getId().equals(id))
            .findFirst();

    if (candidatesTier.isEmpty()) {
      renderError(exchange, "Кандидат с указанным ID не найден!");
      return;
    }

    Candidate c = candidatesTier.get();
    c.addVote();
    FileUtil.saveCandidates(candidates);

    redirect(exchange, "/thankyou?id=" + id);
  }

  private void thankYouHandler(HttpExchange exchange) {
    Map<String, String> params = Utils.parseUrlEncoded(exchange.getRequestURI().getQuery(), "&");

    String id = params.get("id");

    Optional<Candidate> candidatesTier = candidates.stream()
            .filter(c -> c.getId().equals(id))
            .findFirst();

    if (candidatesTier.isEmpty()) {
      renderError(exchange, "Кандидат с указанным ID не найден!");
      return;
    }

    Candidate c = candidatesTier.get();
    int total = candidates.stream().mapToInt(Candidate::getVotes).sum();

    Map<String, Object> data = new HashMap<>();
    data.put("candidate", c);
    data.put("percent", c.getPercent(total));

    renderTemplate(exchange, "thankyou.ftl", data);
  }

  private void votesHandler(HttpExchange exchange) {

    int total = candidates.stream().mapToInt(Candidate::getVotes).sum();

    List<Candidate> sorted = candidates.stream()
            .sorted(Comparator.comparingInt(Candidate::getVotes).reversed())
            .collect(Collectors.toList());

    Map<String, Object> data = new HashMap<>();
    data.put("candidates", sorted);
    data.put("totalVotes", total);

    renderTemplate(exchange, "votes.ftl", data);
  }

  private void renderError(HttpExchange exchange, String errorMessage) {
    Map<String, Object> data = new HashMap<>();
    data.put("error", errorMessage);
    renderTemplate(exchange, "error.ftl", data);
  }

  private void redirect(HttpExchange exchange, String location) {
    exchange.getResponseHeaders().set("Location", location);
    try {
      exchange.sendResponseHeaders(303, -1);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static Configuration initFreeMarker() {
    try {
      Configuration cfg = new Configuration(Configuration.VERSION_2_3_29);
      cfg.setDirectoryForTemplateLoading(new File("data"));

      cfg.setDefaultEncoding("UTF-8");
      cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      cfg.setLogTemplateExceptions(false);
      cfg.setWrapUncheckedExceptions(true);
      cfg.setFallbackOnNullLoopVariable(false);
      return cfg;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected void renderTemplate(HttpExchange exchange, String templateFile, Object dataModel) {
    try {
      Template temp = freemarker.getTemplate(templateFile);

      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (OutputStreamWriter writer = new OutputStreamWriter(stream)) {

        temp.process(dataModel, writer);
        writer.flush();

        var data = stream.toByteArray();

        sendByteData(exchange, ResponseCodes.OK, ContentType.TEXT_HTML, data);
      }
    } catch (IOException | TemplateException e) {
      e.printStackTrace();
      try {
        String errorMessage = "500 Server Error:\n" + e.getMessage();
        byte[] resp = errorMessage.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(500, resp.length);
        exchange.getResponseBody().write(resp);
        exchange.getResponseBody().close();
      } catch (IOException io) {
        System.err.println("Не удалось отправить ошибку клиенту");
      }
    }
  }

  protected static String getRequestBody(HttpExchange exchange) {
    InputStream stream = exchange.getRequestBody();
    Charset charset = StandardCharsets.UTF_8;
    InputStreamReader isr = new InputStreamReader(stream, charset);

    try (BufferedReader br = new BufferedReader(isr)) {
      return br.lines().collect(Collectors.joining(""));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return "";
  }

  private static String makeKey(String method, String route) {
    route = ensureStartsWithSlash(route);
    return String.format("%s %s", method.toUpperCase(), route);
  }

  private static String makeKey(HttpExchange exchange) {
    String method = exchange.getRequestMethod();
    String path = exchange.getRequestURI().getPath();

    if (path.endsWith("/") && path.length() > 1) {
      path = path.substring(0, path.length() - 1);
    }

    int index = path.lastIndexOf(".");
    String extOrPath = index != -1 ? path.substring(index).toLowerCase() : path;

    return makeKey(method, extOrPath);
  }

  private static String ensureStartsWithSlash(String route) {
    if (route.startsWith(".")) {
      return route;
    }
    return route.startsWith("/") ? route : "/" + route;
  }

  private static void setContentType(HttpExchange exchange, ContentType type) {
    exchange.getResponseHeaders().set("Content-Type", String.valueOf(type));
  }

  private static HttpServer createServer(String host, int port) throws IOException {
    var msg = "Starting server on http://%s:%s/%n";
    System.out.printf(msg, host, port);
    var address = new InetSocketAddress(host, port);
    return HttpServer.create(address, 50);
  }

  private void registerCommonHandlers() {
    server.createContext("/", this::handleIncomingServerRequests);

    registerGet("/", this::candidatesHandler);

    registerFileHandler(".css", ContentType.TEXT_CSS);
    registerFileHandler(".html", ContentType.TEXT_HTML);
    registerFileHandler(".jpg", ContentType.IMAGE_JPEG);
    registerFileHandler(".jpeg", ContentType.IMAGE_JPEG);
    registerFileHandler(".png", ContentType.IMAGE_PNG);

  }

  protected final void registerGet(String route, RouteHandler handler) {
    registerGenericHandler("GET", route, handler);
  }

  protected final void registerPost(String route, RouteHandler handler) {
    registerGenericHandler("POST", route, handler);
  }

  protected final void registerGenericHandler(String method, String route, RouteHandler handler) {
    getRoutes().put(makeKey(method, route), handler);
  }

  protected final void registerFileHandler(String fileExt, ContentType type) {
    registerGet(fileExt, exchange -> sendFile(exchange, makeFilePath(exchange), type));
  }

  protected final Map<String, RouteHandler> getRoutes() {
    return routes;
  }

  protected final void sendFile(HttpExchange exchange, Path pathToFile, ContentType contentType) {
    try {
      if (Files.notExists(pathToFile)) {
        respond404(exchange);
        return;
      }
      var data = Files.readAllBytes(pathToFile);
      sendByteData(exchange, ResponseCodes.OK, contentType, data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Path makeFilePath(HttpExchange exchange) {
    return makeFilePath(exchange.getRequestURI().getPath());
  }

  protected Path makeFilePath(String... s) {
    return Path.of(dataDir, s);
  }

  protected final void sendByteData(HttpExchange exchange, ResponseCodes responseCode,
                                    ContentType contentType, byte[] data) throws IOException {
    try (var output = exchange.getResponseBody()) {
      setContentType(exchange, contentType);
      exchange.sendResponseHeaders(responseCode.getCode(), 0);
      output.write(data);
      output.flush();
    }
  }

  private void respond404(HttpExchange exchange) {
    try {
      var data = "404 Not found".getBytes();
      sendByteData(exchange, ResponseCodes.NOT_FOUND, ContentType.TEXT_PLAIN, data);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void handleIncomingServerRequests(HttpExchange exchange) {
    var route = getRoutes().getOrDefault(makeKey(exchange), this::respond404);
    route.handle(exchange);
  }

  public final void start() {
    server.start();
  }
}
