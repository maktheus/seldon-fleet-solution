# Protocolo de decisões

## Regra pré-registrada

Uso a regra de referência da especificação, sem ajuste aos resultados do dataset. Primeiro normalizo o consumo para mAh (`mWh / tensão nominal`) e calculo `dreno = mAh / (tempo de tela / 60)`. O indicador comparável é `dreno / capacidade * 100` (%/h). Na ordem: **INDETERMINADO** se capacidade ou tempo forem zero/ausentes (ou se faltar tensão para mWh); **CRÍTICO** se dreno relativo ≥ 12, falhas ≥ 3 ou jank ≥ 15%; **ATENÇÃO** se dreno relativo ≥ 7, falhas ≥ 1 ou jank ≥ 8%; caso contrário **OK**. A precedência crítica é intencional.

## Defeitos do arquivo

- Unidades mWh são convertidas para mAh usando `nominalVoltage`; nunca comparo o valor bruto.
- Capacidade nula/ausente, tempo de tela zero e mWh sem tensão resultam em INDETERMINADO. A UI informa a causa e não exibe divisão por zero, NaN ou Infinity.

## Ameaça à validade

A métrica supõe que todo consumo registrado ocorreu durante o tempo de tela. Serviços em primeiro plano com a tela desligada podem inflar o dreno e produzir falso CRÍTICO. Além disso, a qualidade da decisão depende da fidelidade da telemetria exportada.
