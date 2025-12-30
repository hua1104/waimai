package com.example.takeout.service;

import com.example.takeout.entity.CustomerOrder;
import com.example.takeout.entity.DeliveryStaff;
import com.example.takeout.repository.CustomerOrderRepository;
import com.example.takeout.repository.DeliveryStaffRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

@Service
public class DeliveryAssignmentService {

    private final CustomerOrderRepository customerOrderRepository;
    private final DeliveryStaffRepository deliveryStaffRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baiduAk;

    public DeliveryAssignmentService(CustomerOrderRepository customerOrderRepository,
                                     DeliveryStaffRepository deliveryStaffRepository,
                                     ObjectMapper objectMapper,
                                     @Value("${baidu.map.ak:}") String baiduAk) {
        this.customerOrderRepository = customerOrderRepository;
        this.deliveryStaffRepository = deliveryStaffRepository;
        this.objectMapper = objectMapper;
        this.baiduAk = baiduAk == null ? "" : baiduAk.trim();
    }

    @Transactional
    public Optional<DeliveryStaff> autoAssignIfPossible(Long orderId) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return Optional.empty();
        CustomerOrder order = orderOpt.get();
        if (!canAssign(order)) return Optional.empty();
        if (order.getDeliveryStaff() != null && order.getDeliveryStaff().getId() != null) {
            return Optional.of(order.getDeliveryStaff());
        }
        List<DeliveryStaff> candidates = deliveryStaffRepository.findAssignable();
        if (candidates.isEmpty()) return Optional.empty();
        DeliveryStaff staff = pickBest(order, candidates);
        assign(order, staff);
        return Optional.of(staff);
    }

    @Transactional
    public Optional<DeliveryStaff> assignOrReassign(Long orderId, Long deliveryStaffId) {
        Optional<CustomerOrder> orderOpt = customerOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return Optional.empty();
        CustomerOrder order = orderOpt.get();
        if (!canAssign(order)) return Optional.empty();

        Optional<DeliveryStaff> staffOpt = deliveryStaffRepository.findById(deliveryStaffId);
        if (staffOpt.isEmpty()) return Optional.empty();
        DeliveryStaff target = staffOpt.get();
        if (target.getStatus() != null && !"ACTIVE".equalsIgnoreCase(target.getStatus())) {
            return Optional.empty();
        }

        if (order.getDeliveryStaff() != null && order.getDeliveryStaff().getId() != null) {
            Long prevId = order.getDeliveryStaff().getId();
            if (prevId.equals(target.getId())) return Optional.of(target);
            decrementLoad(prevId);
        }
        assign(order, target);
        return Optional.of(target);
    }

    @Transactional
    public void decrementLoadIfAssigned(Long deliveryStaffId) {
        if (deliveryStaffId == null) return;
        decrementLoad(deliveryStaffId);
    }

    private static boolean canAssign(CustomerOrder order) {
        if (order == null) return false;
        String payStatus = order.getPayStatus() == null ? "" : order.getPayStatus();
        String status = order.getStatus() == null ? "" : order.getStatus();
        return "PAID".equalsIgnoreCase(payStatus)
                && ("PAID".equalsIgnoreCase(status) || "DELIVERING".equalsIgnoreCase(status));
    }

    private DeliveryStaff pickBest(CustomerOrder order, List<DeliveryStaff> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        Double lat = order == null ? null : order.getDeliveryLat();
        Double lng = order == null ? null : order.getDeliveryLng();
        if (lat == null || lng == null) {
            return candidates.get(0);
        }

        double loadWeight = 0.8;
        DeliveryStaff best = null;
        double bestScore = Double.POSITIVE_INFINITY;
        for (DeliveryStaff s : candidates) {
            if (s == null) continue;
            Double slat = s.getCurrentLat();
            Double slng = s.getCurrentLng();
            if (slat == null || slng == null) continue;
            double distKm = resolveDistanceKm(slat, slng, lat, lng).orElseGet(() -> haversineKm(slat, slng, lat, lng));
            int load = s.getCurrentLoad() == null ? 0 : s.getCurrentLoad();
            double score = distKm + (load * loadWeight);
            if (score < bestScore) {
                bestScore = score;
                best = s;
            }
        }
        if (best != null) return best;
        return candidates.get(0);
    }

    private Optional<Double> resolveDistanceKm(double fromLat, double fromLng, double toLat, double toLng) {
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
            int status = root.path("status").asInt(-1);
            if (status != 0) return Optional.empty();
            JsonNode results = root.path("result");
            if (!results.isArray() || results.isEmpty()) return Optional.empty();
            JsonNode distance = results.get(0).path("distance");
            double meters = distance.path("value").asDouble(-1);
            if (meters <= 0) return Optional.empty();
            return Optional.of(meters / 1000.0);
        } catch (Exception ignored) {
            return Optional.empty();
        }
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

    private void assign(CustomerOrder order, DeliveryStaff staff) {
        if (order == null || staff == null) return;
        order.setDeliveryStaff(staff);
        customerOrderRepository.save(order);

        Integer load = staff.getCurrentLoad() == null ? 0 : staff.getCurrentLoad();
        staff.setCurrentLoad(load + 1);
        deliveryStaffRepository.save(staff);
    }

    private void decrementLoad(Long staffId) {
        deliveryStaffRepository.findById(staffId).ifPresent(staff -> {
            Integer load = staff.getCurrentLoad() == null ? 0 : staff.getCurrentLoad();
            staff.setCurrentLoad(Math.max(0, load - 1));
            deliveryStaffRepository.save(staff);
        });
    }
}
