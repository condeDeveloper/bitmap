package br.com.conde.bitmap;

import java.util.Arrays;

/**
 * POR CORRIDAS: guarda intervalos em vez de elementos.
 *
 * <p>O conjunto {@code 10..19, 50, 80..89} vira tres corridas em vez de vinte e um numeros.
 * Quando os elementos vem em blocos, isso e a melhor representacao que existe: o tamanho
 * deixa de depender de quantos elementos ha e passa a depender de quantos BLOCOS ha.
 *
 * <p>E ela esta aqui principalmente pelo caso em que da errado. Numa nuvem sorteada, quase
 * toda corrida tem UM elemento, e uma corrida de um elemento ocupa dois inteiros: 64 bits
 * para guardar um numero que num vetor ordenado ocuparia 32.
 *
 * <p>Ou seja, esta e uma compressao que, aplicada ao dado errado, dobra o tamanho. Nao e
 * uma nota de rodape: e o comportamento dela em dado sorteado, que e o primeiro dado em que
 * qualquer um testaria.
 */
public final class PorCorridas implements Conjunto {

    private final int[] inicios;
    private final int[] fins;
    private final int universo;
    private final int cardinalidade;

    public PorCorridas(int universo, int[] valores) {
        this.universo = universo;

        int[] ordenados = valores.clone();

        Arrays.sort(ordenados);

        int[] comecos = new int[ordenados.length + 1];
        int[] terminos = new int[ordenados.length + 1];
        int quantas = 0;
        int elementos = 0;

        for (int i = 0; i < ordenados.length; i++) {
            if (ordenados[i] < 0 || ordenados[i] >= universo)
                throw new IllegalArgumentException(
                        "valor " + ordenados[i] + " fora do universo " + universo);

            if (i > 0 && ordenados[i] == ordenados[i - 1]) continue;

            if (quantas > 0 && ordenados[i] == terminos[quantas - 1] + 1) {
                terminos[quantas - 1] = ordenados[i];
            } else {
                comecos[quantas] = ordenados[i];
                terminos[quantas] = ordenados[i];
                quantas++;
            }

            elementos++;
        }

        this.inicios = Arrays.copyOf(comecos, quantas);
        this.fins = Arrays.copyOf(terminos, quantas);
        this.cardinalidade = elementos;
    }

    private PorCorridas(int universo, int[] inicios, int[] fins) {
        this.universo = universo;
        this.inicios = inicios;
        this.fins = fins;

        int elementos = 0;

        for (int i = 0; i < inicios.length; i++) elementos += fins[i] - inicios[i] + 1;

        this.cardinalidade = elementos;
    }

    @Override
    public String nome() {
        return "por corridas";
    }

    public int corridas() {
        return inicios.length;
    }

    /** Quantos elementos a corrida media cobre. Um quer dizer que a compressao nao comprime. */
    public double elementosPorCorrida() {
        return inicios.length == 0 ? 0 : (double) cardinalidade / inicios.length;
    }

    @Override
    public boolean tem(int valor, Contador contador) {
        int baixo = 0;
        int alto = inicios.length - 1;

        while (baixo <= alto) {
            int meio = (baixo + alto) >>> 1;

            contador.compara();

            if (valor < inicios[meio]) alto = meio - 1;
            else if (valor > fins[meio]) baixo = meio + 1;
            else return true;
        }

        return false;
    }

    @Override
    public Conjunto interseccao(Conjunto outro, Contador contador) {
        PorCorridas dele = exigirCorridas(outro);

        contador.operacao();

        int[] comecos = new int[inicios.length + dele.inicios.length];
        int[] terminos = new int[comecos.length];
        int quantas = 0;
        int i = 0;
        int j = 0;

        while (i < inicios.length && j < dele.inicios.length) {
            contador.compara();

            int comeco = Math.max(inicios[i], dele.inicios[j]);
            int termino = Math.min(fins[i], dele.fins[j]);

            if (comeco <= termino) {
                comecos[quantas] = comeco;
                terminos[quantas] = termino;
                quantas++;
            }

            /*
             * AVANCA QUEM TERMINA ANTES, e nunca os dois juntos. Uma corrida longa de um
             * lado pode cruzar varias do outro, e avancar os dois perderia todas menos a
             * primeira.
             */
            if (fins[i] < dele.fins[j]) i++;
            else j++;
        }

        return new PorCorridas(universo, Arrays.copyOf(comecos, quantas),
                Arrays.copyOf(terminos, quantas));
    }

    @Override
    public Conjunto uniao(Conjunto outro, Contador contador) {
        PorCorridas dele = exigirCorridas(outro);

        contador.operacao();

        int[] comecos = new int[inicios.length + dele.inicios.length];
        int[] terminos = new int[comecos.length];
        int quantas = 0;
        int i = 0;
        int j = 0;

        while (i < inicios.length || j < dele.inicios.length) {
            boolean meu = j >= dele.inicios.length
                    || (i < inicios.length && inicios[i] <= dele.inicios[j]);

            if (i < inicios.length && j < dele.inicios.length) contador.compara();

            int comeco = meu ? inicios[i] : dele.inicios[j];
            int termino = meu ? fins[i] : dele.fins[j];

            if (meu) i++;
            else j++;

            if (quantas > 0 && comeco <= terminos[quantas - 1] + 1) {
                terminos[quantas - 1] = Math.max(terminos[quantas - 1], termino);
                continue;
            }

            comecos[quantas] = comeco;
            terminos[quantas] = termino;
            quantas++;
        }

        return new PorCorridas(universo, Arrays.copyOf(comecos, quantas),
                Arrays.copyOf(terminos, quantas));
    }

    private PorCorridas exigirCorridas(Conjunto outro) {
        if (!(outro instanceof PorCorridas dele))
            throw new IllegalArgumentException("so opera com outro por corridas");

        if (dele.universo != universo) throw new IllegalArgumentException("universos diferentes");

        return dele;
    }

    @Override
    public int[] emVetor() {
        int[] saida = new int[cardinalidade];
        int quantos = 0;

        for (int i = 0; i < inicios.length; i++)
            for (int valor = inicios[i]; valor <= fins[i]; valor++) saida[quantos++] = valor;

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

    /** Dois inteiros de 32 bits por corrida: o comeco e o fim. */
    @Override
    public long bitsOcupados() {
        return (long) inicios.length * 64;
    }
}
