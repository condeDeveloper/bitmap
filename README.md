# bitmap

Conjuntos de inteiros do zero em Java 21: mapa de bits, vetor ordenado, compressão por
corridas e um híbrido que escolhe bloco a bloco.

O juiz é um **vetor de booleanos** do tamanho do universo. Ele é o juiz certo e não só o
conveniente: não há nele palavra a deslocar, corrida a mesclar, bloco a escolher nem limiar a
comparar. Cada valor possível tem uma casinha, e a interseção é um E lógico posição por
posição. Ele gasta oito vezes mais que o pior método medido, e é por isso mesmo que não erra.

E ele confere o **conjunto inteiro**, não a cardinalidade. Uma interseção que perca um
elemento e ganhe outro tem a cardinalidade certa e está errada, e esse é exatamente o erro
que um deslocamento de bit trocado produz.

São duas medidas, e elas não têm a mesma resposta: **bits por elemento** e **trabalho por
operação**. A representação que ganha numa pode perder na outra.

## O cruzamento está em um elemento a cada 32, e isso não foi escolhido

```
$ mvn exec:java -Dexec.args="memoria"

    um em cada    elementos representacao         bits por elemento      bits totais
          4096          256 mapa de bits                    4096.00          1048576
          4096          256 vetor ordenado                    32.00             8192
          4096          256 por corridas                      64.00            16384
          4096          256 hibrido                           16.00             4096
            32        32768 mapa de bits                      32.00          1048576
            32        32768 vetor ordenado                    32.00          1048576
            32        32768 por corridas                      62.01          2031936
            32        32768 hibrido                           16.00           524288
             2       524288 mapa de bits                       2.00          1048576
             2       524288 vetor ordenado                    32.00         16777216
             2       524288 por corridas                      32.01         16780736
             2       524288 hibrido                            2.00          1048576
```

O vetor ordenado gasta 32 bits por elemento, em qualquer densidade. O mapa de bits gasta o
universo inteiro dividido pelos elementos. Eles empatam quando há um elemento a cada 32, e
não são só as razões que empatam: são **os mesmos 1.048.576 bits totais**. O 32 é o tamanho
de um inteiro.

## Como rodar

```
mvn test
mvn exec:java -Dexec.args="tudo"
```

Os comandos do medidor são `correcao`, `memoria`, `interseccao`, `corridas`, `hibrido` e
`tudo`.

## O que as medidas me corrigiram

**O mapa de bits cobra o universo, não o dado, e isso aparece inteiro na interseção.**

```
$ mvn exec:java -Dexec.args="interseccao"

  o outro lado representacao                trabalho      resultado
            10 mapa de bits                    32768              6
            10 vetor ordenado                 512793              6
            10 vetor em galope                   290              6
            10 hibrido                            10              6
        524288 mapa de bits                    32768         261660
        524288 vetor ordenado                 786916         261660
        524288 vetor em galope                 786916        261660
        524288 hibrido                          32768         261660
```

Um lado tem sempre meio milhão de elementos. O outro vai de dez a meio milhão, e o mapa de
bits faz **32.768 unidades de trabalho nos quatro casos**: ele lê as 16.384 palavras do
universo duas vezes e não sabe nem olha quantos elementos existem.

O galope faz 290 onde a mescla faz 512.793, mil e setecentas vezes menos.

E o híbrido faz **dez**. Os dez elementos caem em blocos diferentes, cada bloco vira um vetor
de um elemento só, e o caminho de vetor contra mapa consulta o mapa uma vez por elemento. Ele
é o único que cobra exatamente o tamanho da pergunta: 3.276 vezes menos que o mapa de bits e
29 vezes menos que o galope.

**A compressão por corridas dobra o tamanho no primeiro dado em que alguém testaria.**

```
$ mvn exec:java -Dexec.args="corridas"

  entrada                corridas  elementos por corrida    bits por elemento        vezes o vetor
  sorteado                  61527                   1.07                60.08                 1.88
  blocos de 8                7901                   8.29                 7.72                 0.24
  blocos de 64                997                  65.73                 0.97                 0.03
  blocos de 1024               63                1040.25                 0.06                 0.00
  regular de 16             65536                   1.00                64.00                 2.00
```

A coluna do meio é o repositório inteiro em um número. Com um valor a cada 16, **nenhuma
corrida tem mais de um elemento**, e uma corrida de um elemento gasta dois inteiros para
guardar o que o vetor ordenado guarda em um: 64 bits contra 32, exatamente o dobro.

Em dado sorteado é quase a mesma coisa, 1,88 vezes. Uma compressão que, aplicada ao dado
errado, cobra para não comprimir.

E quando há corrida de verdade ela vira a melhor de todas por três ordens de grandeza: 0,06
bits por elemento com blocos de mil. O extremo final é o conjunto cheio, que são **64 bits
no total**, uma corrida só.

**O híbrido perde por 256 vezes num lugar, e eu tinha escrito que ele não perdia em nenhum.**

```
$ mvn exec:java -Dexec.args="hibrido"

  entrada             elementos    blocos mapa   blocos vetor    bits por elemento    vezes o melhor puro
  ralo sorteado            1024              0             16                16.00                   0.50
  denso sorteado         524288             16              0                 2.00                   1.00
  em blocos               16384              0              8                16.00                 256.00
  desigual                65536              4              0                 4.00                   0.25
```

A coluna da direita compara o híbrido com a **melhor** das representações puras naquela
entrada, que é uma régua dura: ela muda de linha para linha e ele tem que chegar perto sem
nunca saber de antemão qual é.

Em três entradas ele ganha ou empata: metade dos bits no ralo, empate no denso, um quarto no
desigual. Na entrada em blocos ele custa **256 vezes** a melhor pura.

E a culpa não é da ideia de escolher por bloco: é de ele só ter **dois containers**. Um
conjunto feito de pedaços contíguos cabe em pouquíssimas corridas, 0,06 bits por elemento, e
o híbrido olha o bloco, vê dois mil elementos, decide "vetor" e paga 16 bits em cada um. A
decisão está certa dentro das opções que ele tem; as opções é que estão faltando uma. As
bibliotecas de verdade têm um terceiro container justamente para isso.

**E a vantagem do híbrido no caso comum não vem da escolha, vem da partida.**

Com o universo partido em pedaços de 65.536, um elemento dentro de um pedaço cabe em **16
bits** em vez de 32. O bloco em vetor já fica pela metade do preço do vetor ordenado comum,
antes de qualquer decisão sobre densidade: é por isso que a coluna dele é 16,00 em cinco
densidades seguidas da tabela da memória.

E dessa mesma conta cai o limiar. Um bloco em mapa custa 65.536 bits fixos, um bloco em vetor
custa 16 por elemento, e eles empatam em 65.536/16 = **4.096**. Esse número não foi escolhido.

## O que não tem aqui

Falta o container de corridas do híbrido, e a tabela mostra o buraco que ele deixa. Faltam
também diferença e complemento, que não acrescentariam pergunta nenhuma: as duas são
interseção e união com um dos lados invertido, e o custo delas é o mesmo.

As operações são sempre entre representações **iguais**. Misturar é um assunto de verdade e
não é este: aqui a pergunta é quanto cada uma custa fazendo o trabalho dela, e uma operação
entre tipos diferentes mediria o custo da conversão.

## Licença

MIT.
