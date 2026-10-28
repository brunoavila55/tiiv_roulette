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
