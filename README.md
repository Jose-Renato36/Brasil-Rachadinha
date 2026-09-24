# Voto com Dados — apoio à decisão eleitoral com dados abertos

Projeto da disciplina **Projeto de Programação** (Java). Um site que roda no seu computador
(`http://localhost:8080`) e reúne dados públicos do **TSE** e da **Câmara dos Deputados** sobre as
candidaturas a **deputado federal** de um estado. Quem usa escolhe o que é importante; o sistema calcula
indicadores e mostra de onde veio cada número.

> O sistema **não diz quem é o melhor candidato**. O foco é a transparência do método, não um veredito.

## Como rodar no IntelliJ

1. **File › Open** e escolha a pasta do projeto (a que tem o `pom.xml`). O IntelliJ reconhece o projeto
   **Maven** e baixa o JUnit (única dependência, só para testes).
2. Confira o JDK: **File › Project Structure › SDK = 17 ou mais novo**.
3. No seletor de execução (canto superior direito) já aparecem três configurações prontas (pasta `.run/`):
   - **1 - Abrir o site (localhost)**: sobe o servidor e abre o navegador em `http://localhost:8080`
     com a demonstração. Para parar, clique em *Stop*.
   - **2 - Coletar dados de SE**: baixa e processa os dados reais de Sergipe (troque a UF nos argumentos).
   - **3 - Rodar os testes**: `mvn test`.

Sem o IntelliJ: `mvn compile exec:java` (abre o site) ou `mvn package` e
`java -jar target/decisao-eleitoral-0.2.0.jar`.

A coleta também pode ser feita **pelo próprio site**, na aba **Dados**: escolha a UF, clique em
*Coletar e abrir* e acompanhe o andamento na tela.

### Linha de comando

| Comando | O que faz |
|---|---|
| `web [UF\|demo] [porta]` | sobe o site (padrão: demonstração, porta 8080 ou a próxima livre) |
| `coletar SE [2026] [--sem-download]` | baixa e processa a UF; `--sem-download` usa só o que está em `dados/brutos/` |
| `ranking SE\|demo` | imprime o ranking com pesos iguais no console |

## O site

| Aba | Para quê |
|---|---|
| **Início** | busca por nome ou número e os três passos |
| **O que importa pra mim** | para cada critério: *Não importa / Importa / Importa muito*; a lista se reorganiza na hora. Opções avançadas: pesos de 0 a 10, inverter o sentido, TOPSIS, cobertura mínima |
| **Minhas opiniões** | projetos já votados no Plenário, um por vez: *Eu votaria SIM / NÃO / Pular*. Alimenta o critério "Vota como eu votaria" |
| **Perfil** | número na urna, situação da candidatura, cada indicador com o cálculo e a fonte, projetos aprovados |
| **Comparar** | até 3 pessoas lado a lado, com gráfico |
| **Panorama** | distribuições (escolaridade, idade, bens, gênero, cor/raça…) e correlação entre duas variáveis |
| **Como funciona** | metodologia e limites |
| **Dados** | trocar de base e coletar um estado |

## Indicadores

| Critério (no site) | Fonte | Cálculo |
|---|---|---|
| Presença nas votações | Câmara | % das votações nominais do Plenário com voto registrado, **só no período em exercício** (licenças não contam como falta) |
| Economia da verba de gabinete | Câmara | z-score do gasto médio mensal da cota (CEAP) contra os deputados da mesma UF |
| Projetos apresentados | Câmara | nº de PL, PLP, PEC, PDL e PRC como autor; aprovados listados no perfil |
| Patrimônio estável | TSE | variação % dos bens declarados entre 2022 e 2026 |
| Vota como eu votaria | Câmara | % dos votos Sim/Não iguais às respostas do usuário |
| Situação da candidatura | TSE | **filtro, não nota** (apta, em análise, com recurso, inapta) |

"Sem dados" nunca vira zero. Por padrão só recebe nota quem tem dados em pelo menos **2 critérios**,
para um estreante não ficar em 1º lugar avaliado só pelo patrimônio. Gênero e cor/raça aparecem só no
Panorama. O CPF é usado apenas para cruzar TSE × Câmara e não é gravado.

## Fontes de dados

Os formatos foram **conferidos antes de escrever os leitores** (cópias públicas dos arquivos de 2026 e
projetos abertos que processam os mesmos arquivos). Detalhes, URLs, codificações e armadilhas em
**[docs/FONTES.md](docs/FONTES.md)**. Resumo:

