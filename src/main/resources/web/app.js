/* Voto com Dados - interface. Conversa com o servidor Java pela API em /api/... */
'use strict';

const CORES = ['#2a78d6', '#eb6834', '#1baf7a'];
const PESOS = [{ valor: 0, texto: 'Não importa' }, { valor: 5, texto: 'Importa' }, { valor: 10, texto: 'Importa muito' }];
const EXPERIENCIAS = [
  { codigo: 'MANDATO_ATUAL', texto: 'Tem mandato hoje', explica: 'Exerce um cargo eletivo agora' },
  { codigo: 'JA_ELEITO', texto: 'Já foi eleito(a)', explica: 'Já exerceu mandato, mas não agora' },
  { codigo: 'JA_CONCORREU', texto: 'Já concorreu', explica: 'Disputou eleições, sem ser eleito(a)' },
  { codigo: 'NOVO', texto: 'Estreante', explica: 'Sem candidaturas nas eleições anteriores analisadas' },
];

const app = {
  estado: null,
  config: carregarConfig(),
  comparar: lerLocal('comparar', []),
  candidatos: {},
};

// ------------------------------------------------------------ utilidades

function lerLocal(chave, padrao) {
  try {
    const v = localStorage.getItem('vcd.' + chave);
    return v ? JSON.parse(v) : padrao;
  } catch (e) {
    return padrao;
  }
}

function gravarLocal(chave, valor) {
  try { localStorage.setItem('vcd.' + chave, JSON.stringify(valor)); } catch (e) { /* modo privado */ }
}

function carregarConfig() {
  return Object.assign({ pesos: {}, invertidos: {}, inaptos: false, cobertura: 2, metodo: 'soma', avancado: false, cargo: null },
    lerLocal('config', {}));
}

function salvarConfig() { gravarLocal('config', app.config); }

async function api(rota, params = {}, post = false) {
  const corpo = new URLSearchParams(params);
  const resp = post
    ? await fetch('/api/' + rota, { method: 'POST', body: corpo })
    : await fetch('/api/' + rota + (corpo.toString() ? '?' + corpo : ''));
  const dados = await resp.json();
  if (!resp.ok) throw new Error(dados.erro || ('Erro ' + resp.status));
  return dados;
}

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const $ = sel => document.querySelector(sel);
const main = () => $('#conteudo');
const semAcento = s => String(s ?? '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

function iniciais(nome) {
  return esc(String(nome).split(/\s+/).filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase());
}

function avatar(c, tamanho) {
  const cls = 'avatar' + (tamanho ? ' ' + tamanho : '');
  if (c.foto) return `<img class="${cls}" src="${esc(c.foto)}" alt="" loading="lazy" onerror="this.outerHTML='<div class=&quot;${cls}&quot;>${iniciais(c.nome)}</div>'">`;
  return `<div class="${cls}" aria-hidden="true">${iniciais(c.nome)}</div>`;
}

function seloExperiencia(c) {
  return `<span class="selo exp-${esc(c.experiencia)}">${esc(c.experienciaTexto)}</span>`;
}

function etiquetas(c) {
  const e = [];
  if (c.elegibilidade === 'INAPTA') e.push(`<span class="etiqueta perigo" title="${esc(c.elegibilidadeExplicacao)}">Candidatura inapta</span>`);
  if (c.elegibilidade === 'SUB_JUDICE') e.push(`<span class="etiqueta alerta" title="${esc(c.elegibilidadeExplicacao)}">Com recurso na Justiça</span>`);
  if (c.elegibilidade === 'EM_ANALISE') e.push(`<span class="etiqueta" title="${esc(c.elegibilidadeExplicacao)}">Registro em análise</span>`);
  if (c.contasIrregulares > 0) e.push('<span class="etiqueta perigo" title="Consta na lista do TCU de contas julgadas irregulares">Contas irregulares (TCU)</span>');
  if (c.vinculoDuvidoso) e.push('<span class="etiqueta alerta" title="Ligamos esta candidatura a um mandato só pelo nome. Confira no perfil.">Confira a identidade</span>');
  return e.join(' ');
}

function moeda(v) {
  return v == null ? 'sem dados' : v.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 });
}

function pct(v) {
  return v == null ? '–' : v.toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + '%';
}

function erroHtml(e) {
  return `<div class="caixa-erro"><strong>Algo deu errado.</strong> ${esc(e.message || e)}</div>`;
}

function pesoDe(codigo) {
  const p = app.config.pesos[codigo];
  if (p != null) return p;
  return codigo === 'alinhamento' ? (app.estado.posicoes > 0 ? 5 : 0) : 5;
}

function cargosPrincipais() {
  return (app.estado.cargos || []).filter(c => c.principal);
}

function cargoPadrao() {
  const lista = cargosPrincipais();
  const salvo = lista.find(c => c.codigo === app.config.cargo);
  return (salvo || lista.find(c => c.codigo === 'DEPUTADO_FEDERAL') || lista[0] || {}).codigo;
}

function rotuloCargo(codigo, plural) {
  const c = (app.estado.cargos || []).find(x => x.codigo === codigo);
  return c ? c.rotulo : 'Todos os cargos';
}

async function listaCandidatos(cargo = 'TODOS') {
  if (!app.candidatos[cargo]) app.candidatos[cargo] = await api('candidatos', cargo === 'TODOS' ? {} : { cargo });
  return app.candidatos[cargo];
}

// ------------------------------------------------------------ roteamento

const rotas = {
  inicio: telaInicio,
  candidatos: telaCandidatos,
  ranking: telaRanking,
  opinioes: telaOpinioes,
  comparar: telaComparar,
  panorama: telaPanorama,
  candidato: telaCandidato,
  'como-funciona': telaComoFunciona,
  dados: telaDados,
};

