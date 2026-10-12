package br.com.conde.bitmap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Os numeros do README, afirmados.
 *
 * <p>Um README e texto: ele nao quebra quando deixa de ser verdade. Cada afirmacao das
 * tabelas esta repetida aqui para quebrar.
 */
class MedidasTest {

    /* As tabelas sao calculadas uma vez so: cada uma monta conjuntos de ate um milhao. */
    private static final List<Medidas.LinhaMemoria> MEMORIA = Medidas.aMemoria();
    private static final List<Medidas.LinhaInterseccao> INTERSECCAO = Medidas.aInterseccao();
    private static final List<Medidas.LinhaCorridas> CORRIDAS = Medidas.asCorridas();
    private static final List<Medidas.LinhaHibrido> HIBRIDO = Medidas.oHibrido();

    // ───────────────────── 1. a correcao ─────────────────────

    @Test
    @DisplayName("nenhuma combinacao diverge do vetor de booleanos")
    void aCorrecaoNaoDiverge() {
        List<Medidas.LinhaCorrecao> linhas = Medidas.aCorrecao();

        assertThat(linhas).hasSize(20);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.divergencias())
                    .as(l.representacao() + " em " + l.entrada()).isZero();

            assertThat(l.perguntas())
                    .as("a pertinencia e perguntada para todo valor do universo")
                    .isEqualTo((1 << 16) + 4);
        });
    }

    // ───────────────────── 2. a memoria ─────────────────────

    /**
     * O CRUZAMENTO PREVISTO, CONFERIDO.
     *
     * <p>O vetor ordenado gasta 32 bits por elemento, sempre. O mapa de bits gasta o
     * universo dividido pelos elementos. Os dois empatam em densidade 1/32, e esse numero
     * nao foi escolhido: ele e o tamanho de um inteiro.
     */
    @Test
    @DisplayName("o mapa de bits e o vetor empatam exatamente em um elemento a cada 32")
    void oCruzamentoEmUmTrintaEDoisAvos() {
        assertThat(memoria(32, "mapa de bits").bitsPorElemento()).isEqualTo(32.0);
        assertThat(memoria(32, "vetor ordenado").bitsPorElemento()).isEqualTo(32.0);

        assertThat(memoria(32, "mapa de bits").bitsTotais())
                .as("e nao e so a razao: sao os mesmos bits totais")
                .isEqualTo(memoria(32, "vetor ordenado").bitsTotais());

        assertThat(memoria(64, "mapa de bits").bitsPorElemento())
                .as("mais ralo que isso e o vetor ganha")
                .isGreaterThan(memoria(64, "vetor ordenado").bitsPorElemento());

        assertThat(memoria(16, "mapa de bits").bitsPorElemento())
                .as("mais denso e o mapa ganha")
                .isLessThan(memoria(16, "vetor ordenado").bitsPorElemento());
    }

    @Test
    @DisplayName("o vetor ordenado gasta 32 bits por elemento em qualquer densidade")
    void oVetorNaoDependeDaDensidade() {
        for (int umEmCada : new int[] {4_096, 1_024, 256, 64, 32, 16, 4, 2, 1})
            assertThat(memoria(umEmCada, "vetor ordenado").bitsPorElemento())
                    .as("um em cada " + umEmCada)
                    .isEqualTo(32.0);
    }

    @Test
    @DisplayName("o mapa de bits ocupa o universo inteiro em qualquer densidade")
    void oMapaNaoDependeDoDado() {
        for (int umEmCada : new int[] {4_096, 1_024, 256, 64, 32, 16, 4, 2, 1})
            assertThat(memoria(umEmCada, "mapa de bits").bitsTotais())
                    .as("um em cada " + umEmCada)
                    .isEqualTo(Medidas.UNIVERSO);

        assertThat(memoria(4_096, "mapa de bits").bitsPorElemento())
                .as("com um elemento a cada 4.096, sao 4.096 bits para guardar cada um")
                .isEqualTo(4_096.0);
    }

    /**
     * O HIBRIDO GASTA 16 BITS E NAO 32, e isso vem da partida em blocos e nao da escolha.
     *
     * <p>Com o universo partido em pedacos de 65.536, um elemento dentro de um pedaco cabe
     * em 16 bits. O bloco em vetor ja fica pela metade do preco do vetor ordenado comum,
     * antes de qualquer decisao sobre densidade.
     */
    @Test
    @DisplayName("o hibrido gasta metade do vetor, so por partir o universo em blocos")
    void oHibridoGanhaNaPartida() {
        for (int umEmCada : new int[] {4_096, 1_024, 256, 64, 32})
            assertThat(memoria(umEmCada, "hibrido").bitsPorElemento())
                    .as("um em cada " + umEmCada)
                    .isEqualTo(16.0);

        assertThat(memoria(2, "hibrido").bitsPorElemento())
                .as("e no denso ele vira mapa de bits e empata com ele")
                .isEqualTo(memoria(2, "mapa de bits").bitsPorElemento());

        assertThat(memoria(1_024, "hibrido").bitsPorElemento())
                .isEqualTo(memoria(1_024, "vetor ordenado").bitsPorElemento() / 2);
    }

    @Test
    @DisplayName("por corridas gasta perto de 64 bits por elemento em dado sorteado")
    void asCorridasExpandemNoSorteado() {
        for (int umEmCada : new int[] {4_096, 1_024, 256, 64, 32})
            assertThat(memoria(umEmCada, "por corridas").bitsPorElemento())
                    .as("um em cada " + umEmCada)
                    .isBetween(60.0, 64.1);

        assertThat(memoria(4_096, "por corridas").bitsPorElemento()).isEqualTo(64.0);

        assertThat(memoria(1, "por corridas").bitsTotais())
                .as("e no outro extremo, o universo inteiro e uma corrida de 64 bits")
                .isEqualTo(64);
    }

    private static Medidas.LinhaMemoria memoria(int umEmCada, String representacao) {
        return MEMORIA.stream()
                .filter(l -> l.umEmCada() == umEmCada && l.representacao().equals(representacao))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 3. a interseccao ─────────────────────

    /**
     * A PROPRIEDADE QUE DECIDE ONDE O MAPA DE BITS SERVE.
     *
     * <p>Ele le as 16.384 palavras do universo duas vezes, tenha o outro lado dez elementos
     * ou meio milhao. O mapa de bits cobra o universo e nao o dado.
     */
    @Test
    @DisplayName("a interseccao do mapa de bits custa o mesmo com 10 ou com meio milhao")
    void oMapaCobraOUniverso() {
        long comDez = interseccao(10, "mapa de bits").trabalho();

        assertThat(comDez).isEqualTo(32_768);

        for (int menor : new int[] {10, 1_000, 100_000, Medidas.UNIVERSO / 2})
            assertThat(interseccao(menor, "mapa de bits").trabalho())
                    .as("com " + menor + " do outro lado")
                    .isEqualTo(comDez);

        assertThat(comDez)
                .as("duas leituras por palavra do universo, e o universo tem 16.384 palavras")
                .isEqualTo(2L * Medidas.UNIVERSO / 64);
    }

    @Test
    @DisplayName("o galope custa 290 onde a mescla custa meio milhao")
    void oGalopeGanhaNoDesequilibrio() {
        assertThat(interseccao(10, "vetor em galope").trabalho()).isEqualTo(290);
        assertThat(interseccao(10, "vetor ordenado").trabalho()).isEqualTo(512_793);

        assertThat(interseccao(10, "vetor ordenado").trabalho()
                / (double) interseccao(10, "vetor em galope").trabalho())
                .isGreaterThan(1_700.0);

        assertThat(interseccao(Medidas.UNIVERSO / 2, "vetor em galope").trabalho())
                .as("e com tamanhos iguais ele cai na mescla, que ali e o certo")
                .isEqualTo(interseccao(Medidas.UNIVERSO / 2, "vetor ordenado").trabalho());
    }

    /**
     * O HIBRIDO E O UNICO QUE COBRA O TAMANHO DA PERGUNTA.
     *
     * <p>Com dez elementos do outro lado ele gasta dez: cada um cai num bloco diferente,
     * vira um vetor de um elemento so, e o caminho de vetor contra mapa consulta o mapa uma
     * vez por elemento.
     */
    @Test
    @DisplayName("o hibrido gasta exatamente dez para intersectar dez elementos")
    void oHibridoCobraOTamanhoDaPergunta() {
        assertThat(interseccao(10, "hibrido").trabalho()).isEqualTo(10);
        assertThat(interseccao(1_000, "hibrido").trabalho()).isEqualTo(1_000);

        assertThat(interseccao(10, "mapa de bits").trabalho()
                / (double) interseccao(10, "hibrido").trabalho())
                .as("3.276 vezes menos que o mapa de bits")
                .isCloseTo(3276.8, within(0.1));

        assertThat(interseccao(10, "vetor em galope").trabalho()
                / (double) interseccao(10, "hibrido").trabalho())
                .as("e 29 vezes menos que o galope")
                .isCloseTo(29.0, within(0.1));

        assertThat(interseccao(Medidas.UNIVERSO / 2, "hibrido").trabalho())
                .as("e quando os dois sao densos ele vira o mapa de bits e empata")
                .isEqualTo(interseccao(Medidas.UNIVERSO / 2, "mapa de bits").trabalho());
    }

    @Test
    @DisplayName("todas as representacoes acham o mesmo resultado em toda interseccao")
    void todasAchamOMesmo() {
        for (int menor : new int[] {10, 1_000, 100_000, Medidas.UNIVERSO / 2}) {
            List<Integer> cardinalidades = INTERSECCAO.stream()
                    .filter(l -> l.menor() == menor)
                    .map(Medidas.LinhaInterseccao::resultado)
                    .distinct()
                    .toList();

            assertThat(cardinalidades).as("com " + menor + " do outro lado").hasSize(1);
        }
    }

    private static Medidas.LinhaInterseccao interseccao(int menor, String representacao) {
        return INTERSECCAO.stream()
                .filter(l -> l.menor() == menor && l.representacao().equals(representacao))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 4. as corridas ─────────────────────

    @Test
    @DisplayName("a compressao por corridas dobra o tamanho quando nao ha corrida nenhuma")
    void aCompressaoQueExpande() {
        assertThat(corridas("regular de 16").elementosPorCorrida()).isEqualTo(1.0);
        assertThat(corridas("regular de 16").bitsPorElemento()).isEqualTo(64.0);
        assertThat(corridas("regular de 16").vezesOVetor()).isEqualTo(2.0);

        assertThat(corridas("sorteado").elementosPorCorrida()).isCloseTo(1.07, within(0.01));
        assertThat(corridas("sorteado").vezesOVetor()).isCloseTo(1.88, within(0.01));

        assertThat(corridas("sorteado").vezesOVetor())
                .as("em dado sorteado ela esta cobrando para nao comprimir")
                .isGreaterThan(1.0);
    }

    @Test
    @DisplayName("com corrida de verdade ela vira a melhor de todas")
    void aCompressaoQueComprime() {
        assertThat(corridas("blocos de 8").elementosPorCorrida()).isCloseTo(8.29, within(0.01));
        assertThat(corridas("blocos de 8").vezesOVetor()).isCloseTo(0.24, within(0.01));

        assertThat(corridas("blocos de 1024").elementosPorCorrida())
                .isCloseTo(1040.25, within(0.01));

        assertThat(corridas("blocos de 1024").bitsPorElemento()).isCloseTo(0.06, within(0.01));

        assertThat(corridas("blocos de 64").vezesOVetor())
                .as("a virada esta entre uma e oito: com 8,29 por corrida ela ja ganha 4 vezes")
                .isLessThan(0.05);
    }

    private static Medidas.LinhaCorridas corridas(String entrada) {
        return CORRIDAS.stream()
                .filter(l -> l.entrada().equals(entrada))
                .findFirst()
                .orElseThrow();
    }

    // ───────────────────── 5. o hibrido ─────────────────────

    /**
     * A CORRECAO QUE A REGUA DURA PEGOU.
     *
     * <p>Eu escrevi que escolher por bloco faz o hibrido nao perder feio em lugar nenhum.
     * Na entrada em blocos ele custa 256 vezes a melhor pura, porque ele so tem dois
     * containers e nenhum deles e de corridas.
     */
    @Test
    @DisplayName("o hibrido perde por 256 vezes onde falta um container de corridas")
    void oHibridoPerdeFeioNumLugar() {
        assertThat(hibrido("em blocos").vezesOMelhorPuro()).isCloseTo(256.0, within(0.5));
        assertThat(hibrido("em blocos").bitsPorElemento()).isEqualTo(16.0);
        assertThat(hibrido("em blocos").blocosEmVetor()).isEqualTo(8);
        assertThat(hibrido("em blocos").blocosEmMapa()).isZero();
    }

    @Test
    @DisplayName("nas outras tres entradas ele ganha ou empata com a melhor pura")
    void oHibridoGanhaNasOutras() {
        assertThat(hibrido("ralo sorteado").vezesOMelhorPuro()).isEqualTo(0.5);
        assertThat(hibrido("denso sorteado").vezesOMelhorPuro()).isEqualTo(1.0);
        assertThat(hibrido("desigual").vezesOMelhorPuro()).isEqualTo(0.25);

        assertThat(hibrido("ralo sorteado").blocosEmMapa())
                .as("no ralo, nenhum bloco passa do limiar")
                .isZero();

        assertThat(hibrido("denso sorteado").blocosEmVetor())
                .as("no denso, todos passam")
                .isZero();

        assertThat(hibrido("desigual").blocosEmMapa())
                .as("no desigual ele usa quatro blocos e deixa doze vazios")
                .isEqualTo(4);
    }

    @Test
    @DisplayName("o limiar de 4.096 cai da conta, e nao de uma escolha")
    void oLimiarCaiDaConta() {
        assertThat(Hibrido.LIMIAR)
                .as("65.536 bits do bloco em mapa, divididos por 16 bits por valor em vetor")
                .isEqualTo(Hibrido.TAMANHO_DO_BLOCO / 16);

        assertThat(Hibrido.LIMIAR).isEqualTo(4_096);
    }

    private static Medidas.LinhaHibrido hibrido(String entrada) {
        return HIBRIDO.stream()
                .filter(l -> l.entrada().equals(entrada))
                .findFirst()
                .orElseThrow();
    }
}
