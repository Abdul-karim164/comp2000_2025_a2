import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
    import java.net.http.HttpRequest;
    import java.net.http.HttpResponse;
    import java.util.List;
    import java.util.concurrent.CopyOnWriteArrayList;
    import java.util.stream.Stream;

    public final class WeatherFeed {
        private static final String ENDPOINT = "http://13.238.167.130/weather";

        private static final WeatherFeed INSTANCE = new WeatherFeed();

        private final List<WeatherListener> listeners = new CopyOnWriteArrayList<>();
        private volatile boolean running = false;

        private WeatherFeed() {
            // private constructor for singleton
        }
        public static WeatherFeed getInstance() {
            return INSTANCE;
        }

        public void registerListener(WeatherListener listener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
                ensureRunning();
            }
        }

        public void unregisterListener(WeatherListener listener) {
            listeners.remove(listener);
        }

        private synchronized void ensureRunning() {
            if (running) return;
            running = true;
            Thread t = new Thread(this::runFeed, "WeatherFeed-Thread");
            t.setDaemon(true);
            t.start();
        }

        private void runFeed() {
            while (running) {
                try {
                    HttpClient client = HttpClient.newHttpClient();
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(ENDPOINT))
                            .GET()
                            .build();
                    HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                        Stream<String> lines = reader.lines();
                        lines.forEach(line -> {
                            WeatherEvent evt = parseLine(line);
                            if (evt != null) {
                                // Notify all listeners
                                for (WeatherListener l : listeners) {
                                    l.onWeatherEvent(evt);
                                }
                            }
                        });
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                    }
                }
            }
        }
        private WeatherEvent parseLine(String line) {
            try {
                String[] parts = line.trim().split(" ");
                if (parts.length != 5) return null;
                long ts = Long.parseLong(parts[0]);
                String attr = parts[1];
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                double val = Double.parseDouble(parts[4]);
                return new WeatherEvent(ts, attr, x, y, val);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
    }
