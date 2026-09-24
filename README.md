# Decisão Eleitoral — apoio à decisão com dados abertos

Projeto da disciplina **Projeto de Programação** (Java). Ferramenta desktop que reúne dados públicos
do **TSE** e da **Câmara dos Deputados** sobre candidaturas a **deputado federal** de uma UF, calcula
indicadores objetivos e deixa o usuário definir o que pesa mais para ele.

> O sistema **não diz quem é o melhor candidato**. O foco é a transparência do método, não um veredito.

## Como executar

Requisitos: **JDK 17+** e **Maven** (ou NetBeans/IntelliJ, que abrem o `pom.xml` diretamente).

```bash
mvn package                      # compila, roda os testes e gera target/decisao-eleitoral-0.1.0.jar
java -jar target/decisao-eleitoral-0.1.0.jar
```

A janela abre com uma **demonstração com dados fictícios** (pessoas "Exemplo", partidos "PX_", UF "XX"),
que funciona sem internet. Para dados reais, use o menu **Dados › Coletar e processar UF** ou a linha de comando:

| Comando | O que faz |
|---|---|
| `java -jar ... ` | abre a interface com a demonstração |
| `java -jar ... coletar SE` | baixa os arquivos do TSE e da Câmara e processa a UF (eleição 2026) |
| `java -jar ... coletar SE 2026 --sem-download` | processa arquivos já colocados em `dados/brutos/` |
| `java -jar ... abrir SE` | abre a interface com `dados/processados/SE` |
| `java -jar ... ranking SE` ou `ranking demo` | imprime o ranking com pesos iguais no console |

Os arquivos do TSE têm centenas de MB; o download é feito uma vez e fica em `dados/brutos/` (cache).

## Telas

- **Ranking** – um slider de peso (0 a 10) por indicador, recalculado em tempo real; opção de inverter o
  sentido de cada indicador; filtro de candidaturas inaptas; soma ponderada ou TOPSIS; cobertura mínima.
- **Perfil** – dados cadastrais, cada indicador com o cálculo e a fonte, "sem dados" em vez de zero,
  proposições aprovadas e aviso quando o vínculo TSE × Câmara foi feito só pelo nome.
- **Comparar** – 2 ou 3 candidatos lado a lado (tabela + gráfico de notas normalizadas).
- **Análise de perfil** – distribuições (escolaridade, faixa etária, bens, gênero, cor/raça, partido) e
  dispersão X × Y com correlação de Pearson e matriz de correlações.
- **Pautas** – o usuário escolhe votações do Plenário e diz como votaria; isso alimenta o *alinhamento*.
- **Metodologia e limites** – fórmulas, viés contra estreantes, dados autodeclarados, "quantidade não é qualidade".

## Indicadores

| Indicador | Fonte | Cálculo |
|---|---|---|
| Assiduidade | Câmara | % de votações nominais do Plenário com voto registrado, no período em que exerceu o mandato |
| Gasto da cota | Câmara | z-score do gasto médio mensal da CEAP em relação aos deputados da mesma UF |
| Produção legislativa | Câmara | nº de PL, PLP, PEC, PDL e PRC como autor proponente; aprovadas em destaque |
| Variação de patrimônio | TSE | % de variação dos bens declarados entre a eleição anterior e a atual |
| Alinhamento por pauta | Câmara | % dos votos Sim/Não do deputado iguais às posições marcadas pelo usuário |
| Elegibilidade | TSE | situação da candidatura – **filtro, não nota** |

Gênero e cor/raça aparecem **só** na análise de perfil, nunca na pontuação. O CPF é usado apenas no
cruzamento TSE × Câmara e não é gravado nos arquivos processados.

## Arquitetura

Só Java SE, sem dependências em tempo de execução (JUnit 5 apenas nos testes).

```
coleta (HttpClient, java.util.zip)  →  persistência (arquivos CSV)  →  indicadores  →  ranking  →  interface Swing
```

