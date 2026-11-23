# FreshStock — estoque persistente e assistente de gestão

Abra Abrir-FreshStock.vbs com dois cliques. Requer Java 8 ou superior no PATH.

## Banco local

Os lotes são salvos imediatamente no banco H2 em dados/freshstock.mv.db, ao lado do aplicativo.
Não precisa instalar um servidor. Não apague a pasta dados. Para fazer backup, feche o programa e copie essa pasta.
Abra uma janela por vez. Falhas de abertura do banco são exibidas; o programa não recria silenciosamente os dados.
A versão nova começa sem lotes fictícios. Os lotes da versão anterior ficavam apenas em memória e não podem ser recuperados depois de fechá-la.
A validade é uma data fixa: dias restantes são recalculados ao abrir e a cada minuto.

## Fluxo da demonstração

1. Cadastre um lote: ele já fica gravado.
2. Selecione-o na tabela e configure Estoque mínimo e temporada.
3. Registre vendas/saídas ou reposição: o saldo é gravado imediatamente.
4. Abra Diagnóstico de IA para visualizar os alertas e sugestões.
5. Feche e reabra: lotes, saldos, validade, mínimo e temporada continuam salvos.

Vencimento próximo = até 3 dias. Estoque baixo = saldo igual ou inferior ao mínimo (inicialmente 10).
Produtos vencidos e esgotados não recebem sugestão de preço. A saída também pode registrar retirada/descarte.
Reposição se refere ao mesmo lote e à mesma validade; para nova validade, cadastre outro lote.

## Temporada e preços

Temporada representa DEMANDA informada pelo gestor (Normal / Alta demanda / Baixa demanda).
Não é previsão automática de safra nem consulta a mercado externo. O gestor atualiza conforme sua região/período.
Alta demanda acrescenta 10% ao cálculo e baixa reduz 10%, antes dos limites do motor.
Quando faltam até 3 dias, a validade tem prioridade e o multiplicador de temporada não é aplicado.
Os preços são sugestões; não são aplicados automaticamente. Sem simulação anterior, a demanda usada é demonstrativa (8 vendas/h, meta 10).
O histórico de simulações fica na sessão; os lotes e suas configurações ficam no banco.

## Gemini

Os alertas funcionam localmente, mesmo sem internet. O botão de diagnóstico consulta o Gemini para complementar a análise.
A chave protegida .gemini-key.xml é carregada pelo iniciador no Windows. Não compartilhe esse arquivo.
Também é possível usar Configurar Gemini ou as variáveis GEMINI_API_KEY/GEMINI_MODEL.
Lotes vencidos ou esgotados são tratados pelo painel local e não enviados para recomendação de preço.

## Compilar

    javac -encoding UTF-8 -d bin src\*.java

Empacote com Main-Class: Main e Class-Path: lib/h2.jar no manifesto. Mantenha lib/h2.jar junto ao aplicativo.
Banco: H2 2.3.232, biblioteca JDBC embarcada. Documentação e licença: https://h2database.com

Testes: TesteBanco.java verifica persistência após reabrir, duplicidade, saldo insuficiente, temporada e vencimento; usa banco temporário separado.
