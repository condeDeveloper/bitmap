package br.com.conde.bitmap;

import java.util.Arrays;

/**
 * O HIBRIDO: parte o universo em blocos e escolhe a representacao DE CADA BLOCO.
 *
 * <p>E a ideia por tras das bibliotecas de mapa de bits comprimido que os bancos de dados
 * usam de verdade, e ela e feita de duas sacadas, nao uma.
 *
 * <h2>A primeira: escolher por bloco</h2>
 *
 * <p>Um conjunto raramente e uniformemente denso ou uniformemente ralo. Ele tem partes
 * cheias e partes vazias, e escolher UMA representacao para o conjunto inteiro obriga a
 * perder numa das duas. Decidindo bloco a bloco, cada pedaco paga o que ele vale.
 *
 * <h2>A segunda, que e a que eu nao tinha pensado</h2>
 *
 * <p>Com o universo partido em blocos de 65.536, um elemento dentro de um bloco cabe em 16
 * bits em vez de 32. O bloco em vetor ja fica pela METADE do preco do vetor ordenado
 * comum, antes de qualquer escolha.
 *
 * <p>E dai sai o limiar: um bloco em mapa de bits custa 65.536 bits fixos, e um bloco em
 * vetor custa 16 bits por elemento. Eles empatam em 65.536/16, que e 4.096 elementos. Esse
 * numero nao foi escolhido, ele caiu da conta.
 *
 * <h2>O que falta aqui, e a tabela mostra</h2>
 *
 * <p>Sao dois containers, e faltam tres. Num conjunto feito de pedacos contiguos, este
 * hibrido custa 256 VEZES o que a representacao por corridas custa: ele olha o bloco, ve
 * dois mil elementos, decide "vetor" e paga 16 bits em cada um, quando aquele bloco inteiro
 * caberia em duas dezenas de corridas.
 *
 * <p>As bibliotecas de verdade tem um terceiro container justamente para isso. A decisao de
 * escolher por bloco esta certa; o que falta e ter o que escolher.
 */
public final class Hibrido implements Conjunto {

    public static final int TAMANHO_DO_BLOCO = 1 << 16;

    /** Acima disto o bloco vira mapa de bits. 65.536 bits dividido por 16 bits por valor. */
    public static final int LIMIAR = TAMANHO_DO_BLOCO / 16;

    private static final int PALAVRAS_DO_BLOCO = TAMANHO_DO_BLOCO / 64;

    private final int universo;
    private final long[][] mapas;
    private final int[][] vetores;
    private final int cardinalidade;

    public Hibrido(int universo, int[] valores) {
        if (universo % TAMANHO_DO_BLOCO != 0)
            throw new IllegalArgumentException(
                    "o universo precisa ser multiplo de " + TAMANHO_DO_BLOCO);

        this.universo = universo;

        int blocos = universo / TAMANHO_DO_BLOCO;
        int[][] soltos = new int[blocos][];
        int[] quantos = new int[blocos];

        for (int valor : valores) {
            if (valor < 0 || valor >= universo)
                throw new IllegalArgumentException("valor " + valor + " fora do universo");

            int bloco = valor / TAMANHO_DO_BLOCO;

            if (soltos[bloco] == null) soltos[bloco] = new int[16];
            if (quantos[bloco] == soltos[bloco].length)
                soltos[bloco] = Arrays.copyOf(soltos[bloco], soltos[bloco].length * 2);

            soltos[bloco][quantos[bloco]++] = valor % TAMANHO_DO_BLOCO;
        }

        this.mapas = new long[blocos][];
        this.vetores = new int[blocos][];

        int total = 0;

        for (int bloco = 0; bloco < blocos; bloco++) {
            if (soltos[bloco] == null) continue;

            int[] doBloco = Arrays.copyOf(soltos[bloco], quantos[bloco]);

            Arrays.sort(doBloco);

            int limpos = 0;

            for (int i = 0; i < doBloco.length; i++) {
                if (i > 0 && doBloco[i] == doBloco[i - 1]) continue;

                doBloco[limpos++] = doBloco[i];
            }

            total += limpos;

            guardarBloco(bloco, Arrays.copyOf(doBloco, limpos));
        }

        this.cardinalidade = total;
    }

