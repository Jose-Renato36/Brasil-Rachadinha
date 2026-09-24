# Fontes de dados: como funcionam de verdade

Este documento registra o que foi **conferido** sobre cada arquivo antes de escrever os leitores,
e de onde veio cada confirmação. Tudo é lido pelo **nome da coluna** (não pela posição), então uma
coluna nova no arquivo não quebra nada; uma coluna renomeada gera um erro dizendo qual falta.

## TSE – Portal de Dados Abertos

Página do conjunto: <https://dadosabertos.tse.jus.br/dataset/candidatos-2026>
(e `candidatos-2022` para a eleição anterior).

| Arquivo | URL | Conteúdo usado |
|---|---|---|
| Candidaturas | `https://cdn.tse.jus.br/estatistica/sead/odsele/consulta_cand/consulta_cand_{ano}.zip` | cadastro, CPF, escolaridade, cor/raça, situação |
| Informações complementares | `.../consulta_cand_complementar/consulta_cand_complementar_{ano}.zip` | `DS_DETALHE_SITUACAO_CAND`, `ST_REELEICAO` (opcional) |
| Bens declarados | `.../bem_candidato/bem_candidato_{ano}.zip` | `VR_BEM_CANDIDATO` somado por `SQ_CANDIDATO` |

Formato conferido em uma cópia pública dos arquivos de 2026 (extração de 17/08/2026, repositório
[leofn/tse-candidatos-2026](https://github.com/leofn/tse-candidatos-2026)):

- Cada `.zip` traz **um CSV por UF** (`consulta_cand_2026_SE.csv`) e um `_BRASIL.csv`.
- Separador `;`, todos os campos entre aspas, codificação **ISO-8859-1 (latin-1)**.
- 50 colunas em `consulta_cand`, **idênticas em 2014, 2018, 2022 e 2026** (conferido pelo projeto
  [elas-no-poder-observatory](https://github.com/natalyxxnunes-stack/elas-no-poder-observatory)).
- `NR_CPF_CANDIDATO` **vem preenchido** (11 dígitos) → dá para cruzar com a Câmara pelo CPF.
- "Sem informação" aparece como `#NULO`, `#NE`, `-1`, `-3` (sem `#` no final, ao contrário do que se vê
  em anos antigos).
- Em agosto de 2026 `DS_SITUACAO_CANDIDATURA` ainda vinha `#NE` para todos (julgamentos em andamento).
  O detalhe (`DEFERIDO`, `INDEFERIDO COM RECURSO`...) fica no arquivo **complementar**.
- `VR_BEM_CANDIDATO` usa vírgula decimal (`"33062,21"`).
- Em SE, 402 candidaturas, **139 a deputado federal**.

**Atenção:** o CDN do TSE devolveu **HTTP 403** para robôs fora do Brasil (ex.: GitHub Actions). De
um computador no Brasil costuma funcionar. Se não funcionar, baixe os `.zip` pelo navegador na página
do conjunto e coloque em `dados/brutos/` sem renomear.

## Câmara dos Deputados – Dados Abertos

Arquivos em lote: `https://dadosabertos.camara.leg.br/arquivos/{nome}/csv/{nome}-{ano}.csv`
(documentação: <https://dadosabertos.camara.leg.br/swagger/api.html?tab=staticfile>).

Colunas conferidas no código de um projeto que processa esses mesmos arquivos só com a biblioteca
padrão do Python ([augusto-dmh/mandato-aberto](https://github.com/augusto-dmh/mandato-aberto)):

| Arquivo | Colunas usadas |
|---|---|
| `votacoes-{ano}.csv` | `id`, `data`, `siglaOrgao` (`PLEN` = Plenário), `descricao`, `aprovacao` |
| `votacoesVotos-{ano}.csv` | `idVotacao`, `voto`, `deputado_id`, `deputado_nome`, `deputado_siglaPartido`, `deputado_siglaUf`, `deputado_idLegislatura`, `deputado_urlFoto`, `dataHoraVoto` |
| `votacoesProposicoes-{ano}.csv` | `idVotacao`, `proposicao_titulo`, `proposicao_ementa` (dá nome legível à votação) |
| `proposicoesAutores-{ano}.csv` | `idProposicao`, `idDeputadoAutor`, `proponente`, `ordemAssinatura` |
| `proposicoes-{ano}.csv` | `id`, `siglaTipo`, `numero`, `ano`, `ementa`, `dataApresentacao`, `ultimoStatus_descricaoSituacao` |
| `deputados.csv` | `uri`, `nome`, `nomeCivil`, `cpf`, `siglaSexo`, `dataNascimento` |

- UTF-8 **com BOM**, separador `;`.
- Filtros: votações a partir do início da legislatura (01/02/2023) e `deputado_idLegislatura = 57`.
- Presença: a API `GET /api/v2/deputados/{id}/historico` informa licenças e reassunções
  (`dataHora`, `situacao`, `descricaoStatus`). O sistema consulta essa API em XML (lido com o parser
  XML que já vem no Java) para contar só as votações em que o deputado estava **em exercício**.
  Se a API falhar, usa a aproximação "entre o primeiro e o último voto".

### Cota parlamentar (CEAP)

`https://www.camara.leg.br/cotas/Ano-{ano}.csv.zip` – um CSV por ano dentro do zip.
Conferido no esquema do projeto [robertfxbr/cota-parlamentar](https://github.com/robertfxbr/cota-parlamentar),
que valida as 32 colunas contra o arquivo de 2025:

- UTF-8 com BOM, `;`, valores com **ponto** decimal (`vlrLiquido`).
- Soma-se `vlrLiquido` (o que foi pago), não `vlrDocumento` (inclui glosas).
- Linhas de **lideranças partidárias** também usam a cota e vêm **sem `ideCadastro`**: são descartadas.
- A API `/deputados/{id}/despesas` devolve lista vazia; por isso o arquivo anual é a fonte.
- O arquivo traz o CPF do parlamentar e nomes de passageiros: nada disso é guardado.

## Trajetória política (TSE, eleições anteriores)

Para mostrar se a pessoa já foi candidata ou eleita (vereador, prefeito, deputado estadual…), o sistema
lê `consulta_cand_{ano}.zip` das **quatro eleições anteriores** (para 2026: 2018, 2020, 2022 e 2024,
gerais e municipais), no mesmo layout de 50 colunas.

- Resultado: `DS_SIT_TOT_TURNO` (`ELEITO`, `ELEITO POR QP`, `ELEITO POR MÉDIA`, `SUPLENTE`, `NÃO ELEITO`,
  `2º TURNO`). Quando há 2º turno, vale a linha de `NR_TURNO = 2`.
- **Não existe chave de pessoa estável entre anos** (`SQ_CANDIDATO` muda a cada eleição). A ligação usa o
  CPF quando os dois anos o publicam; em **2024 o TSE mascarou o CPF (`-4`)**, então usa nome civil +
  data de nascimento, aceitando só correspondência única (homônimos com outra data não entram).
- Limite: só o mesmo estado; quem concorreu em outra UF não aparece.
- É informação de contexto (quem tem mandato hoje, quem já foi eleito), **nunca entra na nota**.

## Todos os cargos da eleição

`consulta_cand` traz, por UF, **todos os cargos** (governador e vice, senador e suplentes, deputados federais
e estaduais/distritais). O sistema lê todos; presidente fica de fora porque não está nos arquivos por UF.

## O que a pessoa fez nos mandatos

| Mandato anterior | Fonte | O que mostramos |
|---|---|---|
| Deputado(a) federal atual (2023–hoje) | Câmara, arquivos em lote | presença, projetos, aprovados, verba de gabinete |
| Deputado(a) federal anterior (2019–2023) | Câmara, **mesmos arquivos de 2019 a 2022** (`deputado_idLegislatura = 56`) | o mesmo resumo; ligação só por CPF ou nome + nascimento |
| Senador(a) em exercício | API do Senado (`legis.senado.leg.br/dadosabertos/senador/...`, XML) | votos nominais e matérias de autoria |
| Prefeito(a), governador(a), vereador(a), deputado(a) estadual | — | período e local do mandato (TSE) e aviso de que não há base nacional de atuação |
| Qualquer gestor | TCU – contas julgadas irregulares | processos com decisão definitiva, ligados pelo CPF |

### API do Senado

Endereços e nomes de campos conferidos no código da biblioteca
[DadosAbertosBrasil](https://pypi.org/project/DadosAbertosBrasil/), que usa a mesma API:

- `senador/lista/atual?uf=XX` → `Parlamentar` › `IdentificacaoParlamentar` (`CodigoParlamentar`,
  `NomeCompletoParlamentar`, `UrlPaginaParlamentar`…)
- `senador/{codigo}` → `DadosBasicosParlamentar/DataNascimento` (usada para confirmar a identidade)
- `senador/{codigo}/votacoes` → `Votacao` (`SiglaDescricaoVoto`, `SessaoPlenaria/DataSessao`)
- `senador/{codigo}/autorias` → `Autoria` (`IndicadorAutorPrincipal`, `Materia/DescricaoIdentificacao`, `Ementa`, `Data`)

A ligação exige **nome completo + data de nascimento** iguais aos do TSE. "Votou" = Sim, Não ou
Abstenção; os demais registros (licenças, missões, ausências) aparecem só no total. A API de autorias não
informa se a matéria foi aprovada, por isso o sistema não mostra "aprovados" para o Senado.

### TCU – contas julgadas irregulares

`https://sites.tcu.gov.br/dados-abertos/inidoneos-irregulares/arquivos/resp-contas-julgadas-irreg-implicacao-eleitoral.csv`

Lista que o TCU envia ao TSE para a Lei da Ficha Limpa (contas julgadas irregulares nos últimos 8 anos,
com decisão definitiva). Traz nome, CPF, UF, município, processo, deliberação e data do trânsito em julgado.
O cabeçalho exato não está documentado publicamente, então as colunas são achadas por trechos do nome
e o separador e a codificação são detectados.

- Liga **só pelo CPF**: completo, ou mascarado com pelo menos 6 dígitos iguais **e** o mesmo nome completo.
  Nome sozinho nunca basta: é uma informação séria e homônimos são comuns.
- "Não consta" só aparece quando a lista foi de fato lida.
- Estar na lista não significa inelegibilidade automática: quem decide é a Justiça Eleitoral.

### Por que não há "avaliação" de prefeitos e governadores

Não existe base nacional padronizada que diga como foi uma gestão municipal ou estadual. Há dados
fiscais (SICONFI/Tesouro) e indicadores setoriais (IDEB, saúde), mas transformá-los em "desempenho do
gestor" exige escolhas de método fortes e ligações por código de município que não temos no TSE. Por
isso o sistema mostra onde e quando a pessoa governou, se há contas irregulares no TCU, e indica os
Tribunais de Contas e portais de transparência como próximo passo.

## Fontes avaliadas para próximos passos

| Fonte | O que traria | Situação |
|---|---|---|
| TSE – prestação de contas eleitorais (`prestacao_de_contas_eleitorais_candidatos_{ano}`) | quem financiou a campanha (fundo eleitoral, partido, pessoas físicas, recursos próprios) | aberta e padronizada; boa próxima etapa |
| TSE – votação por candidato (`votacao_candidato_munzona_{ano}`) | quantos votos recebeu nas eleições anteriores | aberta; arquivos grandes |
| Câmara – legislaturas anteriores (mesmos arquivos em lote, 2019-2022) | presença e projetos de ex-deputados federais que voltam a concorrer | mesma estrutura já lida; exige separar os indicadores por legislatura |
| Câmara – API `/deputados/{id}/orgaos`, `/frentes` | comissões e frentes parlamentares de que participa | aberta; informativo |
| Senado – API de dados abertos (`legis.senado.leg.br/dadosabertos`) | mandatos e votações de ex-senadores | aberta; caso raro para deputado federal |
| Assembleias legislativas e câmaras municipais | atuação de deputados estaduais e vereadores | **sem padrão nacional**: cada casa publica (ou não) de um jeito; algumas têm API própria |
| Portal da Transparência (CGU) – emendas parlamentares | para onde o deputado mandou emendas | exige cadastro de chave de acesso |
| Processos judiciais | — | não há base aberta e estruturada confiável; risco de erro e de dano à reputação: fora do escopo |
