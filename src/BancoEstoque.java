import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Banco SQL embarcado: não requer servidor nem acesso à internet. */
public class BancoEstoque implements AutoCloseable {
    private final Connection con;
    public BancoEstoque(Path arquivo) throws Exception {
        Class.forName("org.h2.Driver");
        Files.createDirectories(arquivo.toAbsolutePath().getParent());
        con = DriverManager.getConnection("jdbc:h2:file:" + arquivo.toAbsolutePath().toString().replace('\\','/'), "sa", "");
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS lotes (codigo VARCHAR(150) PRIMARY KEY, nome VARCHAR(500) NOT NULL, climaterico BOOLEAN, entrada DATE, validade DATE NOT NULL, estoque INT CHECK(estoque>=0), custo DOUBLE PRECISION, base DOUBLE PRECISION, minimo INT, temporada VARCHAR(40))");
        }
    }
    public static Path caminhoPadrao() {
        String override = System.getProperty("freshstock.db");
        if (override != null) return Paths.get(override);
        try {
            Path app = Paths.get(BancoEstoque.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isDirectory(app)) app = app.getParent();
            else if (app.getFileName().toString().equals("bin")) app = app.getParent();
            return app.resolve("dados/freshstock");
        } catch (Exception ex) { throw new IllegalStateException("Não foi possível localizar o banco.", ex); }
    }
    public List<LoteProduto> listar() throws SQLException {
        List<LoteProduto> lista = new ArrayList<>();
        try (Statement st=con.createStatement(); ResultSet r=st.executeQuery("SELECT * FROM lotes ORDER BY codigo")) {
            while(r.next()) {
                LoteProduto l=new LoteProduto(r.getString("codigo"),r.getString("nome"),r.getBoolean("climaterico"),r.getDate("entrada").toLocalDate(),0,r.getInt("estoque"),r.getDouble("custo"),r.getDouble("base"));
                l.setDataValidade(r.getDate("validade").toLocalDate());
                l.configurar(r.getInt("minimo"),r.getString("temporada")); lista.add(l);
            }
        }
        return lista;
    }
    public void inserir(LoteProduto l) throws SQLException {
        try (PreparedStatement st=con.prepareStatement("INSERT INTO lotes VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            st.setString(1,l.getCodigoLote()); st.setString(2,l.getNomeProduto()); st.setBoolean(3,l.isClimaterico());
            st.setDate(4,java.sql.Date.valueOf(l.getDataEntrada())); st.setDate(5,java.sql.Date.valueOf(l.getDataValidade()));
            st.setInt(6,l.getQuantidadeEstoque()); st.setDouble(7,l.getPrecoCusto()); st.setDouble(8,l.getPrecoBase());
            st.setInt(9,l.getEstoqueMinimo()); st.setString(10,l.getTemporada()); st.executeUpdate();
        }
    }
    public void movimentar(String codigo,int unidades) throws SQLException {
        if(unidades==0) throw new IllegalArgumentException("Informe uma quantidade maior que zero.");
        try(PreparedStatement st=con.prepareStatement("UPDATE lotes SET estoque=estoque+? WHERE codigo=? AND CAST(estoque AS BIGINT)+? BETWEEN 0 AND 2147483647")) {
            st.setInt(1,unidades); st.setString(2,codigo); st.setInt(3,unidades);
            if(st.executeUpdate()!=1) throw new IllegalArgumentException("Estoque insuficiente ou quantidade fora do limite.");
        }
    }
    public void configurar(String codigo,int minimo,String temporada) throws SQLException {
        if(minimo<0 || !Arrays.asList("Normal","Alta demanda","Baixa demanda").contains(temporada)) throw new IllegalArgumentException("Configuração inválida.");
        try(PreparedStatement st=con.prepareStatement("UPDATE lotes SET minimo=?, temporada=? WHERE codigo=?")) {
            st.setInt(1,minimo); st.setString(2,temporada); st.setString(3,codigo); st.executeUpdate();
        }
    }
    public void close() throws SQLException { con.close(); }
}
