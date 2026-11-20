| | | |
| :--- | :---: | ---: |
| | <h1>FreshStock (DynamicFresh AI)</h1> | <img src="assets/logo.png" alt="Logo FreshStock" width="100" /> |
| |  |  |

> *Dados inteligentes, produtos sempre frescos.*

Motor de **precificação dinâmica** e **gestão inteligente de estoque** para hortifruti em supermercados, feito em **Java puro (POO)** e executado no **terminal**.

O sistema ajusta o preço de cada lote e orienta o gestor de estoque analisando três fatores:

1. **Validade / tempo biológico**: dias de validade restantes e se o produto é climatérico (banana, mamão etc.).
2. **Volume em estoque**: quantidade disponível no lote.
3. **Fluxo de demanda**: dia da semana, hora, vendas por hora históricas e meta de vendas por hora.

Além do preço, o sistema gera um **parecer de IA** com o risco de perda e uma recomendação prática. Pode ser feito por uma análise local ou pela **API do Gemini**, com fallback automático para a análise local.

---

## Requisitos

- Java **8 ou 11** (também funciona em versões mais novas)
- Nenhuma biblioteca externa, só o JDK
- Opcional: chave da API do Gemini (`GEMINI_API_KEY`)

## Como compilar e rodar

Com todos os arquivos `.java` na mesma pasta:

```bash
javac -encoding UTF-8 *.java
java Main
```

> No Windows, se os acentos saírem quebrados: `chcp 65001` e depois `java -Dfile.encoding=UTF-8 Main`.

## Configurando a IA do Gemini (opcional)

Sem a chave, o programa funciona normalmente com a IA local.

**Linux / macOS**
```bash
export GEMINI_API_KEY="sua_chave_aqui"
```

**Windows (CMD)**
```bat
set GEMINI_API_KEY=sua_chave_aqui
```

**Windows (PowerShell)**
```powershell
$env:GEMINI_API_KEY="sua_chave_aqui"
```

Essas variáveis valem só para o terminal atual. Para deixar permanente no Windows, use `setx GEMINI_API_KEY "sua_chave_aqui"` e abra um terminal novo.

Para trocar o modelo (padrão: `gemini-2.5-flash`), defina `GEMINI_MODEL`:

```bash
export GEMINI_MODEL="gemini-2.5-flash"
```

### Fallback automático

Se a chave não estiver definida, ou se houver erro de rede, erro HTTP (chave inválida, modelo inexistente), limite de uso (HTTP 429) ou resposta vazia, o sistema usa o `ConsultorEstoqueIA` local sem interromper a execução. O parecer indica a origem: `[IA Gemini]`, `[IA Local]` ou `[IA Local | fallback: motivo]`.

A chave é enviada no header `x-goog-api-key`, e não na URL, para não aparecer em logs ou mensagens de erro.

---

## Menu

| Opção | Ação |
|------|------|
| 1 | Cadastrar novo lote de hortifruti (aceita vírgula ou ponto nos decimais) |
| 2 | Simular cenário de demanda e validade para um lote (preço ajustado + parecer da IA) |
| 3 | Listar preços calculados |
| 4 | Listar lotes cadastrados |
| 5 | Executar diagnóstico de IA em todos os lotes |
| 0 | Sair |

O programa já abre com **5 lotes e 5 simulações iniciais** (usando só a IA local, para iniciar rápido mesmo sem internet).

---

## Arquitetura

```
.
├── Main.java                         # Menu interativo (Scanner) e entrada segura
├── LoteProduto.java                  # Lote: validade, estoque, custo e preço base
├── HistoricoDemanda.java             # Cenário de demanda (dia, hora, vendas/h, meta/h)
├── EstrategiaPrecificacao.java       # Interface do padrão Strategy
├── PrecificacaoMargemOptima.java     # Estratégia: ajuste fino de margem
├── PrecificacaoUrgenciaValidade.java # Estratégia: desconto por validade curta
├── PrecificacaoAltaRotatividade.java # Estratégia: acréscimo com demanda alta
├── MotorPrecificacao.java            # Escolhe a estratégia e aplica piso/teto
├── ResultadoPrecificacao.java        # Resultado de uma simulação
├── RegistroPrecificacao.java         # Histórico de simulações e pareceres
├── ConsultorEstoqueIA.java           # IA local: risco de perda e recomendação
├── ClienteGeminiIA.java              # IA via Gemini (HttpURLConnection) + fallback
└── UtilNumerico.java                 # Arredondamento, formatação pt-BR e leitura de decimais
```

### Padrão Strategy

`EstrategiaPrecificacao` define `calcularPrecoAjustado(LoteProduto, HistoricoDemanda)` e `getNomeEstrategia()`. Cada estratégia implementa uma regra diferente, e o `MotorPrecificacao` decide qual usar.

### Como o motor escolhe a estratégia

Primeira regra que se aplicar, nesta ordem:

| Condição | Estratégia |
|----------|-----------|
| Liquidação urgente ou fator de validade < 0,35 | Urgência de Validade |
| Estoque ≥ 100, demanda fraca (índice < 0,8) e fator de validade < 0,6 | Urgência de Validade |
| Demanda forte (índice ≥ 1,2) e fator de validade ≥ 0,5 | Alta Rotatividade |
| Qualquer outro caso | Margem Ótima |

### Conceitos principais

- **Dias de validade efetivos**: para produtos climatéricos, contam só 70% dos dias restantes, já que amadurecem e estragam mais rápido.
- **Fator de validade** (`calcularFatorValidade`): de 0,0 (vencendo) a 1,0 (fresco), com horizonte de 7 dias efetivos.
- **Liquidação urgente** (`precisaLiquidacaoUrgente`): quando restam 1,5 dia efetivo ou menos.
- **Índice de demanda** (`calcularIndiceDemanda`): vendas por hora históricas ÷ meta por hora, limitado entre 0 e 3. O valor 1,0 significa que está no ritmo da meta.

### Piso e teto de preço

| Regra | Valor |
|-------|-------|
| Piso normal | custo + 5% |
| Piso de liquidação (estratégia de Urgência) | 70% do custo |
| Teto | 130% do preço base |

Quando o piso ou o teto é aplicado, a simulação exibe uma observação.

### IA local (`ConsultorEstoqueIA`)

Calcula um **risco de perda de 0 a 100** combinando:

- unidades que provavelmente não serão vendidas antes de vencer, no ritmo atual (loja aberta 12 h por dia);
- quão pouco fresco o lote está;
- quão fraca está a demanda;
- um adicional se o lote exige liquidação urgente.

O risco é classificado em **BAIXO**, **MODERADO**, **ALTO** ou **CRÍTICO**, e o parecer traz uma ação prática (liquidar, reposicionar, monitorar, manter ou repor).

---

## Exemplo de uso

```
----------------------------------------------------------------------
1 - Cadastrar novo lote de hortifruti
2 - Simular cenário de demanda e validade
3 - Listar preços calculados
4 - Listar lotes cadastrados
5 - Executar diagnóstico de IA em todos os lotes
0 - Sair
----------------------------------------------------------------------
Escolha uma opção:
```

## Limitações conhecidas

- O risco de perda assume que o ritmo de vendas da hora simulada se mantém até o vencimento. Dá para ajustar a constante `HORAS_LOJA_POR_DIA` em `ConsultorEstoqueIA`.
- Os dados ficam só em memória e se perdem ao fechar o programa.
- Com a rede fora do ar, cada consulta ao Gemini espera até 5 s antes de cair no fallback.

## Licença

Projeto acadêmico. Defina a licença de acordo com a sua necessidade.