```
src/main/java/br/unit/eleicao/
├── App.java                    ponto de entrada (interface ou linha de comando)
├── modelo/                     Pessoa (abstrata) → Candidato, Deputado; Votacao, Despesa, Proposicao, BaseDados...
├── excecao/                    DadosException (checked) → ArquivoInvalidoException, ColetaException
├── util/                       LeitorCsv, EscritorCsv, Texto, Estatistica (média, desvio, z, Pearson)
├── coleta/                     FontesDados, Downloader, ProcessadorTSE, ProcessadorCamara,
│                               CruzadorIdentidades, PipelineColeta, GeradorDadosDemo
├── persistencia/               RepositorioArquivos (cache local em dados/processados/UF)
├── indicador/                  Indicador (abstrata) → Assiduidade, GastoCota, ProducaoLegislativa,
│                               VariacaoPatrimonio, AlinhamentoPauta
├── ranking/                    Normalizador, MetodoRanking (abstrata) → SomaPonderada, Topsis
└── ui/                         JanelaPrincipal e abas; grafico/ com GraficoBase → GraficoBarras, GraficoDispersao
```

### Onde está cada conteúdo da disciplina

| Conteúdo | Onde |
|---|---|
| Classes, objetos, encapsulamento, construtores, pacotes | todo o `modelo/` (atributos privados, getters/setters, validação em `PosicaoUsuario`) |
| Herança | `Pessoa → Candidato/Deputado`, `Indicador → 5 indicadores`, `GraficoBase → gráficos`, exceções |
| Polimorfismo / override | `Indicador.calcular` e `formatar`; `GastoCota` sobrescreve `calcularTodos`; `paintComponent`; `equals`/`hashCode`/`toString` (classe Object) |
| Overload | `ResultadoIndicador`/`Indicador.formatar(double)` e `formatar(ResultadoIndicador)`; `GraficoBarras.setDados` |
| Métodos estáticos | `Texto`, `Estatistica`, `FontesDados`, `CatalogoIndicadores` |
| Tratamento de exceções | exceções próprias, `try-with-resources`, `throw`, multi-catch em `Downloader` e `JanelaPrincipal` |
| Arquivos texto e binários | `LeitorCsv`/`EscritorCsv`, `Files`, `Properties`, leitura de `.zip` em fluxo |
| Arrays e coleções | `ArrayList`, `HashMap`, `TreeMap`, `LinkedHashMap`, `HashSet`, arrays em `Estatistica` |
| Strings | normalização de nomes, `split`, `format`, `StringBuilder`, text/HTML |
| GUI e programação orientada a eventos | Swing: `JSlider`, `JTable`, listeners, `SwingWorker` para a coleta |

## Decisões tomadas

- **UF**: qualquer uma; **SE** vem pré-selecionada na tela de coleta (8 deputados, volume pequeno).
- **Desktop (Swing)** em vez de Spring Boot: está no JDK, cobre o tópico GUI da ementa e não exige servidor.
- **Arquivos CSV** em vez de SQLite/H2: cobre o tópico de arquivos texto e mantém o projeto sem dependências.
  Trocar por banco depois só afeta `RepositorioArquivos`.
- **Estatística implementada à mão** (sem Apache Commons Math), para cada fórmula poder ser conferida.

## Cruzamento TSE × Câmara

Ordem de confiança: vínculo manual → CPF → nome civil + data de nascimento → só nome civil → nome de urna.
Os dois últimos ficam marcados com ⚠ e vão para `dados/processados/UF/vinculos_para_conferir.csv`.
Correções manuais: criar `dados/vinculos_manuais.csv` com `sq;idDeputado` (0 = forçar "sem vínculo").

## Testes

`mvn test` roda 31 testes: estatística, leitor CSV (aspas, BOM, quebra de linha), cada indicador,
normalização, soma ponderada, TOPSIS, cobertura mínima, filtro de elegibilidade, cruzamento de identidades
(inclusive homônimos), gravação/leitura dos arquivos e a coleta completa sobre arquivos que imitam o layout
real do TSE (zip, ISO-8859-1) e da Câmara (UTF-8 com BOM).

> **Atenção:** os leitores foram escritos para o layout publicado das fontes e testados com arquivos de
> exemplo nesse formato, mas ainda **não foram rodados contra os arquivos reais baixados** (o ambiente de
> desenvolvimento não tinha acesso aos sites do TSE e da Câmara). Se alguma coluna tiver mudado de nome,
> a coleta para com uma mensagem dizendo qual coluna falta e em qual arquivo; basta acrescentar o novo nome
> na chamada `csv.indice(...)` correspondente.
