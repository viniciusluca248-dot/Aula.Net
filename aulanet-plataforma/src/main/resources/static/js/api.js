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

    /** Atualiza os dados guardados do usuário logado (nome, foto) após editar o perfil. */
    function atualizarUsuario(dados) {
        const u = usuario();
        if (!u) return;
        localStorage.setItem(CHAVE_USUARIO, JSON.stringify({ ...u, nome: dados.nome, fotoUrl: dados.fotoUrl }));
        renderNav();
    }

    /** fetch com JWT; lança Error com a mensagem do back-end. Um FormData é enviado como multipart. */
    async function req(metodo, url, corpo) {
        const headers = { 'Accept': 'application/json' };
        const multipart = corpo instanceof FormData;
        if (corpo !== undefined && !multipart) headers['Content-Type'] = 'application/json';
        const t = token();
        if (t) headers['Authorization'] = 'Bearer ' + t;
        const body = corpo === undefined ? undefined : (multipart ? corpo : JSON.stringify(corpo));
        const resp = await fetch(url, { method: metodo, headers, body });
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
        token, usuario, salvarSessao, atualizarUsuario, sair, exigir, paginaInicial,
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
    estrelas: (m) => m == null ? '<span class="suave pequeno">novo na plataforma</span>'
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

/* ---------- ícones (SVG em linha, sem dependências) ---------- */
const ICONES = {
    local: '<path d="M12 21s-7-6.1-7-11.5a7 7 0 1 1 14 0C19 14.9 12 21 12 21z"/><circle cx="12" cy="9.5" r="2.5"/>',
    estrela: '<path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2L12 17.3 6.4 20.2l1.1-6.2L3 9.6l6.2-.9z"/>',
    formacao: '<path d="M2 9l10-5 10 5-10 5z"/><path d="M6 11v5c0 1.7 2.7 3 6 3s6-1.3 6-3v-5"/>',
    experiencia: '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/>',
    aula: '<path d="M4 5h16v11H4z"/><path d="M8 20h8M12 16v4"/>',
    busca: '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>',
    camera: '<path d="M4 8h3l2-3h6l2 3h3v11H4z"/><circle cx="12" cy="13" r="3.5"/>',
    link: '<path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1"/><path d="M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1"/>',
    check: '<path d="m5 12 5 5 9-10"/>',
    pendente: '<circle cx="12" cy="12" r="8"/>',
    usuario: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7"/>',
    relogio: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
    sobre: '<path d="M4 5h16v14H4z"/><path d="M8 9h8M8 13h5"/>',
    carteira: '<rect x="3" y="6" width="18" height="13" rx="2"/><path d="M16 12h2"/>',
};
function icone(nome, extra = '') {
    return `<svg class="icone ${extra}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONES[nome] || ''}</svg>`;
}

/** Avatar: a foto do usuário ou as iniciais sobre uma cor derivada do nome (sempre a mesma para a mesma pessoa). */
function avatar(nome, fotoUrl, tamanho = '') {
    const classe = 'avatar' + (tamanho ? ' avatar-' + tamanho : '');
    if (fotoUrl) return `<img class="${classe}" src="${esc(fotoUrl)}" alt="Foto de ${esc(nome)}">`;
    let h = 0;
    for (const c of String(nome || '')) h = (h * 31 + c.charCodeAt(0)) % 360;
    return `<span class="${classe}" style="background:linear-gradient(135deg,hsl(${h} 65% 52%),hsl(${(h + 40) % 360} 70% 42%))" aria-hidden="true">${fmt.iniciais(nome)}</span>`;
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
const LOGO = '<svg class="logo-marca" viewBox="0 0 32 32" aria-hidden="true"><rect width="32" height="32" rx="9" fill="#2850d8"/><path d="M7 21 16 8l9 13" fill="none" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/><circle cx="16" cy="22" r="2.6" fill="#18b08c"/></svg>';
function renderNav() {
    const nav = document.getElementById('nav');
    if (!nav) return;
    const u = Api.usuario();
    let links = `<a class="btn btn-leve btn-sm" href="index.html">${icone('busca')} Buscar professores</a>`;
    if (u) {
        links += `<a class="btn btn-leve btn-sm" href="${Api.paginaInicial(u.papel)}">Meu painel</a>
                  <a class="nav-usuario" href="perfil.html" title="Meu perfil">${avatar(u.nome, u.fotoUrl, 'xs')}<span class="nome">${esc(u.nome.split(' ')[0])}</span></a>
                  <button class="btn btn-sec btn-sm" onclick="Api.sair()">Sair</button>`;
    } else {
        links += `<a class="btn btn-sec btn-sm" href="login.html">Entrar</a>
                  <a class="btn btn-sm" href="cadastro.html">Criar conta</a>`;
    }
    nav.innerHTML = `<div class="nav-inner"><a class="logo" href="index.html">${LOGO}<span>Aula<em>.net</em></span></a>
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