- **TSE**: `consulta_cand`, `consulta_cand_complementar` e `bem_candidato` de 2026 e 2022
  (ZIP com um CSV por UF, `;`, ISO-8859-1). O CPF vem preenchido, o que permite cruzar com a Câmara.
- **Câmara**: arquivos em lote `votacoes`, `votacoesVotos`, `votacoesProposicoes`, `proposicoes`,
  `proposicoesAutores`, `deputados` (CSV UTF-8 com BOM), a cota `Ano-{ano}.csv.zip` e a API
  `/deputados/{id}/historico` para licenças.
- **Se o TSE recusar o download** (acontece com acesso automático): baixe os `.zip` pelo navegador em
  <https://dadosabertos.tse.jus.br/dataset/candidatos-2026> e `.../candidatos-2022`, coloque em
  `dados/brutos/` sem renomear e colete de novo sem download.

## Arquitetura

O núcleo usa só o que está no material da disciplina (POO, coleções, exceções, arquivos). A coleta
usa `java.net.http` e a interface é HTML/CSS/JavaScript servido pelo `com.sun.net.httpserver` do próprio JDK.
Não há bibliotecas externas em tempo de execução.

```
coleta (TSE + Câmara) → arquivos CSV (cache) → indicadores → ranking → API JSON → site (HTML/CSS/JS)
```

```
src/main/java/br/unit/eleicao/
├── App.java              entrada: sobe o site ou roda a coleta
├── modelo/               Pessoa (abstrata) → Candidato, Deputado; Votacao, Despesa, Proposicao, Elegibilidade…
├── excecao/              DadosException (checked) → ArquivoInvalidoException, ColetaException
├── util/                 LeitorCsv, EscritorCsv, Texto, Estatistica (média, desvio, z, Pearson, regressão)
├── coleta/               FontesDados, Downloader, ProcessadorTSE, ProcessadorCamara, HistoricoDeputados,
│                         CruzadorIdentidades, PipelineColeta, GeradorDadosDemo
├── persistencia/         RepositorioArquivos (cache em dados/processados/UF)
├── indicador/            Indicador (abstrata) → 5 indicadores
├── ranking/              Normalizador, MetodoRanking (abstrata) → SomaPonderada, Topsis
└── web/                  ServidorWeb (HTTP), ServicoApi (regras da API), Json
src/main/resources/web/   index.html, estilo.css, app.js, metodologia.html
```

### Onde está cada conteúdo da disciplina

| Conteúdo | Onde |
|---|---|
| Classes, encapsulamento, construtores, pacotes | `modelo/` (atributos privados, validação em `PosicaoUsuario` e `ConfiguracaoRanking`) |
| Herança | `Pessoa → Candidato/Deputado`, `Indicador → 5 indicadores`, `MetodoRanking → SomaPonderada/Topsis`, exceções |
| Polimorfismo / override | `calcular`, `formatar`, `resumir` em cada indicador; `GastoCota` sobrescreve `calcularTodos`; `equals`/`hashCode`/`toString` |
| Overload | `formatar(double)` / `formatar(ResultadoIndicador)`, `resumir(...)`, construtores das exceções |
| Enum | `GrauInstrucao`, `Elegibilidade`, `Sentido` |
| Métodos estáticos | `Texto`, `Estatistica`, `FontesDados`, `Json` |
| Exceções | exceções próprias, `try-with-resources`, `throw`, multi-catch |
| Arquivos texto e binários | `LeitorCsv`/`EscritorCsv`, `Files`, `Properties`, leitura de `.zip` em fluxo |
| Coleções | `ArrayList`, `HashMap`, `TreeMap`, `LinkedHashMap`, `HashSet`, `Collections.unmodifiable…` |
| Strings | normalização de nomes, `split`, `format`, `StringBuilder`, regex |

## Testes

`mvn test` roda 38 testes: estatística, CSV, cada indicador (inclusive presença descontando licenças),
normalização, soma ponderada, TOPSIS, cobertura mínima, elegibilidade, histórico de deputados, cruzamento
de identidades, gravação/leitura, a coleta completa sobre arquivos no **layout real** do TSE 2026 (50
colunas, `#NULO`, latin-1) e da Câmara, e o servidor web respondendo como o navegador.

> Os leitores seguem os formatos conferidos, mas a coleta completa contra os servidores oficiais ainda
> não foi executada (o ambiente de desenvolvimento não tinha acesso a eles). Se uma coluna mudar de nome,
> a coleta para e diz qual coluna falta e em qual arquivo.
