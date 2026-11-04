# Seldon Fleet

Implementação Android em Kotlin/Compose do desafio. O app lê `app/src/main/assets/frota.json` offline, lista e filtra a frota, abre detalhes justificando o status e persiste favoritos em `SharedPreferences`.

## Executar

Abra a pasta no Android Studio e execute o módulo `app`, ou rode `./gradlew test` para validar o domínio na JVM. O APK debug pode ser gerado com `./gradlew assembleDebug`.

O domínio está em `app/src/main/java/com/seldon/fleet/domain`, sem dependência Android. A decisão de tratamento dos dados defeituosos está em [PROTOCOLO.md](PROTOCOLO.md).

O uso de assistentes de IA foi restrito ao apoio de planejamento, revisão da especificação,
implementação e validação; as decisões finais, o código e os testes foram conferidos no projeto
e no emulador.

## Prints do app

Capturas reais feitas no emulador `Seldon_AOSP_API35`:

### Lista da frota

![Lista da frota](docs/screenshots/app-list.png)

### Filtro por dispositivos críticos

![Filtro crítico](docs/screenshots/app-filter-critico.png)

### Detalhe do dispositivo

![Detalhe do dispositivo](docs/screenshots/app-detail.png)

## Requisitos consolidados

Todos os requisitos normativos do desafio estão em [REQUISITOS.md](REQUISITOS.md).

## O que faria em seguida

Eu adicionaria testes de UI para preservar o filtro na navegação e substituiria o armazenamento simples de favoritos por DataStore, caso o produto precisasse de migrações ou múltiplos perfis.

Desafio técnico de engenharia Android — **Residência Tecnológica, Módulo 1 (Núcleo Comum)**.

> Construir um app Android em Kotlin/Compose de duas telas que lê um `frota.json` local,
> lista os dispositivos classificando cada um como **OK**, **Atenção** ou **Crítico** por uma
> regra de negócio isolada em `domain/` e testável na JVM, abre o detalhe com a justificativa
> da classificação e persiste os favoritos — tratando os dados defeituosos do arquivo e
> documentando as decisões.

## Conteúdo deste repositório

| Caminho | O que é |
|---|---|
| `site/index.html` | Página de apresentação do desafio, com campo 3D, mockup das duas telas e demonstração interativa do defeito de unidade |
| `docs/desafio-tecnico.pdf` | Especificação técnica completa — 17 páginas, normativa |
| `docs/desafio-tecnico.html` | Fonte do PDF (renderizado com Chromium headless) |
| `data/frota.json` | O arquivo de entrada do desafio — 30 dispositivos, com os dois defeitos plantados |
| `tools/gen-frota.js` | Gerador do dataset, com `--seed` para produzir um arquivo distinto por candidato |

## O que o desafio mede

Sete eixos do Módulo 1, com peso proporcional à carga horária das disciplinas:
Kotlin e Android (42h), Arquitetura de Software (18h), Testes e CI (30h),
Requisitos (24h), Metodologia Científica (30h), Ética e registro de decisões (30h)
e Inteligência Artificial (36h, via trilha opcional).

O instrumento central é a **regra de classificação**: uma decisão computada que precisa morar
numa camada sem Android, ser testável em milissegundos e continuar correta diante de um arquivo
de entrada com dois defeitos plantados de propósito — unidade de energia inconsistente
(`mAh` / `mWh`) e divisor nulo em registros incompletos.

## Avaliação

100 pontos: arquitetura e fronteiras (25), correção do domínio (25), interface e navegação (20),
testes e integração contínua (15), documentação e justificativa (15). Trilha de especialização
opcional vale +20. A rubrica completa, os critérios de aceitação de cada requisito e as condições
que limitam a nota estão no PDF.

## Regerar os artefatos

```bash
node tools/gen-frota.js             # gera data/frota.json (use --seed N para variar)
node scripts/topdf.js               # gera docs/desafio-tecnico.pdf
node scripts/build-pages.js         # monta _site/ para o GitHub Pages
```

O gerador do dataset é determinístico: a mesma semente produz o mesmo arquivo byte a byte.
Seis dispositivos têm valores fixos, independentes da semente, porque aparecem com números
literais na especificação e na interface de referência. Antes de escrever o arquivo, o
gerador confere que a distribuição é exatamente 16 OK / 8 Atenção / 4 Crítico /
2 Indeterminado — as contagens exibidas nos filtros da tela de lista — e aborta se divergir.

## Publicação

- Site: https://maktheus.github.io/seldon-chalange/
- Arquivo de entrada: https://maktheus.github.io/seldon-chalange/frota.json
- Especificação: https://maktheus.github.io/seldon-chalange/desafio-tecnico.pdf

O deploy roda pelo GitHub Actions a cada push na branch default.
