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
