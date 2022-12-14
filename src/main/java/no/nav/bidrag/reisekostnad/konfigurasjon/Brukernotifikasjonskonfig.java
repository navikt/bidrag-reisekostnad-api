package no.nav.bidrag.reisekostnad.konfigurasjon;


import static net.javacrumbs.shedlock.support.Utils.getHostname;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import no.nav.bidrag.reisekostnad.database.dao.OppgavebestillingDao;
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Beskjedprodusent;
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Brukernotifikasjonkonsument;
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Ferdigprodusent;
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Oppgaveprodusent;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class Brukernotifikasjonskonfig {

  public static final String NAMESPACE_BIDRAG = "bidrag";

  private Egenskaper egenskaper;

  @Value("${KAFKA_SCHEMA_REGISTRY_USER}:${KAFKA_SCHEMA_REGISTRY_PASSWORD}")
  private String aivenSchemaRegistryCredentials;

  @Value("${KAFKA_BROKERS}")
  private String bootstrapAddress;

  @Value("${KAFKA_SCHEMA_REGISTRY}")
  private String kafkaSchemaRegistryUrlConfig;

  @Value("${KAFKA_KEYSTORE_PATH}")
  private String keyStorePath;

  @Value("${KAFKA_TRUSTSTORE_PATH}")
  private String trustStorePath;

  @Value("${KAFKA_CREDSTORE_PASSWORD}")
  private String trustStorePwd;

  @Value("${KAFKA_CREDSTORE_PASSWORD}")
  private String sslKeyPassword;

  public Brukernotifikasjonskonfig(@Autowired Egenskaper egenskaper) {
    this.egenskaper = egenskaper;
  }

  private Map<String, Object> getKafkaConfigProps() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 1);
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
    configProps.put(ProducerConfig.CLIENT_ID_CONFIG, NAMESPACE_BIDRAG + getHostname());
    configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 40000);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
    configProps.put(KafkaAvroSerializerConfig.USER_INFO_CONFIG, aivenSchemaRegistryCredentials);
    configProps.put(KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
    configProps.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, kafkaSchemaRegistryUrlConfig);
    configProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
    configProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, trustStorePath);
    configProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePwd);
    configProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keyStorePath);
    configProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, trustStorePwd);
    configProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, sslKeyPassword);
    configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
    configProps.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "JKS");
    configProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
    configProps.put("reconnect.backoff.ms", 100);
    configProps.put("specific.avro.reader", "true");
    return configProps;
  }

  @Bean("beskjed")
  public KafkaTemplate<NokkelInput, BeskjedInput> kafkaTemplateBeskjed() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaConfigProps()));
  }

  @Bean("ferdig")
  public KafkaTemplate<NokkelInput, DoneInput> kafkaTemplateFerdig() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaConfigProps()));
  }

  @Bean("oppgave")
  public KafkaTemplate<NokkelInput, OppgaveInput> kafkaTemplateOppgave() {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(getKafkaConfigProps()));
  }

  @Bean
  Brukernotifikasjonkonsument brukernotifikasjonkonsument(Beskjedprodusent beskjedprodusent, Ferdigprodusent ferdigprodusent,
      Oppgaveprodusent oppgaveprodusent, Egenskaper egenskaper) {
    return new Brukernotifikasjonkonsument(beskjedprodusent, ferdigprodusent, oppgaveprodusent, egenskaper);
  }

  @Bean
  Beskjedprodusent beskjedprodusent(@Qualifier("beskjed") KafkaTemplate<NokkelInput, BeskjedInput> kafkaTemplate) throws MalformedURLException {
    return new Beskjedprodusent(kafkaTemplate, toUrl(egenskaper.getUrlReisekostnad()), egenskaper);
  }

  @Bean
  Oppgaveprodusent oppgaveprodusent(
      @Qualifier("oppgave") KafkaTemplate<NokkelInput, OppgaveInput> kafkaTemplate, Databasetjeneste databasetjeneste)
      throws MalformedURLException {
    return new Oppgaveprodusent(kafkaTemplate, databasetjeneste, toUrl(egenskaper.getUrlReisekostnad()), egenskaper);
  }

  @Bean
  Ferdigprodusent ferdigprodusent(@Qualifier("ferdig") KafkaTemplate<NokkelInput, DoneInput> kafkaTemplate, Databasetjeneste databasetjeneste,
      OppgavebestillingDao oppgavebestillingDao) {
    return new Ferdigprodusent(kafkaTemplate, databasetjeneste, oppgavebestillingDao, egenskaper);
  }

  private URL toUrl(String url) throws MalformedURLException {
    return new URL(url);
  }
}
