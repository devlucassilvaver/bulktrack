package bulktrack;

import bulktrack.service.TacoClient;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FluxoMvpTests {
    @LocalServerPort int porta;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void diarioETreinoPersistemEAsContasFicamIsoladas() throws Exception {
        String senha = UUID.randomUUID().toString();
        String hash = encoder.encode(senha);
        String anaUsuario = "ana_" + UUID.randomUUID();
        String biaUsuario = "bia_" + UUID.randomUUID();
        jdbc.update("INSERT INTO conta(usuario, senha_hash) VALUES (?, ?)", anaUsuario, hash);
        jdbc.update("INSERT INTO conta(usuario, senha_hash) VALUES (?, ?)", biaUsuario, hash);
        var ana = new Sessao();
        var bia = new Sessao();
        assertEquals(401, ana.requisicao("GET", "/perfis/me", null).statusCode());
        ana.login(anaUsuario, senha);
        bia.login(biaUsuario, senha);
        assertEquals(200, ana.requisicao("GET", "/", null).statusCode());
        assertEquals(403, ana.enviar("POST", "/perfis", "{}", false).statusCode());

        var dia = LocalDate.now(ZoneId.of("America/Fortaleza"));
        String pedido = """
                {"nome":"Ana","dataNascimento":"%s","altura":1.8,"pesoAtual":80,"parametroTmb":"MAIS_5",
                 "nivelAtividade":"MODERADO","objetivo":"MANUTENCAO"}
                """.formatted(dia.minusYears(30));
        long idAna = ana.corpo(201, "POST", "/perfis", pedido).get("id").asLong();
        long idBia = bia.corpo(201, "POST", "/perfis", pedido.replace("Ana", "Bia")).get("id").asLong();
        String perfilAna = "/perfis/" + idAna;
        String perfilBia = "/perfis/" + idBia;
        String diario = perfilAna + "/diario";
        String porcao = """
                {"gramas":200,"horario":"%sT12:30"}
                """.formatted(dia);
        String arroz = """
                {"alimento":{"nome":"Arroz cozido","por100g":{"calorias":130,"carboidratos":28.2,"proteinas":2.7}},
                 "consumo":%s}
                """.formatted(porcao);
        var previa = ana.corpo(200, "POST", "/api/alimentos/previa",
                """
                {"gramas":200,"por100g":{"calorias":130,"carboidratos":28.2,"proteinas":2.7}}
                """);
        assertEquals(260, previa.get("calorias").asInt());
        var registro = ana.corpo(201, "POST", diario, arroz);
        long idAlimento = registro.get("id").asLong();
        assertEquals(260, registro.at("/nutrientes/calorias").asInt());
        assertTrue(registro.at("/nutrientes/lipidios").isNull());
        var resumo = ana.corpo(200, "GET", diario, null);
        assertEquals(260, resumo.at("/calorias/total").asInt());
        assertEquals(2499, resumo.at("/calorias/restante").asInt());
        assertTrue(resumo.at("/lipidios/total").isNull());
        assertTrue(resumo.at("/lipidios/restante").isNull());
        assertEquals(1, resumo.at("/lipidios/registrosSemDado").asInt());
        assertEquals(0, ana.corpo(200, "GET", diario + "?data=" + dia.minusDays(1), null).get("registros").size());
        assertEquals(404, bia.requisicao("GET", diario, null).statusCode());
        assertEquals(404, bia.requisicao("PUT", perfilAna, pedido).statusCode());
        assertEquals(404, bia.requisicao("PUT", perfilBia + "/diario/" + idAlimento, porcao).statusCode());
        assertEquals(404, bia.requisicao("DELETE", diario + "/" + idAlimento, null).statusCode());
        ana.corpo(200, "PUT", diario + "/" + idAlimento, porcao.replace("200", "100"));
        assertEquals(130, ana.corpo(200, "GET", diario, null).at("/calorias/total").asInt());
        assertEquals(204, ana.requisicao("DELETE", diario + "/" + idAlimento, null).statusCode());
        assertEquals(0, ana.corpo(200, "GET", diario, null).at("/calorias/total").asInt());

        String treino = """
                {"nome":"Pernas","data":"%s","exercicios":[{"nome":"Agachamento","descansoSegundos":90,
                  "series":[{"repeticoes":10,"cargaKg":40},{"repeticoes":8}]}]}
                """.formatted(dia);
        String treinos = perfilAna + "/treinos";
        long idTreino = ana.corpo(201, "POST", treinos, treino).get("id").asLong();
        String editar = treinos + "/" + idTreino;
        assertEquals(8, ana.corpo(200, "GET", treinos + "?data=" + dia, null)
                .at("/0/exercicios/0/series/1/repeticoes").asInt());
        assertEquals(404, bia.requisicao("GET", editar, null).statusCode());
        assertEquals(404, bia.requisicao("PUT", perfilBia + "/treinos/" + idTreino, treino).statusCode());
        assertEquals("Força", ana.corpo(200, "PUT", editar, treino.replace("Pernas", "Força")).get("nome").asString());
        assertEquals(204, ana.requisicao("DELETE", editar, null).statusCode());
        assertEquals(404, ana.requisicao("GET", editar, null).statusCode());
    }

    @Test
    void fonteTacoDistinguePreparoEAceitaBuscaSemAcentos() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/foods.json", exchange -> responderJson(exchange, """
                {"total":5,"foods":[{"id":1,"description":"Arroz, integral, cozido"},
                  {"id":2,"description":"Arroz, integral, cru"},{"id":3,"description":"Arroz, tipo 1, cozido"},
                  {"id":4,"description":"Arroz, tipo 1, cru"},{"id":448,"description":"Feijão, preto, cozido"}]}
                """));
        server.createContext("/foods/1.json", exchange -> responderJson(exchange, """
                {"id":1,"energy_kcal":123.53489,"protein_g":2.58825,"carbohydrate_g":25.80975,"lipids_g":null}
                """));
        server.createContext("/foods/2.json", exchange -> responderJson(exchange, """
                {"id":2,"energy_kcal":360}
                """));
        server.createContext("/foods/3.json", exchange -> responderJson(exchange, """
                {"id":3,"energy_kcal":128}
                """));
        server.createContext("/foods/4.json", exchange -> responderJson(exchange, """
                {"id":4,"energy_kcal":358}
                """));
        server.createContext("/foods/448.json", exchange -> responderJson(exchange, """
                {"id":448,"energy_kcal":77}
                """));
        server.start();
        try {
            var fonte = new TacoClient("http://127.0.0.1:" + server.getAddress().getPort());
            assertEquals(List.of("Arroz branco cozido", "Arroz integral cozido", "Arroz branco cru"),
                    fonte.buscar("arroz").stream().map(bulktrack.dto.Alimento::nome).toList());
            var cozidos = fonte.buscar("cozido arroz integral");
            assertEquals(1, cozidos.size());
            var alimento = cozidos.getFirst();
            assertEquals("Arroz integral cozido", alimento.nome());
            assertEquals("TACO (NEPA/Unicamp) 1", alimento.fonte());
            assertEquals(new java.math.BigDecimal("123.5349"), alimento.por100g().calorias());
            assertEquals(new java.math.BigDecimal("25.8098"), alimento.por100g().carboidratos());
            assertEquals(new java.math.BigDecimal("2.5883"), alimento.por100g().proteinas());
            assertNull(alimento.por100g().lipidios());
            assertEquals("Arroz integral cru", fonte.buscar("arroz integral cru").getFirst().nome());
            assertEquals("Arroz branco cru", fonte.buscar("arroz branco cru").getFirst().nome());
            assertEquals("Feijão, preto, cozido", fonte.buscar("feijao preto").getFirst().nome());
            assertTrue(fonte.buscar("whey marca desconhecida").isEmpty());
            server.removeContext("/foods/1.json");
            var erro = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> fonte.buscar("arroz cozido"));
            assertEquals(503, erro.getStatusCode().value());
        } finally {
            server.stop(0);
        }
    }

    private static void responderJson(com.sun.net.httpserver.HttpExchange exchange, String resposta)
            throws java.io.IOException {
        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var body = exchange.getResponseBody()) {
            body.write(bytes);
        }
    }

    private class Sessao {
        private final HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        private JsonNode csrf;

        void login(String usuario, String senha) throws Exception {
            csrf = corpo(200, "GET", "/csrf", null);
            String form = "username=" + usuario + "&password=" + senha + "&"
                    + csrf.get("parameterName").asString() + "="
                    + URLEncoder.encode(csrf.get("token").asString(), StandardCharsets.UTF_8);
            var post = HttpRequest.newBuilder(uri("/login")).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form)).build();
            assertEquals("/", client.send(post, HttpResponse.BodyHandlers.ofString()).headers()
                    .firstValue("location").map(URI::create).map(URI::getPath).orElseThrow());
            csrf = corpo(200, "GET", "/csrf", null);
        }

        JsonNode corpo(int esperado, String metodo, String caminho, String dados) throws Exception {
            var resposta = requisicao(metodo, caminho, dados);
            assertEquals(esperado, resposta.statusCode(), resposta.body());
            return json.readTree(resposta.body());
        }

        HttpResponse<String> requisicao(String metodo, String caminho, String dados) throws Exception {
            return enviar(metodo, caminho, dados, true);
        }

        HttpResponse<String> enviar(String metodo, String caminho, String dados, boolean incluirCsrf) throws Exception {
            var pedido = HttpRequest.newBuilder(uri(caminho)).method(metodo,
                    dados == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(dados))
                    .header("Content-Type", "application/json");
            if (!metodo.equals("GET") && incluirCsrf) {
                pedido.header(csrf.get("headerName").asString(), csrf.get("token").asString());
            }
            return client.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
        }

        private URI uri(String caminho) {
            return URI.create("http://127.0.0.1:" + porta + caminho);
        }
    }
}
