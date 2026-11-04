# Seldon Fleet — Requisitos do desafio

Documento consolidado a partir da especificação técnica `docs/desafio-tecnico.pdf`.

## 1. Objetivo

Construir um aplicativo Android em Kotlin/Jetpack Compose que:

- leia uma frota local a partir de `assets/frota.json`;
- classifique cada dispositivo como `OK`, `ATENCAO`, `CRITICO` ou `INDETERMINADO`;
- explique o motivo de cada classificação;
- permita consultar detalhes do dispositivo;
- persista os favoritos;
- trate dados defeituosos sem gerar valores inválidos ou encerrar o app.

## 2. Escopo funcional

### Tela de lista da frota

A primeira tela deve:

- exibir todos os dispositivos presentes no arquivo;
- mostrar identificador;
- mostrar modelo;
- mostrar nível de bateria;
- mostrar um chip visualmente diferente para cada estado;
- oferecer filtro por status;
- mostrar a contagem de dispositivos de cada status;
- permitir marcar e desmarcar favoritos.

Estados de domínio:

- `OK` — saudável;
- `ATENCAO` — sob observação;
- `CRITICO` — exige intervenção;
- `INDETERMINADO` — dados insuficientes para decidir.

A interface pode exibir `INDETERMINADO` como `SEM DADOS`.

### Tela de detalhe

A segunda tela deve ser alcançada a partir da lista e exibir:

- identificador;
- modelo;
- SoC;
- memória RAM;
- nível de API Android;
- nível atual da bateria;
- capacidade da bateria;
- consumo normalizado;
- tempo de tela;
- taxa de frames perdidos;
- quantidade de falhas recentes;
- último contato;
- status calculado;
- justificativa textual da classificação.

A justificativa deve citar o sinal e o valor responsável pela decisão. Exemplo:

> Crítico: dreno de 13,4 %/h, acima do limiar de 12 %/h.

O botão voltar do sistema deve retornar à lista preservando o filtro aplicado.

### Persistência

- Favoritos devem sobreviver ao encerramento completo do aplicativo.
- Somente os favoritos precisam ser persistidos.
- `frota.json` permanece como fonte da verdade e deve ser relido a cada abertura.

## 3. Ingestão dos dados

O aplicativo deve ler o arquivo local:

```text
app/src/main/assets/frota.json
```

Não deve existir acesso à rede para carregar a frota.

O objeto raiz contém metadados e uma lista `devices`:

```json
{
  "schemaVersion": "1.0",
  "exportedAt": "2026-10-20T03:12:44Z",
  "devices": []
}
```

Campos de cada dispositivo:

| Campo | Tipo | Unidade | Observação |
|---|---|---|---|
| `id` | String | — | Presente e único |
| `model` | String | — | Presente |
| `soc` | String | — | Presente |
| `tier` | String | — | `low`, `mid` ou `high` |
| `ramMb` | Int | MB | Maior que zero |
| `androidApi` | Int | nível de API | — |
| `batteryCapacityMah` | Int | mAh | Pode ser zero ou ausente |
| `batteryLevelPct` | Int | % | De 0 a 100 |
| `screenOnMinutes` | Int | minutos | Pode ser zero |
| `energyConsumed` | Double | depende de `energyUnit` | Maior ou igual a zero |
| `energyUnit` | String | `mAh` ou `mWh` | Varia entre registros |
| `nominalVoltage` | Double | V | Necessário quando a unidade é `mWh` |
| `jankFramesPct` | Double | % | De 0 a 100 |
| `crashCount7d` | Int | contagem | Maior ou igual a zero |
| `lastSeen` | String | ISO-8601 UTC | Último contato |

## 4. Defeitos obrigatórios do arquivo

### Unidade de energia inconsistente

Alguns registros usam `mAh` e outros usam `mWh`.

Antes de qualquer comparação ou classificação, valores em `mWh` devem ser convertidos:

