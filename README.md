# Tiiv Roulette

Sorteador Android simples, em português. Cada item tem um nome e uma foto opcional. O resultado mostra a foto em destaque e o nome escolhido.

## Instalar no celular

[Baixar o APK do Tiiv Roulette](https://github.com/brunoavila55/tiiv_roulette/raw/refs/heads/main/dist/Tiiv-Roulette.apk)

O APK pronto fica em `dist/Tiiv-Roulette.apk`. Transfira para o Android, abra o arquivo e permita a instalação pela origem utilizada quando o sistema solicitar. Requer Android 9 ou superior.

Esta primeira versão usa uma assinatura de desenvolvimento. Para publicar na Play Store ou distribuir versões de produção, configure uma chave de assinatura própria e mantenha essa chave para as atualizações.

## Usar

1. Toque em **Adicionar item**.
2. Escolha uma foto da galeria, preencha o nome e toque em **Salvar item**.
3. Cadastre os demais itens e toque em **Sortear agora**.
4. O resultado exibe o nome e a foto. Toque em **Sortear de novo** para outra escolha.

Use o botão de opções ao lado de um item para editar ou excluir. A opção **Sem repetir** impede que os itens já sorteados saiam de novo até **Reiniciar rodada**. Itens com o mesmo nome participam separadamente, com a mesma chance de qualquer outro item.

O app funciona sem internet e não pede acesso geral à galeria. As fotos escolhidas são copiadas e reduzidas no armazenamento privado do app. A lista e a rodada são salvas automaticamente. Desinstalar o app ou limpar seus dados remove os cadastros. Não há sincronização nem exportação nesta versão.

## Compilar

Projeto nativo Java, sem dependências externas de execução. Use Android Studio ou JDK 17, Android SDK 36 e Build Tools 35.0.0. Configure `ANDROID_HOME` ou o arquivo local `local.properties` com `sdk.dir=...`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

APK gerado: `app/build/outputs/apk/debug/app-debug.apk`.

Os testes unitários verificam a elegibilidade, o sorteio com um único item, a lista vazia e o encerramento/reinício de uma rodada sem repetição. A verificação funcional no emulador está documentada em `VALIDACAO.md`.

Referências técnicas: [compatibilidade do Android Gradle Plugin](https://developer.android.com/build/releases/agp-8-13-0-release-notes) e [seletor de documentos do Android](https://developer.android.com/training/data-storage/shared/documents-files).