async function navegar() {
  const partes = location.hash.replace(/^#\/?/, '').split('/');
  const nome = rotas[partes[0]] ? partes[0] : 'inicio';
  document.querySelectorAll('.menu a').forEach(a => a.classList.toggle('ativo', a.getAttribute('href') === '#/' + nome));
  $('#menu').classList.remove('aberto');
  main().innerHTML = '<p class="carregando">Carregando…</p>';
  try {
    if (!app.estado) await atualizarEstado();
    if (!app.estado.carregada && nome !== 'dados' && nome !== 'como-funciona') {
      location.hash = '#/dados';
      return;
    }
    await rotas[nome](partes.slice(1).map(decodeURIComponent));
  } catch (e) {
    main().innerHTML = erroHtml(e);
  }
  window.scrollTo(0, 0);
}

async function atualizarEstado() {
  app.estado = await api('estado');
  app.candidatos = {};
  $('#aviso-demo').hidden = !app.estado.demo;
  $('#marca-uf').textContent = app.estado.carregada ? app.estado.uf + ' · Eleições ' + app.estado.anoEleicao : '';
}

window.addEventListener('hashchange', navegar);
window.addEventListener('DOMContentLoaded', () => {
  $('#menu-botao').addEventListener('click', () => {
    const aberto = $('#menu').classList.toggle('aberto');
    $('#menu-botao').setAttribute('aria-expanded', aberto);
  });
  navegar();
});

// ------------------------------------------------------------ início

async function telaInicio() {
  const e = app.estado;
  const blocos = cargosPrincipais().map(c => `
    <a class="bloco-cargo" href="#/candidatos/${esc(c.codigo)}">
      <span class="bloco-orgao">${esc(c.orgao)}</span>
      <strong>${esc(c.rotulo)}</strong>
      <span class="bloco-total">${c.total} candidatura${c.total === 1 ? '' : 's'}</span>
    </a>`).join('');
  main().innerHTML = `
    <section class="hero">
      <p class="sobretitulo">Eleições ${e.anoEleicao}${e.demo ? '' : ' · ' + esc(e.uf)}</p>
      <h1>Conheça quem pede o seu voto antes de decidir.</h1>
      <p>Quem já foi político, o que fez, quando e onde. Quem está estreando. Tudo com dados públicos do TSE,
        da Câmara, do Senado e do TCU, e com a fonte de cada número. <strong>Você decide o que é importante.</strong></p>
      <div class="busca">
        <input type="search" id="busca-inicio" placeholder="Procure pelo nome ou pelo número na urna" aria-label="Procurar candidato" autocomplete="off">
        <div class="sugestoes" id="sugestoes" hidden></div>
      </div>
    </section>
    <h2 class="titulo-secao">Escolha o cargo</h2>
    <div class="grade-cargos">${blocos}</div>
    <h2 class="titulo-secao">Como usar</h2>
    <div class="grade grade-3">
      <a class="cartao passo" href="#/candidatos"><span class="numero">1</span><h3>Veja quem são</h3>
        <p class="fraco">Cartões com a trajetória de cada pessoa: estreante, já eleita, com mandato hoje. Clique para ver o que fez.</p></a>
      <a class="cartao passo" href="#/opinioes"><span class="numero">2</span><h3>Diga como votaria</h3>
        <p class="fraco">Responda sobre projetos que já foram votados e descubra quem votou como você.</p></a>
      <a class="cartao passo" href="#/ranking"><span class="numero">3</span><h3>Monte sua lista</h3>
        <p class="fraco">Diga o que importa (presença, economia, projetos…) e compare até 3 pessoas lado a lado.</p></a>
    </div>`;
  ligarBusca($('#busca-inicio'), $('#sugestoes'));
}

function ligarBusca(input, caixa) {
  input.addEventListener('input', async () => {
    const termo = input.value.trim();
    if (termo.length < 2) { caixa.hidden = true; return; }
    const t = semAcento(termo);
    const achados = (await listaCandidatos())
      .filter(c => semAcento(c.nome + ' ' + c.nomeCivil + ' ' + c.partido).includes(t) || String(c.numero).startsWith(termo))
      .slice(0, 8);
    caixa.innerHTML = achados.length
      ? achados.map(c => `<a href="#/candidato/${esc(c.sq)}">${avatar(c, 'pequeno')}<span><strong>${esc(c.nome)}</strong><br><small>${esc(c.cargoTexto)} · Nº ${esc(c.numero)} · ${esc(c.partido)}</small></span></a>`).join('')
      : '<p style="padding:10px 12px" class="fraco">Ninguém encontrado.</p>';
    caixa.hidden = false;
  });
  input.addEventListener('blur', () => setTimeout(() => { caixa.hidden = true; }, 200));
}

// ------------------------------------------------------------ candidatos (cartões)

const filtroCartoes = { experiencia: 'TODOS', busca: '', ordem: 'nome' };

async function telaCandidatos([cargoRota]) {
  const cargos = app.estado.cargos || [];
  const cargo = cargos.some(c => c.codigo === cargoRota) ? cargoRota : cargoPadrao();
  app.config.cargo = cargo;
  salvarConfig();
  const abas = cargos.map(c => `<a class="aba ${c.codigo === cargo ? 'ativa' : ''}" href="#/candidatos/${esc(c.codigo)}"
      aria-current="${c.codigo === cargo}">${esc(c.rotulo)} <span>${c.total}</span></a>`).join('');
  main().innerHTML = `
    <h1>Candidatos a ${esc(rotuloCargo(cargo))}</h1>
    <p class="fraco">Clique em um cartão para ver a trajetória e o que a pessoa já fez.</p>
    <nav class="abas" aria-label="Cargos">${abas}</nav>
    <div class="filtros">
      <div class="chips" role="group" aria-label="Experiência" id="chips-exp"></div>
      <div class="filtros-linha">
        <input type="search" id="busca-cartoes" placeholder="Nome, número ou partido" aria-label="Filtrar candidatos" value="${esc(filtroCartoes.busca)}">
        <select id="ordem" aria-label="Ordenar">
          <option value="nome">Ordem alfabética</option>
          <option value="numero" ${filtroCartoes.ordem === 'numero' ? 'selected' : ''}>Número na urna</option>
          <option value="partido" ${filtroCartoes.ordem === 'partido' ? 'selected' : ''}>Partido</option>
          <option value="experiencia" ${filtroCartoes.ordem === 'experiencia' ? 'selected' : ''}>Mais experiência primeiro</option>
        </select>
      </div>
    </div>
    <div id="cartoes" class="grade-cartoes" aria-live="polite"></div>`;
  const lista = await listaCandidatos(cargo);
  $('#busca-cartoes').addEventListener('input', ev => { filtroCartoes.busca = ev.target.value; desenharCartoes(lista); });
  $('#ordem').addEventListener('change', ev => { filtroCartoes.ordem = ev.target.value; desenharCartoes(lista); });
  desenharCartoes(lista);
}

function desenharCartoes(lista) {
  const contagem = { TODOS: lista.length };
  lista.forEach(c => { contagem[c.experiencia] = (contagem[c.experiencia] || 0) + 1; });
  $('#chips-exp').innerHTML = [{ codigo: 'TODOS', texto: 'Todos' }, ...EXPERIENCIAS].map(x =>
    `<button data-exp="${x.codigo}" aria-pressed="${filtroCartoes.experiencia === x.codigo}" title="${esc(x.explica || '')}"
      class="${x.codigo === 'TODOS' ? '' : 'exp-chip exp-' + x.codigo}">${esc(x.texto)} <span>${contagem[x.codigo] || 0}</span></button>`).join('');
  $('#chips-exp').querySelectorAll('button').forEach(b => b.addEventListener('click', () => {
    filtroCartoes.experiencia = b.dataset.exp;
    desenharCartoes(lista);
  }));
  const t = semAcento(filtroCartoes.busca.trim());
  const ordemExp = { MANDATO_ATUAL: 0, JA_ELEITO: 1, JA_CONCORREU: 2, NOVO: 3 };
  const itens = lista
    .filter(c => filtroCartoes.experiencia === 'TODOS' || c.experiencia === filtroCartoes.experiencia)
    .filter(c => !t || semAcento(c.nome + ' ' + c.nomeCivil + ' ' + c.partido + ' ' + c.numero).includes(t))
    .sort((a, b) => {
      if (filtroCartoes.ordem === 'numero') return String(a.numero).localeCompare(String(b.numero), 'pt-BR', { numeric: true });
      if (filtroCartoes.ordem === 'partido') return a.partido.localeCompare(b.partido) || a.nome.localeCompare(b.nome, 'pt-BR');
      if (filtroCartoes.ordem === 'experiencia') return ordemExp[a.experiencia] - ordemExp[b.experiencia] || a.nome.localeCompare(b.nome, 'pt-BR');
      return a.nome.localeCompare(b.nome, 'pt-BR');
    });
  const alvo = $('#cartoes');
  alvo.innerHTML = itens.length ? itens.map(cartao).join('') : '<p class="fraco">Ninguém com esses filtros.</p>';
  alvo.querySelectorAll('.cartao-candidato > button.cabeca').forEach(b => b.addEventListener('click', () => alternarCartao(b.closest('.cartao-candidato'))));
}

function miniLinha(c) {
  const anos = app.estado.anosTrajetoria || [];
  return `<ol class="mini-linha" aria-label="Eleições anteriores">${anos.map(ano => {
    const t = (c.linhaDoTempo || []).filter(x => x.ano === ano);
    const eleito = t.some(x => x.eleito);
    const cls = !t.length ? 'vazio' : eleito ? 'eleito' : 'concorreu';
    const dica = !t.length ? `${ano}: não concorreu` : t.map(x => `${ano}: ${x.cargoTexto} – ${x.resultado}`).join('; ');
    return `<li class="${cls}" title="${esc(dica)}"><span class="ponto"></span><small>${ano}</small></li>`;
  }).join('')}</ol>`;
}

function cartao(c) {
  return `<article class="cartao-candidato" data-sq="${esc(c.sq)}">
    <button class="cabeca" aria-expanded="false">
      ${avatar(c)}
      <span class="identidade">
        <strong class="nome">${esc(c.nome)}</strong>
        <span class="linha-info">${esc(c.partido)} · ${esc(c.cargoTexto)}</span>
        ${seloExperiencia(c)}
      </span>
      <span class="numero-mini" aria-label="Número na urna">${esc(c.numero)}</span>
    </button>
    ${miniLinha(c)}
    ${etiquetas(c) ? `<div class="etiquetas">${etiquetas(c)}</div>` : ''}
    <div class="detalhe" hidden></div>
  </article>`;
}

async function alternarCartao(el) {
  const aberto = el.classList.toggle('aberto');
  el.querySelector('.cabeca').setAttribute('aria-expanded', aberto);
  const det = el.querySelector('.detalhe');
  det.hidden = !aberto;
  if (!aberto) return;
  if (!det.dataset.carregado) {
    det.innerHTML = '<p class="carregando">Carregando…</p>';
    try {
      const c = await api('candidato', { sq: el.dataset.sq });
      det.innerHTML = detalheCandidato(c, true);
      ligarDetalhe(det, c);
      det.dataset.carregado = '1';
    } catch (e) { det.innerHTML = erroHtml(e); }
  }
  el.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'nearest' });
}

