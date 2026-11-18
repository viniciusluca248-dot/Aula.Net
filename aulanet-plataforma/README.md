# Aula.net — Plataforma web de aulas particulares

Plataforma para contratação direta de aulas particulares entre alunos e professores autônomos
(TCC — Análise e Desenvolvimento de Sistemas, UEMA — Lucas Vinicius Sousa Costa).

**Tecnologias:** Java 17 · Spring Boot 3.3 · Spring Security + JWT · Spring Data JPA (Hibernate) ·
Bean Validation · MySQL 8 / H2 · HTML + CSS + JavaScript puro (sem framework) · JUnit 5 + Mockito.

---

## 1. Como abrir e rodar no IntelliJ IDEA

1. Descompacte o `.zip` e, no IntelliJ, use **File → Open…** e selecione a pasta `aulanet-plataforma`
   (a que contém o `pom.xml`). Escolha **Open as Project** e aguarde o Maven baixar as dependências.
2. Confira o JDK: **File → Project Structure → SDK = 17 ou superior**.
3. Rode a classe `br.uema.aulanet.AulanetApplication` (botão ▶ ao lado do `main`),
   ou use a configuração pronta **"AulanetApplication (H2)"** no topo da janela.
4. Abra **http://localhost:8080** no navegador.

Sem IntelliJ: `./mvnw spring-boot:run` (Linux/Mac) ou `mvnw.cmd spring-boot:run` (Windows).

> Por padrão o sistema usa o banco **H2 em memória** (não precisa instalar nada) e cria dados de demonstração
> a cada inicialização. Console do banco: http://localhost:8080/h2-console
> (JDBC URL `jdbc:h2:mem:aulanetdb`, usuário `sa`, senha em branco).

### Contas de demonstração

| Papel     | E-mail              | Senha    |
|-----------|---------------------|----------|
| Admin     | admin@aulanet.com   | admin123 |
| Professor | ana@aulanet.com     | 123456   |
| Professor | carlos@aulanet.com  | 123456   |
| Professor | beatriz@aulanet.com | 123456   |
| Aluno     | joao@aulanet.com    | 123456   |
| Aluno     | maria@aulanet.com   | 123456   |

### Usando MySQL 8 (banco `aulanetdb`)

1. Tenha um MySQL rodando em `localhost:3306`.
2. (Opcional) execute `database/aulanetdb.sql` no MySQL Workbench — ou deixe o Hibernate criar as tabelas.
3. No IntelliJ, rode a configuração **"AulanetApplication (MySQL)"** (perfil `mysql`).
   Ajuste usuário/senha nas variáveis `DB_USER` / `DB_PASSWORD` da configuração
   (padrão `root`/`root`) ou em `src/main/resources/application-mysql.properties`.

### Testes

Clique com o botão direito em `src/test/java` → **Run 'All Tests'**, ou `./mvnw test`.
São 43 testes. Além de `PerfilServiceTest` e `PerfilEBuscaIntegrationTest` (perfil, currículo, foto e busca),
`AgendamentoTest` (transições de status na entidade), `CalculadoraFinanceiraTest` (valores de referência da
monografia: Tabela 1, taxa efetiva anual, carência, multa e mora), `FinanceiroServiceTest` (pagamento, encargos,
repasse), `AgendamentoServiceTest` (fluxo e conflitos de horário) e `FluxoCompletoIntegrationTest` (do cadastro
de aluno e professor ao repasse, passando por 401/403 por papel).

---

## 2. Estrutura do projeto

```
aulanet-plataforma/
├── pom.xml
├── database/aulanetdb.sql              script MySQL do modelo (Workbench)
└── src/main/
    ├── java/br/uema/aulanet/
    │   ├── AulanetApplication.java
    │   ├── config/        segurança, parâmetros, dados de demonstração, tarefas
    │   ├── controller/    API REST (/api/...)
    │   ├── dto/           objetos de entrada/saída (records)
    │   ├── exception/     erros de negócio + tratamento global
    │   ├── model/         entidades JPA (tabelas do modelo aulanetdb)
    │   ├── repository/    Spring Data JPA
    │   ├── security/      JWT (geração, filtro, usuário logado)
    │   └── service/       regras de negócio
    └── resources/
        ├── application.properties          H2 (padrão) + parâmetros financeiros
        ├── application-mysql.properties    perfil MySQL
        └── static/                          front-end (HTML/CSS/JS)
            ├── index.html                  busca de professores
            ├── professor.html              perfil público + solicitar aula
            ├── login.html / cadastro.html
            ├── aluno.html                  aulas, pagamentos (parcelamento), avaliação
            ├── painel-professor.html       solicitações, agenda semanal, serviços, financeiro, histórico
            ├── admin.html                  relatório global, repasses, comissões, usuários, disciplinas
            ├── css/style.css
            └── js/api.js                   cliente da API com JWT
```

