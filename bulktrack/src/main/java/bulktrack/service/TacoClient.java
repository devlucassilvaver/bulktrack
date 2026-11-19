package bulktrack.service;

import bulktrack.dto.Alimento;
import bulktrack.dto.Nutrientes;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.http.HttpClient;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class TacoClient {
    private static final int LIMITE_RESULTADOS = 10;
    private static final Set<String> CONECTIVOS = Set.of("de", "da", "do", "das", "dos", "e");
    private final RestClient client;

    public TacoClient(@Value("${bulktrack.taco-url:https://brolesi.github.io/taco}") String url) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
    }

    public List<Alimento> buscar(String pesquisa) {
        var termos = palavras(pesquisa).stream().filter(palavra -> !CONECTIVOS.contains(palavra)).toList();
        if (termos.isEmpty()) {
            return List.of();
        }
        try {
            var catalogo = client.get().uri("/foods.json").retrieve().body(Catalogo.class);
            if (catalogo == null || catalogo.foods() == null) {
                throw indisponivel();
            }
            var encontrados = catalogo.foods().stream()
                    .filter(item -> item.id() > 0 && item.description() != null
                            && palavras(item.description()).containsAll(termos))
                    .limit(LIMITE_RESULTADOS)
                    .toList();
            if (encontrados.isEmpty()) {
                return List.of();
            }
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var detalhes = encontrados.stream()
                        .map(item -> CompletableFuture.supplyAsync(() -> detalhar(item), executor))
                        .toList();
                return detalhes.stream().map(CompletableFuture::join).toList();
            }
        } catch (RestClientException erro) {
            throw indisponivel();
        } catch (CompletionException erro) {
            if (erro.getCause() instanceof RestClientException || erro.getCause() instanceof ResponseStatusException) {
                throw indisponivel();
            }
            throw erro;
        }
    }

    private Alimento detalhar(Item item) {
        var dados = client.get().uri("/foods/{id}.json", item.id()).retrieve().body(Composicao.class);
        if (dados == null || dados.id() != item.id()) {
            throw indisponivel();
        }
        var nutrientes = new Nutrientes(ajustar(dados.calorias()), ajustar(dados.carboidratos()),
                ajustar(dados.proteinas()), ajustar(dados.lipidios()));
        return new Alimento(item.description(), nutrientes, "TACO (NEPA/Unicamp) " + item.id());
    }

    private static List<String> palavras(String texto) {
        String normal = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return Arrays.stream(normal.split("[^a-z0-9]+")).filter(palavra -> !palavra.isEmpty()).toList();
    }

    private static BigDecimal ajustar(BigDecimal valor) {
        return valor == null || valor.signum() < 0 ? null
                : valor.setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private static ResponseStatusException indisponivel() {
        return new ResponseStatusException(SERVICE_UNAVAILABLE, "Busca TACO indisponível. Use o cadastro manual.");
    }

    public record Catalogo(List<Item> foods) {}
    public record Item(int id, String description) {}
    public record Composicao(int id,
                             @JsonProperty("energy_kcal") BigDecimal calorias,
                             @JsonProperty("carbohydrate_g") BigDecimal carboidratos,
                             @JsonProperty("protein_g") BigDecimal proteinas,
                             @JsonProperty("lipids_g") BigDecimal lipidios) {}
}
