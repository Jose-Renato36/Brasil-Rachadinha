# Voto com Dados — apoio à decisão eleitoral com dados abertos

Projeto da disciplina **Projeto de Programação** (Java). Um site que roda no seu computador
(`http://localhost:8080`) e reúne dados públicos do **TSE** e da **Câmara dos Deputados** sobre as
candidaturas de **todos os cargos, no Brasil inteiro** (presidente, governador, senador, deputado federal e
estadual), com filtro por estado, a trajetória política de cada pessoa e o que ela fez nos mandatos anteriores. Quem usa escolhe o que é importante; o sistema calcula
indicadores e mostra de onde veio cada número.

> O sistema **não diz quem é o melhor candidato**. O foco é a transparência do método, não um veredito.

## Como rodar no IntelliJ

1. **File › Open** e escolha a pasta do projeto (a que tem o `pom.xml`). O IntelliJ reconhece o projeto
   **Maven** e baixa o JUnit (única dependência, só para testes).
2. Confira o JDK: **File › Project Structure › SDK = 17 ou mais novo**.
3. No seletor de execução (canto superior direito) já aparecem três configurações prontas (pasta `.run/`):
   - **1 - Abrir o site (localhost)**: sobe o servidor e abre o navegador em `http://localhost:8080`
     com a demonstração. Para parar, clique em *Stop*.
   - **2 - Coletar dados do Brasil**: baixa e processa os dados reais do país inteiro (`coletar BR`). Para um
     estado só, troque o argumento pela sigla (ex.: `coletar SE`), que é bem mais rápido.
   - **3 - Rodar os testes**: `mvn test`.

Sem o IntelliJ: `mvn compile exec:java` (abre o site) ou `mvn package` e
`java -jar target/decisao-eleitoral-0.2.0.jar`.

O **seletor de estado** fica no topo de todas as páginas. A primeira coleta do Brasil inteiro baixa alguns GB
e pode passar de uma hora; depois fica guardada em `dados/` e o site abre direto nela.

A coleta também pode ser feita **pelo próprio site**, na aba **Dados**: escolha a UF, clique em
*Coletar e abrir* e acompanhe o andamento na tela.

### Linha de comando

| Comando | O que faz |
|---|---|
| `web [UF\|demo] [porta]` | sobe o site (padrão: demonstração, porta 8080 ou a próxima livre) |
| `coletar BR [2026] [--sem-download] [--empresas]` | baixa e processa o **Brasil inteiro** (ou `coletar SE` para um estado); `--sem-download` usa só o que está em `dados/brutos/`; `--empresas` baixa também os dados do CNPJ (vários GB) |
| `ranking SE\|demo` | imprime o ranking com pesos iguais no console |

## O site

| Aba | Para quê |
|---|---|
| **Início** | "Onde você vota?", busca por nome ou número e escolha do cargo (os do seu estado + Presidência) |
| **Candidatos** | todas as candidaturas do cargo em **cartões**: estreante, já concorreu, já eleito(a), com mandato hoje, e uma linha do tempo 2018–2024. Ao clicar, o cartão abre com o quadro **Preparo para o cargo**, a trajetória, o que fez nos mandatos (presença, projetos, verba, emendas, gasto com pessoal antes e depois), patrimônio em cada eleição, quem paga a campanha, empresas, serviço público federal e alertas (TCU, CGU, cassações) |
| **O que importa pra mim** | para cada critério: *Não importa / Importa / Importa muito*; a lista se reorganiza na hora. Opções avançadas: pesos de 0 a 10, inverter o sentido, TOPSIS, cobertura mínima |
| **Minhas opiniões** | projetos já votados no Plenário, um por vez: *Eu votaria SIM / NÃO / Pular*. Alimenta o critério "Vota como eu votaria" |
| **Perfil** | número na urna, situação da candidatura, cada indicador com o cálculo e a fonte, projetos aprovados e a **trajetória política** (candidaturas e mandatos desde 2018, inclusive vereador, prefeito e deputado estadual) |
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
| Trajetória política | TSE 2018-2024 | candidaturas anteriores e mandato atual; **contexto, não nota** |
| Atuação em mandatos | Câmara 2019-2023 e 2023-hoje, API do Senado | presença, projetos, aprovados, verba; **contexto, não nota** |
| Contas irregulares | TCU (lista da Ficha Limpa) | processos ligados pelo CPF; **contexto, não nota** |
| Preparo para o cargo | TSE, TCU, CGU | idade mínima na posse, registro, formação, anos de mandato (Executivo/Legislativo), experiência na mesma função, alertas; **contexto, não nota** |
| Patrimônio ao longo do tempo | TSE 2018-2026 | bens declarados em cada eleição disputada |
| Financiamento | TSE (prestação de contas) | receitas da campanha por origem; % de dinheiro público |
| Emendas | Portal da Transparência | emendas individuais: pago, empenhado, locais, áreas, emendas Pix |
| Empresas e sanções | Receita Federal (CNPJ), CGU (CEIS/CNEP) | sociedades e punições, ligadas por nome + CPF parcial |
| Serviço público federal | Portal da Transparência (SIAPE) | cargo, órgão e situação |
| Antes e depois | Tesouro (SICONFI) | gasto com pessoal em % da receita, ano a ano, para ex-prefeitos e governadores |

