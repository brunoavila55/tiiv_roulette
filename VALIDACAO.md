# Validação 2.0 — 28/10/2026

## Compilação

- Migração completa das três classes de produção e da suíte unitária para Kotlin.
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: sucesso.
- Sete testes unitários passaram, sem falhas.
- Android Lint: zero erros; dois avisos de versões mais recentes de Gradle e do runner de testes.
- APK 2.0.0 / versionCode 3 em `dist/Tiiv-Roulette-2.0.0.apk`.
- Assinatura APK verificada com `apksigner verify`.
- Manifesto verificado: mínimo API 28, alvo API 36, sem permissões de internet ou galeria.
- A assinatura de teste é diferente da versão anterior. Uma atualização que preserve
  a instalação existente exige recompilar com a chave original.

## Testes instrumentados

A suíte `ItemStoreTest` usa uma pasta isolada e cobre:

- Leitura do JSON da versão Java, incluindo nomes repetidos, fotos e rodada.
- Falha de dados malformados sem substituir parcialmente a lista existente.
- Persistência da escolha antes da revelação.

A suíte instrumentada foi compilada, mas não executada. O ambiente não tem
`/dev/kvm`; a tentativa de iniciar Android 15/API 35 por emulação de software
não concluiu o primeiro boot em tempo útil. O emulador foi encerrado.
Não foi possível capturar telas nem validar a animação, a galeria e fontes
ampliadas em execução nesta versão. Esses fluxos devem ser conferidos em um
aparelho ou emulador com aceleração antes de distribuir a versão de produção.

A inspeção do código confirma a seleção uniforme antes da animação, o bloqueio
de interações durante o sorteio, a recuperação do resultado salvo após recriação
da Activity e a revelação imediata quando a escala de animações é zero.
Essas verificações estáticas não substituem a validação funcional em Android.

---

## Histórico da versão Java

# Validação — 28/10/2026

## Compilação e verificações

- `./gradlew assembleDebug testDebugUnitTest lintDebug`: concluído com sucesso.
- Cinco testes unitários passaram, sem falhas: elegibilidade sem repetição, repetição permitida, rodada completa/reinício, lista vazia e item único.
- Android Lint: zero erros. Um aviso informa que existe uma versão mais nova do Gradle; a versão utilizada está fixada e é compatível com o plugin Android escolhido.
- Assinatura APK v2 validada com `apksigner verify`.
- Manifesto do APK conferido: Android 9/API 28 mínimo, API 36 de destino, sem permissões de internet ou acesso amplo à galeria.

## Fluxo real no emulador

Testado em Android 15/API 35, imagem Google APIs x86_64, dispositivo Pixel 6:

- Instalação e abertura do APK.
- Nome vazio mantém o formulário aberto e mostra erro.
- Cadastro de dois itens distintos.
- Importação de PNG pelo seletor do Android e cópia JPEG no armazenamento privado.
- Edição de item já cadastrado para adicionar foto.
- Exclusão de item com confirmação.
- Sorteio exibindo o nome e a foto importada no resultado.
- Dois sorteios sem repetição retornando itens diferentes.
- Mensagem de rodada completa quando todos foram sorteados.
- Reinício da rodada liberando os itens novamente.
- Fechamento forçado e reabertura preservando os itens e a rodada.
- Inspeção visual em largura de aproximadamente 375 dp com fonte ampliada em 200%; o conteúdo permanece rolável.

A imagem usada no teste é uma imagem geométrica artificial, sem fotos pessoais. Os cadastros de teste pertencem ao emulador e não são incluídos no APK.

Não foi realizado teste em celular físico. A versão entregue tem assinatura de desenvolvimento e não foi publicada na Play Store.
