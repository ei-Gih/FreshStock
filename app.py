from datetime import date, datetime
from pathlib import Path
import sqlite3
import tkinter as tk
from tkinter import messagebox, ttk

import customtkinter as ctk

from estoque import Estoque, centavos, inteiro


MESES = ("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
CAMPOS = ("Código", "Produto", "Validade", "Saldo", "Situação", "Mês atual",
          "Oferta reduzida", "Sobra estimada", "Preço atual", "Preço sugerido", "Ação sugerida")
FUNDO = "#F5F6ED"
TEXTO = "#23382B"
VERDE = "#286046"
VERDE_ESCURO = "#194633"
VERDE_CLARO = "#E4EFDB"
BORDA = "#D8E1D1"


def dinheiro(valor):
    return "—" if valor is None else f"R$ {valor / 100:.2f}".replace(".", ",")


class Janela:
    def __init__(self, raiz, estoque):
        self.raiz, self.estoque = raiz, estoque
        raiz.title("FreshStock | Estoque de hortifruti")
        raiz.geometry(f"{min(1280, raiz.winfo_screenwidth() - 48)}x650")
        raiz.minsize(720, 420)
        corpo = ctk.CTkFrame(raiz, fg_color="transparent")
        corpo.pack(expand=True, fill="both", padx=22, pady=20)
        cabecalho = ctk.CTkFrame(corpo, fg_color=VERDE_ESCURO, corner_radius=16)
        cabecalho.pack(fill="x")
        marca = ctk.CTkFrame(cabecalho, fg_color="transparent")
        marca.pack(side="left", padx=20, pady=14)
        caminho_logo = Path(__file__).resolve().parent / "assets" / "logo.png"
        if caminho_logo.is_file():
            original = tk.PhotoImage(file=caminho_logo)
            self.logo = original.subsample(8)
            self.icone = original.subsample(16)
            raiz.iconphoto(True, self.icone)
            tk.Label(marca, image=self.logo, background=VERDE_ESCURO, borderwidth=0).pack(
                side="left", padx=(0, 14))
        titulo = ctk.CTkFrame(marca, fg_color="transparent")
        titulo.pack(side="left")
        ctk.CTkLabel(titulo, text="FreshStock", text_color="#FFFFFF",
                     font=("Segoe UI", 28, "bold")).pack(anchor="w")
        ctk.CTkLabel(titulo, text="Estoque de hortifruti", text_color="#D7E7D6",
                     font=("Segoe UI", 14)).pack(anchor="w")

        indicadores = ctk.CTkFrame(corpo, fg_color="transparent")
        indicadores.pack(fill="x", pady=(16, 14))
        self.indicadores = []
        for coluna, (nome, fundo, cor) in enumerate((
            ("Lotes cadastrados", VERDE_CLARO, VERDE_ESCURO),
            ("Unidades vencidas", "#FAE7DF", "#8F382B"),
            ("Vencem em até 3 dias", "#FFF0CF", "#78490C"),
            ("Sobra estimada em até 3 dias", "#EDF1DC", "#50591F"),
        )):
            indicadores.columnconfigure(coluna, weight=1, uniform="indicador")
            cartao = ctk.CTkFrame(indicadores, fg_color=fundo, corner_radius=12)
            cartao.grid(row=0, column=coluna, sticky="nsew", padx=(0, 8) if coluna < 3 else 0)
            numero = ctk.CTkLabel(cartao, text="0", text_color=cor, font=("Segoe UI", 25, "bold"))
            numero.pack(anchor="w", padx=12, pady=(8, 0))
            ctk.CTkLabel(cartao, text=nome, text_color=cor, wraplength=138, justify="left").pack(
                anchor="w", padx=12, pady=(0, 8))
            self.indicadores.append(numero)

        painel = ctk.CTkFrame(corpo, fg_color="#FFFFFF", border_width=1,
                              border_color=BORDA, corner_radius=14)
        painel.pack(fill="both", expand=True)
        titulo_painel = ctk.CTkFrame(painel, fg_color="transparent")
        titulo_painel.pack(fill="x", padx=16, pady=(10, 0))
        ctk.CTkLabel(titulo_painel, text="Lotes", text_color=TEXTO,
                     font=("Segoe UI", 19, "bold")).pack(side="left")
        ctk.CTkButton(titulo_painel, text="Excluir lote", command=self.excluir_lote, width=105,
                      fg_color="#FAE7DF", hover_color="#F3D2C4", text_color="#8F382B").pack(side="right")
        barra = ctk.CTkFrame(painel, fg_color="transparent")
        barra.pack(fill="x", padx=16, pady=(6, 12))
        for nome, acao in (
            ("Cadastrar lote", self.cadastrar),
            ("Registrar venda", lambda: self.movimentar("Venda")),
            ("Registrar descarte", lambda: self.movimentar("Descarte")),
            ("Repor lote", lambda: self.movimentar("Reposição")),
            ("Calendário de safra", self.calendario),
            ("Atualizar", self.atualizar),
        ):
            principal = nome == "Cadastrar lote"
            ctk.CTkButton(barra, text=nome, command=acao, width=0, height=34,
                          fg_color=VERDE if principal else "#EDF3EA",
                          hover_color=VERDE_ESCURO if principal else "#DCEAD7",
                          text_color="#FFFFFF" if principal else VERDE_ESCURO).pack(
                              side="left", padx=(0, 6))
        estilo = ttk.Style(raiz)
        estilo.theme_use("clam")
        estilo.configure("Estoque.Treeview", rowheight=33, background="#FFFFFF",
                         fieldbackground="#FFFFFF", foreground=TEXTO, borderwidth=0,
                         font=("Segoe UI", 10))
        estilo.configure("Estoque.Treeview.Heading", background=VERDE_CLARO,
                         foreground=VERDE_ESCURO, relief="flat", padding=6,
                         font=("Segoe UI", 9, "bold"))
        estilo.map("Estoque.Treeview", background=[("selected", VERDE)],
                   foreground=[("selected", "#FFFFFF")])
        grade = ctk.CTkFrame(painel, fg_color="transparent")
        grade.pack(fill="both", expand=True, padx=16, pady=(0, 14))
        self.tabela = ttk.Treeview(grade, columns=CAMPOS, show="headings", selectmode="browse",
                                   style="Estoque.Treeview")
        vertical = ctk.CTkScrollbar(grade, command=self.tabela.yview)
        horizontal = ctk.CTkScrollbar(grade, orientation="horizontal", command=self.tabela.xview)
        self.tabela.configure(yscrollcommand=vertical.set, xscrollcommand=horizontal.set)
        grade.columnconfigure(0, weight=1)
        grade.rowconfigure(0, weight=1)
        self.tabela.grid(row=0, column=0, sticky="nsew")
        vertical.grid(row=0, column=1, sticky="ns")
        horizontal.grid(row=1, column=0, sticky="ew")
        for campo, largura in zip(CAMPOS, (85, 133, 87, 55, 105, 123, 115, 104, 82, 94, 260)):
            self.tabela.heading(campo, text=campo)
            self.tabela.column(campo, width=largura, stretch=False)
        self.tabela.tag_configure("Vencido", background="#FAE7DF")
        self.tabela.tag_configure("Vence hoje", background="#FFF0CF")
        self.tabela.tag_configure("Vence em breve", background="#FFF7E5")
        self.tabela.tag_configure("Esgotado", background="#F0F2EF")
        self.vazio = ctk.CTkLabel(grade, text="Cadastre um lote para começar a acompanhar o estoque.",
                                 fg_color="#FFFFFF", text_color=VERDE_ESCURO)
        self.atualizar()

    def atualizar(self):
        selecionado = self.tabela.selection()
        lotes = self.estoque.painel()
        prioridade = {"Vencido": 0, "Vence hoje": 1, "Vence em breve": 2, "Em dia": 3, "Esgotado": 4}
        self.tabela.delete(*self.tabela.get_children())
        if lotes:
            self.vazio.place_forget()
        else:
            self.vazio.place(relx=0.5, rely=0.45, anchor="center")
        for lote in sorted(lotes, key=lambda item: (prioridade[item["estado"]], item["validade"])):
            mes = lote["reducao"]
            sobra = lote["sobra"]
            self.tabela.insert("", "end", iid=lote["codigo"], tags=(lote["estado"],), values=(
                lote["codigo"], lote["produto"], date.fromisoformat(lote["validade"]).strftime("%d/%m/%Y"),
                lote["saldo"], lote["estado"], lote["situacao"], MESES[mes - 1] if mes else "—",
                "—" if sobra is None else sobra, dinheiro(lote["preco"]),
                dinheiro(lote["sugestao"]), lote["motivo"],
            ))
        if selecionado and self.tabela.exists(selecionado[0]):
            self.tabela.selection_set(selecionado[0])
        vencidos = sum(l["saldo"] for l in lotes if l["estado"] == "Vencido")
        proximos = sum(l["saldo"] for l in lotes if l["estado"] in ("Vence hoje", "Vence em breve"))
        risco = sum(l["sobra"] or 0 for l in lotes if l["estado"] in ("Vence hoje", "Vence em breve"))
        for indicador, valor in zip(self.indicadores, (len(lotes), vencidos, proximos, risco)):
            indicador.configure(text=str(valor))

    def dialogo(self, titulo):
        janela = ctk.CTkToplevel(self.raiz, fg_color=FUNDO)
        janela.withdraw()
        janela.title(titulo)
        janela.transient(self.raiz)
        quadro = ctk.CTkFrame(janela, fg_color="#FFFFFF", border_color=BORDA, border_width=1)
        quadro.pack(fill="both", expand=True, padx=14, pady=14)
        ctk.CTkLabel(quadro, text=titulo, text_color=VERDE_ESCURO,
                     font=("Segoe UI", 20, "bold")).grid(
                         row=0, column=0, columnspan=4, sticky="w", padx=16, pady=(12, 10))
        return janela, quadro

    def mostrar_dialogo(self, janela, foco):
        janela.update_idletasks()
        largura, altura = janela.winfo_reqwidth(), janela.winfo_reqheight()
        x = max(0, (janela.winfo_screenwidth() - largura) // 2)
        y = max(0, (janela.winfo_screenheight() - altura) // 2)
        janela.geometry(f"+{x}+{y}")
        janela.resizable(False, False)
        janela.deiconify()
        janela.wait_visibility()
        janela.grab_set()
        foco.focus_set()

    def salvar(self, janela, operacao):
        try:
            operacao()
        except ValueError as erro:
            messagebox.showerror("Confira os dados", str(erro), parent=janela)
            return
        except sqlite3.IntegrityError:
            messagebox.showerror("Dados inválidos", "Confira se o código do lote já existe.", parent=janela)
            return
        except sqlite3.Error as erro:
            messagebox.showerror("Erro no banco", str(erro), parent=janela)
            return
        janela.destroy()
        self.atualizar()

    def formulario(self, titulo, campos, operacao):
        janela, quadro = self.dialogo(titulo)
        entradas = []
        for linha, (rotulo, inicial) in enumerate(campos, 1):
            ctk.CTkLabel(quadro, text=rotulo, text_color=TEXTO).grid(
                row=linha, column=0, sticky="w", padx=16, pady=4)
            entrada = ctk.CTkEntry(quadro, width=210, height=34, fg_color="#FBFCF8",
                                   text_color=TEXTO, border_color=BORDA)
            entrada.insert(0, inicial)
            entrada.grid(row=linha, column=1, padx=(0, 16), pady=4)
            entradas.append(entrada)
        def confirmar():
            self.salvar(janela, lambda: operacao(*(entrada.get() for entrada in entradas)))
        ctk.CTkButton(quadro, text="Salvar", command=confirmar, fg_color=VERDE,
                      hover_color=VERDE_ESCURO).grid(
                          row=len(campos) + 1, column=1, sticky="e", padx=(0, 16), pady=(12, 16))
        janela.bind("<Return>", lambda evento: confirmar())
        self.mostrar_dialogo(janela, entradas[0])

    def cadastrar(self):
        def gravar(codigo, produto, validade, saldo, custo, preco):
            try:
                vencimento = datetime.strptime(validade, "%d/%m/%Y").date()
            except ValueError:
                raise ValueError("Informe a validade no formato DD/MM/AAAA.") from None
            self.estoque.cadastrar(codigo, produto, vencimento, inteiro(saldo), centavos(custo), centavos(preco))

        self.formulario("Cadastrar lote", (
            ("Código do lote", ""), ("Produto ou variedade", ""), ("Validade (DD/MM/AAAA)", ""),
            ("Quantidade em unidades", ""), ("Custo por unidade (R$)", ""), ("Preço de venda (R$)", ""),
        ), gravar)

    def movimentar(self, tipo):
        selecao = self.tabela.selection()
        if not selecao:
            messagebox.showinfo(tipo, "Selecione um lote no painel.", parent=self.raiz)
            return
        codigo = selecao[0]
        self.formulario(f"{tipo} | Lote {codigo}", (("Quantidade em unidades", ""),),
                        lambda valor: self.estoque.movimentar(codigo, tipo, inteiro(valor)))

    def excluir_lote(self):
        selecao = self.tabela.selection()
        if not selecao:
            messagebox.showinfo("Excluir lote", "Selecione um lote no painel.", parent=self.raiz)
            return
        codigo = selecao[0]
        produto = self.tabela.set(codigo, "Produto")
        saldo = self.tabela.set(codigo, "Saldo")
        confirmar = messagebox.askyesno(
            "Excluir lote",
            f"Excluir o lote {codigo} de {produto} do estoque?\n\n"
            f"Saldo atual: {saldo} un. As vendas registradas serão preservadas.",
            default=messagebox.NO, parent=self.raiz,
        )
        if confirmar:
            try:
                self.estoque.excluir(codigo)
            except (ValueError, sqlite3.Error) as erro:
                messagebox.showerror("Erro ao excluir lote", str(erro), parent=self.raiz)
            self.atualizar()

    def calendario(self):
        dados = self.estoque.calendario()
        if not dados:
            messagebox.showinfo("Calendário de safra", "Cadastre um lote para adicionar o produto.", parent=self.raiz)
            return
        janela, quadro = self.dialogo("Calendário de safra")
        ctk.CTkLabel(quadro, text="Disponibilidade de cada produto na sua região.", text_color=TEXTO).grid(
            row=1, columnspan=4, sticky="w", padx=16, pady=(0, 8))
        produto = ctk.CTkComboBox(quadro, values=list(dados), state="readonly", width=230,
                                  command=lambda _: carregar(), border_color=BORDA,
                                  button_color=VERDE, button_hover_color=VERDE_ESCURO)
        produto.grid(row=2, columnspan=4, sticky="w", padx=16, pady=(0, 8))
        campos = {}
        for mes, nome in enumerate(MESES, 1):
            linha, coluna = divmod(mes - 1, 2)
            ctk.CTkLabel(quadro, text=nome, text_color=TEXTO).grid(
                row=linha + 3, column=coluna * 2, padx=16, sticky="w")
            campo = ctk.CTkComboBox(quadro, values=["Sem dados", "Safra", "Oferta reduzida"],
                                    state="readonly", width=148, border_color=BORDA,
                                    button_color=VERDE, button_hover_color=VERDE_ESCURO)
            campo.grid(row=linha + 3, column=coluna * 2 + 1, padx=(0, 16), pady=3)
            campos[mes] = campo
        def carregar():
            for mes, campo in campos.items():
                campo.set(dados[produto.get()].get(mes, "Sem dados"))
        produto.set(next(iter(dados)))
        carregar()
        def gravar():
            meses = {mes: campo.get() for mes, campo in campos.items() if campo.get() != "Sem dados"}
            self.salvar(janela, lambda: self.estoque.salvar_calendario(produto.get(), meses))
        ctk.CTkButton(quadro, text="Salvar calendário", command=gravar, fg_color=VERDE,
                      hover_color=VERDE_ESCURO).grid(
                          row=9, columnspan=4, sticky="e", padx=16, pady=(12, 16))
        self.mostrar_dialogo(janela, produto)


def main():
    pasta = Path(__file__).resolve().parent / "dados"
    pasta.mkdir(exist_ok=True)
    ctk.set_appearance_mode("light")
    raiz = ctk.CTk(fg_color=FUNDO)
    Janela(raiz, Estoque(pasta / "freshstock.sqlite3"))
    raiz.mainloop()


if __name__ == "__main__":
    main()
