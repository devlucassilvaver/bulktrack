const $ = (seletor, raiz = document) => raiz.querySelector(seletor);
const paineis = ['dashboard', 'alimentacao', 'treinos', 'perfil'];
const macros = {
  calorias: ['Calorias', 'kcal'],
  carboidratos: ['Carboidratos', 'g'],
  proteinas: ['Proteínas', 'g'],
  lipidios: ['Lipídios', 'g']
};
const formato = new Intl.NumberFormat('pt-BR', {maximumFractionDigits: 2});
const formPerfil = $('#editar-perfil');
const formConsumo = $('#consumo');
const formTreino = $('#treino');
const dia = $('#dia');
let perfil;
let requisicaoCsrf;
let mensagemPainel;
let alimentoSelecionado;
let consumoEmEdicao;
let treinoEmEdicao;
let versaoPrevia = 0;
let timerPrevia;
let versaoPainel = 0;

function elemento(tipo, texto, classe) {
  const no = document.createElement(tipo);
  if (texto != null) no.textContent = texto;
  if (classe) no.className = classe;
  return no;
}

function botao(texto, acao) {
  const no = elemento('button', texto, 'secundario');
  no.type = 'button';
  no.addEventListener('click', () => executar(no, acao));
  return no;
}

function mensagem(texto = '', erro = false) {
  const status = $('#status');
  status.textContent = texto;
  status.hidden = !texto;
  status.classList.toggle('erro', erro);
}

async function executar(botaoAcao, acao) {
  if (botaoAcao) botaoAcao.disabled = true;
  mensagem('Carregando…');
  try {
    mensagem(await acao());
  } catch (erro) {
    mensagem(erro.message, true);
    $('#status').scrollIntoView({block: 'nearest'});
  } finally {
    if (botaoAcao) botaoAcao.disabled = false;
  }
}

function csrf() {
  requisicaoCsrf ??= api('/csrf').catch(erro => {
    requisicaoCsrf = undefined;
    throw erro;
  });
  return requisicaoCsrf;
}

async function api(url, metodo = 'GET', dados) {
  const headers = {};
  if (metodo !== 'GET') {
    const token = await csrf();
    headers[token.headerName] = token.token;
  }
  if (dados !== undefined) headers['Content-Type'] = 'application/json';
  const resposta = await fetch(url, {
    method: metodo,
    headers,
    body: dados === undefined ? undefined : JSON.stringify(dados)
  });
  if (resposta.status === 401 || (resposta.redirected && new URL(resposta.url).pathname === '/assets/login.html')) {
    location.assign('/assets/login.html');
    throw new Error('Entre novamente para continuar.');
  }
  if (!resposta.ok) {
    const detalhe = await resposta.json().catch(() => ({}));
    const erro = new Error([detalhe.detail || 'A operação falhou.', ...(detalhe.campos || [])].join(' '));
    erro.status = resposta.status;
    throw erro;
  }
  if (resposta.status !== 204 && resposta.headers.get('content-type')?.includes('json')) return resposta.json();
}

function enviar(form, acao) {
  form.addEventListener('submit', evento => {
    evento.preventDefault();
    executar(evento.submitter, () => acao(new FormData(form)));
  });
}

function numero(valor) {
  return valor == null || valor === '' ? null : Number(valor);
}

function valor(numero, unidade) {
  return numero == null ? 'indisponível' : formato.format(numero) + ' ' + unidade;
}

function nutrientesTexto(nutrientes) {
  return Object.entries(macros).map(([chave, [rotulo, unidade]]) =>
    rotulo + ' ' + valor(nutrientes[chave], unidade)).join(' · ');
}

function camposNutrientes(container, metas) {
  for (const [chave, [rotulo, unidade]] of Object.entries(macros)) {
    const campo = elemento('input');
    campo.name = metas ? 'meta-' + chave : chave;
    campo.type = 'number';
    campo.min = metas && chave === 'calorias' ? '1' : '0';
    campo.max = metas ? (chave === 'calorias' ? '99999' : '99999.9') : '9999.9999';
    campo.step = metas ? (chave === 'calorias' ? '1' : '0.1') : '0.0001';
    campo.required = metas;
    const label = elemento('label', rotulo + ' (' + unidade + ')');
    label.append(campo);
    container.append(label);
  }
}
camposNutrientes($('#nutrientes-manuais'), false);
camposNutrientes($('#campos-meta'), true);

