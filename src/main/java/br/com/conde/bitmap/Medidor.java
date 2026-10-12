package br.com.conde.bitmap;

import java.util.Locale;

/**
 * O MEDIDOR: imprime as tabelas que o README mostra e que os testes conferem.
 *
 * <p>Toda tabela daqui tem um teste do lado que afirma os mesmos numeros. Texto nao quebra
 * quando deixa de ser verdade; teste quebra.
 */
public final class Medidor {

    private Medidor() {
    }

    public static void main(String[] args) {
        String comando = args.length > 0 ? args[0] : "tudo";

        if (comando.equals("correcao") || comando.equals("tudo")) correcao();
        if (comando.equals("memoria") || comando.equals("tudo")) memoria();
        if (comando.equals("interseccao") || comando.equals("tudo")) interseccao();
        if (comando.equals("corridas") || comando.equals("tudo")) corridas();
        if (comando.equals("hibrido") || comando.equals("tudo")) hibrido();
    }

    private static void correcao() {
        System.out.println();
        System.out.println("TODA REPRESENTACAO RESPONDE O MESMO QUE O VETOR DE BOOLEANOS.");
        System.out.println();

        var linhas = Medidas.aCorrecao();
        long erradas = linhas.stream().filter(l -> l.divergencias() > 0).count();

        System.out.printf("  %-18s %-14s %14s %14s%n",
                "representacao", "entrada", "perguntas", "divergencias");

        for (var l : linhas)
            System.out.printf("  %-18s %-14s %14d %14d%n",
                    l.representacao(), l.entrada(), l.perguntas(), l.divergencias());

        System.out.println();
        System.out.printf("  %d combinacoes, %d com divergencia.%n", linhas.size(), erradas);
        System.out.println();
    }

    private static void memoria() {
        System.out.println();
        System.out.println("QUANTOS BITS POR ELEMENTO, universo de " + Medidas.UNIVERSO + ".");
        System.out.println();
        System.out.printf("  %12s %12s %-18s %20s %16s%n",
                "um em cada", "elementos", "representacao", "bits por elemento", "bits totais");

        for (var l : Medidas.aMemoria())
            System.out.printf(Locale.ROOT, "  %12d %12d %-18s %20.2f %16d%n",
                    l.umEmCada(), l.elementos(), l.representacao(), l.bitsPorElemento(),
                    l.bitsTotais());

        System.out.println();
    }

    private static void interseccao() {
        System.out.println();
        System.out.println("O CUSTO DE UMA INTERSECCAO com " + (Medidas.UNIVERSO / 2)
                + " de um lado.");
        System.out.println();
        System.out.printf("  %12s %-18s %18s %14s%n",
                "o outro lado", "representacao", "trabalho", "resultado");

        for (var l : Medidas.aInterseccao())
            System.out.printf(Locale.ROOT, "  %12d %-18s %18d %14d%n",
                    l.menor(), l.representacao(), l.trabalho(), l.resultado());

        System.out.println();
    }

    private static void corridas() {
        System.out.println();
        System.out.println("QUANDO A COMPRESSAO POR CORRIDAS AUMENTA O TAMANHO.");
        System.out.println();
        System.out.printf("  %-16s %14s %22s %20s %20s%n",
                "entrada", "corridas", "elementos por corrida", "bits por elemento",
                "vezes o vetor");

        for (var l : Medidas.asCorridas())
            System.out.printf(Locale.ROOT, "  %-16s %14d %22.2f %20.2f %20.2f%n",
                    l.entrada(), l.corridas(), l.elementosPorCorrida(), l.bitsPorElemento(),
                    l.vezesOVetor());

        System.out.println();
    }

    private static void hibrido() {
        System.out.println();
        System.out.println("O HIBRIDO ESCOLHE POR BLOCO, blocos de " + Hibrido.TAMANHO_DO_BLOCO
                + ", limiar de " + Hibrido.LIMIAR + ".");
        System.out.println();
        System.out.printf("  %-16s %12s %14s %14s %20s %22s%n",
                "entrada", "elementos", "blocos mapa", "blocos vetor", "bits por elemento",
                "vezes o melhor puro");

        for (var l : Medidas.oHibrido())
            System.out.printf(Locale.ROOT, "  %-16s %12d %14d %14d %20.2f %22.2f%n",
                    l.entrada(), l.elementos(), l.blocosEmMapa(), l.blocosEmVetor(),
                    l.bitsPorElemento(), l.vezesOMelhorPuro());

        System.out.println();
    }
}
