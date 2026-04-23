package com.omnistock.backend.seeder;

import com.omnistock.backend.service.shared.CurrencyRateService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
@Profile("!test")
public class CurrencyRateSeeder implements CommandLineRunner {

    private final CurrencyRateService currencyRateService;

    public CurrencyRateSeeder(CurrencyRateService currencyRateService) {
        this.currencyRateService = currencyRateService;
    }

    @Override
    public void run(String... args) {
        currencyRateService.refreshRates();
        System.out.println("Seeder: Tasas de cambio actualizadas (CurrencyRateSeeder)");
    }
}
