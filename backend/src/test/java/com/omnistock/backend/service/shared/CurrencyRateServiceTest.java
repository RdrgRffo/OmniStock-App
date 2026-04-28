package com.omnistock.backend.service.shared;

import com.omnistock.backend.entity.CurrencyRate;
import com.omnistock.backend.repository.CurrencyRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyRateServiceTest {

    @Mock
    private CurrencyRateRepository currencyRateRepository;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private CurrencyRateService currencyRateService;

    @Captor
    private ArgumentCaptor<CurrencyRate> currencyRateCaptor;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.requestFactory(any())).thenReturn(restClientBuilder);
        when(restClientBuilder.defaultHeader(anyString(), anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        currencyRateService = new CurrencyRateService(currencyRateRepository, restClientBuilder);
    }

    @Nested
    @DisplayName("refreshRates")
    class RefreshRates {

        @Test
        @DisplayName("Debe usar tasas de respaldo cuando la API falla")
        void shouldUseFallbackRatesWhenApiFails() {
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("USD")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("USD"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("GBP")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("GBP"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("CHF")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("CHF"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("JPY")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("JPY"), eq("EUR")))
                    .thenReturn(Optional.empty());

            // Mock the RestClient chain to throw an exception
            when(restClient.get()).thenThrow(new RuntimeException("API unavailable"));

            boolean result = currencyRateService.refreshRates();

            assertFalse(result);
            // EUR->EUR should always be saved
            verify(currencyRateRepository, atLeastOnce()).save(currencyRateCaptor.capture());
        }

        @Test
        @DisplayName("Debe actualizar tasa existente si cambió")
        void shouldUpdateExistingRateIfChanged() {
            CurrencyRate existingRate = new CurrencyRate("EUR", "USD", new BigDecimal("1.0800"));
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("USD")))
                    .thenReturn(Optional.of(existingRate));
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("USD"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("GBP")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("GBP"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("CHF")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("CHF"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("JPY")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("JPY"), eq("EUR")))
                    .thenReturn(Optional.empty());

            when(restClient.get()).thenThrow(new RuntimeException("API unavailable"));

            currencyRateService.refreshRates();

            // The existing EUR->USD rate should be updated since fallback rate (1.0870) differs from existing (1.0800)
            verify(currencyRateRepository).save(existingRate);
            assertEquals(0, new BigDecimal("1.0870").compareTo(existingRate.getRate()));
        }

        @Test
        @DisplayName("Debe crear EUR->EUR si no existe")
        void shouldCreateEurToEurIfNotExists() {
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("USD")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("USD"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("GBP")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("GBP"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("CHF")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("CHF"), eq("EUR")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("EUR"), eq("JPY")))
                    .thenReturn(Optional.empty());
            when(currencyRateRepository.findByFromCurrencyAndToCurrency(eq("JPY"), eq("EUR")))
                    .thenReturn(Optional.empty());

            when(restClient.get()).thenThrow(new RuntimeException("API unavailable"));

            currencyRateService.refreshRates();

            verify(currencyRateRepository, atLeast(1)).save(currencyRateCaptor.capture());
            boolean eurToEurCreated = currencyRateCaptor.getAllValues().stream()
                    .anyMatch(r -> "EUR".equals(r.getFromCurrency()) && "EUR".equals(r.getToCurrency())
                            && BigDecimal.ONE.compareTo(r.getRate()) == 0);
            assertTrue(eurToEurCreated);
        }
    }
}
