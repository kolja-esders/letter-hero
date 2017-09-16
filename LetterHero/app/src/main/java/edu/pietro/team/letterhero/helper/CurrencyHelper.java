package edu.pietro.team.letterhero.helper;


import java.util.Currency;
import java.util.HashMap;

import edu.pietro.team.letterhero.entities.AmountOfMoney;
import edu.pietro.team.letterhero.helper.api.ExchangeRatesAPI;
import edu.pietro.team.letterhero.helper.api.InternalStorageCache;

public class CurrencyHelper {

    private static final String CACHE_PREFIX = "EXCHANGE_RATES";

    private static final AbstractCache<HashMap<String, Double>> CACHE =
            new InternalStorageCache<HashMap<String, Double>>(null, CACHE_PREFIX);

    private static final ExchangeRatesAPI exchangeRates = new ExchangeRatesAPI();

    public static AmountOfMoney convert(AmountOfMoney amount, Currency newCurrency) {

        exchangeRates.getRatesForCurrency(amount.getCurrency(),
                new GenericCallbackInterface<HashMap<Currency, Double>>() {
            @Override
            public void onSuccess(HashMap<Currency, Double> result) {

            }
            @Override
            public void onFailure(Throwable e) {
            }
        });

        return null;
    }

}
