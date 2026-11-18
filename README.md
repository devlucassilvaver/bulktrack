# BulkTrack

Aplicação local para acompanhar metas nutricionais, alimentação, peso e treino. O Spring Boot serve a API e as quatro telas no mesmo endereço.

## Executar no Windows

Use Java 21, MySQL 8 e Windows PowerShell ou PowerShell 7. Na pasta que contém este README, entre no módulo:

```powershell
Set-Location .\bulktrack
java -version
```

Para uma instalação nova, execute no MySQL Workbench ou no cliente MySQL, com uma conta administrativa. Substitua `SENHA_FORTE_DE_EXEMPLO` antes de executar:

```sql
CREATE DATABASE bulktrack CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'bulktrack_local'@'localhost' IDENTIFIED BY 'SENHA_FORTE_DE_EXEMPLO';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES, DROP
ON bulktrack.* TO 'bulktrack_local'@'localhost';
```

Use o banco já existente se você quiser manter os perfis e pesagens. Faça um backup antes da primeira atualização. O Flyway aplica as migrações em `src/main/resources/db/migration`. Em um banco anterior ao Flyway, a versão 1 cria as tabelas originais apenas se estiverem ausentes. As versões seguintes acrescentam as estruturas do MVP.

Configure as variáveis no PowerShell. O arquivo [config.example.ps1](bulktrack/config.example.ps1) contém os nomes e exemplos. Configure a senha antes de iniciar a JVM:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/bulktrack'
$env:DB_USERNAME = 'bulktrack_local'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha MySQL' -AsSecureString)).Password
$env:BULKTRACK_USUARIO = Read-Host 'Novo usuário local'
$env:BULKTRACK_SENHA = [System.Net.NetworkCredential]::new('', (Read-Host 'Senha da conta local' -AsSecureString)).Password
# Opcional para habilitar a busca USDA
$env:FDC_API_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'Chave USDA' -AsSecureString)).Password
.\mvnw.cmd spring-boot:run
```

Abra [localhost:8080](http://127.0.0.1:8080) e entre com a conta local. O usuário aceita de 3 a 64 caracteres entre letras ASCII, números, ponto, hífen e sublinhado. A senha requer pelo menos 12 caracteres e até 72 bytes em UTF-8. O provisionamento é feito ao iniciar e persiste o hash BCrypt. Após verificar que consegue entrar, pare o servidor com Ctrl+C, remova as variáveis de provisionamento e inicie novamente:

```powershell
Remove-Item Env:BULKTRACK_USUARIO, Env:BULKTRACK_SENHA, Env:BULKTRACK_PERFIL_LEGADO -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run
```

Para provisionar uma segunda conta, repita o comando com outro usuário, sem indicar o ID de um perfil já associado. O MVP usa uma conta por perfil e autenticação por sessão com proteção CSRF.

### Vincular um perfil existente

1. Antes de criar um perfil pelo site, consulte `SELECT id, nome FROM perfil ORDER BY id;` no banco.
2. Na primeira execução com as variáveis de provisionamento, acrescente `$env:BULKTRACK_PERFIL_LEGADO = '123'`, com o ID correto. Uma conta já provisionada pode reivindicar um perfil legado se ainda não tiver perfil. O vínculo preserva o ID e as pesagens.
3. Entre na conta e abra Perfil. Escolha o parâmetro fisiológico e salve para iniciar as metas. Registros anteriores continuam disponíveis. Uma data anterior à primeira meta aparece sem meta.

## Metas e peso

A estimativa usa [Mifflin–St Jeor](https://pubmed.ncbi.nlm.nih.gov/2305711/). A TMB usa `10 × peso (kg) + 6,25 × altura (cm) − 5 × idade + constante`. A constante selecionada é +5 ou −161, sem inferência a partir da identidade informada. O perfil aceita datas de nascimento entre 18 e 130 anos atrás. A altura é informada em metros.

| Atividade | Fator do GET |
| --- | ---: |
| Sedentário | 1,20 |
| Leve | 1,38 |
| Moderado | 1,55 |
| Alto | 1,73 |
| Muito alto | 1,90 |

O GET é a TMB multiplicada pelo fator de atividade. A meta padrão usa o GET na manutenção, +10% no ganho e −15% na perda. O percentual pode ser ajustado no perfil. A divisão inicial é 50% carboidratos, 25% proteínas e 25% lipídios usando 4, 4 e 9 kcal/g. A estimativa arredonda calorias para inteiros e macros para décimos de grama. Uma meta personalizada pode variar até 1 kcal da energia calculada a partir dos macros.

O cálculo usa o peso atual do perfil. Uma pesagem por dia mais recente, ou a última registrada na mesma data, atualiza esse peso. Uma pesagem retroativa mais antiga entra no histórico. Editar o peso no perfil acrescenta uma pesagem na data local. A meta vigente é gravada na data da mudança. Mudanças no mesmo dia substituem a meta daquele dia, e as dos dias anteriores mantêm a vigência histórica.

## Alimentação

A busca no back-end usa [USDA FoodData Central](https://fdc.nal.usda.gov/api-guide/), dados SR Legacy por 100 g, publicados sob CC0. A chave gratuita é fornecida pelo data.gov. O catálogo é dos EUA, os resultados aparecem em inglês e a cobertura de marcas e preparações brasileiras é limitada. Alguns termos comuns são traduzidos, incluindo arroz branco e integral nas versões crua e cozida. Confira a descrição antes de selecionar.

Na tela Alimentação, busque **arroz cozido**, selecione uma descrição que contenha **cooked**, digite **200 g** e ajuste o horário para **12h30**. A prévia é calculada pela API. Se a busca estiver indisponível ou o alimento não aparecer, preencha manualmente o nome e a composição por 100 g. O consumo armazena os valores usados no registro. Alterar quantidade ou horário não consulta a fonte novamente.

O dashboard filtra pelo dia no fuso IANA do perfil, inicialmente `America/Fortaleza`. Consumos ficam gravados como instantes. Alterar o fuso pode mover um consumo próximo à meia-noite entre datas locais. Um nutriente ausente fica indisponível. O resumo exibe apenas a soma conhecida quando parcial, com a contagem de registros incompletos e sem saldo definitivo para aquele nutriente.

## Treinos

Crie uma sessão com nome e data, adicione exercícios na ordem desejada e informe descanso em segundos. Adicione as séries com suas repetições e carga opcional em kg. Use o filtro de data no histórico para consultar, editar ou excluir uma sessão.

## API e testes

Use a sessão criada pelo formulário de login. Clientes HTTP podem obter o token em `GET /csrf`, enviar `username`, `password` e o parâmetro CSRF para `POST /login`, preservar o cookie e obter um novo token depois do login. Envie o token atualizado no cabeçalho informado pelo endpoint ao modificar dados.

| Método | Rota | Uso |
| --- | --- | --- |
| GET | `/perfis/me` | Perfil da conta autenticada |
| POST, PUT | `/perfis`, `/perfis/{id}` | Criar ou editar perfil |
| GET, POST | `/perfis/{id}/pesos` | Listar ou registrar pesagens |
| GET | `/api/alimentos?q=arroz%20cozido` | Buscar nutrientes por 100 g |
| POST | `/api/alimentos/previa` | Calcular nutrientes para uma quantidade |
| GET, POST | `/perfis/{id}/diario` | Resumo e lista por `?data=AAAA-MM-DD`, ou registrar consumo |
| PUT, DELETE | `/perfis/{id}/diario/{consumo}` | Editar horário e quantidade ou excluir consumo |
| GET, POST | `/perfis/{id}/treinos` | Listar sessões, com filtro opcional de data, ou criar |
| GET, PUT, DELETE | `/perfis/{id}/treinos/{treino}` | Consultar, editar ou excluir sessão |

```powershell
.\mvnw.cmd test
```

A suíte usa H2 em memória no modo MySQL e simula o USDA por HTTP local. Ela cobre metas, pesagens, histórico, porções, totais, treinos, autenticação com sessão, CSRF e isolamento entre contas. Para mudar as cores, edite o bloco `:root` de `bulktrack/src/main/resources/static/assets/style.css`.

## Antes de disponibilizar a convidados

O servidor inicia em loopback. Para hospedar, prepare HTTPS com cookies seguros, configuração de produção e gerenciamento de segredos, backup e restauração do MySQL, monitoramento, proteção contra tentativas repetidas de login e um fluxo de convites e recuperação de senha. Avalie privacidade e retenção dos dados corporais antes de abrir o acesso. O provisionamento local continua sendo a forma de criar contas no MVP.