/** Explicação para mandatos sem dados de atuação em base nacional. */
function semDadosDoMandato(t) {
  const local = t.local ? ' de ' + t.local : '';
  const textos = {
    PREFEITO: `Não existe base nacional padronizada sobre a gestão das prefeituras. Consulte o Tribunal de Contas do Estado e o portal da transparência da prefeitura${local}.`,
    VICE_PREFEITO: 'Não existe base nacional padronizada sobre a gestão das prefeituras. Consulte o Tribunal de Contas do Estado.',
    VEREADOR: `As câmaras municipais não publicam dados num padrão nacional. Consulte o site da Câmara Municipal${local}.`,
    GOVERNADOR: 'Não existe base nacional padronizada sobre a gestão dos governos estaduais. Consulte o Tribunal de Contas do Estado e o portal da transparência do estado.',
    VICE_GOVERNADOR: 'Consulte o Tribunal de Contas do Estado e o portal da transparência do estado.',
    DEPUTADO_ESTADUAL: 'As Assembleias Legislativas não publicam dados num padrão nacional. Consulte o site da Assembleia do estado.',
    DEPUTADO_DISTRITAL: 'Consulte o site da Câmara Legislativa do Distrito Federal.',
  };
  return textos[t.tipo] || 'Sem dados de atuação nas fontes usadas.';
}

function blocoAtuacao(a) {
  const presenca = a.presenca == null ? '' : `
    <div class="medida"><span class="rotulo">Presença nas votações</span>
      <div class="medidor" role="img" aria-label="${pct(a.presenca)}"><span style="width:${Math.min(100, a.presenca)}%"></span></div>
      <strong>${pct(a.presenca)}</strong></div>
    <small class="fraco">${esc(a.detalhePresenca)}</small>`;
  return `<div class="mandato-dados">
    <div class="mandato-titulo"><strong>${esc(a.cargo)}</strong> · ${esc(a.casa)} · ${esc(a.periodo)}</div>
    ${presenca}
    <div class="numeros-mandato">
      <div><strong>${a.projetos}</strong><span>projetos apresentados${a.observacaoProjetos ? ' (' + esc(a.observacaoProjetos) + ')' : ''}</span></div>
      ${a.casa.startsWith('Câmara') ? `<div><strong>${a.aprovados}</strong><span>aprovados</span></div>` : ''}
      ${a.gastoMensal != null ? `<div><strong>${moeda(a.gastoMensal)}</strong><span>gasto médio por mês da verba de gabinete</span></div>` : ''}
    </div>
    ${a.destaques.length ? `<details><summary>Ver projetos em destaque</summary><ul class="destaques">${a.destaques.map(d => `<li>${esc(d)}</li>`).join('')}</ul></details>` : ''}
    ${a.url ? `<a href="${esc(a.url)}" target="_blank" rel="noopener">Página oficial ↗</a>` : ''}
  </div>`;
}

function detalheCandidato(c, resumido) {
  const anoAtual = app.estado.anoEleicao;
  const traj = c.trajetoria.length ? `<ol class="linha-tempo">${c.trajetoria.map(t => {
    const exercendo = t.eleito && t.fimMandato >= anoAtual && t.ano < anoAtual;
    const cls = t.eleito ? 'ok' : t.resultado === 'Suplente' ? 'alerta' : '';
    const periodo = t.eleito ? `<br><small>Mandato ${t.ano + 1}–${t.fimMandato}${exercendo ? ' (em exercício)' : ''}</small>` : '';
    return `<li class="marco ${t.eleito ? 'eleito' : ''}"><span class="ano">${t.ano}</span>
      <div><strong>${esc(t.cargoTexto)}</strong>${t.local ? ' em ' + esc(t.local) : ''} · ${esc(t.partido)}
        <span class="etiqueta ${cls}">${esc(t.resultado)}</span>${periodo}</div></li>`;
  }).join('')}</ol>` : `<p class="fraco">Nenhuma candidatura encontrada nas eleições de ${c.trajetoriaDe} a ${c.trajetoriaAte} neste estado.
      Pode ser a primeira eleição, ou a pessoa concorreu em outro estado.</p>`;

  const mandatosSemDados = c.trajetoria.filter(t => t.eleito && t.tipo !== 'DEPUTADO_FEDERAL' && t.tipo !== 'SENADOR' && t.tipo !== 'SUPLENTE_SENADOR');
  const atuacao = c.atuacao.map(blocoAtuacao).join('')
    + mandatosSemDados.map(t => `<div class="mandato-dados sem">
        <div class="mandato-titulo"><strong>${esc(t.cargoTexto)}</strong>${t.local ? ' em ' + esc(t.local) : ''} · ${t.ano + 1}–${t.fimMandato}</div>
        <p>${esc(semDadosDoMandato(t))}</p></div>`).join('');

  let tcu = '';
  if (c.contas.length) {
    tcu = `<div class="caixa-erro"><strong>Consta na lista do TCU de contas julgadas irregulares</strong> (decisão definitiva, lista enviada ao TSE para a Lei da Ficha Limpa):
      <ul>${c.contas.map(x => `<li>Processo ${esc(x.processo)} · ${esc(x.deliberacao)} · ${esc(x.local)}${x.transito ? ' · trânsito em julgado ' + esc(x.transito) : ''} <small>(ligado por ${esc(x.criterio)})</small></li>`).join('')}</ul>
      <small>Estar na lista não torna a candidatura automaticamente inelegível: quem decide é a Justiça Eleitoral.</small></div>`;
  } else if (app.estado.tcuVerificado) {
    tcu = '<p class="caixa-ok">Não consta na lista do TCU de contas julgadas irregulares com implicação eleitoral (verificação pelo CPF).</p>';
  }

  const bens = c.patrimonio == null ? '<p class="fraco">Sem declaração de bens.</p>' : (() => {
    const max = Math.max(c.patrimonio || 0, c.patrimonioAnterior || 0) || 1;
    const linha = (ano, v) => `<div class="barra-bens"><span class="ano">${ano}</span>
      <div class="trilho"><span style="width:${v == null ? 0 : Math.max(1, 100 * v / max)}%"></span></div><strong>${v == null ? 'não declarou' : moeda(v)}</strong></div>`;
    const variacao = c.patrimonioAnterior ? (100 * (c.patrimonio - c.patrimonioAnterior) / c.patrimonioAnterior) : null;
    return linha(c.anoAnterior, c.patrimonioAnterior) + linha(c.anoEleicao, c.patrimonio)
      + (variacao != null ? `<p class="fraco">${variacao >= 0 ? 'Aumento' : 'Redução'} de ${pct(Math.abs(variacao))} (valores declarados pela própria pessoa, sem correção pela inflação).</p>` : '');
  })();

  const naLista = app.comparar.includes(c.sq);
  return `
    <div class="detalhe-grade">
      <section><h3>Trajetória política</h3>${traj}</section>
      <section><h3>O que fez nos mandatos</h3>
        ${atuacao || '<p class="fraco">Sem mandatos anteriores com dados de atuação.</p>'}
        ${tcu}</section>
      <section><h3>Bens declarados</h3>${bens}</section>
      <section><h3>Quem é</h3>
        <dl class="ficha-dados">
          <dt>Nome completo</dt><dd>${esc(c.nomeCivil)}</dd>
          <dt>Idade</dt><dd>${c.idade != null ? c.idade + ' anos' : 'não informada'}</dd>
          <dt>Escolaridade</dt><dd>${esc(c.escolaridade)}</dd>
          <dt>Ocupação</dt><dd>${esc(c.ocupacao || 'não informada')}</dd>
          <dt>Situação no TSE</dt><dd>${esc(c.elegibilidadeTexto)} <small class="fraco">${esc(c.elegibilidadeExplicacao)}</small></dd>
        </dl></section>
    </div>
    <div class="acoes-detalhe">
      ${resumido ? `<a class="botao" href="#/candidato/${esc(c.sq)}">Ver perfil completo</a>` : ''}
      <button class="botao secundario" data-comparar="${esc(c.sq)}">${naLista ? '✓ Na comparação' : '+ Comparar'}</button>
    </div>`;
}

