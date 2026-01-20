package com.example.demo.dispatch.service;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleMapsService {

    private final GeoApiContext geoApiContext;

    // Cache dla odległości - klucz to "origin|destination"
    private final ConcurrentHashMap<String, DistanceResult> distanceCache = new ConcurrentHashMap<>();

    /**
     * Oblicza dystans między dwoma adresami.
     * Zwraca wynik z cache jeśli dostępny.
     */
    public DistanceResult getDistance(String origin, String destination) {
        String cacheKey = origin.toLowerCase().trim() + "|" + destination.toLowerCase().trim();

        // Sprawdź cache
        if (distanceCache.containsKey(cacheKey)) {
            log.debug("Cache hit for distance: {} -> {}", origin, destination);
            return distanceCache.get(cacheKey);
        }

        try {
            DistanceMatrix result = DistanceMatrixApi.newRequest(geoApiContext)
                    .origins(origin)
                    .destinations(destination)
                    .mode(TravelMode.DRIVING)
                    .language("pl")
                    .await();

            if (result.rows.length > 0 && result.rows[0].elements.length > 0) {
                DistanceMatrixElement element = result.rows[0].elements[0];

                if (element.status == DistanceMatrixElementStatus.OK) {
                    double distanceKm = element.distance.inMeters / 1000.0;
                    int durationMinutes = (int) (element.duration.inSeconds / 60);

                    DistanceResult distanceResult = new DistanceResult(distanceKm, durationMinutes, true);
                    distanceCache.put(cacheKey, distanceResult);

                    log.info("Distance calculated: {} -> {} = {} km, {} min",
                            origin, destination, distanceKm, durationMinutes);

                    return distanceResult;
                } else {
                    log.warn("Distance Matrix returned status: {} for {} -> {}",
                            element.status, origin, destination);
                }
            }
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Error calculating distance for {} -> {}: {}", origin, destination, e.getMessage());
        }

        // Fallback - szacunkowa odległość
        return new DistanceResult(estimateFallbackDistance(origin, destination), 0, false);
    }

    /**
     * Oblicza macierz odległości i czasu między wieloma punktami.
     */
    public DistanceMatrixResult getDistanceAndDurationMatrix(List<String> origins, List<String> destinations) {
        int n = origins.size();
        int m = destinations.size();
        double[][] distanceMatrix = new double[n][m];
        double[][] durationMatrix = new double[n][m];

        try {
            DistanceMatrix result = DistanceMatrixApi.newRequest(geoApiContext)
                    .origins(origins.toArray(new String[0]))
                    .destinations(destinations.toArray(new String[0]))
                    .mode(TravelMode.DRIVING)
                    .language("pl")
                    .await();

            for (int i = 0; i < result.rows.length; i++) {
                DistanceMatrixRow row = result.rows[i];
                for (int j = 0; j < row.elements.length; j++) {
                    DistanceMatrixElement element = row.elements[j];
                    if (element.status == DistanceMatrixElementStatus.OK) {
                        distanceMatrix[i][j] = element.distance.inMeters / 1000.0;
                        durationMatrix[i][j] = element.duration.inSeconds / 60.0;
                    } else {
                        distanceMatrix[i][j] = Double.MAX_VALUE;
                        durationMatrix[i][j] = Double.MAX_VALUE;
                    }
                }
            }

            log.info("Distance and duration matrices calculated: {} origins, {} destinations", n, m);

        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Error calculating distance/duration matrices: {}", e.getMessage());
            // Fallback - wypełnij szacunkowymi wartościami
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < m; j++) {
                    if (origins.get(i).equalsIgnoreCase(destinations.get(j))) {
                        distanceMatrix[i][j] = 0;
                        durationMatrix[i][j] = 0;
                    } else {
                        distanceMatrix[i][j] = estimateFallbackDistance(origins.get(i), destinations.get(j));
                        durationMatrix[i][j] = distanceMatrix[i][j] / 50 * 60; // zakładając średnią prędkość 50 km/h
                    }
                }
            }
        }

        return new DistanceMatrixResult(distanceMatrix, durationMatrix);
    }

    /**
     * Oblicza macierz odległości między wieloma punktami.
     * Przydatne do optymalizacji tras.
     */
    public double[][] getDistanceMatrix(List<String> locations) {
        int n = locations.size();
        double[][] matrix = new double[n][n];

        try {
            String[] origins = locations.toArray(new String[0]);
            String[] destinations = locations.toArray(new String[0]);

            DistanceMatrix result = DistanceMatrixApi.newRequest(geoApiContext)
                    .origins(origins)
                    .destinations(destinations)
                    .mode(TravelMode.DRIVING)
                    .language("pl")
                    .await();

            for (int i = 0; i < result.rows.length; i++) {
                DistanceMatrixRow row = result.rows[i];
                for (int j = 0; j < row.elements.length; j++) {
                    DistanceMatrixElement element = row.elements[j];
                    if (element.status == DistanceMatrixElementStatus.OK) {
                        matrix[i][j] = element.distance.inMeters / 1000.0;
                    } else {
                        matrix[i][j] = Double.MAX_VALUE;
                    }
                }
            }

            log.info("Distance matrix calculated for {} locations", n);

        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Error calculating distance matrix: {}", e.getMessage());
            // Fallback - wypełnij szacunkowymi wartościami
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (i == j) {
                        matrix[i][j] = 0;
                    } else {
                        matrix[i][j] = estimateFallbackDistance(locations.get(i), locations.get(j));
                    }
                }
            }
        }

        return matrix;
    }

    /**
     * Optymalizuje kolejność punktów na trasie używając algorytmu Nearest Neighbor
     * z rzeczywistymi odległościami z Google Maps.
     */
    public List<Integer> optimizeRouteOrder(List<String> waypoints) {
        if (waypoints.size() <= 2) {
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < waypoints.size(); i++) {
                order.add(i);
            }
            return order;
        }

        // Pobierz macierz odległości
        double[][] distanceMatrix = getDistanceMatrix(waypoints);

        // Algorytm Nearest Neighbor
        int n = waypoints.size();
        boolean[] visited = new boolean[n];
        List<Integer> order = new ArrayList<>();

        // Zacznij od pierwszego punktu
        int current = 0;
        visited[current] = true;
        order.add(current);

        while (order.size() < n) {
            double minDistance = Double.MAX_VALUE;
            int nearest = -1;

            for (int j = 0; j < n; j++) {
                if (!visited[j] && distanceMatrix[current][j] < minDistance) {
                    minDistance = distanceMatrix[current][j];
                    nearest = j;
                }
            }

            if (nearest != -1) {
                visited[nearest] = true;
                order.add(nearest);
                current = nearest;
            } else {
                break;
            }
        }

        log.info("Route optimized: {} waypoints", waypoints.size());
        return order;
    }

    /**
     * Oblicza całkowity dystans dla listy punktów w podanej kolejności.
     */
    public double calculateTotalRouteDistance(List<String> waypoints) {
        if (waypoints.size() < 2) {
            return 0;
        }

        double total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            DistanceResult result = getDistance(waypoints.get(i), waypoints.get(i + 1));
            total += result.getDistanceKm();
        }

        return Math.round(total * 10.0) / 10.0;
    }

    /**
     * Oblicza całkowity czas podróży dla listy punktów w podanej kolejności.
     */
    public int calculateTotalRouteDuration(List<String> waypoints) {
        if (waypoints.size() < 2) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < waypoints.size() - 1; i++) {
            DistanceResult result = getDistance(waypoints.get(i), waypoints.get(i + 1));
            total += result.getDurationMinutes();
        }

        return total;
    }

    /**
     * Fallback - szacunkowa odległość gdy API nie jest dostępne.
     */
    private double estimateFallbackDistance(String origin, String destination) {
        if (origin.equalsIgnoreCase(destination)) {
            return 5.0;
        }
        // Prosta heurystyka bazująca na hashach
        long hash1 = Math.abs((long) origin.toLowerCase().hashCode());
        long hash2 = Math.abs((long) destination.toLowerCase().hashCode());
        return ((hash1 + hash2) % 190) + 10;
    }

    /**
     * Czyści cache odległości.
     */
    public void clearCache() {
        distanceCache.clear();
        log.info("Distance cache cleared");
    }

    /**
     * Zwraca rozmiar cache.
     */
    public int getCacheSize() {
        return distanceCache.size();
    }

    // ========== WALIDACJA ADRESÓW ==========

    /**
     * Waliduje adres używając Google Geocoding API.
     * Zwraca true jeśli adres jest rozpoznawalny.
     */
    public boolean validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }

        try {
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address)
                    .language("pl")
                    .await();

            if (results != null && results.length > 0) {
                log.info("Address validated: {} -> {}", address, results[0].formattedAddress);
                return true;
            }
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Error validating address {}: {}", address, e.getMessage());
        }

        log.warn("Address not found: {}", address);
        return false;
    }

    /**
     * Geocoduje adres i zwraca sformatowany adres + współrzędne.
     */
    public AddressValidationResult geocodeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return new AddressValidationResult(false, null, null, 0, 0, "Adres nie może być pusty");
        }

        try {
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address)
                    .language("pl")
                    .await();

            if (results != null && results.length > 0) {
                GeocodingResult result = results[0];
                double lat = result.geometry.location.lat;
                double lng = result.geometry.location.lng;
                String formattedAddress = result.formattedAddress;

                // Wyciągnij nazwę miasta/miejscowości
                String placeName = extractPlaceName(result);

                log.info("Geocoded: {} -> {} ({}, {})", address, formattedAddress, lat, lng);

                return new AddressValidationResult(true, formattedAddress, placeName, lat, lng, null);
            }
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("Error geocoding address {}: {}", address, e.getMessage());
            return new AddressValidationResult(false, null, null, 0, 0, "Błąd API: " + e.getMessage());
        }

        return new AddressValidationResult(false, null, null, 0, 0, "Nie znaleziono adresu");
    }

    /**
     * Wyciąga nazwę miejscowości z wyniku geocodingu.
     */
    private String extractPlaceName(GeocodingResult result) {
        for (var component : result.addressComponents) {
            for (var type : component.types) {
                if (type.name().equals("LOCALITY") || type.name().equals("ADMINISTRATIVE_AREA_LEVEL_3")) {
                    return component.longName;
                }
            }
        }
        // Fallback - użyj pierwszej części sformatowanego adresu
        String[] parts = result.formattedAddress.split(",");
        return parts.length > 0 ? parts[0].trim() : result.formattedAddress;
    }

    /**
     * Klasa wynikowa dla walidacji adresu.
     */
    public static class AddressValidationResult {
        private final boolean valid;
        private final String formattedAddress;
        private final String placeName;
        private final double latitude;
        private final double longitude;
        private final String errorMessage;

        public AddressValidationResult(boolean valid, String formattedAddress, String placeName,
                double latitude, double longitude, String errorMessage) {
            this.valid = valid;
            this.formattedAddress = formattedAddress;
            this.placeName = placeName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getFormattedAddress() {
            return formattedAddress;
        }

        public String getPlaceName() {
            return placeName;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Klasa wynikowa dla odległości.
     */
    public static class DistanceResult {
        private final double distanceKm;
        private final int durationMinutes;
        private final boolean fromApi;

        public DistanceResult(double distanceKm, int durationMinutes, boolean fromApi) {
            this.distanceKm = distanceKm;
            this.durationMinutes = durationMinutes;
            this.fromApi = fromApi;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public int getDurationMinutes() {
            return durationMinutes;
        }

        public boolean isFromApi() {
            return fromApi;
        }
    }

    /**
     * Klasa wynikowa dla macierzy odległości.
     */
    public static class DistanceMatrixResult {
        public final double[][] distanceMatrix;
        public final double[][] durationMatrix;

        public DistanceMatrixResult(double[][] distanceMatrix, double[][] durationMatrix) {
            this.distanceMatrix = distanceMatrix;
            this.durationMatrix = durationMatrix;
        }
    }
}