```text
mAh = mWh / nominalVoltage
```

Exemplo:

```text
3773 mWh / 3,85 V = 980 mAh
```

Somar ou comparar valores brutos de `mAh` e `mWh` é incorreto.

### Registros incompletos

O arquivo possui registros com:

- `batteryCapacityMah` igual a zero ou ausente;
- `screenOnMinutes` igual a zero;
- potencialmente `nominalVoltage` ausente quando a unidade é `mWh`.

O aplicativo deve escolher e documentar um tratamento explícito. Não pode:

- dividir por zero;
- exibir `NaN`;
- exibir `Infinity`;
- inventar um valor;
- encerrar inesperadamente.

Tratamento adotado neste projeto: classificar como `INDETERMINADO` e informar a causa.

## 5. Regra de classificação

### Derivadas

Após normalizar a energia para mAh:

```text
horasDeTela = screenOnMinutes / 60
drenoAbsoluto = energiaMah / horasDeTela
drenoRelativo = (drenoAbsoluto / batteryCapacityMah) * 100
```

O indicador usado na classificação é o dreno relativo, em `%/h`.

### Limiares e precedência

As regras devem ser avaliadas nesta ordem:

1. `INDETERMINADO` se qualquer uma destas condições for verdadeira:
   - capacidade menor ou igual a zero ou ausente;
   - tempo de tela menor ou igual a zero;
   - unidade `mWh` sem tensão nominal válida.
2. `CRITICO` se qualquer uma destas condições for verdadeira:
   - dreno relativo maior ou igual a `12 %/h`;
   - `crashCount7d` maior ou igual a `3`;
   - `jankFramesPct` maior ou igual a `15`.
3. `ATENCAO` se qualquer uma destas condições for verdadeira:
   - dreno relativo maior ou igual a `7 %/h`;
   - `crashCount7d` maior ou igual a `1`;
   - `jankFramesPct` maior ou igual a `8`.
4. `OK` se nenhuma regra anterior for satisfeita.

Um sinal crítico deve vencer qualquer sinal de atenção simultâneo.

## 6. Requisitos obrigatórios

### REQ-01 — Ingestão local

O app deve carregar os dispositivos de `assets/frota.json`, sem acesso à rede.

Aceitação: a lista deve aparecer em modo avião e o manifest não deve declarar a permissão `INTERNET`.

### REQ-02 — Normalização de unidades

Valores em `mWh` devem ser convertidos para `mAh` usando `nominalVoltage` antes de qualquer comparação ou classificação.

Aceitação: um registro em `mWh` e outro em `mAh` com o mesmo consumo físico devem produzir o mesmo dreno e o mesmo status em teste unitário.

### REQ-03 — Registros incompletos

Divisores nulos ou campos ausentes devem receber tratamento explícito e documentado.

Aceitação: testes para capacidade zero e tempo de tela zero devem passar sem exceção, `NaN` ou `Infinity`.

### REQ-04 — Regra no domínio

A classificação deve viver em `domain/`, em Kotlin puro, sem:

- `import android.*`;
- JSON;
- arquivo;
- `Context`;
- framework de UI.

Aceitação: a regra deve ser testável na JVM sem emulador.

### REQ-05 — Tela de lista

A tela deve listar todos os dispositivos, mostrar os dados principais, status visual e filtros com contagem.

Aceitação: a soma das contagens deve ser igual ao total do arquivo e cada filtro deve reduzir a lista coerentemente.

### REQ-06 — Tela de detalhe e justificativa

Deve existir navegação lista → detalhe, com todos os atributos relevantes e justificativa do status.

Aceitação: para um dispositivo crítico, a justificativa deve nomear o sinal e o valor responsável. O retorno deve preservar o filtro.

### REQ-07 — Favoritos persistentes

Marcar um dispositivo como favorito deve sobreviver ao encerramento e reabertura do aplicativo.

### REQ-08 — Testes unitários JVM