function ligarDetalhe(raiz, c) {
  raiz.querySelectorAll('[data-comparar]').forEach(b => b.addEventListener('click', () => {
    alternarComparacao(c.sq);
    b.textContent = app.comparar.includes(c.sq) ? '✓ Na comparação' : '+ Comparar';
  }));
}

function alternarComparacao(sq) {
  if (app.comparar.includes(sq)) {
    app.comparar = app.comparar.filter(s => s !== sq);
  } else {
    if (app.comparar.length >= 3) app.comparar.shift();
    app.comparar.push(sq);
  }
  gravarLocal('comparar', app.comparar);
}

// ------------------------------------------------------------ perfil

async function telaCandidato([sq]) {
  const c = await api('candidato', { sq: sq || '' });
  const inds = app.estado.indicadores.map(ind => {
    const v = c.indicadores[ind.codigo];
    return `<div class="cartao indicador-cartao ${v.temDados ? '' : 'sem'}">
      <h3>${esc(ind.titulo)}</h3>
      <div class="valor">${esc(v.temDados ? v.texto : 'Sem dados')}</div>
      <p>${esc(v.temDados ? v.resumo : v.detalhe)}</p>
      ${v.temDados ? `<small>Como calculamos: ${esc(v.detalhe)}</small><br>` : ''}
      <small>Fonte: ${esc(ind.fonte)}</small>
    </div>`;
  }).join('');
  main().innerHTML = `
    <p><button class="botao discreto" id="voltar">← Voltar</button></p>
    <section class="cartao perfil-topo">
      ${avatar(c, 'grande')}
      <div style="flex:1;min-width:240px">
        <p class="sobretitulo">${esc(c.cargoTexto)} · ${esc(c.partido)}</p>
        <h1 style="margin-bottom:6px">${esc(c.nome)}</h1>
        <div class="etiquetas">${seloExperiencia(c)} ${etiquetas(c)}</div>
      </div>
      <div class="urna"><small>Número na urna</small><div class="numero-urna">${esc(c.numero)}</div></div>
    </section>
    ${c.elegibilidade !== 'APTA' ? `<p class="${c.elegibilidade === 'INAPTA' ? 'caixa-erro' : 'caixa-alerta'}" style="margin-top:16px"><strong>Situação: ${esc(c.elegibilidadeTexto)}.</strong> ${esc(c.elegibilidadeExplicacao)}</p>` : ''}
    ${c.deputado && c.vinculoDuvidoso ? `<p class="caixa-alerta">Ligamos esta candidatura ao mandato de <strong>${esc(c.deputado.nome)}</strong> só pelo nome. Confira se é a mesma pessoa.</p>` : ''}
    <div class="cartao" style="margin-top:16px">${detalheCandidato(c, false)}</div>
    <h2 class="titulo-secao">Indicadores usados na comparação</h2>
    <p class="fraco">Calculados com dados da Câmara (mandato atual) e do TSE. Quem nunca foi deputado(a) federal não tem os da Câmara: isso não é nota ruim.</p>
    <div class="grade grade-3">${inds}</div>`;
  $('#voltar').addEventListener('click', () => { if (history.length > 1) history.back(); else location.hash = '#/candidatos'; });
  ligarDetalhe(main(), c);
}

// ------------------------------------------------------------ comparar

async function telaComparar() {
  const todos = await listaCandidatos();
  const opcoes = sel => '<option value="">— escolher —</option>' + todos.map(c => `<option value="${esc(c.sq)}" ${c.sq === sel ? 'selected' : ''}>${esc(c.nome)} (${esc(c.cargoTexto)}, ${esc(c.partido)} ${esc(c.numero)})</option>`).join('');
  main().innerHTML = `
    <h1>Comparar candidatos</h1>
    <p class="fraco">Escolha até 3 pessoas. Você também pode adicionar pelos cartões ou pelo perfil.</p>
    <div class="grade grade-3">${[0, 1, 2].map(i => `<select data-i="${i}" aria-label="Candidato ${i + 1}">${opcoes(app.comparar[i])}</select>`).join('')}</div>
    <div id="comparacao" style="margin-top:20px"></div>`;
  document.querySelectorAll('select[data-i]').forEach(s => s.addEventListener('change', () => {
    const escolhidos = [...document.querySelectorAll('select[data-i]')].map(x => x.value).filter(Boolean);
    app.comparar = [...new Set(escolhidos)];
    gravarLocal('comparar', app.comparar);
    desenharComparacao();
  }));
  await desenharComparacao();
}

async function desenharComparacao() {
  const alvo = $('#comparacao');
  if (!app.comparar.length) { alvo.innerHTML = '<p class="caixa-info">Nenhuma pessoa escolhida ainda.</p>'; return; }
  const perfis = await Promise.all(app.comparar.map(sq => api('candidato', { sq })));
  // notas normalizadas com o mesmo peso em tudo, entre candidatos do mesmo cargo de cada pessoa
  const cargos = [...new Set(perfis.map(c => c.cargo))];
  const notas = {};
  for (const cargo of cargos) {
    const p = { inaptos: 1, cobertura: 1, cargo };
    app.estado.indicadores.forEach(ind => { p['peso.' + ind.codigo] = 10; });
    (await api('ranking', p)).itens.forEach(i => { notas[i.sq] = i.indicadores; });
  }
  const linhas = [
    ['Cargo disputado', c => esc(c.cargoTexto)],
    ['Número / partido', c => `<strong>${esc(c.numero)}</strong> · ${esc(c.partido)}`],
    ['Experiência', c => seloExperiencia(c)],
    ['Eleições anteriores', c => c.trajetoria.length ? c.trajetoria.map(t => `${t.ano}: ${esc(t.cargoTexto)} – ${esc(t.resultado)}`).join('<br>') : 'nenhuma encontrada'],
    ['Contas irregulares (TCU)', c => c.contas.length ? `<span class="etiqueta perigo">${c.contas.length} processo(s)</span>` : (app.estado.tcuVerificado ? 'Não consta' : '–')],
    ['Situação', c => esc(c.elegibilidadeTexto)],
    ['Idade', c => c.idade != null ? c.idade + ' anos' : '–'],
    ['Escolaridade', c => esc(c.escolaridade)],
    ['Bens declarados', c => moeda(c.patrimonio)],
    ...app.estado.indicadores.map(ind => [ind.titulo, c => {
      const v = c.indicadores[ind.codigo];
      return v.temDados ? `<strong>${esc(v.texto)}</strong><br><small>${esc(v.resumo)}</small>` : '<span class="fraco"><em>sem dados</em></span>';
    }]),
  ];
  const series = perfis.map((c, i) => ({
    nome: c.nome, cor: CORES[i],
    valores: app.estado.indicadores.map(ind => {
      const n = notas[c.sq]?.[ind.codigo]?.nota;
      return n == null ? null : Math.round(n * 100);
    }),
  }));
  alvo.innerHTML = `
    <div class="cartao rolagem-x"><table class="tabela">
      <tr><th></th>${perfis.map((c, i) => `<th><a href="#/candidato/${esc(c.sq)}" style="color:${CORES[i]}">${esc(c.nome)}</a></th>`).join('')}</tr>
      ${linhas.map(([rot, f]) => `<tr><th>${esc(rot)}</th>${perfis.map(c => `<td>${f(c)}</td>`).join('')}</tr>`).join('')}
    </table></div>
    <div class="cartao grafico" style="margin-top:16px">
      <h2>Posição em cada critério</h2>
      <p class="fraco">0 = pior e 100 = melhor entre os candidatos ao <strong>mesmo cargo</strong>, no sentido padrão de cada critério. “s/d” = sem dados (não é zero).</p>
      <div class="legenda">${series.map(s => `<span style="--cor:${s.cor}">${esc(s.nome)}</span>`).join('')}</div>
      ${graficoBarras(app.estado.indicadores.map(i => i.titulo), series, { maximo: 100 })}
    </div>`;
}

