package br.com.conde.bitmap;

/**
 * O MAPA DE BITS: um bit por valor possivel do universo.
 *
 * <p>Ele e a representacao mais simples que existe e e a unica com uma propriedade estranha:
 * o custo dela nao depende do DADO, depende do UNIVERSO. Um conjunto de dez elementos num
 * universo de um milhao ocupa as mesmas 16.384 palavras que um conjunto de um milhao, e a
 * intersecao dos dois le as mesmas 16.384.
 *
 * <p>Isso e otimo quando o conjunto e denso e e desastroso quando ele e ralo, e a tabela da
 * memoria mostra exatamente onde fica a virada.
 *
 * <p>E a intersecao dele nao tem desvio nenhum: e um E logico palavra por palavra, 64
 * elementos por instrucao, do comeco ao fim. Esse e o motivo de ele continuar ganhando em
 * SEGUNDOS bem depois de ter perdido em palavras lidas, e e tambem o motivo de este
 * repositorio nao medir segundos: a comparacao ficaria sobre o processador e nao sobre a
 * estrutura.
 */
public final class MapaDeBits implements Conjunto {

    private final long[] palavras;
    private final int universo;
    private final int cardinalidade;

    public MapaDeBits(int universo, int[] valores) {
        this.universo = universo;
        this.palavras = new long[(universo + 63) / 64];

        int quantos = 0;

        for (int valor : valores) {
            exigirNoUniverso(valor);

            int indice = valor >>> 6;
            long bit = 1L << valor;

            if ((palavras[indice] & bit) == 0) quantos++;

            palavras[indice] |= bit;
        }

        this.cardinalidade = quantos;
    }

    private MapaDeBits(int universo, long[] palavras) {
        this.universo = universo;
        this.palavras = palavras;

        int quantos = 0;

        for (long palavra : palavras) quantos += Long.bitCount(palavra);

        this.cardinalidade = quantos;
    }

    private void exigirNoUniverso(int valor) {
        if (valor < 0 || valor >= universo)
            throw new IllegalArgumentException("valor " + valor + " fora do universo " + universo);
    }

    @Override
    public String nome() {
        return "mapa de bits";
    }

    @Override
    public boolean tem(int valor, Contador contador) {
        exigirNoUniverso(valor);

        contador.lePalavra();

        /*
         * O DESLOCAMENTO NAO PRECISA DO RESTO. Em Java, deslocar um long por n usa so os
         * seis bits baixos de n, entao 1L << valor ja e o bit certo dentro da palavra sem
         * nenhum "valor % 64" escrito. E a mesma razao de o indice ser valor >>> 6.
         */
        return (palavras[valor >>> 6] & (1L << valor)) != 0;
    }

    @Override
    public Conjunto interseccao(Conjunto outro, Contador contador) {
        return combinar(outro, contador, true);
    }

    @Override
    public Conjunto uniao(Conjunto outro, Contador contador) {
        return combinar(outro, contador, false);
    }

    private Conjunto combinar(Conjunto outro, Contador contador, boolean intersectar) {
        MapaDeBits dele = exigirMapa(outro);

        contador.operacao();

        long[] saida = new long[palavras.length];

        for (int i = 0; i < palavras.length; i++) {
            /*
             * DUAS PALAVRAS LIDAS POR PALAVRA DO UNIVERSO, aconteca o que acontecer com os
             * dados. Nao ha como sair cedo, nao ha como pular: o laco nao sabe nem olha
             * quantos elementos existem.
             */
            contador.lePalavra(2);

            saida[i] = intersectar
                    ? palavras[i] & dele.palavras[i]
                    : palavras[i] | dele.palavras[i];
        }

        return new MapaDeBits(universo, saida);
    }

    private MapaDeBits exigirMapa(Conjunto outro) {
        if (!(outro instanceof MapaDeBits dele))
            throw new IllegalArgumentException("so opera com outro mapa de bits");

        if (dele.universo != universo)
            throw new IllegalArgumentException("universos diferentes");

        return dele;
    }

    @Override
    public int[] emVetor() {
        int[] saida = new int[cardinalidade];
        int quantos = 0;

        for (int i = 0; i < palavras.length; i++) {
            long palavra = palavras[i];

            while (palavra != 0) {
                saida[quantos++] = i * 64 + Long.numberOfTrailingZeros(palavra);
                palavra &= palavra - 1;
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

    @Override
    public long bitsOcupados() {
        return (long) palavras.length * 64;
    }
}
