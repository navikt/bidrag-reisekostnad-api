package no.nav.bidrag.reisekostnad.konfigurasjon;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
@Profile(Profil.I_SKY)
public class Cachekonfig {

  public static final String CACHE_PERSON = "person-cache";

  public static final String CACHE_FAMILIE = "familie-cache";

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
    caffeineCacheManager.registerCustomCache(CACHE_PERSON,
        Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .recordStats()
            .build()
    );
    caffeineCacheManager.registerCustomCache(CACHE_FAMILIE,
        Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .recordStats()
            .build()
    );

    return caffeineCacheManager;
  }
}