// ------------------------------------------------------------ panorama

const DISTRIBUICOES = [['experiencia', 'Experiência'], ['escolaridade', 'Escolaridade'], ['idade', 'Idade'],
  ['patrimonio', 'Bens declarados'], ['genero', 'Gênero'], ['corRaca', 'Cor/raça'], ['partido', 'Partido'],
  ['elegibilidade', 'Situação']];
let panorama = { tipo: 'experiencia' };

async function telaPanorama() {
  const vars = await api('variaveis');
  const cargo = app.config.panoramaCargo || 'TODOS';
  const opcoes = sel => Object.entries(vars).map(([k, n]) => `<option value="${k}" ${k === sel ? 'selected' : ''}>${esc(n)}</option>`).join('');
  main().innerHTML = `
    <h1>Panorama das candidaturas</h1>
    <p class="fraco">Quem são as pessoas que disputam a eleição? Estes gráficos descrevem o grupo, sem avaliar ninguém.
      Gênero e cor/raça aparecem só aqui e <strong>nunca</strong> entram na nota.</p>
    <label for="pan-cargo">Cargo</label>
    <select id="pan-cargo" style="max-width:360px;margin-bottom:16px"><option value="TODOS">Todos os cargos</option>
      ${(app.estado.cargos || []).map(c => `<option value="${c.codigo}" ${c.codigo === cargo ? 'selected' : ''}>${esc(c.rotulo)}</option>`).join('')}</select>
    <div class="cartao grafico">
      <div class="chips" role="group" aria-label="Escolha o gráfico">${DISTRIBUICOES.map(([k, n]) => `<button data-d="${k}" aria-pressed="${k === panorama.tipo}">${n}</button>`).join('')}</div>
      <div id="distribuicao"></div>
    </div>
    <div class="cartao grafico" style="margin-top:16px">
      <h2>Uma coisa tem a ver com a outra?</h2>
      <p class="fraco">Escolha duas informações. Cada ponto é uma pessoa que tem as duas.</p>
      <div class="grade grade-2">
        <div><label for="var-x">Eixo horizontal</label><select id="var-x">${opcoes('escolaridade')}</select></div>
        <div><label for="var-y">Eixo vertical</label><select id="var-y">${opcoes('assiduidade')}</select></div>
      </div>
      <div id="dispersao" style="margin-top:12px"></div>
    </div>`;
  document.querySelectorAll('[data-d]').forEach(b => b.addEventListener('click', () => {
    document.querySelectorAll('[data-d]').forEach(x => x.setAttribute('aria-pressed', x === b));
    panorama.tipo = b.dataset.d;
    desenharDistribuicao();
  }));
  $('#pan-cargo').addEventListener('change', ev => { app.config.panoramaCargo = ev.target.value; salvarConfig(); desenharDistribuicao(); desenharDispersao(); });
  $('#var-x').addEventListener('change', desenharDispersao);
  $('#var-y').addEventListener('change', desenharDispersao);
  await Promise.all([desenharDistribuicao(), desenharDispersao()]);
}

function cargoPanorama() {
  const c = $('#pan-cargo').value;
  return c === 'TODOS' ? {} : { cargo: c };
}

async function desenharDistribuicao() {
  const d = await api('distribuicao', Object.assign({ tipo: panorama.tipo }, cargoPanorama()));
  $('#distribuicao').innerHTML = `<h2>${esc(d.titulo)}</h2><p class="fraco">${d.total} candidaturas</p>`
    + graficoBarras(d.barras.map(b => b.rotulo), [{ nome: 'Candidaturas', cor: CORES[0], valores: d.barras.map(b => b.valor) }], { rotulos: true });
}

async function desenharDispersao() {
  const d = await api('correlacao', Object.assign({ x: $('#var-x').value, y: $('#var-y').value }, cargoPanorama()));
  const r = d.r == null ? '–' : d.r.toLocaleString('pt-BR', { maximumFractionDigits: 2 });
  $('#dispersao').innerHTML = graficoDispersao(d) + `
    <p style="margin-top:10px"><strong>Correlação (r de Pearson) = ${r}</strong> com ${d.n} pessoas: ${esc(d.interpretacao)}.
      ${d.n < 10 ? '<em>Poucas pessoas: interprete com muito cuidado.</em>' : ''}</p>
    <p class="fraco">r vai de −1 a 1. Perto de 0 = sem relação linear. Mesmo uma correlação forte não prova que uma coisa causa a outra.</p>`;
}

// ------------------------------------------------------------ ranking

async function telaRanking() {
  const cargo = cargoPadrao();
  main().innerHTML = `
    <h1>O que importa pra você?</h1>
    <p class="fraco">Escolha quanto cada critério pesa. A lista se reorganiza na hora.
      <a class="so-celular" href="#resultados-titulo">Ver a lista ↓</a></p>
    <div class="linha-cargo"><label for="rk-cargo">Comparar candidatos a</label>
      <select id="rk-cargo">${cargosPrincipais().map(c => `<option value="${c.codigo}" ${c.codigo === cargo ? 'selected' : ''}>${esc(c.rotulo)} (${c.total})</option>`).join('')}</select></div>
    <p class="caixa-info" id="aviso-cargo" hidden></p>
    <div class="layout-ranking">
      <aside class="cartao painel-pesos" aria-label="Critérios">
        <div id="criterios"></div>
        <details id="avancado" ${app.config.avancado ? 'open' : ''} style="margin-top:12px">
          <summary>Opções avançadas</summary>
          <div class="pilha" style="margin-top:12px">
            <label><input type="checkbox" id="opt-inaptos" ${app.config.inaptos ? 'checked' : ''}> Mostrar candidaturas inaptas</label>
            <div><label for="opt-cobertura">Só dar nota a quem tem dados em pelo menos</label>
              <select id="opt-cobertura">${[1, 2, 3, 4, 5].map(n => `<option value="${n}" ${app.config.cobertura == n ? 'selected' : ''}>${n} critério${n > 1 ? 's' : ''}</option>`).join('')}</select>
              <small>Com 1, quem nunca foi deputado pode receber nota só pelo patrimônio. Aumentar evita isso, mas tira essas pessoas da lista.</small></div>
            <div><label for="opt-metodo">Método de cálculo</label>
              <select id="opt-metodo"><option value="soma">Média ponderada (recomendado)</option><option value="topsis" ${app.config.metodo === 'topsis' ? 'selected' : ''}>TOPSIS (só quem tem todos os dados)</option></select></div>
          </div>
        </details>
      </aside>
      <section>
        <h2 id="resultados-titulo" class="so-celular">Resultado</h2>
        <input type="search" id="filtro" placeholder="Filtrar a lista por nome, número ou partido" aria-label="Filtrar a lista" style="margin-bottom:12px">
        <p class="caixa-info" id="explicacao-nota">A nota vai de 0 a 100 e compara só as pessoas desta lista, com os pesos que você escolheu.
          Quem não tem dados de um critério não perde pontos por isso: o critério fica fora da conta dessa pessoa.</p>
        <div id="resultados" class="lista-resultados" aria-live="polite"></div>
      </section>
    </div>`;
  desenharCriterios();
  $('#avancado').addEventListener('toggle', ev => { app.config.avancado = ev.target.open; salvarConfig(); desenharCriterios(); });
  $('#opt-inaptos').addEventListener('change', ev => { app.config.inaptos = ev.target.checked; mudouConfig(); });
  $('#opt-cobertura').addEventListener('change', ev => { app.config.cobertura = +ev.target.value; mudouConfig(); });
  $('#opt-metodo').addEventListener('change', ev => { app.config.metodo = ev.target.value; mudouConfig(); });
  $('#filtro').addEventListener('input', () => desenharResultados());
  $('#rk-cargo').addEventListener('change', ev => { app.config.cargo = ev.target.value; mudouConfig(); avisoCargo(); });
  avisoCargo();
  await carregarRanking();
}

