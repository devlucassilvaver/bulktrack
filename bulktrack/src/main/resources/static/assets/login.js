const mensagem = document.querySelector('#mensagem');
const parametros = new URLSearchParams(location.search);
if (parametros.has('error')) {
  mensagem.textContent = 'Usuário ou senha inválidos.';
  mensagem.classList.add('erro');
}
if (parametros.has('logout')) mensagem.textContent = 'Sessão encerrada.';
try {
  const resposta = await fetch('/csrf');
  if (!resposta.ok) throw new Error();
  const token = await resposta.json();
  const campo = document.querySelector('input[type=hidden]');
  campo.name = token.parameterName;
  campo.value = token.token;
  document.querySelector('button').disabled = false;
} catch {
  mensagem.textContent = 'Falha ao iniciar a sessão. Recarregue a página.';
  mensagem.classList.add('erro');
}