function lerNutrientes(dados, prefixo = '') {
  return Object.fromEntries(Object.keys(macros).map(chave => [chave, numero(dados.get(prefixo + chave))]));
}

function caminho(sufixo = '') {
  return '/perfis/' + perfil.id + sufixo;
}

function pagina() {
  const nome = location.hash.slice(1);
  return perfil ? (paineis.includes(nome) ? nome : 'dashboard') : 'perfil';
}

async function mostrar() {
  const versao = ++versaoPainel;
  const atual = pagina();
  for (const nome of paineis) $('#' + nome).hidden = nome !== atual;
  for (const link of document.querySelectorAll('nav a')) {
    const destino = link.hash.slice(1);
    link.toggleAttribute('hidden', !perfil && destino !== 'perfil');
    if (destino === atual) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  }
  if (!perfil && atual === 'perfil') return 'Preencha o perfil para começar.';
  if (atual === 'dashboard') await mostrarDiario(versao);
  if (atual === 'treinos') await mostrarTreinos(versao);
  if (atual === 'perfil') await mostrarPeso(versao);
}

async function mostrarDiario(versao = ++versaoPainel) {
  const dados = await api(caminho('/diario?data=' + encodeURIComponent(dia.value || perfil.hoje)));
  if (versao !== versaoPainel) return;
  const resumo = $('#resumo');
  resumo.replaceChildren();
  for (const [chave, [rotulo, unidade]] of Object.entries(macros)) {
    const item = dados[chave];
    const card = elemento('article', null, 'painel');
    card.append(elemento('h2', rotulo), elemento('p', valor(item.total, unidade) + ' consumidos', 'destaque'));
    if (item.meta != null) {
      const barra = elemento('progress');
      barra.setAttribute('aria-label', rotulo);
      barra.max = Math.max(item.meta, 1);
      barra.value = item.total || 0;
      card.append(barra, elemento('p', 'Meta de ' + valor(item.meta, unidade)));
    } else card.append(elemento('p', 'Sem meta para esta data.'));
    if (item.restante != null) {
      card.append(elemento('p', item.excedente > 0
        ? 'Excedente de ' + valor(item.excedente, unidade)
        : 'Restam ' + valor(item.restante, unidade)));
    }
    if (item.registrosSemDado) card.append(elemento('p', 'Dados parciais em ' + item.registrosSemDado + ' registro(s).', 'nota'));
    resumo.append(card);
  }
  const diario = $('#diario');
  diario.replaceChildren();
  if (!dados.registros.length) diario.append(elemento('p', 'Nenhum consumo nesta data.'));
  for (const registro of dados.registros) {
    const linha = elemento('div', null, 'linha');
    const info = elemento('div');
    info.append(elemento('strong', registro.alimento.nome), elemento('div',
      registro.horario.slice(11, 16) + ' · ' + valor(registro.gramas, 'g') + ' · ' + valor(registro.nutrientes.calorias, 'kcal')));
    const acoes = elemento('div', null, 'acoes');
    acoes.append(botao('Editar', async () => {
      configurarConsumo(registro);
      location.hash = 'alimentacao';
    }), botao('Excluir', async () => {
      if (!confirm('Excluir este consumo?')) return;
      await api(caminho('/diario/' + registro.id), 'DELETE');
      await mostrarDiario();
      return 'Consumo excluído.';
    }));
    linha.append(info, acoes);
    diario.append(linha);
  }
}

function configurarConsumo(registro, alimento) {
  consumoEmEdicao = registro?.id;
  alimentoSelecionado = registro?.alimento ?? alimento;
  $('#manual').hidden = !!alimentoSelecionado;
  $('#manual').disabled = !!alimentoSelecionado;
  $('#selecionado').hidden = !alimentoSelecionado;
  $('#selecionado').textContent = alimentoSelecionado ? alimentoSelecionado.nome + ' · ' + alimentoSelecionado.fonte : '';
  $('#titulo-consumo').textContent = registro ? 'Editar consumo' : 'Registrar consumo';
  if (!alimento || registro) {
    formConsumo.reset();
    formConsumo.elements.gramas.value = registro?.gramas ?? '';
    formConsumo.elements.horario.value = registro?.horario.slice(0, 16) ?? ((dia.value || perfil?.hoje || '') + 'T12:00');
  } else if (!formConsumo.elements.horario.value) {
    formConsumo.elements.horario.value = (dia.value || perfil.hoje) + 'T12:00';
  }
  agendarPrevia();
}