function desenharCriterios() {
  const html = app.estado.indicadores.map(ind => {
    const peso = pesoDe(ind.codigo);
    const semOpinioes = ind.codigo === 'alinhamento' && app.estado.posicoes === 0;
    const botoes = PESOS.map(p => `<button type="button" data-cod="${ind.codigo}" data-peso="${p.valor}" aria-pressed="${peso === p.valor}">${p.texto}</button>`).join('');
    const avancado = app.config.avancado ? `
      <div class="avancado-criterio">
        <input type="range" min="0" max="10" value="${peso}" data-cod="${ind.codigo}" aria-label="Peso de ${esc(ind.titulo)}">
        <span>${peso}</span>
        <label style="font-weight:400"><input type="checkbox" data-inv="${ind.codigo}" ${app.config.invertidos[ind.codigo] ? 'checked' : ''}> inverter</label>
      </div><small>Padrão: ${esc(ind.sentido)}. Fonte: ${esc(ind.fonte)}</small>` : '';
    return `<div class="criterio">
      <h3>${esc(ind.titulo)}</h3>
      <p>${esc(ind.pergunta)}</p>
      ${semOpinioes ? '<p class="caixa-alerta" style="padding:8px 12px">Para usar este critério, responda algumas perguntas em <a href="#/opinioes">Minhas opiniões</a>.</p>' : ''}
      <div class="segmentado" role="group" aria-label="Peso de ${esc(ind.titulo)}">${botoes}</div>
      ${avancado}
    </div>`;
  }).join('');
  $('#criterios').innerHTML = html;
  $('#criterios').querySelectorAll('.segmentado button').forEach(b => b.addEventListener('click', () => {
    app.config.pesos[b.dataset.cod] = +b.dataset.peso;
    desenharCriterios();
    mudouConfig();
  }));
  $('#criterios').querySelectorAll('input[type=range]').forEach(r => r.addEventListener('input', () => {
    app.config.pesos[r.dataset.cod] = +r.value;
    r.nextElementSibling.textContent = r.value;
    r.closest('.criterio').querySelectorAll('.segmentado button').forEach(b => b.setAttribute('aria-pressed', +b.dataset.peso === +r.value));
    mudouConfig();
  }));
  $('#criterios').querySelectorAll('input[data-inv]').forEach(c => c.addEventListener('change', () => {
    app.config.invertidos[c.dataset.inv] = c.checked;
    mudouConfig();
  }));
}

function avisoCargo() {
  const c = app.config.cargo || cargoPadrao();
  const aviso = $('#aviso-cargo');
  aviso.hidden = c === 'DEPUTADO_FEDERAL';
  aviso.textContent = 'Os critérios de presença, verba e projetos vêm da Câmara dos Deputados: para este cargo, só quem é '
    + 'deputado(a) federal hoje tem esses dados. Veja a trajetória de todos na página Candidatos.';
}

let temporizador;
function mudouConfig() {
  salvarConfig();
  clearTimeout(temporizador);
  temporizador = setTimeout(carregarRanking, 150);
}

let ultimoRanking = null;
async function carregarRanking() {
  const p = { inaptos: app.config.inaptos ? 1 : 0, cobertura: app.config.cobertura, metodo: app.config.metodo,
    cargo: app.config.cargo || cargoPadrao() };
  app.estado.indicadores.forEach(ind => {
    p['peso.' + ind.codigo] = pesoDe(ind.codigo);
    if (app.config.invertidos[ind.codigo]) p['inv.' + ind.codigo] = 1;
  });
  try {
    ultimoRanking = await api('ranking', p);
    desenharResultados();
  } catch (e) {
    $('#resultados').innerHTML = erroHtml(e);
  }
}

function desenharResultados() {
  if (!ultimoRanking) return;
  const termo = ($('#filtro')?.value || '').trim().toLowerCase();
  const itens = ultimoRanking.itens.filter(c => !termo || (c.nome + ' ' + c.partido + ' ' + c.numero).toLowerCase().includes(termo));
  const semNota = itens.filter(c => c.pontuacao == null).length;
  const cartoes = itens.map(c => {
    const temNota = c.pontuacao != null;
    const fichas = app.estado.indicadores.map(ind => {
      const v = c.indicadores[ind.codigo];
      return `<span class="ficha ${v.temDados ? '' : 'sem'}" title="${esc(ind.titulo + ': ' + v.detalhe)}">${esc(v.temDados ? v.resumo : ind.titulo + ': sem dados')}</span>`;
    }).join('');
    const naLista = app.comparar.includes(c.sq);
    return `<article class="cartao resultado ${temNota ? '' : 'sem-pontuacao'}">
      <div class="posicao">${temNota ? c.posicao + 'º' : '–'}</div>
      ${avatar(c)}
      <div>
        <div class="nome-linha"><h3>${esc(c.nome)}</h3><small>Nº ${esc(c.numero)} · ${esc(c.partido)}</small></div>
        <div class="etiquetas">${seloExperiencia(c)} ${etiquetas(c)}</div>
        <div class="nota">${temNota
          ? `<div class="barra" role="img" aria-label="Nota ${Math.round(c.pontuacao)} de 100"><span style="width:${c.pontuacao}%"></span></div><strong>${Math.round(c.pontuacao)}</strong><small>com ${c.cobertura} de ${c.coberturaPedida} critérios</small>`
          : `<small>Sem nota: dados em ${c.cobertura} de ${c.coberturaPedida} critérios escolhidos</small>`}</div>
        <div class="fichas">${fichas}</div>
      </div>
      <div class="acoes">
        <a class="botao secundario" href="#/candidato/${esc(c.sq)}">Ver perfil</a>
        <button class="botao discreto" data-comparar="${esc(c.sq)}">${naLista ? '✓ Na comparação' : '+ Comparar'}</button>
      </div>
    </article>`;
  }).join('');
  $('#resultados').innerHTML = (cartoes || '<p class="fraco">Ninguém encontrado com esse filtro.</p>')
    + (semNota ? `<p class="fraco">${semNota} pessoa(s) no fim da lista não receberam nota por falta de dados nos critérios escolhidos.</p>` : '');
  $('#resultados').querySelectorAll('[data-comparar]').forEach(b => b.addEventListener('click', () => {
    alternarComparacao(b.dataset.comparar);
    desenharResultados();
  }));
}


// ------------------------------------------------------------ opiniões

