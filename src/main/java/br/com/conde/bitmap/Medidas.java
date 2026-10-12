package br.com.conde.bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * As MEDIDAS, em bits por elemento e em trabalho por operacao. Nunca segundos.
 *
 * <p>Sao duas perguntas e elas nao tem a mesma resposta. Quanto ESPACO a representacao
 * ocupa e quanto TRABALHO uma operacao custa sao coisas diferentes, e a representacao que
 * ganha numa pode perder na outra.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros.
 */
public final class Medidas {

    private Medidas() {
    }

    /**
     * O universo das medidas: pouco mais de um milhao.
     *
     * <p>Multiplo de 65.536 porque o hibrido parte o universo em blocos desse tamanho, e
     * grande o bastante para a densidade varrer quatro ordens de grandeza sem que o
     * conjunto fique pequeno demais para medir.
     */
    public static final int UNIVERSO = 1 << 20;

    public static final long SEMENTE = 20_251_012L;

    // ───────────────────── 1. a correcao ─────────────────────

    public record LinhaCorrecao(String representacao, String entrada, int perguntas,
                                int divergencias) {
    }

    /** TODA REPRESENTACAO RESPONDE O MESMO QUE O VETOR DE BOOLEANOS, em toda entrada. */
    public static List<LinhaCorrecao> aCorrecao() {
        List<LinhaCorrecao> linhas = new ArrayList<>();
        int universo = 1 << 16;
        int quantos = 4_000;

        for (var entrada : Conjuntos.todos(universo, quantos, SEMENTE).entrySet()) {
            int[] outro = Conjuntos.sorteado(universo, quantos, SEMENTE + 50);

            for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
                Juiz.Veredito veredito = Juiz.julgar(representacao.getKey(), universo,
                        entrada.getValue(), outro, representacao.getValue());

                linhas.add(new LinhaCorrecao(representacao.getKey(), entrada.getKey(),
                        veredito.perguntas(), veredito.divergencias()));
            }
        }