    private Hibrido(int universo, long[][] mapas, int[][] vetores) {
        this.universo = universo;
        this.mapas = mapas;
        this.vetores = vetores;

        int total = 0;

        for (int bloco = 0; bloco < mapas.length; bloco++) {
            if (vetores[bloco] != null) total += vetores[bloco].length;
            else if (mapas[bloco] != null) for (long palavra : mapas[bloco])
                total += Long.bitCount(palavra);
        }

        this.cardinalidade = total;
    }

    /** A escolha. Acima do limiar vira mapa de bits, abaixo fica vetor de 16 bits. */
    private void guardarBloco(int bloco, int[] valores) {
        if (valores.length == 0) return;

        if (valores.length <= LIMIAR) {
            vetores[bloco] = valores;
            return;
        }

        long[] palavras = new long[PALAVRAS_DO_BLOCO];

        for (int valor : valores) palavras[valor >>> 6] |= 1L << valor;

        mapas[bloco] = palavras;
    }

    @Override
    public String nome() {
        return "hibrido";
    }

    /** Quantos blocos viraram mapa de bits. So as tabelas usam. */
    public int blocosEmMapa() {
        int quantos = 0;

        for (long[] mapa : mapas) if (mapa != null) quantos++;

        return quantos;
    }

    public int blocosEmVetor() {
        int quantos = 0;

        for (int[] vetor : vetores) if (vetor != null) quantos++;

        return quantos;
    }

    @Override
    public boolean tem(int valor, Contador contador) {
        int bloco = valor / TAMANHO_DO_BLOCO;
        int dentro = valor % TAMANHO_DO_BLOCO;

        if (mapas[bloco] != null) {
            contador.lePalavra();
            return (mapas[bloco][dentro >>> 6] & (1L << dentro)) != 0;
        }

        if (vetores[bloco] == null) return false;

        int[] vetor = vetores[bloco];
        int baixo = 0;
        int alto = vetor.length - 1;

        while (baixo <= alto) {
            int meio = (baixo + alto) >>> 1;

            contador.compara();

            if (vetor[meio] == dentro) return true;
            if (vetor[meio] < dentro) baixo = meio + 1;
            else alto = meio - 1;
        }

        return false;
    }

    @Override
    public Conjunto interseccao(Conjunto outro, Contador contador) {
        Hibrido dele = exigirHibrido(outro);

        contador.operacao();

        long[][] novosMapas = new long[mapas.length][];
        int[][] novosVetores = new int[mapas.length][];

        for (int bloco = 0; bloco < mapas.length; bloco++) {
            int[] resultado = intersectarBloco(bloco, dele, contador);

            if (resultado == null || resultado.length == 0) continue;

            if (resultado.length <= LIMIAR) {
                novosVetores[bloco] = resultado;
                continue;
            }

            long[] palavras = new long[PALAVRAS_DO_BLOCO];

            for (int valor : resultado) palavras[valor >>> 6] |= 1L << valor;

            novosMapas[bloco] = palavras;
        }

        return new Hibrido(universo, novosMapas, novosVetores);
    }

    /**
     * A intersecao de um bloco, com um caminho para cada par de representacoes.
     *
     * <p>Mapa com mapa e um E logico. Vetor com vetor e uma mescla. E MAPA COM VETOR nao e
     * nenhum dos dois: percorre o vetor e consulta o mapa, que custa o tamanho do vetor e
     * nao o do bloco. Sem esse terceiro caminho, um bloco denso contra um bloco ralo sairia
     * pelo preco do denso, e o hibrido perderia metade da vantagem dele.
     */
    private int[] intersectarBloco(int bloco, Hibrido dele, Contador contador) {
        if (mapas[bloco] != null && dele.mapas[bloco] != null) {
            int[] saida = new int[TAMANHO_DO_BLOCO];
            int quantos = 0;

            for (int i = 0; i < PALAVRAS_DO_BLOCO; i++) {
                contador.lePalavra(2);

                long palavra = mapas[bloco][i] & dele.mapas[bloco][i];

                while (palavra != 0) {
                    saida[quantos++] = i * 64 + Long.numberOfTrailingZeros(palavra);
                    palavra &= palavra - 1;
                }
            }

            return Arrays.copyOf(saida, quantos);
        }

        if (vetores[bloco] != null && dele.vetores[bloco] != null)
            return mesclar(vetores[bloco], dele.vetores[bloco], contador);

        int[] vetor = vetores[bloco] != null ? vetores[bloco] : dele.vetores[bloco];
        long[] mapa = mapas[bloco] != null ? mapas[bloco] : dele.mapas[bloco];

        if (vetor == null || mapa == null) return null;

        int[] saida = new int[vetor.length];
        int quantos = 0;

        for (int valor : vetor) {
            contador.lePalavra();

            if ((mapa[valor >>> 6] & (1L << valor)) != 0) saida[quantos++] = valor;
        }

        return Arrays.copyOf(saida, quantos);
    }

