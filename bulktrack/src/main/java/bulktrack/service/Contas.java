package bulktrack.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Contas implements UserDetailsService, ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Environment env;

    public Contas(JdbcTemplate jdbc, PasswordEncoder encoder, Environment env) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.env = env;
    }

    public long id(String usuario) {
        return jdbc.queryForObject("SELECT id FROM conta WHERE usuario = ?", Long.class, usuario);
    }

    @Override
    public UserDetails loadUserByUsername(String usuario) {
        return jdbc.query("SELECT usuario, senha_hash FROM conta WHERE usuario = ?",
                (rs, row) -> User.withUsername(rs.getString(1)).password(rs.getString(2))
                        .roles("USER").build(), usuario.toLowerCase(Locale.ROOT))
                .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Conta inexistente"));
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String usuario = env.getProperty("BULKTRACK_USUARIO", "").strip().toLowerCase(Locale.ROOT);
        String senha = env.getProperty("BULKTRACK_SENHA", "");
        Long legado = env.getProperty("BULKTRACK_PERFIL_LEGADO", Long.class);
        if (usuario.isEmpty() && senha.isEmpty() && legado == null) {
            return;
        }
        if (!usuario.matches("[a-z0-9_.-]{3,64}") || senha.length() < 12
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("Configure usuário e senha válidos para provisionar a conta.");
        }
        var contas = jdbc.queryForList("SELECT senha_hash FROM conta WHERE usuario = ?", String.class, usuario);
        if (contas.isEmpty()) {
            jdbc.update("INSERT INTO conta(usuario, senha_hash) VALUES (?, ?)", usuario, encoder.encode(senha));
        } else if (!encoder.matches(senha, contas.getFirst())) {
            throw new IllegalStateException("A senha de provisionamento difere da senha da conta existente.");
        }
        if (legado != null) {
            long conta = id(usuario);
            int atualizadas = jdbc.update("""
                    UPDATE perfil SET conta_id = ? WHERE id = ? AND conta_id IS NULL
                    """, conta, legado);
            if (atualizadas == 0 && jdbc.queryForObject(
                    "SELECT COUNT(*) FROM perfil WHERE id = ? AND conta_id = ?", Long.class, legado, conta) == 0) {
                throw new IllegalStateException("Perfil legado inexistente ou associado a outra conta.");
            }
        }
    }
}
