package com.example.takeout.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
public class BaiduMapService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String baiduAk;

    public BaiduMapService(ObjectMapper objectMapper, @Value("${baidu.map.ak:}") String baiduAk) {
        this.objectMapper = objectMapper;
        this.baiduAk = baiduAk == null ? "" : baiduAk.trim();
    }

    public String getAk() {
        return baiduAk;
    }

    public Optional<Coord> geocode(String address) {
        if (baiduAk.isEmpty()) return Optional.empty();
        if (address == null || address.isBlank()) return Optional.empty();
        try {
            String url = "https://api.map.baidu.com/geocoding/v3/"
                    + "?output=json"
                    + "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
                    + "&ak=" + baiduAk;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .header("User-Agent", "takeout-backend")
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return Optional.empty();

            JsonNode root = objectMapper.readTree(resp.body());
            if (root.path("status").asInt(-1) != 0) return Optional.empty();
            JsonNode loc = root.path("result").path("location");
            double lat = loc.path("lat").asDouble(Double.NaN);
            double lng = loc.path("lng").asDouble(Double.NaN);
            if (Double.isNaN(lat) || Double.isNaN(lng)) return Optional.empty();
            return Optional.of(new Coord(lat, lng));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public Optional<RouteInfo> drivingRoute(double fromLat, double fromLng, double toLat, double toLng) {
        if (baiduAk.isEmpty()) return Optional.empty();
        try {
            String url = "https://api.map.baidu.com/routematrix/v2/driving"
                    + "?output=json"
                    + "&origins=" + fromLat + "," + fromLng
                    + "&destinations=" + toLat + "," + toLng
                    + "&ak=" + baiduAk;
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .header("User-Agent", "takeout-backend")
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) return Optional.empty();

            JsonNode root = objectMapper.readTree(resp.body());
            if (root.path("status").asInt(-1) != 0) return Optional.empty();
            JsonNode results = root.path("result");
            if (!results.isArray() || results.isEmpty()) return Optional.empty();
            JsonNode r0 = results.get(0);
            double meters = r0.path("distance").path("value").asDouble(-1);
            double seconds = r0.path("duration").path("value").asDouble(-1);
            if (meters <= 0) return Optional.empty();
            Double durationMin = seconds > 0 ? seconds / 60.0 : null;
            return Optional.of(new RouteInfo(meters / 1000.0, durationMin, "baidu_driving"));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static RouteInfo haversineFallback(double fromLat, double fromLng, double toLat, double toLng) {
        double distKm = haversineKm(fromLat, fromLng, toLat, toLng);
        double speedKmh = 25.0;
        Double durationMin = distKm > 0 ? (distKm / speedKmh) * 60.0 : null;
        return new RouteInfo(distKm, durationMin, "haversine");
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.asin(Math.min(1, Math.sqrt(a)));
        return r * c;
    }

    public record Coord(double lat, double lng) {
    }

    public record RouteInfo(double distanceKm, Double durationMin, String source) {
    }
}