function agendarPrevia() {
  const versao = ++versaoPrevia;
  clearTimeout(timerPrevia);
  const dados = new FormData(formConsumo);
  const gramas = numero(dados.get('gramas'));
  const por100g = alimentoSelecionado?.por100g ?? lerNutrientes(dados);
  const saida = $('#previa');
  if (!gramas || gramas < 0 || !formConsumo.elements.gramas.validity.valid) {
    saida.textContent = '';
    return;
  }
  saida.textContent = 'Calculando porção…';
  timerPrevia = setTimeout(async () => {
    try {
      const previa = await api('/api/alimentos/previa', 'POST', {por100g, gramas});
      if (versao === versaoPrevia) saida.textContent = nutrientesTexto(previa);
    } catch (erro) {
      if (versao === versaoPrevia) saida.textContent = erro.message;
    }
  }, 250);
}

enviar($('#busca'), async dados => {
  const resultados = $('#resultados');
  resultados.textContent = 'Buscando…';
  try {
    const alimentos = await api('/api/alimentos?q=' + encodeURIComponent(dados.get('q')));
    resultados.replaceChildren();
    if (!alimentos.length) resultados.append(elemento('p', 'Sem resultados. Experimente outro termo ou use a entrada manual.'));
    for (const alimento of alimentos) {
      const item = elemento('div', null, 'linha');
      const info = elemento('div');
      info.append(elemento('strong', alimento.nome), elemento('div', nutrientesTexto(alimento.por100g)));
      item.append(info, botao('Selecionar', async () => {
        configurarConsumo(null, alimento);
        formConsumo.elements.gramas.focus();
      }));
      resultados.append(item);
    }
  } catch (erro) {
    resultados.textContent = erro.message;
    throw erro;
  }
});

enviar(formConsumo, async dados => {
  const consumo = {gramas: numero(dados.get('gramas')), horario: dados.get('horario')};
  const alimento = alimentoSelecionado ?? {nome: dados.get('nome'), por100g: lerNutrientes(dados)};
  await (consumoEmEdicao ? api(caminho('/diario/' + consumoEmEdicao), 'PUT', consumo)
    : api(caminho('/diario'), 'POST', {alimento, consumo}));
  dia.value = consumo.horario.slice(0, 10);
  configurarConsumo();
  mensagemPainel = 'Consumo salvo.';
  location.hash = 'dashboard';
});

function campo(rotulo, nome, valorInicial, opcoes) {
  const label = elemento('label', rotulo);
  const input = elemento('input');
  Object.assign(input, {name: nome, ...opcoes});
  input.value = valorInicial ?? '';
  label.append(input);
  return label;
}

function adicionarSerie(container, serie = {}) {
  const linha = elemento('div', null, 'campos serie');
  linha.append(campo('Repetições', 'repeticoes', serie.repeticoes ?? 10,
    {type: 'number', required: true, min: 1, max: 1000}), campo('Carga (kg, opcional)', 'cargaKg', serie.cargaKg,
    {type: 'number', step: '0.01', min: 0, max: 99999.99}),
  botao('Remover série', async () => {
    if (container.children.length === 1) throw new Error('O exercício precisa de pelo menos uma série.');
    linha.remove();
  }));
  container.append(linha);
}

