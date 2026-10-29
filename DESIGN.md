# Interface 2.0 — cartas na mesa

Aplicativo Android offline para decisões do dia a dia, com nomes e fotos próprios.
A ação principal é juntar opções e revelar uma escolha de forma lúdica.

## Direção e tokens

- Névoa `#F1F5FB`: fundo; branco `#FFFFFF`: ações e carta revelada.
- Cobalto `#254EDB`: carta principal, botões e foco.
- Tinta `#172D4D`: texto; ardósia `#586A83`: texto secundário.
- Azul claro `#DFE8FF` e pêssego `#FFD9BA`: cartas de apoio.
- Títulos em sans-serif-condensed bold; corpo em sans-serif; controles em sans-serif-medium.
- Escala: 44 / 34 / 28 / 17 / 16 / 14 / 12 sp. Controles com pelo menos 48 dp.

## Composição

Tudo alinhado à esquerda, exceto o palco de cartas e a revelação.
A lista é contínua, com divisórias; o botão principal fica acessível no rodapé.

```text
tiiv / roulette                 + Item
Uma carta.
Uma escolha.
          [ cartas ]
       opções disponíveis
Sem repetir                    chave
Na mesa                       n itens
foto  nome                    opções
foto  nome                    opções
------------------------------------
         Sortear uma carta
```

## Revisão antes da implementação

A primeira ideia de um painel de estatísticas foi descartada: o produto precisa
mostrar a surpresa, não métricas. A pilha de cartas é o único gesto visual forte.
A lista não repete contêineres decorativos e as fotos continuam sendo opcionais.

## Movimento

Um corte lateral alterna as cartas e desacelera. A última carta vira no eixo
horizontal da tela, mostrando foto e nome. Duração nominal de 2,4 segundos,
respeitando a escala de animação do sistema; escala zero revela imediatamente.
O resultado é escolhido com SecureRandom e persistido antes da animação.
A animação não interfere nas probabilidades e usa apenas opções elegíveis.

## Verificação visual

A interface usa ScrollView, texto em sp, insets das barras do sistema, formulários
roláveis e botão principal fora da lista. A revelação anuncia o nome por
acessibilidade. A carta no palco abrevia nomes longos; lista e resultado mostram
o nome completo. Testes em aparelho/emulador continuam necessários para validar
layout, fonte ampliada, galeria e movimento reais.
