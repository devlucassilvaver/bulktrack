package bulktrack.service;

import bulktrack.dto.*;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class FoodDataCentral {
    private static final java.util.regex.Pattern COZIDO = java.util.regex.Pattern.compile("\\bcooked\\b");
    private static final java.util.regex.Pattern CRU = java.util.regex.Pattern.compile("\\b(raw|uncooked)\\b");
    private final RestClient client;
    private final String chave;

    public FoodDataCentral(@Value("${FDC_API_KEY:}") String chave,
                           @Value("${bulktrack.fdc-url:https://api.nal.usda.gov}") String url) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(url).requestFactory(factory).build();
        this.chave = chave.isBlank() ? "DEMO_KEY" : chave.strip();
    }

    public List<Alimento> buscar(String pesquisa) {
        String consulta = traduzir(pesquisa);
        try {
            var resposta = client.get().uri(builder -> builder.path("/fdc/v1/foods/search")
                    .queryParam("api_key", "{key}").queryParam("query", "{query}")
                    .queryParam("dataType", "SR Legacy").queryParam("pageSize", 20)
                    .build(chave, consulta)).retrieve().body(Resultado.class);
            if (resposta == null || resposta.foods() == null) {
                throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Resposta USDA incompleta. Use o cadastro manual.");
            }
            return resposta.foods().stream().filter(item -> item.description() != null
                    && item.description().length() <= 255 && preparoCompativel(consulta, item.description()))
                    .map(FoodDataCentral::mapear).toList();
        } catch (RestClientException erro) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Busca USDA indisponível. Use o cadastro manual.");
        }
    }

    private static boolean preparoCompativel(String consulta, String descricao) {
        String item = descricao.toLowerCase(Locale.ROOT);
        return switch (consulta) {
            case "rice white cooked", "rice brown cooked" -> tipoDeArroz(consulta, item)
                    && COZIDO.matcher(item).find();
            case "rice white raw", "rice brown raw" -> tipoDeArroz(consulta, item)
                    && CRU.matcher(item).find();
            default -> true;
        };
    }

    private static boolean tipoDeArroz(String consulta, String item) {
        return item.startsWith(consulta.startsWith("rice white") ? "rice, white," : "rice, brown,");
    }

    private static String traduzir(String pesquisa) {
        String normal = Normalizer.normalize(pesquisa.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("\\s+", " ");
        return switch (normal) {
            case "arroz cozido", "arroz branco cozido" -> "rice white cooked";
            case "arroz cru", "arroz branco cru" -> "rice white raw";
            case "arroz integral cozido" -> "rice brown cooked";
            case "arroz integral cru" -> "rice brown raw";
            case "feijao preto cozido" -> "beans black cooked";
            case "banana" -> "banana";
            case "peito de frango cozido" -> "chicken breast cooked";
            default -> pesquisa.strip();
        };
    }

    private static Alimento mapear(Item item) {
        Map<Integer, BigDecimal> valores = new HashMap<>();
        if (item.foodNutrients() != null) {
            for (Nutriente nutriente : item.foodNutrients()) {
                if (nutriente.value() != null && nutriente.value().signum() >= 0) {
                    valores.putIfAbsent(nutriente.nutrientId(), nutriente.value()
                            .setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros());
                }
            }
        }
        return new Alimento(item.description(), new Nutrientes(valores.get(1008), valores.get(1005),
                valores.get(1003), valores.get(1004)), "USDA FDC " + item.fdcId());
    }

    public record Resultado(List<Item> foods) {}
    public record Item(long fdcId, String description, List<Nutriente> foodNutrients) {}
    public record Nutriente(int nutrientId, BigDecimal value) {}
}