    private static int[] mesclar(int[] a, int[] b, Contador contador) {
        int[] saida = new int[Math.min(a.length, b.length)];
        int quantos = 0;
        int i = 0;
        int j = 0;

        while (i < a.length && j < b.length) {
            contador.compara();

            if (a[i] == b[j]) {
                saida[quantos++] = a[i];
                i++;
                j++;
            } else if (a[i] < b[j]) {
                i++;
            } else {
                j++;
            }
        }

        return Arrays.copyOf(saida, quantos);
    }

    @Override
    public Conjunto uniao(Conjunto outro, Contador contador) {
        Hibrido dele = exigirHibrido(outro);

        contador.operacao();

        long[][] novosMapas = new long[mapas.length][];
        int[][] novosVetores = new int[mapas.length][];

        for (int bloco = 0; bloco < mapas.length; bloco++) {
            long[] palavras = new long[PALAVRAS_DO_BLOCO];
            boolean vazio = true;

            vazio &= !despejar(bloco, this, palavras, contador);
            vazio &= !despejar(bloco, dele, palavras, contador);

            if (vazio) continue;

            int quantos = 0;

            for (long palavra : palavras) quantos += Long.bitCount(palavra);

            if (quantos == 0) continue;

            if (quantos > LIMIAR) {
                novosMapas[bloco] = palavras;
                continue;
            }

            int[] vetor = new int[quantos];
            int escritos = 0;

            for (int i = 0; i < PALAVRAS_DO_BLOCO; i++) {
                long palavra = palavras[i];

                while (palavra != 0) {
                    vetor[escritos++] = i * 64 + Long.numberOfTrailingZeros(palavra);
                    palavra &= palavra - 1;
                }
            }

            novosVetores[bloco] = vetor;
        }

        return new Hibrido(universo, novosMapas, novosVetores);
    }

    /** Joga o bloco de um dos lados num mapa de trabalho. Devolve se havia algo. */
    private static boolean despejar(int bloco, Hibrido dono, long[] destino, Contador contador) {
        if (dono.mapas[bloco] != null) {
            for (int i = 0; i < PALAVRAS_DO_BLOCO; i++) {
                contador.lePalavra();
                destino[i] |= dono.mapas[bloco][i];
            }
            return true;
        }

        if (dono.vetores[bloco] == null) return false;

        for (int valor : dono.vetores[bloco]) {
            contador.lePalavra();
            destino[valor >>> 6] |= 1L << valor;
        }

        return true;
    }

    private Hibrido exigirHibrido(Conjunto outro) {
        if (!(outro instanceof Hibrido dele))
            throw new IllegalArgumentException("so opera com outro hibrido");

        if (dele.universo != universo) throw new IllegalArgumentException("universos diferentes");

        return dele;
    }

    @Override
    public int[] emVetor() {
        int[] saida = new int[cardinalidade];
        int quantos = 0;

        for (int bloco = 0; bloco < mapas.length; bloco++) {
            int base = bloco * TAMANHO_DO_BLOCO;

            if (vetores[bloco] != null) {
                for (int valor : vetores[bloco]) saida[quantos++] = base + valor;
                continue;
            }

            if (mapas[bloco] == null) continue;

            for (int i = 0; i < PALAVRAS_DO_BLOCO; i++) {
                long palavra = mapas[bloco][i];

                while (palavra != 0) {
                    saida[quantos++] = base + i * 64 + Long.numberOfTrailingZeros(palavra);
                    palavra &= palavra - 1;
                }
            }
        }

        return saida;
    }

    @Override
    public int cardinalidade() {
        return cardinalidade;
    }

    @Override
    public int universo() {
        return universo;
    }

    /** Bloco em mapa custa 65.536 bits fixos; bloco em vetor custa 16 bits por elemento. */
    @Override
    public long bitsOcupados() {
        long bits = 0;

        for (int bloco = 0; bloco < mapas.length; bloco++) {
            if (mapas[bloco] != null) bits += TAMANHO_DO_BLOCO;
            else if (vetores[bloco] != null) bits += 16L * vetores[bloco].length;
        }

        return bits;
    }
}
