package br.com.conde.bitmap;

import java.util.Arrays;

/**
 * O JUIZ: um vetor de booleanos do tamanho do universo.
 *
 * <p>Ele e o juiz certo e nao so o conveniente. Nao ha nele palavra a deslocar, corrida a
 * mesclar, bloco a escolher nem limiar a comparar: cada valor possivel tem uma casinha, e a
 * intersecao e um E logico posicao por posicao. Ele gasta um byte por valor do universo,
 * que e oito vezes o pior dos metodos medidos, e e por isso mesmo que ele nao erra.
 *
 * <h2>O que ele confere</h2>
 *
 * <p>Nao so a cardinalidade: o CONJUNTO inteiro, elemento por elemento. Uma intersecao que
 * perca um elemento e ganhe outro tem a cardinalidade certa e esta errada, e esse e
 * exatamente o tipo de erro que um deslocamento de bit trocado produz.
 */
public final class Juiz {

    private Juiz() {
    }

    public record Veredito(int perguntas, int divergencias, String falha) {

        public boolean passou() {
            return divergencias == 0;
        }
    }

    /** O conjunto como vetor de booleanos, que e a forma que nao tem o que errar. */
    public static boolean[] comoCasinhas(int universo, int[] valores) {
        boolean[] casinhas = new boolean[universo];

        for (int valor : valores) casinhas[valor] = true;

        return casinhas;
    }

    public static int[] deCasinhas(boolean[] casinhas) {
        int quantos = 0;

        for (boolean tem : casinhas) if (tem) quantos++;

        int[] valores = new int[quantos];
        int escritos = 0;

        for (int valor = 0; valor < casinhas.length; valor++)
            if (casinhas[valor]) valores[escritos++] = valor;

        return valores;
    }

    /**
     * Confere uma representacao em pertinencia, intersecao, uniao e cardinalidade.
     *
     * <p>A pertinencia e perguntada para TODO valor do universo, e nao so para os que estao
     * guardados. Um deslocamento errado responde certo sobre quase todo valor presente e
     * inventa valores ausentes, e so a varredura completa pega isso.
     */
    public static Veredito julgar(String nome, int universo, int[] a, int[] b,
                                  java.util.function.BiFunction<Integer, int[], Conjunto> fabrica) {

        boolean[] casinhasA = comoCasinhas(universo, a);
        boolean[] casinhasB = comoCasinhas(universo, b);

        Conjunto conjuntoA = fabrica.apply(universo, a);
        Conjunto conjuntoB = fabrica.apply(universo, b);

        int perguntas = 0;
        int divergencias = 0;
        String falha = null;

        Contador contador = new Contador();

        for (int valor = 0; valor < universo; valor++) {
            perguntas++;

            if (conjuntoA.tem(valor, contador) == casinhasA[valor]) continue;

            divergencias++;

            if (falha == null) falha = "pertinencia do valor " + valor;
        }

        boolean[] daInterseccao = new boolean[universo];
        boolean[] daUniao = new boolean[universo];

        for (int valor = 0; valor < universo; valor++) {
            daInterseccao[valor] = casinhasA[valor] && casinhasB[valor];
            daUniao[valor] = casinhasA[valor] || casinhasB[valor];
        }

        perguntas++;

        int[] meuCorte = conjuntoA.interseccao(conjuntoB, contador).emVetor();

        if (!Arrays.equals(meuCorte, deCasinhas(daInterseccao))) {
            divergencias++;

            if (falha == null)
                falha = "interseccao: " + meuCorte.length + " elementos contra "
                        + deCasinhas(daInterseccao).length;
        }

        perguntas++;

        int[] minhaUniao = conjuntoA.uniao(conjuntoB, contador).emVetor();

        if (!Arrays.equals(minhaUniao, deCasinhas(daUniao))) {
            divergencias++;

            if (falha == null)
                falha = "uniao: " + minhaUniao.length + " elementos contra "
                        + deCasinhas(daUniao).length;
        }

        perguntas++;

        if (conjuntoA.cardinalidade() != deCasinhas(casinhasA).length) {
            divergencias++;

            if (falha == null)
                falha = "cardinalidade " + conjuntoA.cardinalidade() + " contra "
                        + deCasinhas(casinhasA).length;
        }

        perguntas++;

        if (!Arrays.equals(conjuntoA.emVetor(), deCasinhas(casinhasA))) {
            divergencias++;

            if (falha == null) falha = "o conjunto em vetor nao bate";
        }

        return new Veredito(perguntas, divergencias, falha == null ? null : nome + ": " + falha);
    }
}