function adicionarExercicio(exercicio = {}, focar = false) {
  const container = $('#exercicios');
  if (container.children.length >= 50) throw new Error('Limite de 50 exercícios por sessão.');
  const bloco = elemento('fieldset', null, 'exercicio');
  const campos = elemento('div', null, 'campos');
  campos.append(campo('Nome do exercício', 'exercicio', exercicio.nome, {required: true, maxLength: 120}),
    campo('Descanso (segundos)', 'descanso', exercicio.descansoSegundos ?? 60,
      {type: 'number', required: true, min: 0, max: 3600}));
  const series = elemento('div');
  for (const serie of exercicio.series ?? [{}]) adicionarSerie(series, serie);
  const acoes = elemento('div', null, 'acoes');
  acoes.append(botao('Adicionar série', async () => {
    if (series.children.length >= 50) throw new Error('Limite de 50 séries por exercício.');
    adicionarSerie(series);
    $('input', series.lastElementChild).focus();
  }), botao('Remover exercício', async () => {
    if (container.children.length === 1) throw new Error('A sessão precisa de pelo menos um exercício.');
    bloco.remove();
  }));
  bloco.append(elemento('legend', 'Exercício'), campos, series, acoes);
  container.append(bloco);
  if (focar) $('input', campos).focus();
}

function configurarTreino(registro) {
  treinoEmEdicao = registro?.id;
  formTreino.reset();
  formTreino.elements.nome.value = registro?.nome ?? '';
  formTreino.elements.data.value = registro?.data ?? perfil?.hoje ?? '';
  $('#titulo-treino').textContent = registro ? 'Editar sessão' : 'Nova sessão';
  $('#exercicios').replaceChildren();
  for (const exercicio of registro?.exercicios ?? [{}]) adicionarExercicio(exercicio);
}

async function mostrarTreinos(versao = ++versaoPainel) {
  const filtro = $('#filtro-treino').value;
  const treinos = await api(caminho('/treinos' + (filtro ? '?data=' + encodeURIComponent(filtro) : '')));
  if (versao !== versaoPainel) return;
  const historico = $('#historico-treinos');
  historico.replaceChildren();
  if (!treinos.length) historico.append(elemento('p', 'Nenhum treino encontrado.'));
  for (const registro of treinos) {
    const item = elemento('div', null, 'linha');
    const detalhes = elemento('div');
    detalhes.append(elemento('strong', registro.nome), elemento('div', dataPt(registro.data)));
    const lista = elemento('ul');
    for (const exercicio of registro.exercicios) {
      const series = exercicio.series.map(serie => serie.repeticoes + ' rep.'
        + (serie.cargaKg == null ? '' : ' (' + valor(serie.cargaKg, 'kg') + ')')).join(', ');
      lista.append(elemento('li', exercicio.nome + ' · ' + series + ' · descanso ' + exercicio.descansoSegundos + ' s'));
    }
    detalhes.append(lista);
    const acoes = elemento('div', null, 'acoes');
    acoes.append(botao('Editar', async () => {
      configurarTreino(registro);
      formTreino.elements.nome.focus();
    }), botao('Excluir', async () => {
      if (!confirm('Excluir esta sessão?')) return;
      await api(caminho('/treinos/' + registro.id), 'DELETE');
      await mostrarTreinos();
      return 'Sessão excluída.';
    }));
    item.append(detalhes, acoes);
    historico.append(item);
  }
}

enviar(formTreino, async dados => {
  const exercicios = [...document.querySelectorAll('.exercicio')].map(bloco => ({
    nome: $('[name=exercicio]', bloco).value,
    descansoSegundos: numero($('[name=descanso]', bloco).value),
    series: [...bloco.querySelectorAll('.serie')].map(serie => ({
      repeticoes: numero($('[name=repeticoes]', serie).value),
      cargaKg: numero($('[name=cargaKg]', serie).value)
    }))
  }));
  const pedido = {nome: dados.get('nome'), data: dados.get('data'), exercicios};
  await (treinoEmEdicao ? api(caminho('/treinos/' + treinoEmEdicao), 'PUT', pedido)
    : api(caminho('/treinos'), 'POST', pedido));
  if ($('#filtro-treino').value) $('#filtro-treino').value = pedido.data;
  configurarTreino();
  await mostrarTreinos();
  return 'Sessão salva.';
});

function dataPt(data) {
  return data.split('-').reverse().join('/');
}