Os testes devem cobrir, no mínimo:

- conversão de unidade;
- capacidade nula ou ausente;
- tempo de tela igual a zero;
- valor imediatamente abaixo do limiar de atenção;
- valor imediatamente acima do limiar de atenção;
- valor imediatamente abaixo do limiar crítico;
- valor imediatamente acima do limiar crítico;
- precedência de um sinal crítico sobre um sinal de atenção.

Os testes devem falhar se os limiares forem alterados.

### REQ-09 — Integração contínua

O workflow deve executar a cada push e pull request:

- análise estática, como `lint`, `ktlint` ou `detekt`;
- testes JVM com `./gradlew test`.

Não é necessário emulador no pipeline.

### REQ-10 — Protocolo de decisões

O arquivo `PROTOCOLO.md` deve conter:

- regra de classificação adotada;
- raciocínio da regra;
- tratamento da unidade inconsistente;
- tratamento dos registros incompletos;
- ao menos uma ameaça à validade da conclusão.

Um avaliador deve conseguir prever o status de um registro lendo apenas o protocolo.

## 7. Arquitetura exigida

As dependências devem apontar para dentro:

```text
ui/    → domain/ ← data/
```

### `domain/`

- Kotlin puro;
- modelos de domínio;
- enumeração de status;
- regra de classificação;
- interfaces de repositório, quando necessárias;
- sem dependência de Android ou framework de UI.

### `data/`

- leitura de `frota.json`;
- parsing dos dados;
- implementação de repositórios;
- acesso a Android somente nesta camada quando necessário.

### `ui/`

- Compose;
- ViewModel ou MVI;
- navegação;
- filtros;
- apresentação dos status;
- interação de favoritos.

`ClassifyDevice` e `DeviceRepository` devem ser recebidos por injeção, e não criados como globais dentro de quem os consome.

## 8. Restrições e fora de escopo

Não fazer:

- rede;
- autenticação;
- cadastro;
- login;
- backend;
- banco relacional;
- Room;
- SQLite;
- migrações;
- telas extras;
- chamadas HTTP.

Os favoritos podem usar `DataStore`, `SharedPreferences` ou equivalente simples.

## 9. Entregáveis

O repositório deve conter:

- histórico Git com mensagens legíveis;
- projeto Android compilável;
- código organizado em camadas;
- `PROTOCOLO.md`;
- `README.md` com execução, decisões e trade-offs;
- workflow de CI verde;
- captura ou demonstração da tela de lista;
- captura ou demonstração da tela de detalhe;
- seção “O que faria em seguida”.

## 10. Critérios de avaliação

| Critério | Pontos |
|---|---:|
| Arquitetura e fronteiras | 25 |
| Correção do domínio | 25 |
| Interface e navegação | 20 |
| Testes e integração contínua | 15 |
| Documentação e justificativa | 15 |
| Trilha opcional de especialização | +20 |

## 11. Checklist de validação

- [ ] App abre sem rede.
- [ ] `frota.json` é lido de `assets/`.
- [ ] Todos os dispositivos aparecem.
- [ ] Contagens exibidas: 16 OK, 8 Atenção, 4 Crítico e 2 Sem dados.
- [ ] Filtro por status funciona.
- [ ] Detalhe mostra os atributos exigidos.
- [ ] Justificativa cita sinal e valor.
- [ ] Voltar preserva o filtro.
- [ ] Favoritos sobrevivem à reabertura.
- [ ] mWh é convertido para mAh.
- [ ] Dados incompletos resultam em `INDETERMINADO`.
- [ ] Não há `NaN`, `Infinity` ou crash por divisão zero.
- [ ] Domínio não importa Android.
- [ ] Testes JVM passam.
- [ ] Lint passa.
- [ ] CI executa lint e testes.
- [ ] `PROTOCOLO.md` está documentado.
- [ ] `README.md` explica como executar e o que fazer depois.