let filaPerguntas = [];
async function telaOpinioes() {
  main().innerHTML = `
    <h1>Como você votaria?</h1>
    <p class="fraco">Estes projetos já foram votados no Plenário da Câmara. Diga como você votaria e o critério
      <strong>“Vota como eu votaria”</strong> passa a mostrar quem votou igual a você. Não existe resposta certa: o site
      não considera nenhuma posição melhor que outra.</p>
    <section class="cartao pergunta" id="pergunta" aria-live="polite"></section>
    <h2 style="margin-top:32px">Suas respostas</h2>
    <div class="cartao" id="minhas"></div>
    <h2 style="margin-top:32px">Procurar um tema</h2>
    <div class="grade grade-2">
      <input type="search" id="busca-votacoes" placeholder="Ex.: saúde, imposto, educação, PL 1234" aria-label="Procurar votações">
      <label style="align-self:center;font-weight:400"><input type="checkbox" id="so-principais" checked> Só as votações do texto principal (mais fáceis de entender)</label>
    </div>
    <div class="cartao" id="lista-votacoes" style="margin-top:12px"></div>`;
  let t;
  $('#busca-votacoes').addEventListener('input', () => { clearTimeout(t); t = setTimeout(carregarVotacoes, 250); });
  $('#so-principais').addEventListener('change', carregarVotacoes);
  await carregarVotacoes(true);
}

async function carregarVotacoes(reiniciarFila) {
  const busca = $('#busca-votacoes').value;
  const r = await api('votacoes', { busca, principais: $('#so-principais').checked ? 1 : 0, limite: 15 });
  if (reiniciarFila || !filaPerguntas.length) {
    const fila = await api('votacoes', { principais: 1, limite: 400 });
    filaPerguntas = fila.votacoes.filter(v => !v.meuVoto);
    desenharPergunta(fila.minhas.length);
  }
  $('#lista-votacoes').innerHTML = (r.votacoes.length ? r.votacoes.map(itemVotacao).join('') : '<p class="fraco">Nenhuma votação encontrada.</p>')
    + (r.total > r.votacoes.length ? `<p class="fraco">Mostrando ${r.votacoes.length} de ${r.total}. Refine a busca para ver outras.</p>` : '');
  $('#minhas').innerHTML = r.minhas.length ? r.minhas.map(itemVotacao).join('') : '<p class="fraco">Você ainda não respondeu nenhuma.</p>';
  document.querySelectorAll('.escolha button').forEach(b => b.addEventListener('click', () => responder(b.dataset.id, b.dataset.voto)));
}

function itemVotacao(v) {
  const btn = (voto, cls, texto) => `<button class="${cls}" data-id="${esc(v.id)}" data-voto="${v.meuVoto === voto ? '' : voto}" aria-pressed="${v.meuVoto === voto}">${texto}</button>`;
  return `<div class="item-votacao">
    <div><strong>${esc(v.proposicao || 'Votação ' + v.id)}</strong> <small>${esc(v.data)}</small><br>
      ${esc(v.ementa || v.descricao)}${v.ementa ? `<br><small>Em votação: ${esc(v.descricao)}</small>` : ''}</div>
    <div class="escolha">${btn('Sim', 's', 'Sim')}${btn('Não', 'n', 'Não')}</div>
  </div>`;
}

function desenharPergunta(respondidas) {
  const v = filaPerguntas[0];
  const caixa = $('#pergunta');
  if (!v) {
    caixa.innerHTML = `<h2>Você respondeu todas as perguntas sugeridas 🎉</h2>
      <p><a class="botao" href="#/ranking">Ver quem votou como eu</a></p>`;
    return;
  }
  caixa.innerHTML = `
    <p class="progresso">${respondidas} resposta(s) até agora · votação de ${esc(v.data)}</p>
    <div class="proposicao">${esc(v.proposicao || 'Votação ' + v.id)}</div>
    <p class="ementa">${esc(v.ementa || v.descricao)}</p>
    ${v.ementa ? `<p class="fraco">O que foi votado: ${esc(v.descricao)}</p>` : ''}
    <div class="botoes-voto">
      <button class="botao grande sim" data-voto="Sim">Eu votaria SIM</button>
      <button class="botao grande nao" data-voto="Não">Eu votaria NÃO</button>
      <button class="botao grande secundario" data-voto="">Pular</button>
    </div>
    ${respondidas >= 3 ? '<p style="margin-top:14px"><a href="#/ranking">Já quero ver o resultado →</a></p>' : ''}`;
  caixa.querySelectorAll('button').forEach(b => b.addEventListener('click', async () => {
    filaPerguntas.shift();
    if (b.dataset.voto) {
      await responder(v.id, b.dataset.voto);
    } else {
      desenharPergunta(respondidas);
    }
  }));
}

async function responder(id, voto) {
  try {
    const r = await api('posicao', { idVotacao: id, voto }, true);
    const antes = app.estado.posicoes;
    app.estado.posicoes = r.posicoes;
    // ao responder a primeira, liga o critério de alinhamento se o usuário não mexeu nele
    if (antes === 0 && r.posicoes > 0 && app.config.pesos.alinhamento == null) app.config.pesos.alinhamento = 5;
    salvarConfig();
    filaPerguntas = filaPerguntas.filter(v => v.id !== id);
    await carregarVotacoes(false);
    desenharPergunta(r.posicoes);
  } catch (e) {
    alert(e.message);
  }
}

// ------------------------------------------------------------ gráficos SVG

function numeroCurto(v) {
  const a = Math.abs(v);
  if (a >= 1e6) return (v / 1e6).toLocaleString('pt-BR', { maximumFractionDigits: 1 }) + ' mi';
  if (a >= 1e4) return Math.round(v / 1e3) + ' mil';
  return v.toLocaleString('pt-BR', { maximumFractionDigits: a < 10 ? 1 : 0 });
}

function tetoRedondo(v) {
  if (v <= 0) return 1;
  const p = Math.pow(10, Math.floor(Math.log10(v)));
  return [1, 2, 2.5, 5, 10].map(m => m * p).find(x => x >= v);
}

function graficoBarras(categorias, series, opts = {}) {
  const L = 760, A = 300, mE = 44, mD = 8, mT = 16, mB = 56;
  const larg = L - mE - mD, alt = A - mT - mB;
  const valores = series.flatMap(s => s.valores).filter(v => v != null);
  const max = opts.maximo || tetoRedondo(Math.max(1, ...valores));
  const grupo = larg / categorias.length;
  const barra = Math.min(46, grupo * 0.72 / series.length);
  let svg = `<svg viewBox="0 0 ${L} ${A}" role="img" aria-label="Gráfico de barras">`;
  for (let i = 0; i <= 4; i++) {
    const y = mT + alt - alt * i / 4;
    svg += `<line x1="${mE}" x2="${L - mD}" y1="${y}" y2="${y}" style="stroke:var(--grade)"/><text x="${mE - 6}" y="${y + 4}" text-anchor="end">${numeroCurto(max * i / 4)}</text>`;
  }
  categorias.forEach((cat, c) => {
    const x0 = mE + c * grupo + (grupo - barra * series.length) / 2;
    series.forEach((s, i) => {
      const v = s.valores[c];
      const x = x0 + i * barra + 1, w = Math.max(2, barra - 2);
      if (v == null) {
        svg += `<text x="${x + w / 2}" y="${mT + alt - 4}" text-anchor="middle">s/d</text>`;
        return;
      }
      const h = Math.max(2, alt * v / max), y = mT + alt - h;
      svg += `<path d="M${x},${mT + alt} V${y + 4} q0,-4 4,-4 h${w - 8} q4,0 4,4 V${mT + alt} Z" fill="${s.cor}"><title>${esc(s.nome ? s.nome + ' · ' : '')}${esc(cat)}: ${numeroCurto(v)}</title></path>`;
      if (opts.rotulos) svg += `<text class="rotulo-valor" x="${x + w / 2}" y="${y - 4}" text-anchor="middle">${numeroCurto(v)}</text>`;
    });
    const palavras = String(cat).split(' ');
    const meio = Math.ceil(palavras.length / 2);
    const linhas = String(cat).length > 14 && palavras.length > 1 ? [palavras.slice(0, meio).join(' '), palavras.slice(meio).join(' ')] : [String(cat)];
    linhas.forEach((ln, k) => {
      svg += `<text x="${mE + c * grupo + grupo / 2}" y="${mT + alt + 18 + k * 14}" text-anchor="middle">${esc(ln.length > 18 ? ln.slice(0, 17) + '…' : ln)}</text>`;
    });
  });
  svg += `<line x1="${mE}" x2="${L - mD}" y1="${mT + alt}" y2="${mT + alt}" style="stroke:var(--eixo)"/></svg>`;
  return svg;
}