function atualizarFormPerfil() {
  if (!perfil) return;
  for (const campo of formPerfil.elements) {
    if (campo.name && Object.hasOwn(perfil, campo.name)) campo.value = perfil[campo.name] ?? '';
  }
  for (const chave of Object.keys(macros)) {
    formPerfil.elements['meta-' + chave].value = (perfil.metaPersonalizada ?? perfil.meta)?.[chave] ?? '';
  }
  $('#personalizar').checked = !!perfil.metaPersonalizada;
  mostrarMetas();
  $('#estimativa').textContent = !perfil.parametroTmb ? 'Escolha o parâmetro fisiológico para calcular suas metas.'
    : 'TMB estimada ' + valor(perfil.tmb, 'kcal') + ' · GET estimado ' + valor(perfil.get, 'kcal')
      + ' · Meta ' + nutrientesTexto(perfil.meta);
  $('#pesagem').hidden = false;
  $('#pesagem').elements.data.value ||= perfil.hoje;
  $('#pesagem').elements.data.max = perfil.hoje;
}

function mostrarMetas() {
  const ativa = $('#personalizar').checked;
  $('#metas').hidden = !ativa;
  $('#metas').disabled = !ativa;
}

async function mostrarPeso(versao = ++versaoPainel) {
  const pesos = await api(caminho('/pesos'));
  if (versao !== versaoPainel) return;
  $('#historico-peso').replaceChildren(...(pesos.length ? pesos.map(pesagem =>
    elemento('li', dataPt(pesagem.data) + ' · ' + valor(pesagem.peso, 'kg')))
    : [elemento('li', 'Nenhuma pesagem registrada.')]));
}

enviar(formPerfil, async dados => {
  const pedido = Object.fromEntries(dados);
  for (const chave of ['altura', 'pesoAtual', 'percentualObjetivo']) pedido[chave] = numero(pedido[chave]);
  pedido.sexo ||= null;
  pedido.metaPersonalizada = $('#personalizar').checked ? lerNutrientes(dados, 'meta-') : null;
  const novo = !perfil;
  perfil = await (novo ? api('/perfis', 'POST', pedido) : api(caminho(), 'PUT', pedido));
  atualizarFormPerfil();
  if (novo) {
    dia.value = perfil.hoje;
    configurarConsumo();
    configurarTreino();
  }
  await mostrar();
  return 'Perfil salvo.';
});

enviar($('#pesagem'), async dados => {
  await api(caminho('/pesos'), 'POST', {peso: numero(dados.get('peso')), data: dados.get('data')});
  perfil = await api('/perfis/me');
  atualizarFormPerfil();
  $('#pesagem').elements.peso.value = '';
  await mostrarPeso();
  return 'Pesagem registrada.';
});

$('#sair').addEventListener('click', evento => executar(evento.currentTarget, async () => {
  const token = await csrf();
  const form = elemento('form');
  form.method = 'post';
  form.action = '/logout';
  const campo = elemento('input');
  Object.assign(campo, {name: token.parameterName, value: token.token, type: 'hidden'});
  form.append(campo);
  document.body.append(form);
  form.submit();
}));
$('#personalizar').addEventListener('change', mostrarMetas);
formConsumo.addEventListener('input', agendarPrevia);
$('#limpar-consumo').addEventListener('click', () => configurarConsumo());
$('#limpar-treino').addEventListener('click', () => configurarTreino());
$('#novo-exercicio').addEventListener('click', evento => executar(evento.currentTarget, async () => adicionarExercicio({}, true)));
dia.addEventListener('change', () => executar(null, async () => {
  if (!consumoEmEdicao && !alimentoSelecionado && !formConsumo.elements.gramas.value) {
    formConsumo.elements.horario.value = (dia.value || perfil.hoje) + 'T12:00';
  }
  await mostrarDiario();
}));
$('#filtro-treino').addEventListener('change', () => executar(null, () => mostrarTreinos()));

await executar(null, async () => {
  try {
    perfil = await api('/perfis/me');
  } catch (erro) {
    if (erro.status !== 404) throw erro;
  }
  if (perfil) dia.value = perfil.hoje;
  atualizarFormPerfil();
  configurarConsumo();
  configurarTreino();
  window.addEventListener('hashchange', () => executar(null, async () => {
    const aviso = mensagemPainel;
    mensagemPainel = undefined;
    return (await mostrar()) ?? aviso;
  }));
  return mostrar();
});
