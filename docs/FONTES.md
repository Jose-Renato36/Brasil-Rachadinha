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

## Versão nacional

Com `coletar BR`, o sistema lê de cada zip do TSE o arquivo do país inteiro (`_BRASIL.csv`) e o de
abrangência nacional (`_BR.csv`, presidência), sem contar duas vezes o que aparece nos dois (teste
`ModoNacionalTest`). Câmara, Senado e TCU são lidos sem filtro de UF. Cuidados:

- o z-score da verba de gabinete continua **por estado** (cada UF tem um teto de cota diferente);
- ligações só por nome (candidato × deputado) exigem **a mesma UF**, porque homônimos entre estados são comuns;
- a trajetória passa a achar candidaturas em **qualquer estado** (quem mudou de domicílio eleitoral);
- rankings e comparações são sempre **por cargo e estado**: você vota em quem está na urna do seu estado.

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

## Preparo para o cargo

O quadro "Preparo para o cargo" (pacote `preparo/`) não cria nota: mostra fatos verificáveis em três grupos.

| Item | Regra | Fonte |
|---|---|---|
| Idade mínima | CF art. 14, § 3º, VI: 35 anos (presidente, vice, senador), 30 (governador, vice), 21 (deputados, prefeito, vice), 18 (vereador), **na data da posse**: 1º/1 (municipais), 5/1 (presidente) e 6/1 (governador) a partir de 2027 (EC 111/2021), 1º/2 (Congresso e Assembleias) | TSE (data de nascimento) |
| Registro | situação do registro (apta, em análise, com recurso, inapta) | TSE |
| Formação | escolaridade declarada; a lei só exige saber ler e escrever. O curso não é publicado | TSE |
| Experiência em cargos eletivos | anos de mandato desde 2018, separando Executivo e Legislativo | TSE (trajetória) |
| Experiência na mesma função | já exerceu o mesmo cargo, ou outro do mesmo poder | TSE (trajetória) |
| Alertas | contas irregulares (TCU), cassações (TSE), sanções (CGU). "Não consultado" é diferente de "nada consta" | TCU, TSE, CGU |

## Patrimônio ao longo do tempo e cassações (TSE)

- `bem_candidato_{ano}.zip` de **todas** as eleições da trajetória (2018, 2020, 2022, 2024), ligados pelo
  `SQ_CANDIDATO` da candidatura antiga (descoberto ao montar a trajetória). Mesmo layout do arquivo atual.
  Quando o arquivo existe e a pessoa não aparece nele, conta como "declarou não ter bens" (R$ 0).
- `motivo_cassacao/motivo_cassacao_{ano}.zip` (entrada `motivo_cassacao_{ano}_{UF}.csv`): colunas
  `SQ_CANDIDATO` e `DS_MOTIVO_CASSACAO` (conferidas em projetos abertos que leem o arquivo).

## Financiamento da campanha (TSE)

- `prestacao_contas/prestacao_de_contas_eleitorais_candidatos_{ano}.zip`, entrada `receitas_candidatos_{ano}_{UF}.csv`
  (o zip também traz `receitas_candidatos_doador_originario_...`, que **não** é lido: por isso o leitor exige
  o prefixo além do sufixo da UF). 48 colunas, `;`, ISO-8859-1; usadas `SQ_CANDIDATO`, `DS_FONTE_RECEITA`
  (Fundo Especial / Fundo Partidário / Outros Recursos), `DS_ORIGEM_RECEITA`, `SQ_RECEITA`, `VR_RECEITA`.
- Agrupamos em: fundo eleitoral, fundo partidário (os dois = dinheiro público), doações de pessoas, recursos
  próprios, vaquinha on-line, repasses de partidos/candidatos, outras.
- Durante a campanha o arquivo é parcial. Se o TSE ainda não tiver publicado o de 2026, a ficha diz
  "não consultado".

## Emendas parlamentares (Portal da Transparência / CGU)

