<h1> <img src="assets/logo.png" alt="Logo FreshStock" width="64" align="absmiddle" />FreshStock</h1>

**Gestão de estoque e precificação de hortifruti com apoio de IA.**

🌐 **[Acesse o site de demonstração](https://freshstockpr.netlify.app)**

Projeto acadêmico desenvolvido em Java para aplicar conceitos de Programação Orientada a Objetos a um problema do varejo: acompanhar produtos perecíveis, identificar risco de perda e apoiar decisões de reposição e preço.

O FreshStock é um aplicativo desktop com interface gráfica, banco SQL local e um consultor de IA integrado por API. As regras de estoque e os cálculos funcionam localmente; a IA complementa a análise com recomendações em linguagem natural.

## O problema

Produtos próximos do vencimento, falta de mercadoria e preços desalinhados com a demanda exigem atenção constante. O sistema reúne essas informações para ajudar o gestor a perceber prioridades e decidir o que fazer.

## Funcionalidades

| Recurso | Comportamento |
|---|---|
| Cadastro de lotes | Produto, código, validade restante, estoque, custo, preço base e característica climatérica |
| Persistência local | Guarda lotes, saldos, data de validade, estoque mínimo e temporada ao fechar |
| Movimentações | Registra vendas/saídas e reposições do mesmo lote |
| Alertas visuais | Destaca estoque baixo, esgotado, validade próxima e produtos vencidos |
| Preços calculados | Mostra sugestões por lote automaticamente ao abrir e permite recalcular |
| Simulação | Considera dia, horário, vendas por hora e meta de vendas |
| Temporada de demanda | Permite informar demanda normal, alta ou baixa |
| Consultor de IA | Produz parecer complementar sem bloquear a interface |
| Análise local | Mantém recomendações por regras quando a API está indisponível |

## Tecnologias e conceitos

- **Java e Swing:** lógica de negócio e interface desktop.
- **H2 e JDBC:** banco SQL embarcado, sem servidor separado.
- **HTTP e API de IA:** integração para geração de pareceres.
- **SwingWorker:** consultas em segundo plano.
- **POO:** encapsulamento, composição, interfaces e polimorfismo.
- **Padrão Strategy:** diferentes estratégias de precificação selecionadas pelo motor.
- **PreparedStatement:** parâmetros nas operações de cadastro e atualização do banco.

## Como executar

### Requisitos

- JDK 8 ou superior, com `java`, `javaw`, `javac` e `jar` disponíveis no PATH.
- A biblioteca `lib/h2.jar`, incluída nesta pasta.
- Internet e uma chave compatível com a API implementada somente para o consultor externo de IA.

### Windows

1. Baixe ou clone este repositório.
2. Na pasta do projeto, execute no PowerShell:

   ```powershell
   powershell -NoProfile -ExecutionPolicy Bypass -File .\Compilar.ps1
   ```

3. Dê dois cliques em **Abrir-FreshStock.vbs** para abrir a janela sem terminal.

O banco é criado automaticamente em `dados/freshstock.mv.db`. A distribuição do repositório começa sem lotes cadastrados.

### Configurar a IA

Na aba **Diagnóstico de IA**, clique em **Configurar IA** e informe sua chave. Deixe o modelo vazio para usar o padrão. A chave digitada na janela vale apenas durante a sessão.

Também são aceitas as variáveis de ambiente `IA_API_KEY` e `IA_MODEL`. O iniciador no Windows aceita uma credencial local protegida em `.ia-key.xml`, quando previamente configurada no computador.

O consultor utiliza uma API externa: ao solicitar um diagnóstico, os dados do lote e do cenário são enviados ao serviço configurado. Disponibilidade, permissões e cotas dependem da conta utilizada. O projeto não inclui uma chave e não implementa um modelo de IA próprio. O identificador do modelo e o endpoint permanecem no cliente HTTP por necessidade técnica.

## Regras da demonstração

- **Estoque baixo:** saldo igual ou inferior ao mínimo configurado; o padrão é 10.
- **Validade próxima:** produto vence hoje ou nos próximos 3 dias.
- **Vencido:** data de validade anterior à data atual; o sistema recomenda retirada da venda.
- **Esgotado:** saldo zero; o sistema recomenda reposição.
- **Alta/baixa temporada:** representa demanda informada pelo gestor, não uma previsão automática de safra ou pesquisa de mercado.
- **Ajuste por temporada:** alta demanda acrescenta 10% e baixa reduz 10% ao cálculo, antes dos limites do motor; esse ajuste não é aplicado quando restam até 3 dias.
- **Limites:** piso normal de custo + 5%; piso de liquidação de 70% do custo; teto de 130% do preço base.
- **Sem simulação na sessão:** a sugestão usa o cenário demonstrativo de 8 vendas/h e meta de 10 vendas/h.

Produtos vencidos ou esgotados não recebem sugestão de preço. Os valores calculados são recomendações: o preço base não é alterado automaticamente.

## Roteiro de demonstração

1. Cadastre produtos com variedades específicas, como uva Thompson, maçã Fuji e tomate italiano.
2. Inclua lotes com poucos dias de validade e outros com saldo abaixo do mínimo.
3. Configure o mínimo e a temporada na aba de cadastro.
4. Registre uma saída e observe a atualização dos alertas.
5. Consulte a tabela de preços e compare cenários de demanda.
6. Solicite o parecer da IA.
7. Feche e reabra o aplicativo para demonstrar a persistência dos lotes.

## Estrutura

```text
src/
  Main.java                         Entrada do aplicativo
  JanelaFreshStock.java              Interface e ações do usuário
  PainelGestao.java                  Painel visual de alertas
  BancoEstoque.java                  Persistência SQL com JDBC
  LoteProduto.java                   Dados e regras do lote
  HistoricoDemanda.java              Cenário de demanda
  MotorPrecificacao.java             Seleção de estratégia e limites
  EstrategiaPrecificacao.java        Interface Strategy
  Precificacao*.java                 Estratégias de preço
  ResultadoPrecificacao.java         Resultado dos cálculos
  RegistroPrecificacao.java          Simulações da sessão
  ConsultorEstoqueIA.java            Análise local por regras
  ClienteIA.java                     Integração HTTP com IA externa
  UtilNumerico.java                  Formatação e leitura de números
lib/h2.jar                          Banco embarcado
Compilar.ps1                        Compilação e empacotamento
Abrir-FreshStock.vbs                 Iniciador sem terminal
Abrir.ps1                           Inicialização no Windows
```

## Testes e limitações

`TesteBanco.java` verifica persistência após reabrir o banco, duplicidade de código, saldo insuficiente, temporada, vencimento e preenchimento automático da tabela de preços. Os testes usam banco temporário separado.

O aplicativo é um protótipo acadêmico. O histórico de simulações fica apenas na sessão; os lotes e suas configurações são persistidos. A utilização prevista é local, com uma janela por vez. Não há integração com caixa, vendas reais, fornecedores ou dados externos de sazonalidade.

Reposições atualizam o mesmo lote e mantêm sua validade. Mercadorias com outra validade devem ser cadastradas em outro lote. Para backup, feche o aplicativo e copie a pasta `dados`.

## Publicação e arquivos privados

O `.gitignore` exclui banco, backups, credenciais e arquivos gerados. Esta distribuição não contém os lotes de apresentação nem credenciais pessoais. Não adicione esses arquivos manualmente ao repositório.

## Autoria e licença

Projeto acadêmico de **Gisele**, desenvolvido para estudo de Programação Orientada a Objetos.

Uma licença para o código do projeto ainda não foi definida. A biblioteca H2 possui licença própria; seus avisos devem ser preservados. Consulte [o projeto H2](https://h2database.com).
