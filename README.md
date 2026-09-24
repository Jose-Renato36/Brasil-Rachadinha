<div align="center">

# ☑️ Voto com Dados

**Conheça quem pede o seu voto antes de decidir.**

Um site que reúne dados públicos oficiais sobre **todas as candidaturas das eleições de 2026, no Brasil inteiro**,
e mostra, em linguagem simples, se a pessoa tem preparo para o cargo, o que já fez na política e de onde vem cada número.

![Java 17](https://img.shields.io/badge/Java-17%2B-1f5fae)
![Maven](https://img.shields.io/badge/build-Maven-1f5fae)
![Sem dependências](https://img.shields.io/badge/depend%C3%AAncias%20em%20execu%C3%A7%C3%A3o-nenhuma-1b7f4b)
![Testes](https://img.shields.io/badge/testes-70%20passando-1b7f4b)
![Licença](https://img.shields.io/badge/licen%C3%A7a-somente%20visualiza%C3%A7%C3%A3o-5d5c57)
![Dados abertos](https://img.shields.io/badge/dados-TSE%20%C2%B7%20C%C3%A2mara%20%C2%B7%20Senado%20%C2%B7%20TCU%20%C2%B7%20CGU%20%C2%B7%20IBGE%20%C2%B7%20INEP%20%C2%B7%20Tesouro%20%C2%B7%20BCB-5d5c57)

<img src="docs/imagens/inicio.png" alt="Página inicial: busca por nome ou número, escolha do estado e do cargo" width="820">

</div>

> [!IMPORTANT]
> **O Voto com Dados não diz em quem votar.** Ele não dá nota à pessoa: organiza fatos verificáveis,
> mostra a fonte de cada um e deixa você decidir o que pesa mais.

---

## Por que existe

Na hora de votar, a maioria das pessoas conhece poucos candidatos além do nome e do número. As informações
existem (o TSE, a Câmara, o Senado, o TCU, a CGU, a Receita e o Tesouro publicam tudo em dados abertos), mas
estão espalhadas em dezenas de arquivos, com formatos técnicos e sem ligação entre si.

Este projeto junta essas fontes, liga cada candidatura às informações da mesma pessoa e responde às perguntas
que o eleitor faz de verdade:

- **Tem preparo para o cargo?** Idade mínima, formação e anos de experiência no mesmo tipo de função.
- **Já foi político? O que fez, quando e onde?** Mandatos anteriores, presença em votações, projetos, emendas
  e, para quem já foi prefeito, governador ou presidente, como estavam a economia, o emprego, a educação e as
  contas do lugar antes e durante o mandato, comparados com o estado ou o Brasil.
- **Tem algo que mereça atenção?** Contas rejeitadas pelo TCU, cassações e sanções administrativas.
- **Quem paga a campanha e como mudou o patrimônio?**

## O que você encontra

<table>
<tr>
<td width="50%"><img src="docs/imagens/candidatos.png" alt="Cartões de candidatos com linha do tempo"></td>
<td width="50%"><img src="docs/imagens/resumo.png" alt="Em resumo: o essencial da ficha em poucas frases"></td>
</tr>
<tr>
<td><b>Candidatos em cartões.</b> Filtre por estado e cargo e veja de relance quem é estreante, quem já concorreu,
quem já foi eleito(a) e quem tem mandato hoje, com uma linha do tempo de 2018 a 2024.</td>
<td><b>Em resumo.</b> O essencial da ficha em poucas frases do dia a dia, com o botão para ler o plano de governo.
Logo abaixo vem o <b>Preparo para o cargo</b>: requisitos da lei, formação, experiência e alertas, sempre com a fonte.</td>
</tr>
<tr>
<td width="50%"><img src="docs/imagens/antes-depois.png" alt="Como estava a cidade quando a pessoa foi prefeita, comparada com o estado"></td>
<td width="50%" align="center"><img src="docs/imagens/celular.png" alt="Ficha no celular" width="220"></td>
</tr>
<tr>
<td><b>Como estava o lugar quando a pessoa governou.</b> PIB, empregos, IDEB e contas antes e durante o mandato,
sempre ao lado do estado ou do Brasil no mesmo período. Vale para prefeito, governador e presidente.</td>
<td><b>Funciona no celular e no tema escuro.</b></td>
</tr>
</table>

<sub>As imagens usam a base de demonstração: pessoas, partidos, cidades e empresas são fictícios.</sub>

### Na ficha de cada candidato

| Seção | O que mostra | Fonte |
|---|---|---|
| **Em resumo** | poucas frases em linguagem do dia a dia com o essencial da ficha, e o botão para ler o plano de governo | todas as fontes |
| **Preparo para o cargo** | idade mínima na posse (Constituição, art. 14), situação do registro, escolaridade, anos de mandato no Executivo e no Legislativo, experiência na mesma função | TSE |
| **Trajetória política** | candidaturas de 2018 a 2024 (inclusive vereador e prefeito), resultado e votos de cada uma | TSE |
| **O que fez nos mandatos** | presença nas votações, projetos, aprovados e verba de gabinete (deputados e senadores) | Câmara, Senado |
| **Emendas parlamentares** | quanto indicou e quanto foi pago, para quais cidades e áreas, parte em "emenda Pix" | Portal da Transparência |
| **Como estava o lugar quando governou** | ex-prefeitos, ex-governadores e ex-presidentes: PIB, empregos, desemprego, IDEB, gasto com pessoal, investimento e contas; para presidente também inflação, dívida, medidas provisórias e projetos aprovados. Sempre ao lado do estado ou do Brasil no mesmo período | IBGE, INEP, Tesouro, Banco Central, Câmara |
| **Patrimônio** | bens declarados em cada eleição disputada | TSE |
| **Dinheiro da campanha** | de onde vem (fundo eleitoral, partido, doações, dinheiro próprio) e em que é gasto | TSE (prestação de contas) |
| **Fora da política** | cargos públicos de gestão (ministro, secretário, estatais), empresas de que é sócio(a), contratos dessas empresas com o governo federal e vínculo com o serviço público | CGU, Receita Federal, Portal da Transparência |
| **Registros que merecem atenção** | contas julgadas irregulares, cassações, sanções CEIS/CNEP e expulsões do serviço público (CEAF) | TCU, TSE, CGU |

### Outras páginas

| Página | Para quê |
|---|---|
| **O que importa pra mim** | você diz o que é importante (*Não importa / Importa / Importa muito*) e a lista se reorganiza na hora; opções avançadas com pesos de 0 a 10 e TOPSIS |
| **Minhas opiniões** | projetos já votados no Plenário, um por vez: *eu votaria SIM / NÃO*; mostra quem votou como você |
| **Comparar** | até 3 pessoas lado a lado |
| **Panorama** | quem são os candidatos: escolaridade, idade, experiência, bens, gênero, cor/raça |
| **Como funciona** | toda a metodologia e os limites, em linguagem simples |

## Começando

Você precisa do **Java 17 ou mais novo**. O Maven vem embutido no IntelliJ.

### No IntelliJ

1. **File › Open** e escolha a pasta do projeto (a que tem o `pom.xml`).
2. Confira o JDK em **File › Project Structure › SDK** (17+).
3. No seletor de execução (canto superior direito) já há três configurações prontas:
   - **1 - Abrir o site (localhost)**: abre `http://localhost:8080` com a **demonstração** (dados fictícios, sem internet).
   - **2 - Coletar dados do Brasil**: baixa e processa os dados reais do país inteiro.
   - **3 - Rodar os testes**.

### No terminal

```bash
mvn compile exec:java                                # abre o site com a demonstração
mvn package && java -jar target/decisao-eleitoral-0.2.0.jar coletar SE   # coleta um estado (mais rápido)
java -jar target/decisao-eleitoral-0.2.0.jar coletar BR                  # coleta o Brasil inteiro
java -jar target/decisao-eleitoral-0.2.0.jar web                         # abre o site com os dados coletados
```

| Comando | O que faz |
|---|---|
| `web [UF\|demo] [porta]` | sobe o site (porta 8080 ou a próxima livre) |
| `coletar BR\|UF [2026] [--sem-download] [--sem-empresas]` | baixa e processa **tudo**; `--sem-download` usa só o que já está em `dados/brutos/`; `--sem-empresas` pula os dados do CNPJ e dos contratos federais (vários GB), para uma coleta mais rápida |
| `ranking UF\|demo` | imprime um ranking com pesos iguais no console |

A coleta também pode ser feita **pelo próprio site**, na página **Dados**. Por padrão ela busca **tudo** (todas as
fontes, inclusive empresas e contratos). A primeira coleta do Brasil inteiro baixa **cerca de 15 GB** e pode levar
**várias horas**; reserve uns 30 GB livres. Depois tudo fica guardado em `dados/` e as próximas coletas reaproveitam
o que já foi baixado.

Depois de coletar, o site abre sozinho os **dados reais**: o Brasil inteiro, se foi coletado, ou o último estado
coletado. A demonstração (dados fictícios) só aparece enquanto não houver nenhuma coleta, e pode ser aberta
a qualquer momento na página **Dados**.

> [!TIP]
> Se o site do TSE recusar o download automático, baixe os `.zip` pelo navegador em
> <https://dadosabertos.tse.jus.br>, coloque em `dados/brutos/` sem renomear e rode `coletar ... --sem-download`.

## De onde vêm os dados

Todos os formatos foram **conferidos antes de escrever os leitores**, e nenhum dado é inventado.
Os detalhes de cada fonte (URLs, colunas, codificação e armadilhas) estão em **[docs/FONTES.md](docs/FONTES.md)**.

| Órgão | Arquivos usados |
|---|---|
| **TSE** | candidaturas, bens, cassações e prestação de contas de 2018 a 2026 |
| **Câmara dos Deputados** | votações, votos, proposições, autores, cota parlamentar, histórico de licenças |
| **Senado Federal** | API de dados abertos: mandatos, votações e autorias |
| **TCU** | lista de responsáveis com contas julgadas irregulares (Ficha Limpa) |
| **CGU / Portal da Transparência** | emendas parlamentares, CEIS, CNEP, servidores federais (SIAPE) |
| **Receita Federal** | quadro de sócios do CNPJ (opcional) |
| **Tesouro Nacional** | SICONFI: gestão fiscal e execução orçamentária de estados e municípios |
| **IBGE** | PIB, empregos formais (CEMPRE) e desemprego (PNAD Contínua) |
| **INEP** | IDEB por município, estado e Brasil |
| **Banco Central** | crescimento do PIB, inflação e dívida pública |

## Princípios

- **Sem veredito.** Trajetória, preparo e alertas são contexto, não nota. O ranking só existe se você escolher os critérios.
- **"Sem dados" não é zero.** Quem não tem mandato não fica com nota ruim por isso.
- **Não consultado ≠ nada consta.** Se uma fonte faltou na coleta, o site diz isso.
- **Identidade com cuidado.** Ligações só por CPF, ou por nome completo + dígitos visíveis do CPF quando a fonte o mascara;
  ligações feitas só pelo nome aparecem com aviso para conferir. **O CPF não é gravado** nos arquivos processados.
- **Dados sensíveis fora.** Gênero e cor/raça só aparecem no Panorama agregado; filiação sindical não é tratada (LGPD);
  processos judiciais ficam de fora por não haver base aberta confiável.
- **Demonstração claramente fictícia.** Pessoas "Exemplo", partidos "PX", estados "XA/XB/XC".

## Como é feito

O núcleo é **Java puro, sem nenhuma biblioteca em tempo de execução** (JUnit só nos testes). O servidor é o
`com.sun.net.httpserver` do próprio JDK, e a interface é HTML, CSS e JavaScript sem framework.

```
arquivos públicos ──► coleta ──► CSV em cache (dados/processados) ──► indicadores e preparo ──► API JSON ──► site
```

```
src/main/java/br/unit/eleicao/
├── App.java          entrada: sobe o site ou roda a coleta
├── modelo/           Pessoa (abstrata) → Candidato, Deputado; Cargo, Elegibilidade, Financiamento, IndicadorFiscal…
├── preparo/          CriterioPreparo (abstrata) → 8 critérios; QuadroPreparo aplica todos
├── indicador/        Indicador (abstrata) → presença, verba, projetos, patrimônio, alinhamento
├── ranking/          MetodoRanking (abstrata) → SomaPonderada, Topsis
├── coleta/           um leitor por fonte (TSE, Câmara, Senado, TCU, emendas, CNPJ, CEIS/CNEP, SIAPE)
│   └── mandato/      FonteMandato (abstrata) → IBGE (PIB, emprego, desemprego), SICONFI, INEP, Banco Central, Câmara
├── persistencia/     RepositorioArquivos: cache em CSV
├── excecao/          DadosException → ArquivoInvalidoException, ColetaException
├── util/             LeitorCsv, EscritorCsv, Texto, Estatistica, Xml
└── web/              ServidorWeb, ServicoApi, FichaComplementar, Json
src/main/resources/web/   index.html, estilo.css, app.js, metodologia.html
```

<details>
<summary><b>Projeto acadêmico: onde está cada conteúdo da disciplina</b></summary>

Feito para a disciplina **Projeto de Programação** (Java). O núcleo usa só o conteúdo do curso; a coleta e a
interface usam recursos padrão do JDK e da web.

| Conteúdo | Onde |
|---|---|
| Classes, encapsulamento, construtores, pacotes | `modelo/` (validação em `PosicaoUsuario` e `ConfiguracaoRanking`) |
| Herança | `Pessoa → Candidato/Deputado`, `Indicador → 5 indicadores`, `CriterioPreparo → 8 critérios`, `FonteMandato → 7 fontes`, `MetodoRanking → SomaPonderada/Topsis`, exceções |
| Polimorfismo / override | `avaliar` em cada critério de preparo; `calcular`, `formatar`, `resumir` em cada indicador; `equals`/`hashCode`/`toString` |
| Overload | `formatar(...)`, `resumir(...)`, `ArquivosBrutos.abrir(...)`, construtores |
| Enum | `GrauInstrucao`, `Elegibilidade`, `Cargo`, `Avaliacao`, `TemaMandato`, `FormaResumo`, `Sentido` |
| Exceções | exceções próprias, `try-with-resources`, `throw`, multi-catch |
| Arquivos | `LeitorCsv`/`EscritorCsv`, `Properties`, leitura de `.zip` em fluxo, JSON e XML |
| Coleções | `ArrayList`, `HashMap`, `TreeMap`, `LinkedHashMap`, `HashSet`, `Collections.unmodifiable…` |
| Strings | normalização de nomes, `split`, `format`, `StringBuilder`, regex |

</details>

## Testes

```bash
mvn test
```

São 70 testes. Eles cobrem estatística, leitura de CSV, cada indicador, ranking (soma ponderada, TOPSIS,
cobertura mínima), elegibilidade, cruzamento de identidades, o quadro de preparo e a coleta completa sobre
arquivos pequenos no **layout real** de cada fonte (TSE, Câmara, Senado, TCU, receitas, emendas, CEIS/CNEP,
SIAPE, CNPJ, SICONFI, IBGE, INEP, Banco Central, projetos do Executivo, PEP, CEAF, contratos, planos de governo,
verba do Senado, gastos e votos) e o "Em resumo". Também testam o servidor web respondendo como o navegador.

## Limitações conhecidas

- A coleta completa ainda **não foi executada contra os servidores oficiais**: os formatos foram conferidos em
  documentação e arquivos publicados. Se uma coluna mudar de nome, a coleta para e diz qual coluna e qual arquivo.
- Mandatos em **assembleias e câmaras municipais** não têm base nacional padronizada: o site indica onde consultar.
- Empresas, sanções e serviço público dependem de o TSE publicar o CPF completo; quando vem mascarado, essas
  partes aparecem como "não consultado".
- O "antes e depois" não inclui saúde nem os mínimos constitucionais de saúde e educação: o Tesouro não publica
  esses anexos pela API. O PIB dos municípios sai com cerca de 2 anos de atraso.

## Uso e licença

Este repositório é **público apenas para visualização**: o código pode ser lido e consultado aqui, mas não pode
ser copiado, modificado nem reutilizado sem autorização do autor. **Não são aceitas contribuições** (pull requests).
Detalhes em [LICENSE](LICENSE).

Os dados vêm de fontes públicas oficiais e continuam sujeitos às regras de uso de cada órgão
(veja [docs/FONTES.md](docs/FONTES.md)).