        return linhas;
    }

    // ───────────────────── 2. a memoria ─────────────────────

    public record LinhaMemoria(int umEmCada, int elementos, String representacao,
                               double bitsPorElemento, long bitsTotais) {
    }

    /**
     * QUANTOS BITS POR ELEMENTO, por densidade, com valores sorteados.
     *
     * <p>E a tabela central do repositorio, e ela tem um cruzamento previsto. O vetor
     * ordenado gasta 32 bits por elemento, sempre. O mapa de bits gasta o universo inteiro
     * dividido pela quantidade de elementos. Os dois empatam quando ha um elemento a cada
     * 32 do universo, e esse numero nao foi escolhido: ele e o tamanho de um inteiro.
     *
     * <p>A linha das corridas e a surpresa: com valores sorteados ela gasta 64 bits por
     * elemento ate densidades bem altas, porque cada corrida tem um elemento so e custa
     * dois inteiros. Uma compressao que DOBRA o tamanho.
     */
    public static List<LinhaMemoria> aMemoria() {
        List<LinhaMemoria> linhas = new ArrayList<>();

        for (int umEmCada : new int[] {4_096, 1_024, 256, 64, 32, 16, 4, 2, 1}) {
            int elementos = UNIVERSO / umEmCada;
            int[] valores = Conjuntos.sorteado(UNIVERSO, elementos, SEMENTE);

            for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
                if (representacao.getKey().equals("vetor em galope")) continue;

                Conjunto conjunto = representacao.getValue().apply(UNIVERSO, valores);

                linhas.add(new LinhaMemoria(umEmCada, elementos, representacao.getKey(),
                        conjunto.bitsPorElemento(), conjunto.bitsOcupados()));
            }
        }

        return linhas;
    }

    // ───────────────────── 3. a interseccao ─────────────────────

    public record LinhaInterseccao(int menor, String representacao, long trabalho,
                                   int resultado) {
    }

    /**
     * O CUSTO DE UMA INTERSECCAO entre um conjunto grande e um pequeno.
     *
     * <p>O conjunto grande tem sempre metade do universo. O pequeno vai de dez elementos a
     * meio milhao, e a pergunta e quanto o tamanho dele muda a conta.
     *
     * <p>Para o mapa de bits a resposta e: nada. Ele le as 16.384 palavras do universo duas
     * vezes, tenha o outro lado dez elementos ou quinhentos mil. O MAPA DE BITS COBRA O
     * UNIVERSO E NAO O DADO, e essa e a propriedade que decide onde ele serve.
     *
     * <p>Para o vetor em mescla a conta e a soma dos dois tamanhos, que com um lado grande e
     * quase o tamanho do grande. Para o vetor em galope e o menor vezes o logaritmo da
     * razao: 290 unidades de trabalho contra as 32.768 do mapa de bits, 113 vezes menos.
     *
     * <p>E o hibrido faz melhor que os dois: com dez elementos do outro lado ele gasta DEZ.
     * Os dez caem em blocos diferentes, cada bloco vira um vetor de um elemento so, e o
     * caminho de vetor contra mapa consulta o mapa uma vez por elemento. Ele e o unico que
     * cobra exatamente o tamanho da pergunta.
     */
    public static List<LinhaInterseccao> aInterseccao() {
        List<LinhaInterseccao> linhas = new ArrayList<>();
        int[] grande = Conjuntos.sorteado(UNIVERSO, UNIVERSO / 2, SEMENTE);

        for (int menor : new int[] {10, 1_000, 100_000, UNIVERSO / 2}) {
            int[] pequeno = Conjuntos.sorteado(UNIVERSO, menor, SEMENTE + 9);

            for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
                Contador contador = new Contador();

                Conjunto a = representacao.getValue().apply(UNIVERSO, grande);
                Conjunto b = representacao.getValue().apply(UNIVERSO, pequeno);

                Conjunto corte = a.interseccao(b, contador);

                linhas.add(new LinhaInterseccao(menor, representacao.getKey(),
                        contador.trabalho(), corte.cardinalidade()));
            }
        }

        return linhas;
    }

    // ───────────────────── 4. as corridas ─────────────────────

    public record LinhaCorridas(String entrada, int corridas, double elementosPorCorrida,
                                double bitsPorElemento, double vezesOVetor) {
    }

    /**
     * QUANDO A COMPRESSAO POR CORRIDAS AUMENTA O TAMANHO.
     *
     * <p>A coluna do meio e quantos elementos a corrida media cobre, e ela e o repositorio
     * inteiro em um numero. Enquanto ela for maior que dois, as corridas comprimem; abaixo
     * disso elas expandem, porque uma corrida de um elemento gasta 64 bits para guardar um
     * numero que o vetor ordenado guarda em 32.
     *
     * <p>A coluna da direita e quantas vezes o vetor ordenado as corridas custam. Acima de
     * 1,0 a compressao esta cobrando para nao comprimir.
     */
    public static List<LinhaCorridas> asCorridas() {
        List<LinhaCorridas> linhas = new ArrayList<>();
        int quantos = UNIVERSO / 16;

        var entradas = new java.util.LinkedHashMap<String, int[]>();

        entradas.put("sorteado", Conjuntos.sorteado(UNIVERSO, quantos, SEMENTE));
        entradas.put("blocos de 8", Conjuntos.emBlocos(UNIVERSO, quantos, 8, SEMENTE + 1));
        entradas.put("blocos de 64", Conjuntos.emBlocos(UNIVERSO, quantos, 64, SEMENTE + 2));
        entradas.put("blocos de 1024", Conjuntos.emBlocos(UNIVERSO, quantos, 1_024, SEMENTE + 3));
        entradas.put("regular de 16", Conjuntos.regular(UNIVERSO, 16));

        for (var entrada : entradas.entrySet()) {
            PorCorridas corridas = new PorCorridas(UNIVERSO, entrada.getValue());
            VetorOrdenado vetor = new VetorOrdenado(UNIVERSO, entrada.getValue(), false);

            linhas.add(new LinhaCorridas(entrada.getKey(), corridas.corridas(),
                    corridas.elementosPorCorrida(), corridas.bitsPorElemento(),
                    corridas.bitsPorElemento() / vetor.bitsPorElemento()));
        }

        return linhas;
    }

    // ───────────────────── 5. o hibrido ─────────────────────

    public record LinhaHibrido(String entrada, int elementos, int blocosEmMapa,
                               int blocosEmVetor, double bitsPorElemento,
                               double vezesOMelhorPuro) {
    }

    /**
     * O HIBRIDO ESCOLHE POR BLOCO.
     *
     * <p>As colunas do meio dizem quantos blocos ele resolveu guardar de cada jeito, e a da
     * direita compara ele com a MELHOR das representacoes puras naquela entrada, que e uma
     * regua dura: ela muda de linha para linha conforme a entrada, e o hibrido tem que
     * chegar perto dela sem nunca saber de antemao qual e.
     *
     * <h2>E essa regua pegou ele</h2>
     *
     * <p>Eu escrevi que escolher por bloco faz ele nao perder feio em lugar nenhum. Na
     * entrada em blocos ele custa 256 VEZES a melhor pura, e a culpa nao e da ideia de
     * escolher: e de ele so ter duas opcoes para escolher.
     *
     * <p>Um conjunto feito de pedacos contiguos cabe em pouquissimas corridas, 0,06 bits por
     * elemento, e o hibrido nao tem container de corridas: ele olha o bloco, ve 2.048
     * elementos, decide "vetor" e paga 16 bits em cada um. A decisao esta certa dentro das
     * opcoes que ele tem, e as opcoes e que estao faltando uma.
     *
     * <p>Nas outras tres entradas ele ganha: metade dos bits da melhor pura na rala, empate
     * na densa, e um quarto na desigual.
     */
    public static List<LinhaHibrido> oHibrido() {
        List<LinhaHibrido> linhas = new ArrayList<>();

        var entradas = new java.util.LinkedHashMap<String, int[]>();

        entradas.put("ralo sorteado", Conjuntos.sorteado(UNIVERSO, UNIVERSO / 1_024, SEMENTE));
        entradas.put("denso sorteado", Conjuntos.sorteado(UNIVERSO, UNIVERSO / 2, SEMENTE + 1));
        entradas.put("em blocos", Conjuntos.emBlocos(UNIVERSO, UNIVERSO / 64, 1_024, SEMENTE + 2));
        entradas.put("desigual", Conjuntos.desigual(UNIVERSO, UNIVERSO / 16, SEMENTE + 3));

        for (var entrada : entradas.entrySet()) {
            Hibrido hibrido = new Hibrido(UNIVERSO, entrada.getValue());
            double melhorPuro = Double.MAX_VALUE;

            for (var representacao : Conjuntos.asRepresentacoes().entrySet()) {
                if (representacao.getKey().equals("hibrido")) continue;
                if (representacao.getKey().equals("vetor em galope")) continue;

                melhorPuro = Math.min(melhorPuro,
                        representacao.getValue().apply(UNIVERSO, entrada.getValue())
                                .bitsPorElemento());
            }

            linhas.add(new LinhaHibrido(entrada.getKey(), hibrido.cardinalidade(),
                    hibrido.blocosEmMapa(), hibrido.blocosEmVetor(),
                    hibrido.bitsPorElemento(), hibrido.bitsPorElemento() / melhorPuro));
        }

        return linhas;
    }
}
