# Tiiv Roulette

Sorteador Android nativo em Kotlin, em português. Cadastre nomes e fotos opcionais.
O sorteio corta uma pilha de cartas, desacelera e vira a carta escolhida para revelar
foto e nome. Não usa uma roleta giratória.

## Instalar a versão 2.0

APK de teste: [Tiiv-Roulette-2.0.0.apk](dist/Tiiv-Roulette-2.0.0.apk).
A chave de teste desta compilação é diferente da versão 1.0.1. O Android não
permite atualizar a instalação antiga com esta assinatura. Para atualizar
preservando os cadastros, recompile com a chave original; desinstalar o app
apaga os dados.
A chave de desenvolvimento desta compilação foi preservada localmente em
`.tools/signing/debug.keystore` (ignorada pelo Git); ela não é uma chave de produção.

## Usar

1. Toque em **+ Item**, preencha o nome e, se quiser, escolha uma foto.
2. Toque em **Salvar item** e adicione as demais opções.
3. Toque em **Sortear uma carta** para ver a revelação.
4. Use **Sortear outra carta** para uma nova escolha ou **Voltar à lista**.

O botão de opções de cada item permite editar e excluir. **Sem repetir** remove os
itens já sorteados das próximas escolhas até **Reiniciar rodada**. Nomes iguais
continuam sendo itens distintos, cada um com a mesma chance.

No Android 13 ou superior, o app abre o seletor de fotos do sistema. Nas versões
anteriores, ou quando esse seletor não estiver disponível, abre a galeria do
aparelho. Não usa o seletor de documentos do Google Drive.

A interface respeita a escala de animações do Android. Com animações desativadas,
o resultado aparece imediatamente. A escolha é salva antes da animação para que
sair do app durante o sorteio não permita escolher outro resultado.

## Dados e compatibilidade

Funciona offline, sem backend e sem acesso geral à galeria. As fotos escolhidas são
copiadas e reduzidas no armazenamento privado do app. A lista e a rodada são salvas
automaticamente. Limpar os dados ou desinstalar o app remove os cadastros.

A versão 2.0 mantém o applicationId, o formato de `items.json` e os nomes dos
arquivos de fotos da versão Java. Atualizações que preservem a assinatura de
instalação mantêm os dados existentes. Não há sincronização nem exportação.

## Compilar

Requer JDK 17, Android SDK 36 e Build Tools 35.0.0. Configure `ANDROID_HOME` ou o
arquivo local `local.properties` com `sdk.dir=...`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Android 9 ou superior.

Stack: Kotlin 2.3.10, Views e Canvas nativos do Android, Android Gradle Plugin
8.13.2 e Gradle 8.13. JUnit 4 verifica elegibilidade, rodadas sem repetição e a
seleção de todos os índices elegíveis. O app usa a biblioteca padrão do Kotlin,
sem bibliotecas externas de interface ou de animação.

O APK antigo em `dist/Tiiv-Roulette.apk` é da versão Java e não representa o
código 2.0. Builds de desenvolvimento só atualizam uma instalação existente
quando usam a mesma chave de assinatura. Para distribuição, configure e preserve
uma chave própria.

A direção visual e o comportamento da animação estão em [DESIGN.md](DESIGN.md).
Veja [VALIDACAO.md](VALIDACAO.md) para o registro das verificações.

Referências: [configuração Kotlin/Gradle](https://kotlinlang.org/docs/gradle-configure-project.html),
[AGP 8.13.2 e Kotlin 2.3](https://developer.android.com/build/releases/agp-8-13-0-release-notes)
e [seletor de fotos](https://developer.android.com/training/data-storage/shared/photopicker).