As tabelas seguem o modelo do MySQL Workbench (`usuario`, `disciplina`, `oferta_aula`, `agendamento`,
`avaliacao`, `pagamento`, `parcela`, `repasse`). Foram acrescentadas a coluna `usuario.bio` e duas tabelas
1 : 1 com `usuario`, previstas como trabalho futuro na seção 5.2 da monografia:

- `perfil_usuario`: cidade, UF, formação, experiência, link do currículo (Lattes/LinkedIn) e a versão da foto;
- `foto_usuario`: a foto de perfil (JPEG, PNG ou WebP, até 2 MB), separada para não pesar nas listagens.

### Perfil e busca

- **Meu perfil** (`perfil.html`): foto (recortada em quadrado e reduzida a 512 px no navegador antes do envio),
  dados de contato, apresentação e, para o professor, formação, experiência e link do currículo. Um indicador
  mostra quanto do perfil está completo e o que falta; o painel do professor lembra quando está incompleto.
- **Busca** (`index.html`): um cartão por professor, com foto, cidade, nota, aulas dadas, disciplinas e preço a
  partir de. Busca por nome ou disciplina sem diferenciar acentos, filtros de modalidade, cidade, preço e nota,
  e ordenação. Os filtros ficam no endereço da página, então o link da busca pode ser compartilhado.
- **Perfil público** (`professor.html`): capa, foto, apresentação, formação e experiência em linha do tempo,
  serviços, distribuição das notas e o cartão de agendamento ao lado.
- A foto é validada pelos primeiros bytes do arquivo (não pelo nome), e o link do currículo só aceita http/https.

---

## 3. Requisitos atendidos (numeração da monografia, seção 5.1)

| Requisito | Onde |
|-----------|------|
| RF01 Cadastrar usuário como aluno ou professor | `cadastro.html`, `POST /api/auth/cadastro`. ADMIN só é criado por outro admin (`POST /api/admin/usuarios`). |
| RF02 Autenticar com JWT de 8 h | `POST /api/auth/login`, `JwtService` (`aulanet.jwt.expiracao-horas=8`) |
| RF03 Restringir cada operação ao papel | `SecurityConfig` + `@PreAuthorize` nos controllers + verificação de propriedade nos serviços |
| RF04 Manter ofertas de aula | aba "Meus serviços", `/api/ofertas` |
| RF05 Catálogo público por disciplina, modalidade e preço | `index.html`, `GET /api/ofertas?disciplinaId=&modalidade=&precoMin=&precoMax=` |
| RF06 Perfil público do professor com avaliações | `professor.html`, `GET /api/professores/{id}` |
| RF07 Solicitar aula com verificação de conflito de horário | `professor.html` (5 passos), `POST /api/agendamentos`; o aluno não pode ter duas aulas no mesmo horário |
| RF08 Confirmar ou recusar solicitação | aba "Solicitações", `PATCH /api/agendamentos/{id}/confirmar\|recusar`; transições em `Agendamento` |
| RF09 Concluir aula, gerando pagamento e repasse | aba "Aulas", `PATCH /api/agendamentos/{id}/concluir`, na mesma transação |
| RF10 Agenda do aluno e do professor | "Minhas aulas" (aluno) e "Agenda semanal" (professor) |
| RF11 Simular a Tabela Price com taxa efetiva anual e total | `GET /api/pagamentos/simulacao`, modal "Alterar forma de pagamento" |
| RF12 Pagamento à vista ou parcelado e quitação de parcelas | `POST /api/pagamentos/{id}/parcelamento`, `POST /api/parcelas/{id}/pagar` |
| RF13 Multa e juros de mora diários | `@Scheduled` em `FinanceiroService.processarAtrasos` (1h) + ao iniciar + botão no admin |
| RF14 Repasse = bruto − comissão da oferta | gerado na conclusão; o admin registra a transferência só depois da quitação |
| RF15 Painel financeiro do professor | aba "Financeiro", `GET /api/financeiro/painel` |
| RF16 Avaliar aula concluída uma única vez | `aluno.html`, `POST /api/avaliacoes` |
| RF17 Gerenciar usuários, disciplinas e comissões | `admin.html`; disciplina com ofertas não pode ser excluída |
| RF18 Relatórios globais e registro de repasses | `admin.html`, `GET /api/admin/relatorio`, `PATCH /api/admin/repasses/{id}/realizar` |
| RNF01 Interface responsiva | `css/style.css` (layout em grade, testado em 390 px) |
| RNF02 Solicitação em até cinco passos | formulário de `professor.html` |
| RNF03 BCrypt com custo ≥ 10 | `BCryptPasswordEncoder(12)` |
| RNF04 401 sem token e 403 sem permissão | `SecurityConfig` |
| RNF05 BigDecimal arredondado a centavos | `CalculadoraFinanceira` e entidades |

