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
São 24 testes: unitários dos serviços (cálculo financeiro, agendamento, multas, repasses) e
de integração da API (autenticação, 401/403 por papel, fluxo completo solicitar → confirmar →
concluir → parcelar → pagar → avaliar).

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
`avaliacao`, `pagamento`, `parcela`, `repasse`). Foi acrescentada a coluna `usuario.bio` para o perfil
público do professor (RF05).

---

## 3. Requisitos atendidos

| Requisito | Onde |
|-----------|------|
| RF01 Cadastro (nome, e-mail, senha, CPF, papel) | `cadastro.html`, `POST /api/auth/cadastro`. Cadastro público cria ALUNO/PROFESSOR; ADMIN só é criado por outro admin (`POST /api/admin/usuarios`). |
| RF02 Login com JWT de 8 h | `POST /api/auth/login`, `JwtService` (`aulanet.jwt.expiracao-horas=8`) |
| RF03 Acesso por papel | `SecurityConfig` + `@PreAuthorize` nos controllers |
| RF04 Professor cadastra serviços | aba "Meus serviços", `/api/ofertas` |
| RF05 Perfil público (bio, disciplinas, média, modalidade) | `professor.html`, `GET /api/professores/{id}` |
| RF06 Busca por disciplina, modalidade e faixa de preço | `index.html`, `GET /api/ofertas?disciplinaId=&modalidade=&precoMin=&precoMax=` |
| RF07 Aluno solicita agendamento | `professor.html` (5 passos), `POST /api/agendamentos` |
| RF08 Professor confirma/recusa | aba "Solicitações", `PATCH /api/agendamentos/{id}/confirmar\|recusar` |
| RF09 Concluir aula gera pagamento | aba "Aulas", `PATCH /api/agendamentos/{id}/concluir` → `FinanceiroService.gerarPagamento` |
| RF10 Calendário semanal | aba "Agenda semanal", `GET /api/agendamentos/professor/semana` |
| RF11 Pagamento à vista + repasse | gerado na conclusão da aula (1 parcela) |
| RF12 Parcelamento com juros compostos | aluno → "Alterar forma de pagamento" (valor por parcela em tempo real); Tabela Price, `aulanet.financeiro.taxa-juros-mensal` |
| RF13 Multa por atraso via tarefa diária | `@Scheduled` em `FinanceiroService.processarAtrasos` (00:05) + execução ao iniciar + botão no admin |
| RF14 Repasse = bruto − comissão (PENDENTE/REALIZADO) | criado com a aula; vira REALIZADO automaticamente ao quitar o pagamento, ou manualmente pelo admin |
| RF15 Painel financeiro do professor | aba "Financeiro", `GET /api/financeiro/painel` |
| RF16 Avaliação 1–5 após aula concluída | `aluno.html`, `POST /api/avaliacoes` |
| RF17 Histórico por período com totalizadores | aba "Histórico", `GET /api/agendamentos/professor/historico` |
| RF18 Relatórios globais do admin | `admin.html`, `GET /api/admin/relatorio` |
| RNF01 | consultas com `join fetch` (sem N+1) e índices nas colunas de busca |
| RNF03 | BCrypt custo 10, Bean Validation, JPA com consultas parametrizadas, escape de HTML no front-end |
| RNF04 | API stateless; 401 sem token válido e 403 para papel insuficiente |
| RNF05 | testes JUnit 5 + Mockito nos serviços; valores monetários sempre em `BigDecimal` |

### Regras financeiras (configuráveis em `application.properties`)

- **Valor da aula** = valor/hora × duração.
- **Parcelamento** (2 a 12x): Tabela Price, `PMT = PV·i / (1 − (1+i)^−n)`, com `i = 1,99% a.m.`.
  À vista não tem juros. A última parcela absorve a diferença de arredondamento.
  O parcelamento pode ser alterado enquanto nenhuma parcela tiver sido paga.
- **Atraso**: multa de 2% + mora de 0,033% ao dia sobre o valor da parcela, recalculadas diariamente.
- **Repasse**: comissão = valor da aula × % de comissão da oferta; líquido = bruto − comissão.
  Juros e multas não entram no repasse.
- Não há gateway de pagamento real (fora do escopo): o pagamento de cada parcela é registrado no sistema.

---

## 4. Principais endpoints

| Método | Rota | Acesso |
|--------|------|--------|
| POST | `/api/auth/cadastro`, `/api/auth/login` | público |
| GET | `/api/disciplinas`, `/api/ofertas`, `/api/professores/{id}` | público |
| GET/PUT | `/api/usuarios/me` | autenticado |
| GET/POST/PUT/DELETE | `/api/ofertas/minhas`, `/api/ofertas/{id}` | PROFESSOR |
| POST | `/api/agendamentos` · GET `/api/agendamentos/meus` · PATCH `/{id}/cancelar` | ALUNO |
| GET | `/api/agendamentos/professor`, `/professor/semana`, `/professor/historico` | PROFESSOR |
| PATCH | `/api/agendamentos/{id}/confirmar\|recusar\|concluir` | PROFESSOR |
| GET | `/api/pagamentos/simulacao?valor=&parcelas=` | autenticado |
| GET/POST | `/api/pagamentos/meus`, `/api/pagamentos/{id}/parcelamento`, `/api/parcelas/{id}/pagar` | ALUNO |
| POST | `/api/avaliacoes` | ALUNO |
| GET | `/api/financeiro/painel?ano=&mes=` | PROFESSOR |
| GET/POST/PATCH | `/api/admin/**` (relatorio, usuarios, repasses, ofertas/comissao, disciplinas, processar-atrasos) | ADMIN |

Em produção, defina a variável de ambiente `AULANET_JWT_SECRET` (mínimo de 32 caracteres) e use HTTPS.