"Sem dados" nunca vira zero. Por padrão só recebe nota quem tem dados em pelo menos **2 critérios**,
para um estreante não ficar em 1º lugar avaliado só pelo patrimônio. Gênero e cor/raça aparecem só no
Panorama. O CPF é usado apenas para cruzar TSE × Câmara e não é gravado.

## Fontes de dados

Os formatos foram **conferidos antes de escrever os leitores** (cópias públicas dos arquivos de 2026 e
projetos abertos que processam os mesmos arquivos). Detalhes, URLs, codificações e armadilhas em
**[docs/FONTES.md](docs/FONTES.md)**. Resumo:

- **TSE**: `consulta_cand`, `consulta_cand_complementar` e `bem_candidato` de 2026 e 2022
  (ZIP com um CSV por UF, `;`, ISO-8859-1). O CPF vem preenchido, o que permite cruzar com a Câmara.
- **Complementares**: bens e cassações das eleições anteriores, prestação de contas (TSE); emendas, CEIS/CNEP
  e SIAPE (Portal da Transparência); CNPJ (Receita, opcional); SICONFI (Tesouro). Se uma delas faltar,
  a ficha diz "não consultado" em vez de "nada consta".
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
│                         CruzadorIdentidades, HistoricoTse, FinanciamentoCampanha, EmendasParlamentares,
│                         EmpresasReceita, SancoesCgu, ServidoresFederais, GestaoFiscalSiconfi,
│                         PipelineColeta, GeradorDadosDemo
├── persistencia/         RepositorioArquivos (cache em dados/processados/UF)
├── preparo/              CriterioPreparo (abstrata) → 8 critérios; QuadroPreparo aplica todos
├── indicador/            Indicador (abstrata) → 5 indicadores
├── ranking/              Normalizador, MetodoRanking (abstrata) → SomaPonderada, Topsis
└── web/                  ServidorWeb (HTTP), ServicoApi (regras da API), Json
src/main/resources/web/   index.html, estilo.css, app.js, metodologia.html
```

### Onde está cada conteúdo da disciplina

| Conteúdo | Onde |
|---|---|
| Classes, encapsulamento, construtores, pacotes | `modelo/` (atributos privados, validação em `PosicaoUsuario` e `ConfiguracaoRanking`) |
| Herança | `Pessoa → Candidato/Deputado`, `Indicador → 5 indicadores`, `CriterioPreparo → 8 critérios`, `MetodoRanking → SomaPonderada/Topsis`, exceções |
| Polimorfismo / override | `calcular`, `formatar`, `resumir` em cada indicador; `GastoCota` sobrescreve `calcularTodos`; `equals`/`hashCode`/`toString` |
| Overload | `formatar(double)` / `formatar(ResultadoIndicador)`, `resumir(...)`, construtores das exceções |
| Enum | `GrauInstrucao`, `Elegibilidade`, `Sentido`, `Cargo`, `Avaliacao` |
| Métodos estáticos | `Texto`, `Estatistica`, `FontesDados`, `Json` |
| Exceções | exceções próprias, `try-with-resources`, `throw`, multi-catch |
| Arquivos texto e binários | `LeitorCsv`/`EscritorCsv`, `Files`, `Properties`, leitura de `.zip` em fluxo |
| Coleções | `ArrayList`, `HashMap`, `TreeMap`, `LinkedHashMap`, `HashSet`, `Collections.unmodifiable…` |
| Strings | normalização de nomes, `split`, `format`, `StringBuilder`, regex |

## Testes

`mvn test` roda 55 testes (a coleta completa inclui Senado e TCU em cache): estatística, CSV, cada indicador (inclusive presença descontando licenças),
normalização, soma ponderada, TOPSIS, cobertura mínima, elegibilidade, histórico de deputados, trajetória
política (CPF mascarado em 2024, 2º turno, homônimos), cruzamento
de identidades, gravação/leitura, o quadro de preparo (idade na posse, anos de mandato, "não consultado" × "nada consta"), os leitores de receitas, emendas, CEIS/CNEP, SIAPE, CNPJ, bens anteriores, cassações e SICONFI, a coleta completa sobre arquivos no **layout real** do TSE 2026 (50
colunas, `#NULO`, latin-1) e da Câmara, e o servidor web respondendo como o navegador.

> Os leitores seguem os formatos conferidos, mas a coleta completa contra os servidores oficiais ainda
> não foi executada (o ambiente de desenvolvimento não tinha acesso a eles). Se uma coluna mudar de nome,
> a coleta para e diz qual coluna falta e em qual arquivo.
