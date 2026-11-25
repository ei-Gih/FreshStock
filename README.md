# FreshStock

Aplicativo desktop em Python para acompanhar a validade de lotes de hortifruti, registrar vendas e descartes e sugerir preços e promoções. O painel estima sobras com base nas vendas registradas e mostra os períodos de safra e de oferta reduzida cadastrados para a sua região.

## Como abrir no Windows

Instale o Python 3.10 ou superior com Tkinter. Na pasta do repositório, prepare o ambiente uma vez:

```powershell
py -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Depois, dê dois cliques em **Abrir-FreshStock.vbs** ou execute `.\.venv\Scripts\python.exe app.py`.

## Uso

1. Cadastre um lote por data de validade. Informe quantidade em unidades, custo e preço de venda por unidade.
2. Selecione o lote para registrar vendas, descartes ou reposições. Priorize o lote válido que vence primeiro.
3. Em **Calendário de safra**, marque os meses de safra e oferta reduzida de cada produto na sua região.
4. Use o painel para acompanhar vencimentos, sobra estimada e preços sugeridos. O gestor decide quando aplicar os preços.

A projeção usa até 14 dias completos de vendas registradas por produto e começa após sete dias completos. O sistema usa o calendário informado para mostrar a disponibilidade deste mês e sinaliza oferta reduzida nos três meses seguintes.

O banco local fica em `dados/freshstock.sqlite3`. Para fazer backup, feche o aplicativo e copie a pasta `dados`.

Projeto acadêmico de **Gisele**.