### Regras financeiras (configuráveis em `application.properties`)

- **Valor da aula** = valor/hora × duração.
- **Parcelamento** (até 12x): Tabela Price, `PMT = PV · [i(1+i)^n] / [(1+i)^n − 1]`, com `i = 2% a.m.`
  (taxa efetiva anual de 26,82%). A carência de juros (`parcelas-sem-juros`) define quantas parcelas saem sem
  juros; o padrão é 1 (só à vista). A última parcela absorve a diferença de arredondamento. Só a prestação e o
  vencimento são gravados; a decomposição em juros e amortização é recalculada pela calculadora.
  O parcelamento pode ser alterado enquanto nenhuma parcela tiver sido paga nem estiver em atraso.
- **Atraso**: multa de 2% (`valor_multa`, aplicada uma vez) e juros de mora de 1% ao mês pro rata die
  (`valor_juros`, recalculados a cada dia).
- **Repasse**: comissão = valor da aula × % de comissão da oferta; líquido = bruto − comissão.
  Juros e multas não entram no repasse. O repasse fica liberado quando o aluno quita o pagamento e passa a
  REALIZADO quando o administrador registra a transferência.
- Não há gateway de pagamento real (fora do escopo): o pagamento de cada parcela é registrado no sistema.

---

## 4. Principais endpoints

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/auth/cadastro`, `/api/auth/login` | público |
| GET | `/api/disciplinas`, `/api/ofertas`, `/api/professores`, `/api/professores/{id}`, `/api/usuarios/{id}/foto` | público |
| PUT/DELETE | `/api/usuarios/me/foto` (multipart, campo `foto`) | autenticado |
| GET/PUT | `/api/usuarios/me` | autenticado |
| GET/POST/PUT/DELETE | `/api/ofertas/minhas`, `/api/ofertas/{id}` | PROFESSOR |
| POST | `/api/agendamentos` · GET `/api/agendamentos/meus` · PATCH `/{id}/cancelar` | ALUNO |
| GET | `/api/agendamentos/professor`, `/professor/semana`, `/professor/historico` | PROFESSOR |
| PATCH | `/api/agendamentos/{id}/confirmar\|recusar\|concluir` | PROFESSOR |
| GET | `/api/pagamentos/simulacao?valor=&parcelas=` | autenticado |
| GET/POST | `/api/pagamentos/meus`, `/api/pagamentos/{id}/parcelamento`, `/api/parcelas/{id}/pagar` | ALUNO |
| POST | `/api/avaliacoes` | ALUNO |
| GET | `/api/financeiro/painel?ano=&mes=` | PROFESSOR |
| GET/POST/PATCH/DELETE | `/api/admin/**` (relatorio, usuarios, repasses, ofertas/comissao, disciplinas, processar-atrasos) | ADMIN |

Em produção, defina a variável de ambiente `AULANET_JWT_SECRET` (mínimo de 32 caracteres) e use HTTPS.