- `https://dadosabertos-download.cgu.gov.br/PortalDaTransparencia/saida/emendas-parlamentares/EmendasParlamentares.zip`
  (sem chave de acesso), entrada `EmendasParlamentares.csv`, `;`, Windows-1252, desde 2014.
- Colunas usadas: `Ano da Emenda`, `Tipo de Emenda`, `Nome do Autor da Emenda`, `Localidade de aplicação do recurso`,
  `Nome Função`, `Valor Empenhado`, `Valor Pago`.
- Só **emendas individuais** (em "de relator" o autor é o relator-geral; bancada e comissão não são pessoas).
  "Transferências Especiais" = as chamadas emendas Pix.
- O arquivo não tem CPF: a ligação é pelo nome parlamentar, **apenas** para quem foi deputado(a) federal ou
  senador(a) e só quando o nome aponta para uma única candidatura. A ficha pede para conferir.

## Empresas (Receita Federal) e sanções (CGU)

- **CNPJ**: `arquivos.receitafederal.gov.br/dados/cnpj/dados_abertos_cnpj/AAAA-MM/` com `Socios0..9.zip`,
  `Empresas0..9.zip`, `Qualificacoes.zip`. Sem cabeçalho, `;`, ISO-8859-1. Sócios: `cnpj_basico;
  identificador (2 = pessoa física); nome; cpf mascarado ***123456**; qualificação; data de entrada (AAAAMMDD); ...`.
  São vários GB: só baixados com `coletar BR --empresas` (ou coloque os zips em `dados/brutos/cnpj/`).
- **CEIS/CNEP**: `portaldatransparencia.gov.br/download-de-dados/ceis/AAAAMMDD` (o portal guarda poucos dias; o
  coletor tenta os últimos 10), zip com `AAAAMMDD_CEIS.csv`, `;`, Windows-1252. Colunas: `TIPO DE PESSOA`,
  `CPF OU CNPJ DO SANCIONADO` (CPF mascarado), `NOME DO SANCIONADO`, `CATEGORIA DA SANÇÃO`, `DATA INÍCIO SANÇÃO`,
  `DATA FINAL SANÇÃO`, `ÓRGÃO SANCIONADOR`.
- **Ligação**: pessoa física por nome completo **e** os 6 dígitos visíveis do CPF iguais aos do CPF informado ao
  TSE; empresa pelos 8 primeiros dígitos do CNPJ das empresas de que a pessoa é sócia.
- Não tratamos filiação sindical: é dado pessoal sensível (LGPD, art. 5º, II) e não há base pública por pessoa.

## Servidores federais (SIAPE)

- `portaldatransparencia.gov.br/download-de-dados/servidores/AAAAMM_Servidores_SIAPE` (zip com `AAAAMM_Cadastro.csv`),
  `;`, Windows-1252. Colunas: `NOME`, `CPF` (mascarado), `DESCRICAO_CARGO`, `ORG_LOTACAO`, `ORG_EXERCICIO`,
  `SITUACAO_VINCULO`, `DATA_INGRESSO_SERVICOPUBLICO`. Mesma regra de ligação (nome + CPF parcial).
- Cobre só servidores civis do **Executivo federal**; servidores estaduais e municipais não aparecem.

## Antes e depois de quem já governou (prefeito, governador, presidente)

Para cada mandato no Executivo encontrado na trajetória (pacote `coleta/mandato/`), o sistema consulta as fontes
abaixo do ano anterior à eleição até o fim do mandato (ou o último ano encerrado). O ano da eleição é o "antes":
quem governava era o antecessor. Cada indicador aparece **ao lado de uma referência no mesmo período**: o estado
(para prefeituras) ou o Brasil (para estados). Assim se vê se o lugar foi melhor ou pior que a média, sem atribuir
causa ao governante. As respostas ficam em `dados/brutos/mandatos/FONTE/`.

