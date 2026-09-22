/* Aula.net — utilitários compartilhados do front-end (JavaScript puro). */
const Api = (() => {
    const CHAVE_TOKEN = 'aulanet.token';
    const CHAVE_USUARIO = 'aulanet.usuario';

    function token() { return localStorage.getItem(CHAVE_TOKEN); }

    function usuario() {
        try { return JSON.parse(localStorage.getItem(CHAVE_USUARIO)); } catch { return null; }
    }

    function salvarSessao(resp) {
        localStorage.setItem(CHAVE_TOKEN, resp.token);
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify(resp.usuario));
    }

    function sair() {
        localStorage.removeItem(CHAVE_TOKEN);
        localStorage.removeItem(CHAVE_USUARIO);
        window.location.href = 'index.html';
    }

    /** fetch com JWT; lança Error com a mensagem do back-end. */
    async function req(metodo, url, corpo) {
        const headers = { 'Accept': 'application/json' };
        if (corpo !== undefined) headers['Content-Type'] = 'application/json';
        const t = token();
        if (t) headers['Authorization'] = 'Bearer ' + t;
        const resp = await fetch(url, { method: metodo, headers, body: corpo !== undefined ? JSON.stringify(corpo) : undefined });
        if (resp.status === 401 && t) {
            localStorage.removeItem(CHAVE_TOKEN);
            localStorage.removeItem(CHAVE_USUARIO);
            window.location.href = 'login.html?expirado=1';
            throw new Error('Sessão expirada.');
        }
        if (resp.status === 204) return null;
        const dados = await resp.json().catch(() => null);
        if (!resp.ok) throw new Error((dados && (dados.erro || dados.message)) || ('Erro ' + resp.status));
        return dados;
    }

    /** Página exige login (opcionalmente um papel específico). */
    function exigir(...papeis) {
        const u = usuario();
        if (!token() || !u) { window.location.href = 'login.html'; return null; }
        if (papeis.length && !papeis.includes(u.papel)) { window.location.href = paginaInicial(u.papel); return null; }
        return u;
    }

    function paginaInicial(papel) {
        return { ALUNO: 'aluno.html', PROFESSOR: 'painel-professor.html', ADMIN: 'admin.html' }[papel] || 'index.html';
    }

    return {
        token, usuario, salvarSessao, sair, exigir, paginaInicial,
        get: (u) => req('GET', u),
        post: (u, c) => req('POST', u, c === undefined ? {} : c),
        put: (u, c) => req('PUT', u, c),
        patch: (u, c) => req('PATCH', u, c === undefined ? {} : c),
        del: (u) => req('DELETE', u),
    };
})();

/* ---------- formatação ---------- */
const fmt = {
    moeda: (v) => Number(v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }),
    data: (iso) => iso ? new Date(iso + 'T00:00:00').toLocaleDateString('pt-BR') : '-',
    hora: (h) => h ? h.substring(0, 5) : '-',
    pct: (v) => Number(v || 0).toLocaleString('pt-BR', { maximumFractionDigits: 2 }) + '%',
    iso: (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`,
    estrelas: (m) => m == null ? '<span class="suave pequeno">sem avaliações</span>'
        : '<span class="estrelas">' + '★'.repeat(Math.round(m)) + '☆'.repeat(5 - Math.round(m)) + '</span> ' + Number(m).toFixed(1),
    iniciais: (nome) => (nome || '?').split(' ').filter(Boolean).slice(0, 2).map(p => p[0]).join('').toUpperCase(),
    rotulo: (s) => ({
        A_VISTA: 'À vista', PARCELADO: 'Parcelado', ONLINE: 'Online', PRESENCIAL: 'Presencial',
        SOLICITADO: 'Solicitado', CONFIRMADO: 'Confirmado', CANCELADO: 'Cancelado', CONCLUIDO: 'Concluído',
        PENDENTE: 'Pendente', PAGO: 'Pago', ATRASADO: 'Atrasado', REALIZADO: 'Realizado',
        ALUNO: 'Aluno', PROFESSOR: 'Professor', ADMIN: 'Admin'
    }[s] || s),
};

function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function selo(status) { return `<span class="selo selo-${esc(status)}">${esc(fmt.rotulo(status))}</span>`; }

function toast(msg, tipo = 'ok') {
    let el = document.getElementById('toast');
    if (!el) { el = document.createElement('div'); el.id = 'toast'; document.body.appendChild(el); }
    el.className = 'toast ' + tipo;
    el.textContent = msg;
    requestAnimationFrame(() => el.classList.add('visivel'));
    clearTimeout(el._t);
    el._t = setTimeout(() => el.classList.remove('visivel'), 3500);
}

/** Executa uma ação assíncrona mostrando erro/sucesso. */
async function acao(fn, msgOk) {
    try {
        const r = await fn();
        if (msgOk) toast(msgOk, 'ok');
        return r;
    } catch (e) {
        toast(e.message, 'erro');
        throw e;
    }
}

/* ---------- barra de navegação ---------- */
function renderNav() {
    const nav = document.getElementById('nav');
    if (!nav) return;
    const u = Api.usuario();
    let links = '<a class="btn btn-leve btn-sm" href="index.html">Buscar professores</a>';
    if (u) {
        links += `<a class="btn btn-leve btn-sm" href="${Api.paginaInicial(u.papel)}">Meu painel</a>
                  <span class="nav-usuario">${esc(u.nome)} ${selo(u.papel)}</span>
                  <button class="btn btn-sec btn-sm" onclick="Api.sair()">Sair</button>`;
    } else {
        links += `<a class="btn btn-sec btn-sm" href="login.html">Entrar</a>
                  <a class="btn btn-sm" href="cadastro.html">Criar conta</a>`;
    }
    nav.innerHTML = `<div class="nav-inner"><a class="logo" href="index.html">Aula<span>.net</span></a>
                     <div class="nav-links">${links}</div></div>`;
}

/* ---------- abas ---------- */
function ativarAbas(aoTrocar) {
    const abas = document.querySelectorAll('.aba');
    abas.forEach(aba => aba.addEventListener('click', () => {
        abas.forEach(a => a.classList.remove('ativa'));
        document.querySelectorAll('.painel').forEach(p => p.classList.remove('ativo'));
        aba.classList.add('ativa');
        document.getElementById(aba.dataset.alvo).classList.add('ativo');
        if (aoTrocar) aoTrocar(aba.dataset.alvo);
    }));
}

function abrirModal(id) { document.getElementById(id).classList.add('aberto'); }
function fecharModal(id) { document.getElementById(id).classList.remove('aberto'); }

document.addEventListener('DOMContentLoaded', renderNav);