function graficoDispersao(d) {
  const L = 760, A = 380, mE = 56, mD = 12, mT = 12, mB = 48;
  const larg = L - mE - mD, alt = A - mT - mB;
  if (d.pontos.length < 2) return '<p class="fraco">Dados insuficientes para este gráfico.</p>';
  const xs = d.pontos.map(p => p.x), ys = d.pontos.map(p => p.y);
  let [x0, x1, y0, y1] = [Math.min(...xs), Math.max(...xs), Math.min(...ys), Math.max(...ys)];
  const fx = (x1 - x0 || 1) * 0.06, fy = (y1 - y0 || 1) * 0.08;
  x0 -= fx; x1 += fx; y0 -= fy; y1 += fy;
  const px = x => mE + (x - x0) / (x1 - x0) * larg;
  const py = y => mT + alt - (y - y0) / (y1 - y0) * alt;
  let svg = `<svg viewBox="0 0 ${L} ${A}" role="img" aria-label="${esc(d.nomeY)} por ${esc(d.nomeX)}">`;
  for (let i = 0; i <= 4; i++) {
    const vy = y0 + (y1 - y0) * i / 4, vx = x0 + (x1 - x0) * i / 4;
    svg += `<line x1="${mE}" x2="${L - mD}" y1="${py(vy)}" y2="${py(vy)}" style="stroke:var(--grade)"/>`;
    svg += `<text x="${mE - 6}" y="${py(vy) + 4}" text-anchor="end">${numeroCurto(vy)}</text>`;
    svg += `<text x="${px(vx)}" y="${mT + alt + 18}" text-anchor="middle">${numeroCurto(vx)}</text>`;
  }
  svg += `<line x1="${mE}" x2="${L - mD}" y1="${mT + alt}" y2="${mT + alt}" style="stroke:var(--eixo)"/>`;
  if (d.reta) {
    const [a, b] = d.reta, xa = x0 + fx, xb = x1 - fx;
    svg += `<line x1="${px(xa)}" y1="${py(a + b * xa)}" x2="${px(xb)}" y2="${py(a + b * xb)}" style="stroke:var(--texto-2)" stroke-width="1.5" stroke-dasharray="6 5"/>`;
  }
  d.pontos.forEach(p => {
    svg += `<circle cx="${px(p.x)}" cy="${py(p.y)}" r="6" fill="${CORES[0]}" style="stroke:var(--superficie)" stroke-width="2"><title>${esc(p.nome)} – ${esc(d.nomeX)}: ${numeroCurto(p.x)}; ${esc(d.nomeY)}: ${numeroCurto(p.y)}</title></circle>`;
  });
  svg += `<text x="${mE + larg / 2}" y="${A - 6}" text-anchor="middle" style="fill:var(--texto)">${esc(d.nomeX)}</text>`;
  svg += `<text transform="rotate(-90)" x="${-(mT + alt / 2)}" y="14" text-anchor="middle" style="fill:var(--texto)">${esc(d.nomeY)}</text></svg>`;
  return svg;
}

// ------------------------------------------------------------ como funciona

async function telaComoFunciona() {
  const html = await (await fetch('/metodologia.html')).text();
  const corpo = new DOMParser().parseFromString(html, 'text/html').body.innerHTML;
  main().innerHTML = `<h1>Como funciona</h1><div class="cartao metodologia">${corpo}</div>`;
}

// ------------------------------------------------------------ dados

const UFS = ['AC', 'AL', 'AM', 'AP', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MG', 'MS', 'MT', 'PA', 'PB', 'PE', 'PI', 'PR', 'RJ', 'RN', 'RO', 'RR', 'RS', 'SC', 'SE', 'SP', 'TO'];

async function telaDados() {
  const e = app.estado;
  main().innerHTML = `
    <h1>Dados</h1>
    <div class="grade grade-2">
      <section class="cartao">
        <h2>Base em uso</h2>
        ${e.carregada ? `<p><strong>${esc(e.descricao)}</strong></p>
          <p class="fraco">${e.candidatos} candidaturas · ${e.deputados} deputados · ${e.votacoes} votações${e.geradoEm ? ' · processado em ' + esc(e.geradoEm) : ''}</p>` : '<p>Nenhuma base carregada.</p>'}
        <h3 style="margin-top:16px">Abrir outra base</h3>
        <div class="chips">${e.basesDisponiveis.map(b => `<button data-base="${esc(b)}" aria-pressed="${b === e.base}">${b === 'demo' ? 'Demonstração (fictícia)' : esc(b)}</button>`).join('')}</div>
      </section>
      <section class="cartao">
        <h2>Coletar dados reais</h2>
        <p class="fraco">Baixa os arquivos públicos do TSE e da Câmara do seu estado. Na primeira vez pode levar
          <strong>vários minutos</strong> (são centenas de MB); depois fica guardado no computador.</p>
        <div class="grade grade-2">
          <div><label for="uf">Estado</label><select id="uf">${UFS.map(u => `<option ${u === 'SE' ? 'selected' : ''}>${u}</option>`).join('')}</select></div>
          <div><label for="ano">Eleição</label><select id="ano"><option>2026</option><option>2022</option></select></div>
        </div>
        <p style="margin-top:10px"><label style="font-weight:400"><input type="checkbox" id="baixar" checked> Baixar arquivos que ainda não estão no computador</label></p>
        <button class="botao" id="coletar">Coletar e abrir</button>
        <div id="progresso" style="margin-top:14px"></div>
      </section>
    </div>
    <section class="cartao" style="margin-top:16px">
      <h2>Se o download do TSE falhar</h2>
      <p>O site do TSE às vezes recusa downloads automáticos. Nesse caso: abra
        <a href="https://dadosabertos.tse.jus.br/dataset/candidatos-2026" target="_blank" rel="noopener">Candidatos 2026 no portal do TSE</a>,
        baixe <em>consulta_cand_2026.zip</em>, <em>bem_candidato_2026.zip</em> e (se houver) <em>consulta_cand_complementar_2026.zip</em>;
        faça o mesmo em <a href="https://dadosabertos.tse.jus.br/dataset/candidatos-2022" target="_blank" rel="noopener">Candidatos 2022</a>.
        Coloque os arquivos, sem renomear, na pasta <code>dados/brutos</code> do projeto e colete de novo.</p>
    </section>`;
  document.querySelectorAll('[data-base]').forEach(b => b.addEventListener('click', async () => {
    main().innerHTML = '<p class="carregando">Abrindo…</p>';
    try {
      await api('abrir', { base: b.dataset.base }, true);
      await atualizarEstado();
      location.hash = '#/inicio';
    } catch (err) { main().innerHTML = erroHtml(err); }
  }));
  $('#coletar').addEventListener('click', async () => {
    try {
      await api('coleta', { uf: $('#uf').value, ano: $('#ano').value, baixar: $('#baixar').checked ? 1 : 0 }, true);
      acompanharColeta();
    } catch (err) { $('#progresso').innerHTML = erroHtml(err); }
  });
  const s = await api('coleta');
  if (s.coletando || s.log.length) acompanharColeta();
}

async function acompanharColeta() {
  const alvo = $('#progresso');
  if (!alvo) return;
  const s = await api('coleta');
  $('#coletar').disabled = s.coletando;
  alvo.innerHTML = `${s.coletando ? '<p><strong>Coletando…</strong> pode deixar esta página aberta.</p>' : ''}
    ${s.erro ? erroHtml(s.erro) : ''}
    ${!s.coletando && !s.erro && s.log.length ? '<p class="caixa-info">Pronto! <a href="#/inicio">Ir para o início</a></p>' : ''}
    <div class="log">${esc(s.log.join('\n'))}</div>`;
  const log = alvo.querySelector('.log');
  log.scrollTop = log.scrollHeight;
  if (s.coletando) {
    setTimeout(acompanharColeta, 1500);
  } else if (!s.erro && s.log.length) {
    await atualizarEstado();
  }
}