| Indicador | Prefeito | Governador | Presidente | Fonte (conferida) |
|---|---|---|---|---|
| PIB e PIB por pessoa | ✓ (ref. estado) | ✓ (ref. Brasil) | | IBGE, API de agregados v3, tabela **5938**, variáveis **37** (R$ mil) e **543** (R$) |
| Empregos formais | ✓ (ref. estado) | ✓ (ref. Brasil) | | IBGE/CEMPRE, tabelas **1685** (até 2021) e **9509** (desde 2022), variável **708** |
| Desemprego | | ✓ (ref. Brasil) | ✓ | IBGE/PNAD Contínua, tabela **4099**, variável **4099**, média dos 4 trimestres |
| IDEB | rede municipal, anos iniciais (ref. rede municipal do estado) | rede estadual, anos finais e médio (ref. redes estaduais do Brasil) | escolas públicas, 3 etapas | INEP, pacotes `divulgacao_*_{ano}.zip` (xlsx, cabeçalho na 10ª linha, colunas `VL_OBSERVADO_AAAA`) |
| Gasto com pessoal e limite da LRF | ✓ | ✓ | | SICONFI, RGF Anexo 1 (`DespesaComPessoalTotal`, `LimiteMaximoDespesaComPessoalTotal`) |
| Investimento (% da despesa) | ✓ | ✓ | | SICONFI, RREO Anexo 1, 6º bimestre (`Investimentos` / `TotalDespesas`, liquidadas) |
| Sobrou ou faltou no ano | ✓ | ✓ | | SICONFI, RREO Anexo 1 (`TotalReceitas` realizada − `TotalDespesas` liquidada) |
| Crescimento real do PIB | | | ✓ | Banco Central, SGS **7326** |
| Inflação (IPCA no ano) | | | ✓ | Banco Central, SGS **13522** (12 meses, dezembro) |
| Dívida pública bruta (% do PIB) | | | ✓ | Banco Central, SGS **13762** (dezembro) |
| Medidas provisórias e projetos do governo (enviados e aprovados) | | | ✓ | Câmara, `proposicoesAutores` (`codTipoAutor` 30000, "Poder Executivo") + `proposicoes` |

Detalhes e cuidados:

- **Resposta do IBGE** (v3): `[{"id":"37","variavel":...,"resultados":[{"series":[{"localidade":...,"serie":{"2020":"123"}}]}]}]`;
  `-`, `...` e `X` (sigilo) são tratados como "sem dado". PIB e empregos são comparados pela **variação** no mandato; os
  gráficos mostram um índice (ano da eleição = 100), porque cidade e estado têm escalas muito diferentes.
- **Valores correntes**: o PIB do IBGE não desconta a inflação; como a referência passa pela mesma inflação, a
  comparação continua justa. O PIB dos municípios sai com cerca de 2 anos de atraso.
- **CEMPRE**: o IBGE mudou a metodologia em 2022 (tabela 9509). A referência muda junto.
- **SICONFI**: 1 pedido por segundo; municípios pequenos podem publicar o RGF por semestre (tentamos `S/2` se `Q/3` vier vazio).
  Os anexos de **saúde (RREO 12) e educação (RREO 8)**, com os mínimos de 15% e 25%, **não são publicados pela API**
  (dois projetos abertos que testaram ao vivo confirmam); ficaram de fora.
- **IDEB** é bienal (anos ímpares); a edição anterior ao mandato conta como "antes". As edições de 2021 e 2023 foram
  afetadas pela pandemia. A planilha é lida por um leitor de `.xlsx` feito só com o JDK (`util/LeitorXlsx`, StAX).
- **Inflação e juros** dependem do Banco Central (autônomo desde 2021) e do cenário mundial; aparecem só no mandato
  presidencial, como contexto.
- **Prefeitos e governadores não têm base nacional de projetos de lei**: as câmaras municipais e assembleias publicam cada
  uma de um jeito.

