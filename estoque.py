from contextlib import closing, contextmanager
from datetime import date, timedelta
from decimal import Decimal, InvalidOperation, ROUND_HALF_UP
from math import floor
import sqlite3


def centavos(valor):
    try:
        numero = Decimal(valor.replace(",", "."))
        if not numero.is_finite() or not 0 < numero <= 1_000_000:
            raise ValueError
        resultado = int((numero * 100).quantize(Decimal("1"), rounding=ROUND_HALF_UP))
        if resultado == 0:
            raise ValueError
        return resultado
    except (InvalidOperation, ValueError):
        raise ValueError("Informe um preço positivo, como 4,90.") from None


def inteiro(valor, minimo=1):
    numero = int(valor)
    if not minimo <= numero <= 1_000_000:
        raise ValueError(f"Informe uma quantidade entre {minimo} e 1.000.000.")
    return numero


class Estoque:
    def __init__(self, arquivo):
        self.arquivo = arquivo
        with self.conexao() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS produtos (
                    nome TEXT COLLATE NOCASE PRIMARY KEY,
                    cadastro TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS lotes (
                    codigo TEXT COLLATE NOCASE PRIMARY KEY,
                    produto TEXT NOT NULL REFERENCES produtos(nome),
                    validade TEXT NOT NULL,
                    saldo INTEGER NOT NULL CHECK(saldo >= 0),
                    custo INTEGER NOT NULL,
                    preco INTEGER NOT NULL
                );
                CREATE TABLE IF NOT EXISTS movimentos (
                    id INTEGER PRIMARY KEY,
                    lote TEXT NOT NULL REFERENCES lotes(codigo),
                    dia TEXT NOT NULL,
                    tipo TEXT NOT NULL,
                    quantidade INTEGER NOT NULL
                );
                CREATE INDEX IF NOT EXISTS movimentos_dia ON movimentos(dia, tipo);
                CREATE INDEX IF NOT EXISTS lotes_produto ON lotes(produto, validade);
                CREATE TABLE IF NOT EXISTS lotes_excluidos (
                    codigo TEXT COLLATE NOCASE PRIMARY KEY REFERENCES lotes(codigo),
                    dia TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS calendario (
                    produto TEXT NOT NULL REFERENCES produtos(nome),
                    mes INTEGER NOT NULL CHECK(mes BETWEEN 1 AND 12),
                    situacao TEXT NOT NULL CHECK(situacao IN ('Safra', 'Oferta reduzida')),
                    PRIMARY KEY(produto, mes)
                );
            """)

    @contextmanager
    def conexao(self):
        with closing(sqlite3.connect(self.arquivo)) as db, db:
            db.row_factory = sqlite3.Row
            db.execute("PRAGMA foreign_keys = ON")
            yield db

    def cadastrar(self, codigo, produto, validade, saldo, custo, preco, hoje=None):
        hoje = hoje or date.today()
        codigo, produto = codigo.strip(), " ".join(produto.split())
        if not codigo or not produto:
            raise ValueError("Informe código e produto.")
        with self.conexao() as db:
            db.execute("INSERT OR IGNORE INTO produtos VALUES (?, ?)", (produto, hoje.isoformat()))
            nome = db.execute("SELECT nome FROM produtos WHERE nome = ?", (produto,)).fetchone()["nome"]
            db.execute("INSERT INTO lotes VALUES (?, ?, ?, ?, ?, ?)",
                       (codigo, nome, validade.isoformat(), saldo, custo, preco))
            db.execute("INSERT INTO movimentos(lote, dia, tipo, quantidade) VALUES (?, ?, 'Entrada', ?)",
                       (codigo, hoje.isoformat(), saldo))

    def movimentar(self, codigo, tipo, quantidade, hoje=None):
        hoje = hoje or date.today()
        if quantidade <= 0 or tipo not in ("Venda", "Descarte", "Reposição"):
            raise ValueError("Confira o tipo e a quantidade da movimentação.")
        with self.conexao() as db:
            db.execute("BEGIN IMMEDIATE")
            lote = db.execute("SELECT * FROM lotes WHERE codigo = ?", (codigo,)).fetchone()
            if not lote:
                raise ValueError("Lote inexistente.")
            if db.execute("SELECT 1 FROM lotes_excluidos WHERE codigo = ?", (codigo,)).fetchone():
                raise ValueError("Este lote foi excluído do estoque.")
            if tipo != "Descarte" and lote["validade"] < hoje.isoformat():
                raise ValueError("Lote vencido. Registre o descarte ou cadastre um novo lote.")
            saldo = lote["saldo"] + (quantidade if tipo == "Reposição" else -quantidade)
            if saldo < 0:
                raise ValueError("Saldo insuficiente.")
            db.execute("UPDATE lotes SET saldo = ? WHERE codigo = ?", (saldo, codigo))
            db.execute("INSERT INTO movimentos(lote, dia, tipo, quantidade) VALUES (?, ?, ?, ?)",
                       (codigo, hoje.isoformat(), tipo, quantidade))

    def excluir(self, codigo, hoje=None):
        hoje = hoje or date.today()
        with self.conexao() as db:
            registro = db.execute("""
                INSERT OR IGNORE INTO lotes_excluidos
                SELECT codigo, ? FROM lotes WHERE codigo = ?
            """, (hoje.isoformat(), codigo))
            if registro.rowcount != 1:
                raise ValueError("O lote já foi excluído ou não existe.")

    def salvar_calendario(self, produto, meses):
        with self.conexao() as db:
            db.execute("DELETE FROM calendario WHERE produto = ?", (produto,))
            db.executemany("INSERT INTO calendario VALUES (?, ?, ?)",
                           [(produto, mes, valor) for mes, valor in meses.items() if valor])

    def calendario(self):
        with self.conexao() as db:
            resultado = {r["nome"]: {} for r in db.execute("SELECT nome FROM produtos ORDER BY nome")}
            for r in db.execute("SELECT * FROM calendario"):
                resultado[r["produto"]][r["mes"]] = r["situacao"]
            return resultado

    def painel(self, hoje=None):
        hoje = hoje or date.today()
        inicio = hoje - timedelta(days=14)
        with self.conexao() as db:
            db.execute("BEGIN")
            lotes = list(db.execute("""
                SELECT l.*, p.cadastro FROM lotes l JOIN produtos p ON p.nome = l.produto
                WHERE NOT EXISTS (SELECT 1 FROM lotes_excluidos e WHERE e.codigo = l.codigo)
                ORDER BY l.produto, l.validade, l.codigo
            """))
            vendas = dict(db.execute("""
                SELECT l.produto, SUM(m.quantidade)
                FROM movimentos m JOIN lotes l ON l.codigo = m.lote
                WHERE m.tipo = 'Venda' AND m.dia >= ? AND m.dia < ?
                GROUP BY l.produto
            """, (inicio.isoformat(), hoje.isoformat())))
            sazonal = {(r["produto"], r["mes"]): r["situacao"]
                       for r in db.execute("SELECT * FROM calendario")}
        comprometido = {}
        resultado = []
        for lote in lotes:
            produto = lote["produto"]
            dias = (date.fromisoformat(lote["validade"]) - hoje).days
            saldo = lote["saldo"]
            observados = min(14, (hoje - date.fromisoformat(lote["cadastro"])).days)
            ritmo = vendas.get(produto, 0) / observados if observados >= 7 else None
            situacao = sazonal.get((produto, hoje.month), "Sem calendário")
            reducao = next((mes for salto in range(1, 4)
                            if sazonal.get((produto, mes := (hoje.month + salto - 1) % 12 + 1))
                            == "Oferta reduzida"), None)
            estado = "Esgotado" if saldo == 0 else "Vencido" if dias < 0 else (
                "Vence hoje" if dias == 0 else "Vence em breve" if dias <= 3 else "Em dia"
            )
            sobra = None
            sugestao = None
            motivo = "Retirar da venda" if estado == "Vencido" else ""
            if saldo and dias >= 0:
                if ritmo is not None:
                    anteriores = comprometido.get(produto, 0)
                    capacidade = max(0, floor(ritmo * (dias + 1)) - anteriores)
                    venda_prevista = min(saldo, capacidade)
                    sobra = saldo - venda_prevista
                    comprometido[produto] = anteriores + venda_prevista
                base, custo = lote["preco"], lote["custo"]
                if dias <= 3 and sobra is not None and sobra > 0:
                    desconto = 30 if dias == 0 else 20 if sobra * 2 >= saldo else 10
                    candidato = max((custo * 70 + 99) // 100, base * (100 - desconto) // 100)
                    if candidato < base:
                        sugestao, motivo = candidato, "Promoção por risco de sobra"
                    else:
                        motivo = "Revisar lote: preço já no piso de liquidação"
                elif dias > 3 and situacao != "Sem calendário":
                    fator = 95 if situacao == "Safra" else 105
                    candidato = max(custo, (base * fator + 50) // 100)
                    if situacao == "Safra":
                        candidato = min(base, candidato)
                    sugestao, motivo = candidato, f"Ajuste por {situacao.lower()}"
                elif ritmo is None:
                    motivo = "Aguardando 7 dias de histórico"
            resultado.append(dict(lote) | dict(estado=estado, situacao=situacao, reducao=reducao,
                                               sobra=sobra, sugestao=sugestao, motivo=motivo))
        return resultado