## Cargos de gestão, expulsões, contratos, plano de governo, verba do Senado, gastos e votos

| Dado | Arquivo / endereço | Formato e ligação |
|---|---|---|
| Cargos públicos de destaque | Portal da Transparência, `download-de-dados/pep/AAAAMM` (zip com `AAAAMM_PEP.csv`) | `;`, ISO-8859-1; `CPF` (mascarado), `Nome_PEP`, `Descrição_Função`, `Nome_Órgão`, `Data_Início_Exercício`, `Data_Fim_Exercício`. Cobre quem está na função ou saiu há menos de 5 anos. Ligação por nome + CPF parcial |
| Expulsões do serviço público federal | Portal da Transparência, `download-de-dados/ceaf/AAAAMMDD` | mesmo layout do CEIS/CNEP (`CPF OU CNPJ DO SANCIONADO`, `CATEGORIA DA SANÇÃO`...), só pessoa física |
| Contratos federais das empresas do candidato | Portal da Transparência, `download-de-dados/compras/AAAAMM` (zip com `AAAAMM_Compras.csv`) | `Código Contratado` (CNPJ), `Nome Contratado`, `Objeto`, `Valor Inicial Compra`, `Nome Órgão`, `Data Assinatura Contrato`; cruzado com os 8 primeiros dígitos do CNPJ das empresas de que a pessoa é sócia. Baixado com `--empresas` |
| Plano de governo | TSE, `proposta_governo/proposta_governo_ANO_UF.zip` (`BR` = Presidência) | PDFs nomeados com o número da candidatura (`2026SE260001234567_01.pdf`); copiados para `dados/processados/UF/planos/` e servidos em `/planos/` |
| Verba de gabinete dos senadores | Senado, `senado.leg.br/transparencia/LAI/verba/despesa_ceaps_ANO.csv` | Windows-1252, `;`; a 1ª linha é a data de atualização. `SENADOR` (nome parlamentar), `ANO`, `MES`, `VALOR_REEMBOLSADO`; vira gasto médio por mês |
| Gastos da campanha | TSE, mesmo zip da prestação de contas: `despesas_contratadas_candidatos_ANO_UF.csv` | `SQ_CANDIDATO`, `DS_ORIGEM_DESPESA`, `VR_DESPESA_CONTRATADA`; agrupados em 8 tipos simples (anúncios na internet, propaganda, vídeo/rádio/TV, pessoal, viagens, serviços, eventos, repasses) |
| Votos em eleições anteriores | TSE, `votacao_candidato_munzona/votacao_candidato_munzona_ANO.zip` | soma de `QT_VOTOS_NOMINAIS` do 1º turno por `SQ_CANDIDATO` da candidatura antiga |

Contratos de **estados e prefeituras** ficaram de fora: o Portal Nacional de Contratações Públicas só tem busca
por fornecedor numa API não documentada.

## "Em resumo"

`preparo/ResumoCidadao` monta até ~9 frases curtas a partir de tudo o que foi coletado: situação do registro,
experiência (mandatos e cargos de gestão), escolaridade, como estava o lugar que governou, alertas, patrimônio,
dinheiro da campanha, empresas e contratos, e se registrou plano de governo. Cada frase vem com um sinal (ok,
informação, atenção) com ícone e texto, nunca só cor. Não é nota nem recomendação de voto.

## Fontes avaliadas e não usadas

| Fonte | Motivo |
|---|---|
| Assembleias legislativas e câmaras municipais | sem padrão nacional |
| Filiação sindical | dado sensível (LGPD) e sem base pública individual |
| Processos judiciais | sem base aberta estruturada confiável; risco de erro e dano à reputação |
| DATASUS (mortalidade infantil, vacinação) | sistemas TabNet sem API estável; próximo passo |
| SIOPS / SIOPE (mínimos de saúde e educação) | fora da API do SICONFI; exigem outros sistemas; próximo passo |
